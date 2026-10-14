Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  Building and Running OBL-Enhanced Cloud Task Scheduler  " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

$workspace = $PSScriptRoot
Set-Location $workspace

# 1. Check CloudSim JAR
$cloudsimJar = Join-Path $workspace "lib\cloudsim-3.0.3.jar"
if (-not (Test-Path $cloudsimJar)) {
    Write-Host "[ERROR] CloudSim jar not found at: $cloudsimJar" -ForegroundColor Red
    exit 1
}

# 2. Compile Java Source Files
$targetDir = Join-Path $workspace "target\classes"
if (-not (Test-Path $targetDir)) {
    New-Item -ItemType Directory -Path $targetDir -Force | Out-Null
}

$javaFiles = Get-ChildItem -Path "$workspace\src\main\java" -Recurse -Filter "*.java" | ForEach-Object { $_.FullName }
Write-Host "`n[1/3] Compiling $($javaFiles.Count) Java source files..." -ForegroundColor Yellow

$compileArgs = @(
    "-encoding", "UTF-8",
    "-cp", $cloudsimJar,
    "-d", $targetDir
) + $javaFiles

& javac $compileArgs

if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] Java compilation failed!" -ForegroundColor Red
    exit $LASTEXITCODE
}
Write-Host "Java compilation successful." -ForegroundColor Green

# 3. Run MainSimulation
Write-Host "`n[2/3] Running CloudSim Simulation (MainSimulation)..." -ForegroundColor Yellow
$classpath = "$targetDir;$cloudsimJar"

& java -cp $classpath org.cloudscheduler.simulation.MainSimulation

if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] Simulation failed during execution!" -ForegroundColor Red
    exit $LASTEXITCODE
}

# 4. Run Multi-Scenario Benchmark (HGA-SSA vs OBL+HGA-SSA)
Write-Host "`n[3/4] Running Multi-Scenario Benchmark (3 Workload Configurations)..." -ForegroundColor Yellow
& java -cp $classpath org.cloudscheduler.simulation.MultiScenarioSimulation

# 5. Run Python Analysis & Visualizations if Python is available
Write-Host "`n[4/4] Generating Python Plots & Analysis..." -ForegroundColor Yellow
if (Get-Command python -ErrorAction SilentlyContinue) {
    Write-Host "Running Python analysis..." -ForegroundColor Green
    python analysis/generate_tables.py
    python analysis/compare_baseline.py
    python analysis/plot_convergence.py
    python analysis/plot_multi_scenario.py
} else {
    Write-Host "Python not found on PATH. You can run python analysis scripts manually." -ForegroundColor DarkYellow
}

Write-Host "`nSimulation run complete! Results generated in ./results directory." -ForegroundColor Cyan
