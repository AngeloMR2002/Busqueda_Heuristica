@echo off
echo =============================================
echo   Compilando N-Reinas Busquedas Heuristicas
echo =============================================
echo.

cd /d "%~dp0"

if not exist "build" mkdir build

echo Compilando archivos Java...
javac -d build src\NQueensState.java src\HillClimbing.java src\SimulatedAnnealing.java src\TabuSearch.java src\NQueensServer.java

if %ERRORLEVEL% neq 0 (
    echo.
    echo [ERROR] Fallo en la compilacion.
    pause
    exit /b 1
)

echo Compilacion exitosa.
echo.
echo Iniciando servidor en http://localhost:8080
echo Presiona Ctrl+C para detener.
echo.

java -cp build NQueensServer
pause
