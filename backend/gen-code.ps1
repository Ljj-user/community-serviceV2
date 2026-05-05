# MyBatis-Plus codegen script (Windows PowerShell)
# Usage:
#   PS> cd backend
#   PS> .\gen-code.ps1
#
# Optional env overrides:
#   $env:DB_URL=...
#   $env:DB_USERNAME=...
#   $env:DB_PASSWORD=...

$ErrorActionPreference = 'Stop'

Write-Host 'Run codegen via exec-maven-plugin...' -ForegroundColor Cyan
# 说明：你本机 Maven 版本为 3.6.0，较新的 maven-dependency-plugin / surefire 插件版本会要求 Maven>=3.6.3。
# 这里用 exec-maven-plugin 直接以 test classpath 运行 main()，无需执行测试。
mvn -q -DskipTests test-compile exec:java "-Dexec.classpathScope=test" "-Dexec.mainClass=com.community.platform.generator.MyBatisPlusCodeGenerator"
if ($LASTEXITCODE -ne 0) {
  throw ("mvn failed, exitCode=" + $LASTEXITCODE)
}

Write-Host 'Done. Generated:' -ForegroundColor Green
Write-Host ' - src/main/java/com/community/platform/generated' -ForegroundColor Green
Write-Host ' - src/main/resources/mapper' -ForegroundColor Green

