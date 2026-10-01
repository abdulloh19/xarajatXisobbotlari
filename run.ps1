# Script to run Hisobchi Bot with environment variables from .env
$ErrorActionPreference = "Stop"

if (Test-Path ".env") {
    Get-Content ".env" | ForEach-Object {
        $line = $_.Trim()
        if ($line -and -not $line.StartsWith("#") -and $line.Contains("=")) {
            $parts = $line.Split("=", 2)
            $name = $parts[0].Trim()
            $value = $parts[1].Trim()
            [System.Environment]::SetEnvironmentVariable($name, $value, "Process")
        }
    }
    Write-Host "Loaded environment variables from .env" -ForegroundColor Green
}

& .\mvnw.cmd spring-boot:run
