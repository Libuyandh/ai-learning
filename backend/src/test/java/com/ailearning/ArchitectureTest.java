package com.ailearning;

import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
/**
 * Architecture Rules
 * 分层访问规则：controller 只能被外部入口调用，不允许被业务层/基础设施层访问
 * service 只能被 controller/service 访问
 * mapper 只能被 service/mapper 访问
 * ai 只能被 service/ai 访问
 * search 只能被 ai/search 访问
 * rag 只能被 service/ai/rag 访问
 *
 * 反向依赖禁止：domain、dto 不允许依赖 controller/service/mapper/ai/search/config
 * mapper 不允许依赖 controller/service/ai/search
 * service 不允许依赖 controller
 *
 * 命名与注解规则：@RestController 必须在 ..controller.. 且类名以 Controller 结尾
 * @Service 必须在 ..service.. 且类名以 Service 结尾
 * ..mapper.. 下必须是接口，类名以 Mapper 结尾
 *
 * 包循环规则：com.ailearning.(*).. 一级业务包之间无循环依赖
 */
@AnalyzeClasses(packages = "com.ailearning", importOptions = DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule layered_dependencies = layeredArchitecture()
            .consideringAllDependencies()
            .layer("Controller").definedBy("..controller..")
            .layer("Service").definedBy("..service..")
            .layer("Mapper").definedBy("..mapper..")
            .layer("Ai").definedBy("..ai..")
            .layer("Search").definedBy("..search..")
            .layer("Rag").definedBy("..rag..")
            .whereLayer("Controller").mayNotBeAccessedByAnyLayer()
            .whereLayer("Service").mayOnlyBeAccessedByLayers("Controller", "Service")
            .whereLayer("Mapper").mayOnlyBeAccessedByLayers("Service", "Mapper")
            .whereLayer("Ai").mayOnlyBeAccessedByLayers("Service", "Ai")
            .whereLayer("Search").mayOnlyBeAccessedByLayers("Ai", "Search")
            .whereLayer("Rag").mayOnlyBeAccessedByLayers("Service", "Ai", "Rag");

    @ArchTest
    static final ArchRule domain_should_not_depend_on_application_or_infrastructure_packages = noClasses()
            .that().resideInAnyPackage("..domain..", "..dto..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..controller..",
                    "..service..",
                    "..mapper..",
                    "..ai..",
                    "..search..",
                    "..rag..",
                    "..config.."
            );

    @ArchTest
    static final ArchRule mapper_should_not_depend_on_upper_layers = noClasses()
            .that().resideInAPackage("..mapper..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..controller..",
                    "..service..",
                    "..ai..",
                    "..search..",
                    "..rag.."
            );

    @ArchTest
    static final ArchRule service_should_not_depend_on_controller = noClasses()
            .that().resideInAPackage("..service..")
            .should().dependOnClassesThat().resideInAPackage("..controller..");

    @ArchTest
    static final ArchRule rest_controllers_should_be_in_controller_package = classes()
            .that().areAnnotatedWith(RestController.class)
            .should().resideInAPackage("..controller..")
            .andShould().haveSimpleNameEndingWith("Controller");

    @ArchTest
    static final ArchRule services_should_be_in_service_package = classes()
            .that().areAnnotatedWith(Service.class)
            .should().resideInAPackage("..service..")
            .andShould().haveSimpleNameEndingWith("Service");

    @ArchTest
    static final ArchRule mappers_should_be_interfaces_and_named_mapper = classes()
            .that().resideInAPackage("..mapper..")
            .should().beInterfaces()
            .andShould().haveSimpleNameEndingWith("Mapper");

    @ArchTest
    static final ArchRule top_level_packages_should_not_have_cycles = slices()
            .matching("com.ailearning.(*)..")
            .should().beFreeOfCycles();
}
