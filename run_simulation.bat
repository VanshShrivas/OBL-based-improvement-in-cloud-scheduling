@echo off
setlocal
cd /d "%~dp0"

echo ==========================================================
echo   Building and Running OBL-Enhanced Cloud Task Scheduler
echo ==========================================================

REM 1. Compile Java Source Files
if not exist "target\classes" mkdir "target\classes"

echo [1/3] Compiling Java source files...
dir /s /b src\main\java\*.java > sources.txt
javac -encoding UTF-8 -cp "lib\cloudsim-3.0.3.jar" -d target\classes @sources.txt
del sources.txt

if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Java compilation failed!
    pause
    exit /b %ERRORLEVEL%
)

REM 2. Run MainSimulation
echo [2/3] Running CloudSim Simulation (MainSimulation)...
java -cp "target\classes;lib\cloudsim-3.0.3.jar" org.cloudscheduler.simulation.MainSimulation

if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Simulation failed!
    pause
    exit /b %ERRORLEVEL%
)

REM 3. Run Multi-Scenario Benchmark (HGA-SSA vs OBL+HGA-SSA)
echo [3/4] Running Multi-Scenario Benchmark (3 Workload Configurations)...
java -cp "target\classes;lib\cloudsim-3.0.3.jar" org.cloudscheduler.simulation.MultiScenarioSimulation

REM 4. Run Python Analysis & Visualizations
echo [4/4] Generating Python Analysis and Plots...
python analysis\generate_tables.py
python analysis\compare_baseline.py
python analysis\plot_convergence.py
python analysis\plot_multi_scenario.py

echo.
echo ==========================================================
echo Simulation complete! Check the results\ folder.
echo ==========================================================
pause
