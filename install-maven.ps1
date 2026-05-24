# Script tự động cài Maven
Write-Host "Đang tải Maven..." -ForegroundColor Green

$mavenVersion = "3.9.6"
$mavenUrl = "https://archive.apache.org/dist/maven/maven-3/$mavenVersion/binaries/apache-maven-$mavenVersion-bin.zip"
$downloadPath = "$env:TEMP\apache-maven.zip"
$installPath = "$env:USERPROFILE\apache-maven-$mavenVersion"

# Tải Maven
Write-Host "Tải từ: $mavenUrl" -ForegroundColor Yellow
Invoke-WebRequest -Uri $mavenUrl -OutFile $downloadPath

# Giải nén
Write-Host "Giải nén Maven..." -ForegroundColor Yellow
Expand-Archive -Path $downloadPath -DestinationPath $env:USERPROFILE -Force

# Thêm vào PATH
Write-Host "Cấu hình biến môi trường..." -ForegroundColor Yellow
$mavenBin = "$installPath\bin"
$currentPath = [Environment]::GetEnvironmentVariable("Path", "User")

if ($currentPath -notlike "*$mavenBin*") {
    [Environment]::SetEnvironmentVariable("Path", "$currentPath;$mavenBin", "User")
    $env:Path = "$env:Path;$mavenBin"
    Write-Host "Đã thêm Maven vào PATH" -ForegroundColor Green
}

# Xóa file tải về
Remove-Item $downloadPath -Force

Write-Host "`nCài đặt thành công!" -ForegroundColor Green
Write-Host "Maven đã được cài tại: $installPath" -ForegroundColor Cyan
Write-Host "`nKiểm tra phiên bản:" -ForegroundColor Yellow
& "$mavenBin\mvn.cmd" --version

Write-Host "`nBạn có thể đóng và mở lại terminal, sau đó chạy:" -ForegroundColor Cyan
Write-Host "mvn spring-boot:run -Dspring-boot.run.profiles=local" -ForegroundColor White
