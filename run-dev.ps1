$ErrorActionPreference = "Stop"

$projectRoot = $PSScriptRoot
$localRepository = Join-Path $projectRoot ".m2\repository"

Push-Location $projectRoot
try {
    $previousConfigLocation = $env:SPRING_CONFIG_LOCATION
    $previousMassiveApiKey = $env:MASSIVE_API_KEY
    $previousAlphaVantageApiKey = $env:ALPHA_VANTAGE_API_KEY
    $env:SPRING_CONFIG_LOCATION = "classpath:/"
    $env:MASSIVE_API_KEY = "YUgi_ODusFMhwzSXya_HsZmmwN0Md_IC"
    $env:ALPHA_VANTAGE_API_KEY = "RWY50J2QAVH5Q2GA"
    & mvn "-Dmaven.repo.local=$localRepository" spring-boot:run "-Dspring-boot.run.profiles=dev"
    if ($LASTEXITCODE -ne 0) {
        throw "Maven exited with code $LASTEXITCODE"
    }
} finally {
    $env:SPRING_CONFIG_LOCATION = $previousConfigLocation
    $env:MASSIVE_API_KEY = $previousMassiveApiKey
    $env:ALPHA_VANTAGE_API_KEY = $previousAlphaVantageApiKey
    Pop-Location
}
