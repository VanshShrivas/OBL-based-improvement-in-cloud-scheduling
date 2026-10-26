#!/usr/bin/env bash

# ==============================================================================
# OBL-Enhanced Cloud Task Scheduling Simulation Suite
# Compatible with Linux Bash, macOS, and Windows Git Bash / WSL.
# ==============================================================================

set -u

# Terminal Styling & Colors
BOLD="\033[1m"
DIM="\033[2m"
CYAN="\033[1;36m"
GREEN="\033[1;32m"
YELLOW="\033[1;33m"
BLUE="\033[1;34m"
MAGENTA="\033[1;35m"
WHITE="\033[1;37m"
RED="\033[1;31m"
RESET="\033[0m"

# Track total execution time
START_TIME=$(date +%s)

# Always run from the directory containing this script.
SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR" || exit 1

# Ensure UTF-8 locale so box characters display cleanly
export LC_ALL=C.UTF-8
export LANG=C.UTF-8

# Print Presentation Header Banner
echo -e "${CYAN}┌─────────────────────────────────────────────────────────────────────────────┐${RESET}"
echo -e "${CYAN}│      ${BOLD}OBL-ENHANCED CLOUD TASK SCHEDULING SIMULATION SUITE${RESET}${CYAN}                    │${RESET}"
echo -e "${CYAN}│      Multi-Objective Optimization & Validation using CloudSim 3.0.3         │${RESET}"
echo -e "${CYAN}│      Evaluation Benchmark Against ICTCS 2025 Paper Baseline                 │${RESET}"
echo -e "${CYAN}└─────────────────────────────────────────────────────────────────────────────┘${RESET}"

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
    echo -e "${RED}[ERROR] Python 3 is not installed or not working.${RESET}"
    exit 1
fi

# Ensure matplotlib has a writable cache directory
export MPLCONFIGDIR="${TMPDIR:-/tmp}/matplotlib_cache"
mkdir -p "$MPLCONFIGDIR" 2>/dev/null || true

# Check Java tools.
if ! command -v javac >/dev/null 2>&1 || ! command -v java >/dev/null 2>&1; then
    echo -e "${RED}[ERROR] JDK not found. Ensure java and javac are in PATH.${RESET}"
    exit 1
fi

JAVA_VERSION_STR=$(java -version 2>&1 | head -n 1)
PYTHON_VERSION_STR=$("${PYTHON[@]}" --version 2>&1)

echo -e "${DIM}  System Platform : $(uname -s) (${CLASSPATH})${RESET}"
echo -e "${DIM}  Python Engine   : ${PYTHON_VERSION_STR} (${PYTHON[*]})${RESET}"
echo -e "${DIM}  Java Runtime    : ${JAVA_VERSION_STR}${RESET}"

# Java UTF-8 encoding flags to guarantee proper character rendering
JAVA_OPTS=("-Dfile.encoding=UTF-8" "-Dstdout.encoding=UTF-8" "-Dstderr.encoding=UTF-8")

# ==============================================================================
# Step 1: Compile Java source files
# ==============================================================================
echo -e "\n${BOLD}${BLUE}═══════════════════════════════════════════════════════════════════════════════${RESET}"
echo -e "${BOLD}${BLUE} [1/4] COMPILING JAVA SOURCES WITH CLOUDSIM 3.0.3${RESET}"
echo -e "${BOLD}${BLUE}═══════════════════════════════════════════════════════════════════════════════${RESET}"

mkdir -p target/classes || exit 1

SOURCE_LIST="$(mktemp)"
trap 'rm -f "$SOURCE_LIST"' EXIT

find src/main/java -type f -name '*.java' > "$SOURCE_LIST"
SOURCE_COUNT=$(wc -l < "$SOURCE_LIST")

echo -e "  Found ${BOLD}${SOURCE_COUNT}${RESET} Java source files to compile."

if ! javac -encoding UTF-8 \
    -cp "lib/cloudsim-3.0.3.jar" \
    -d target/classes \
    @"$SOURCE_LIST"; then
    echo -e "${RED}[ERROR] Java compilation failed!${RESET}"
    exit 1
fi
echo -e "  ${GREEN}[OK] Compilation successful -> target/classes/${RESET}"

# ==============================================================================
# Step 2: Run MainSimulation
# ==============================================================================
echo -e "\n${BOLD}${CYAN}═══════════════════════════════════════════════════════════════════════════════${RESET}"
echo -e "${BOLD}${CYAN} [2/4] PRIMARY CLOUDSIM SIMULATION (MainSimulation)${RESET}"
echo -e "${BOLD}${CYAN}═══════════════════════════════════════════════════════════════════════════════${RESET}\n"

if ! java "${JAVA_OPTS[@]}" -cp "$CLASSPATH" \
    org.cloudscheduler.simulation.MainSimulation; then
    echo -e "${RED}[ERROR] MainSimulation failed!${RESET}"
    exit 1
fi

# ==============================================================================
# Step 3: Run Multi-Scenario Scalability Benchmark
# ==============================================================================
echo -e "\n${BOLD}${MAGENTA}═══════════════════════════════════════════════════════════════════════════════${RESET}"
echo -e "${BOLD}${MAGENTA} [3/4] MULTI-SCENARIO SCALABILITY BENCHMARKS (3 Workload Tiers)${RESET}"
echo -e "${BOLD}${MAGENTA}═══════════════════════════════════════════════════════════════════════════════${RESET}\n"

if ! java "${JAVA_OPTS[@]}" -cp "$CLASSPATH" \
    org.cloudscheduler.simulation.MultiScenarioSimulation; then
    echo -e "${RED}[ERROR] MultiScenarioSimulation failed!${RESET}"
    exit 1
fi

# ==============================================================================
# Step 4: Run Python Analysis & Comparative Benchmark Scripts
# ==============================================================================
echo -e "\n${BOLD}${YELLOW}═══════════════════════════════════════════════════════════════════════════════${RESET}"
echo -e "${BOLD}${YELLOW} [4/4] GENERATING RESEARCH TABLES & COMPARATIVE BENCHMARKS${RESET}"
echo -e "${BOLD}${YELLOW}═══════════════════════════════════════════════════════════════════════════════${RESET}"

for script in \
    analysis/generate_tables.py \
    analysis/compare_baseline.py
do
    echo -e "  > Executing ${BOLD}$script${RESET}..."
    if ! "${PYTHON[@]}" "$script"; then
        echo -e "${RED}[ERROR] Failed to execute $script${RESET}"
        exit 1
    fi
done

# ==============================================================================
# Executive Presentation Summary Card
# ==============================================================================
END_TIME=$(date +%s)
ELAPSED=$((END_TIME - START_TIME))
TIME_STR="${ELAPSED} seconds"

echo -e "\n${GREEN}┌─────────────────────────────────────────────────────────────────────────────┐${RESET}"
echo -e "${GREEN}│  SIMULATION & BENCHMARK SUITE COMPLETED SUCCESSFULLY                        │${RESET}"
echo -e "${GREEN}├─────────────────────────────────────────────────────────────────────────────┤${RESET}"
printf "${GREEN}│${RESET}  Total Run Time : %-57s ${GREEN}│${RESET}\n" "${TIME_STR}"
echo -e "${GREEN}│                                                                             │${RESET}"
echo -e "${GREEN}│${RESET}  Generated Artifacts in results/:                                           ${GREEN}│${RESET}"
echo -e "${GREEN}│${RESET}    - best_schedule.csv              Task-to-VM mapping & execution times    ${GREEN}│${RESET}"
echo -e "${GREEN}│${RESET}    - convergence.csv                100-generation trajectory log           ${GREEN}│${RESET}"
echo -e "${GREEN}│${RESET}    - multi_scenario_comparison.csv  Benchmark across all 3 scenarios        ${GREEN}│${RESET}"
echo -e "${GREEN}│${RESET}    - summary.txt                    Executive evaluation report             ${GREEN}│${RESET}"
echo -e "${GREEN}└─────────────────────────────────────────────────────────────────────────────┘${RESET}\n"
