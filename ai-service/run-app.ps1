# Script chạy app
Write-Host "=== Starting AI Service ===" -ForegroundColor Green

# Check Java version
Write-Host "`nChecking Java version..." -ForegroundColor Cyan
$javaVersion = java -version 2>&1 | Select-String "version"
Write-Host $javaVersion

# Check MySQL connection
Write-Host "`nChecking MySQL..." -ForegroundColor Cyan
try {
    mysql -u root -proot -e "USE ai_service;" 2>&1 | Out-Null
    Write-Host "✅ MySQL connection OK" -ForegroundColor Green
} catch {
    Write-Host "❌ MySQL connection failed. Make sure MySQL is running!" -ForegroundColor Red
    exit 1
}

# Check Gemini API Key
Write-Host "`nChecking Gemini API Key..." -ForegroundColor Cyan
if ($env:GEMINI_API_KEY) {
    Write-Host "✅ GEMINI_API_KEY is set" -ForegroundColor Green
} else {
    Write-Host "⚠️  GEMINI_API_KEY is not set!" -ForegroundColor Yellow
    Write-Host "Set it with: `$env:GEMINI_API_KEY='your-key'" -ForegroundColor Yellow
}

# Run app
Write-Host "`nStarting app..." -ForegroundColor Cyan
Write-Host "App will run on: http://localhost:8080" -ForegroundColor Yellow
Write-Host "Press Ctrl+C to stop" -ForegroundColor Yellow
Write-Host ""

mvn spring-boot:run


