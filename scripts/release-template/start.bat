@echo off
setlocal enabledelayedexpansion

echo ==============================================================
echo                 SUCHIKA STARTUP SCRIPT
echo ==============================================================

:: 1. Check for Java 21+
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo [INFO] Java is not installed or not in PATH.
    echo [INFO] Attempting to install Microsoft OpenJDK 21 via winget...
    winget install Microsoft.OpenJDK.21 --accept-package-agreements --accept-source-agreements
    if %errorlevel% neq 0 (
        echo [ERROR] Failed to install Java. Please install Java 21+ manually and try again.
        pause
        exit /b 1
    )
    echo [INFO] Java installed. Please close this window and run start.bat again so Windows loads the new PATH.
    pause
    exit /b 0
)

:: 2. Load Environment Variables from .env
if exist .env (
    echo [INFO] Loading environment variables from .env...
    for /F "usebackq eol=# tokens=1,* delims==" %%i in (".env") do (
        set "%%i=%%j"
    )
) else (
    echo [WARNING] No .env file found. Using default/system variables.
)

:: 3. Check for Postgres Password
if "%QUARKUS_DATASOURCE_PASSWORD%"=="" (
    echo ==============================================================
    echo [ERROR] PostgreSQL Password is not configured!
    echo.
    echo Suchika requires PostgreSQL to be installed on your machine.
    echo If you haven't installed it, please install PostgreSQL and create
    echo a database named "app_db".
    echo.
    echo Once installed, open the ".env" file in this folder and set:
    echo QUARKUS_DATASOURCE_PASSWORD=your_password_here
    echo ==============================================================
    pause
    exit /b 1
)

:: 4. Start Backend Services
echo [INFO] Starting Backend Services...

:: We start domains in the background using 'start /b'
start /b "" java -jar bin\profile\quarkus-run.jar
start /b "" java -jar bin\wealth\quarkus-run.jar
start /b "" java -jar bin\health\quarkus-run.jar
start /b "" java -jar bin\household\quarkus-run.jar

echo [INFO] Waiting 10 seconds for domains to initialize...
timeout /t 10 /nobreak >nul

:: 5. Open Browser (Gateway runs on 8080)
echo [INFO] Opening Suchika in your web browser...
start http://localhost:8080

:: 6. Start Gateway in the foreground
echo [INFO] Starting Web Gateway (Press Ctrl+C to stop all)...
java -jar bin\web-gateway\quarkus-run.jar

echo [INFO] Shutting down...
:: Note: The background java processes will remain if Ctrl-C only kills the batch script.
:: A proper shutdown mechanism is usually handled via taskkill in simple scripts:
taskkill /F /IM java.exe >nul 2>&1
pause
