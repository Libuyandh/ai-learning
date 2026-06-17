@echo off
setlocal
where mvn.cmd >nul 2>nul
if not errorlevel 1 (
  call mvn.cmd %*
  goto end
)
if exist "C:\Program Files\JetBrains\IntelliJ IDEA 2023.2.1\plugins\maven\lib\maven3\bin\mvn.cmd" (
  call "C:\Program Files\JetBrains\IntelliJ IDEA 2023.2.1\plugins\maven\lib\maven3\bin\mvn.cmd" %*
  goto end
)
call "C:\Program Files (x86)\IntelliJ IDEA 2023.2.1\plugins\maven\lib\maven3\bin\mvn.cmd" %*
:end
set EXIT_CODE=%errorlevel%
endlocal
exit /b %EXIT_CODE%
