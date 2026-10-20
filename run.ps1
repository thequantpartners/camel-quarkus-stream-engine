# ==============================================================================
# 1-Click Automation Harness for Quarkus Camel Stream Engine
# Runs 100% inside Docker (Zero local Java/Maven dependencies required)
# ==============================================================================

param (
    [Parameter(Position=0)]
    [ValidateSet("test", "build", "dev", "down", "demo", "help")]
    [string]$Command = "help"
)

$ErrorActionPreference = "Stop"

function Show-Help {
    Write-Host ""
    Write-Host "======================================================================" -ForegroundColor Cyan
    Write-Host "   Quarkus Camel Stream Engine - Deterministic Docker Harness         " -ForegroundColor Yellow
    Write-Host "======================================================================" -ForegroundColor Cyan
    Write-Host "Usage: .\run.ps1 [command]"
    Write-Host ""
    Write-Host "  test  - Run unit, architecture and integration tests in Maven Docker" -ForegroundColor Green
    Write-Host "  build - Build production JVM JARs inside Docker" -ForegroundColor Green
    Write-Host "  dev   - Launch stack (PostgreSQL + Kafka + Engine) with docker compose" -ForegroundColor Green
    Write-Host "  demo  - Send real-time test transactions (Standard, High-Value, Fraud)" -ForegroundColor Green
    Write-Host "  down  - Stop all running containers" -ForegroundColor Green
    Write-Host "======================================================================" -ForegroundColor Cyan
    Write-Host ""
}

switch ($Command) {
    "test" {
        Write-Host "[TEST] Running test suite inside maven:3.9.9-eclipse-temurin-21..." -ForegroundColor Cyan
        $cwd = (Get-Location).Path
        docker run --rm `
            -v "${cwd}:/workspace" `
            -v "maven_cache:/root/.m2" `
            -w /workspace `
            maven:3.9.9-eclipse-temurin-21-alpine `
            mvn clean test
    }

    "build" {
        Write-Host "[BUILD] Compiling application inside Docker..." -ForegroundColor Cyan
        $cwd = (Get-Location).Path
        docker run --rm `
            -v "${cwd}:/workspace" `
            -v "maven_cache:/root/.m2" `
            -w /workspace `
            maven:3.9.9-eclipse-temurin-21-alpine `
            mvn package -DskipTests
        Write-Host "[BUILD] Application packaged successfully!" -ForegroundColor Green
    }

    "dev" {
        Write-Host "[DEV] Starting PostgreSQL, Kafka (Redpanda), and Quarkus Engine..." -ForegroundColor Cyan
        docker compose up --build -d
        Write-Host "[DEV] Stack started! Access endpoints at:" -ForegroundColor Green
        Write-Host "  - Health:   http://localhost:8080/q/health" -ForegroundColor Yellow
        Write-Host "  - Swagger:  http://localhost:8080/q/swagger-ui" -ForegroundColor Yellow
        Write-Host "  - Metrics:  http://localhost:8080/q/metrics" -ForegroundColor Yellow
        Write-Host "  - Stream:   POST http://localhost:8080/api/v1/transactions" -ForegroundColor Yellow
    }

    "down" {
        Write-Host "[STOP] Stopping stack..." -ForegroundColor Yellow
        docker compose down
        Write-Host "[STOP] Stack stopped." -ForegroundColor Green
    }

    "demo" {
        Write-Host "[DEMO] Dispatching real-time test transactions..." -ForegroundColor Cyan
        
        Write-Host "1. Verifying Engine Health..." -ForegroundColor Yellow
        try {
            $health = Invoke-RestMethod -Uri "http://localhost:8080/q/health" -Method Get
            Write-Host "   Health Status: $($health.status)" -ForegroundColor Green
        } catch {
            Write-Host "   Engine not responding on port 8080. Start dev stack first with .\run.ps1 dev" -ForegroundColor Red
            return
        }

        Write-Host ""
        Write-Host "2. Dispatching Standard Transaction (150.00 USD)..." -ForegroundColor Yellow
        $bodyStd = @{
            eventId = [System.Guid]::NewGuid().ToString()
            idempotencyKey = "idem-std-" + [System.Guid]::NewGuid().ToString().Substring(0, 8)
            accountId = "acc-retail-101"
            amount = 150.00
            currency = "USD"
            metadata = @{ channel = "E_COMMERCE" }
        } | ConvertTo-Json
        $resStd = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/transactions" -Method Post -Body $bodyStd -ContentType "application/json"
        Write-Host "   Result: Destination -> $($resStd.destination) | Status -> $($resStd.assignedStatus)" -ForegroundColor Green

        Write-Host ""
        Write-Host "3. Dispatching High-Value Transaction (45,000.00 USD)..." -ForegroundColor Yellow
        $bodyHigh = @{
            eventId = [System.Guid]::NewGuid().ToString()
            idempotencyKey = "idem-high-" + [System.Guid]::NewGuid().ToString().Substring(0, 8)
            accountId = "acc-corp-500"
            amount = 45000.00
            currency = "USD"
            metadata = @{ clientTier = "ENTERPRISE" }
        } | ConvertTo-Json
        $resHigh = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/transactions" -Method Post -Body $bodyHigh -ContentType "application/json"
        Write-Host "   Result: Destination -> $($resHigh.destination) | Status -> $($resHigh.assignedStatus)" -ForegroundColor Green

        Write-Host ""
        Write-Host "4. Dispatching Suspicious Transaction (risk_level: CRITICAL)..." -ForegroundColor Yellow
        $bodyFraud = @{
            eventId = [System.Guid]::NewGuid().ToString()
            idempotencyKey = "idem-fraud-" + [System.Guid]::NewGuid().ToString().Substring(0, 8)
            accountId = "acc-flagged-007"
            amount = 999.00
            currency = "USD"
            metadata = @{ risk_level = "CRITICAL"; ip_geo = "TOR_EXIT_NODE" }
        } | ConvertTo-Json
        $resFraud = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/transactions" -Method Post -Body $bodyFraud -ContentType "application/json"
        Write-Host "   Result: Destination -> $($resFraud.destination) | Status -> $($resFraud.assignedStatus)" -ForegroundColor Red

        Write-Host ""
        Write-Host "[DEMO] All routing decisions executed deterministically!" -ForegroundColor Green
    }

    Default {
        Show-Help
    }
}
