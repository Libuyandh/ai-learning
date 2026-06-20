package com.ailearning.config;

import javax.sql.DataSource;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@ConditionalOnProperty(name = "rag.provider", havingValue = "pgvector")
public class PgVectorConfig {
    @Bean
    public DataSource pgVectorDataSource(
            @Value("${rag.pgvector.datasource.url}") String url,
            @Value("${rag.pgvector.datasource.username}") String username,
            @Value("${rag.pgvector.datasource.password}") String password
    ) {
        return DataSourceBuilder.create()
                .url(url)
                .username(username)
                .password(password)
                .driverClassName("org.postgresql.Driver")
                .build();
    }

    @Bean
    public VectorStore vectorStore(
            @Qualifier("pgVectorDataSource") DataSource pgVectorDataSource,
            EmbeddingModel embeddingModel,
            @Value("${rag.pgvector.dimensions:1536}") int dimensions
    ) {
        return PgVectorStore.builder(new JdbcTemplate(pgVectorDataSource), embeddingModel)
                .dimensions(dimensions)
                .distanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE)
                .indexType(PgVectorStore.PgIndexType.HNSW)
                .initializeSchema(true)
                .vectorTableName("material_vector_store_1024")
                .build();
    }
}
