# Script để chạy Spring Boot với production profile (MySQL)
# Sử dụng: .\run.ps1

Write-Host "Starting Spring Boot with production profile (MySQL)..." -ForegroundColor Green
Write-Host "Make sure MySQL is running and database 'ai_service' exists!" -ForegroundColor Yellow
Write-Host "Profile: default (production)" -ForegroundColor Cyan
Write-Host ""

# Kiểm tra API key
if (-not $env:GEMINI_API_KEY) {
    Write-Host "⚠️  WARNING: GEMINI_API_KEY chua duoc set!" -ForegroundColor Yellow
    Write-Host "   Set bang: `$env:GEMINI_API_KEY='your-api-key'" -ForegroundColor White
    Write-Host "   Hoac xem: FIX-ISSUES.md" -ForegroundColor White
    Write-Host ""
} else {
    $maskedKey = if ($env:GEMINI_API_KEY.Length -gt 8) {
        $env:GEMINI_API_KEY.Substring(0, 4) + "..." + $env:GEMINI_API_KEY.Substring($env:GEMINI_API_KEY.Length - 4)
    } else {
        "***"
    }
    Write-Host "✅ GEMINI_API_KEY da duoc set: $maskedKey" -ForegroundColor Green
    Write-Host ""
}

# Chạy Spring Boot (sẽ dùng application.yaml - MySQL)
# Không set profile = dùng default (production)
.\mvnw.cmd spring-boot:run

