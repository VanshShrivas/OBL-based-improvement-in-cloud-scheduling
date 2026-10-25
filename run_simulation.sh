
#!/usr/bin/env bash

# OBL-Enhanced Cloud Task Scheduler
# Compatible with Linux Bash and Windows Git Bash / WSL.

set -u

# Always run from the directory containing this script.
SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR" || exit 1

echo "=========================================================="
echo " Building and Running OBL-Enhanced Cloud Task Scheduler"
echo "=========================================================="

# Detect the OS and configure the Java classpath.
case "$(uname -s)" in
    MINGW*|MSYS*|CYGWIN*)
        CLASSPATH="target/classes;lib/cloudsim-3.0.3.jar"
        VENV_PY="analysis/venv/Scripts/python.exe"
        ;;
    *)
        CLASSPATH="target/classes:lib/cloudsim-3.0.3.jar"
        VENV_PY="analysis/venv/bin/python"
        ;;
esac

# Find a working Python executable.
if [ -n "${VENV_PY:-}" ] && [ -f "$VENV_PY" ] && "$VENV_PY" --version >/dev/null 2>&1; then
    PYTHON=("$VENV_PY")
elif [ -f "analysis/venv/bin/python" ] && "analysis/venv/bin/python" --version >/dev/null 2>&1; then
    PYTHON=("analysis/venv/bin/python")
elif [ -f "analysis/venv/Scripts/python.exe" ] && "analysis/venv/Scripts/python.exe" --version >/dev/null 2>&1; then
    PYTHON=("analysis/venv/Scripts/python.exe")
elif command -v python3 >/dev/null 2>&1 && python3 --version >/dev/null 2>&1; then
    PYTHON=(python3)
elif command -v python >/dev/null 2>&1 && python --version >/dev/null 2>&1; then
    PYTHON=(python)
elif command -v py >/dev/null 2>&1 && py -3 --version >/dev/null 2>&1; then
    PYTHON=(py -3)
else
    echo "[ERROR] Python 3 is not installed or not working."
    exit 1
fi

# Ensure matplotlib has a writable cache directory
export MPLCONFIGDIR="${TMPDIR:-/tmp}/matplotlib_cache"
mkdir -p "$MPLCONFIGDIR" 2>/dev/null || true

echo "Using Python: ${PYTHON[*]}"
"${PYTHON[@]}" --version

# Check Java tools.
if ! command -v javac >/dev/null 2>&1 ||
   ! command -v java >/dev/null 2>&1; then
    echo "[ERROR] JDK not found. Ensure java and javac are in PATH."
    exit 1
fi

# 1. Compile Java source files.
echo
echo "[1/4] Compiling Java source files..."

mkdir -p target/classes || exit 1

SOURCE_LIST="$(mktemp)"
trap 'rm -f "$SOURCE_LIST"' EXIT

find src/main/java -type f -name '*.java' > "$SOURCE_LIST"

if ! javac -encoding UTF-8 \
    -cp "lib/cloudsim-3.0.3.jar" \
    -d target/classes \
    @"$SOURCE_LIST"; then
    echo "[ERROR] Java compilation failed!"
    exit 1
fi

# 2. Run MainSimulation.
echo
echo "[2/4] Running CloudSim Simulation (MainSimulation)..."

if ! java -cp "$CLASSPATH" \
    org.cloudscheduler.simulation.MainSimulation; then
    echo "[ERROR] MainSimulation failed!"
    exit 1
fi

# 3. Run the multi-scenario benchmark.
echo
echo "[3/4] Running Multi-Scenario Benchmark..."

if ! java -cp "$CLASSPATH" \
    org.cloudscheduler.simulation.MultiScenarioSimulation; then
    echo "[ERROR] MultiScenarioSimulation failed!"
    exit 1
fi

# 4. Run Python analysis and visualization scripts.
echo
echo "[4/4] Generating Python Analysis and Plots..."

for script in \
    analysis/generate_tables.py \
    analysis/compare_baseline.py \
    analysis/plot_convergence.py \
    analysis/plot_multi_scenario.py
do
    echo "Running $script..."

    if ! "${PYTHON[@]}" "$script"; then
        echo "[ERROR] Failed to execute $script"
        exit 1
    fi
done

echo
echo "=========================================================="
echo " Simulation complete! Check the results/ folder."
echo "=========================================================="
