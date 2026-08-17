@echo off
setlocal

REM Avoid Chinese encoding issues: use English prompts only
cd /d "%~dp0"

echo ==========================================
echo web-admin-react dev server startup script
echo Project: %CD%
echo Default URL: http://localhost:5173
echo ==========================================

REM Check if node_modules exists
if not exist "node_modules" (
    echo [INFO] node_modules not found, running npm install...
    call npm install
    if errorlevel 1 (
        echo [ERROR] npm install failed. Please check your Node.js / npm environment.
        pause
        exit /b 1
    )
    echo [INFO] npm install completed.
)

REM Check if backend port 8080 is listening (best-effort, use full path to avoid PATH issues)
set BACKEND_READY=0
for /f "tokens=5" %%a in ('C:\Windows\System32\netstat.exe -ano ^| C:\Windows\System32\findstr.exe "0.0.0.0:8080"') do set BACKEND_READY=1
if "%BACKEND_READY%"=="0" (
    echo [WARNING] Backend service on port 8080 is not detected.
    echo [WARNING] The admin page may show login errors if the backend is not running.
) else (
    echo [INFO] Backend service on port 8080 detected.
)

echo [INFO] Launching Vite dev server...
echo [INFO] Opening browser after 3 seconds...

REM Open browser after a short delay so Vite has time to start
start /b cmd /c "timeout /t 3 /nobreak >nul && start "" "http://localhost:5173""

call npm run dev

if errorlevel 1 (
    echo [ERROR] Dev server exited with error.
    pause
    exit /b 1
)

endlocal
