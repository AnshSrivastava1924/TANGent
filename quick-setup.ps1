$ErrorActionPreference = "Stop"

Write-Host "TANGent MySQL setup" -ForegroundColor Cyan
$rootPassword = Read-Host "MySQL root password" -AsSecureString
$rootPasswordPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($rootPassword)
$rootPasswordText = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($rootPasswordPointer)

try {
    Write-Host "Creating the canonical database schema..." -ForegroundColor Yellow
    Get-Content ".\database\tangent_schema_minimal.sql" |
        & mysql -u root "--password=$rootPasswordText"
    if ($LASTEXITCODE -ne 0) { throw "MySQL schema setup failed with code $LASTEXITCODE" }

    $appPassword = [Convert]::ToBase64String(
        [Security.Cryptography.RandomNumberGenerator]::GetBytes(24))
    $jwtSecret = [Convert]::ToBase64String(
        [Security.Cryptography.RandomNumberGenerator]::GetBytes(48))

    $userSql = @"
CREATE USER IF NOT EXISTS 'tangent_app'@'localhost' IDENTIFIED BY '$appPassword';
ALTER USER 'tangent_app'@'localhost' IDENTIFIED BY '$appPassword';
GRANT SELECT, INSERT, UPDATE, DELETE ON tangent_database.* TO 'tangent_app'@'localhost';
FLUSH PRIVILEGES;
"@
    $userSql | & mysql -u root "--password=$rootPasswordText"
    if ($LASTEXITCODE -ne 0) { throw "MySQL user setup failed with code $LASTEXITCODE" }

    New-Item -ItemType Directory -Path ".\config" -Force | Out-Null
    $config = @"
spring.datasource.url=jdbc:mysql://127.0.0.1:3306/tangent_database?useSSL=false&serverTimezone=UTC
spring.datasource.username=tangent_app
spring.datasource.password=$appPassword
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
jwt.secret=$jwtSecret
market.massive.api-key=
market.alpha-vantage.api-key=
server.port=8080
logging.level.com.tangent=INFO
"@
    $config | Out-File ".\config\application.properties" -Encoding utf8

    $count = "SELECT COUNT(*) FROM users;" |
        & mysql -u tangent_app "--password=$appPassword" -D tangent_database -s -N
    if ($LASTEXITCODE -ne 0) { throw "Application database connection check failed" }

    Write-Host "Setup complete. Database connection verified with $count user(s)." -ForegroundColor Green
    Write-Host "Run .\run-dev.ps1 for embedded development, or .\mvnw.cmd spring-boot:run for MySQL."
    Write-Host "Application: http://localhost:8080"
    Write-Host "Development login: student@tangent.local / training123"
    Write-Host "Generated secrets were written only to config\application.properties." -ForegroundColor Yellow
} finally {
    if ($rootPasswordPointer -ne [IntPtr]::Zero) {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($rootPasswordPointer)
    }
    $rootPasswordText = $null
}
