# OBL-Enhanced HGA-SSA Cloud Task Scheduler

An implementation of the **Opposition-Based Learning (OBL) Enhanced Hybrid Genetic Algorithm – Salp Swarm Algorithm (OBL-HGA-SSA)** for multi-objective cloud task scheduling, validated using the **CloudSim 3.0.3** discrete-event simulator.

This project extends and improves upon the benchmark baseline proposed in:
> *"Hybrid Metaheuristic-Based Cloud Task Scheduling Using Genetic Algorithm and Salp Swarm Algorithm"* (IEEE ICTCS 2025)

---

## Key Enhancements Over the Baseline

| Dimension | Baseline HGA-SSA (ICTCS 2025) | Proposed OBL-HGA-SSA |
|---|---|---|
| **Population Initialization** | Purely random (30 schedules) | **OBL Pool + Elite Seeding**: 50 random + 50 opposite schedules ($\tilde{x} = \text{minVM} + \text{maxVM} - x$), top 30 seeded into Generation 0 |
| **Objective Function** | Single-objective (Makespan only) | **Normalized Multi-Objective**: Makespan ($\alpha = 0.4$), Energy ($\beta = 0.3$), Degree of Imbalance ($\gamma = 0.3$) |
| **Simulation Validation** | Standard CloudSim setup | Custom `OBLHGASSABroker` validating calculated schedules against CloudSim `CloudletSchedulerTimeShared` execution |
| **Convergence Tracking** | Final makespan comparison | Tracks **Generation Difference (`Gen Diff %`)** alongside initial Gen 0 quality across 3 scalability tiers |

---

## System Architecture & Workflow

The pipeline combines metaheuristic optimization in Java with CloudSim datacenter simulation and Python tabular/visual post-processing:

```text
 ┌───────────────────────────────────────────────────────────────────────────┐
 │                     PHASE 1: OBL POOL GENERATION                          │
 │  Generate 50 random schedules + 50 opposite schedules (x̃ = min+max - x)   │
 │  Total initial candidate pool = 100 schedules                             │
 └─────────────────────────────────────┬─────────────────────────────────────┘
                                       ▼
 ┌───────────────────────────────────────────────────────────────────────────┐
 │               PHASE 2: MULTI-OBJECTIVE FITNESS EVALUATION                 │
 │  Compute Makespan, Energy, Degree of Imbalance (DI)                       │
 │  Apply Min-Max normalization to [0, 1] -> F(i) = α·M + β·E + γ·DI         │
 └─────────────────────────────────────┬─────────────────────────────────────┘
                                       ▼
 ┌───────────────────────────────────────────────────────────────────────────┐
 │                         PHASE 3: ELITE SEEDING                            │
 │  Sort 100 candidates ascending by F(i) -> Top 30 form Generation 0        │
 └─────────────────────────────────────┬─────────────────────────────────────┘
                                       ▼
 ┌───────────────────────────────────────────────────────────────────────────┐
 │                PHASE 4: HYBRID GA-SSA EVOLUTION (100 GENS)                │
 │  1. GA Step : Elitism (top 2), Tournament Selection (k=3), 2-Point        │
 │               Crossover, Mutation (rate=0.1)                              │
 │  2. SSA Step: 30 salps refine top 2 elites with decaying exploration      │
 │               c1 = 2·exp(-(4t/T)²) and discretize to valid VM indices     │
 │  3. Merge   : Replace weakest individuals if refined elites improve F(i)  │
 └─────────────────────────────────────┬─────────────────────────────────────┘
                                       ▼
 ┌───────────────────────────────────────────────────────────────────────────┐
 │                 PHASE 5: CLOUDSIM 3.0.3 VALIDATION                        │
 │  Pass optimal taskToVM mapping into OBLHGASSABroker -> Run CloudSim on    │
 │  Datacenter_0 (32-core Host, heterogeneous VMs) -> Export CSVs & Summary  │
 └───────────────────────────────────────────────────────────────────────────┘
```

### Mathematical Formulation

For a schedule mapping tasks $i \in \{0, \dots, n-1\}$ onto VMs $j \in \{0, \dots, m-1\}$ with task durations $d_i$ and VM processing powers $P_j$:

- **Opposite Candidate (OBL)**: $\tilde{x}_i = \text{minVM} + \text{maxVM} - x_i$
- **VM Execution Time**: $T_j = \frac{\sum_{i \to j} d_i}{P_j}$
- **Makespan**: $M = \max_j (T_j)$
- **Energy Cost Model**: $E = \sum_{j=0}^{m-1} P_j \cdot \left(\sum_{i \to j} d_i\right)$
- **Degree of Imbalance (DI)**: $\text{DI} = \frac{\max_j(T_j) - \min_j(T_j)}{T_{\text{avg}}}$
- **Normalized Multi-Objective Fitness**: $F(i) = \alpha \cdot M_{\text{norm}} + \beta \cdot E_{\text{norm}} + \gamma \cdot \text{DI}_{\text{norm}}$
- **Convergence Speedup (`Gen Diff %`)**: $\frac{G_{\text{base}} - G_{\text{OBL-target}}}{G_{\text{base}}} \times 100\%$

---

## Project Structure & Module Reference

```text
OBL-based-improvement-in-cloud-scheduling/
├── pom.xml                                  # Maven build configuration (Java 11, CloudSim, JUnit 5)
├── run_simulation.sh                        # Cross-platform Bash runner (Linux / macOS / Git Bash / WSL)
├── run_simulation.ps1                       # Windows PowerShell runner
├── run_simulation.bat                       # Windows Batch runner
├── lib/
│   └── cloudsim-3.0.3.jar                   # Bundled CloudSim 3.0.3 library
├── src/
│   ├── main/java/org/cloudscheduler/
│   │   ├── algorithm/                       # Core optimization engine
│   │   ├── broker/                          # Custom CloudSim DatacenterBroker
│   │   ├── config/                          # Simulation & hyperparameter constants
│   │   ├── metrics/                         # Makespan, energy, and imbalance formulas
│   │   ├── model/                           # Schedule & VM metadata models
│   │   ├── output/                          # CSV and summary report exporters
│   │   └── simulation/                      # CloudSim environment & entry points
│   └── test/java/org/cloudscheduler/        # JUnit 5 unit tests
├── analysis/                                # Python post-processing & plotting scripts
│   ├── requirements.txt
│   ├── generate_tables.py
│   ├── compare_baseline.py
│   ├── plot_convergence.py
│   └── plot_multi_scenario.py
├── docs/
│   └── IMPLEMENTATION_PLAN.md               # Architecture and design specification
└── results/                                 # Generated CSVs, summary reports, and plots (gitignored)
```

### Core Packages

1. **Optimization Engine (`src/main/java/org/cloudscheduler/algorithm/`)**
   - [HybridGASSA.java](src/main/java/org/cloudscheduler/algorithm/HybridGASSA.java): Orchestrates the 100-generation GA-SSA hybrid loop, supports toggling OBL (`useOBL = true/false`), and records per-generation convergence telemetry.
   - [OBLInitializer.java](src/main/java/org/cloudscheduler/algorithm/OBLInitializer.java): Generates 50 random schedules and computes their 50 mathematical opposites to build a 100-schedule initial pool.
   - [FitnessEvaluator.java](src/main/java/org/cloudscheduler/algorithm/FitnessEvaluator.java): Evaluates raw metrics, applies pool-wide Min-Max normalization into `[0.0, 1.0]`, and computes the weighted objective $F(i)$.
   - [EliteSelector.java](src/main/java/org/cloudscheduler/algorithm/EliteSelector.java): Ranks candidate schedules ascending by fitness and extracts deep copies of top elites (top 30 for Generation 0).
   - [GeneticAlgorithm.java](src/main/java/org/cloudscheduler/algorithm/GeneticAlgorithm.java): Implements tournament selection (`TOURNAMENT_SIZE = 3`), 2-point crossover, random VM reassignment mutation (`MUTATION_RATE = 0.1`), and elitism (`NUM_ELITES = 2`).
   - [SalpSwarmAlgorithm.java](src/main/java/org/cloudscheduler/algorithm/SalpSwarmAlgorithm.java): Refines top elites using 30 salps (`NUM_SALPS = 30`), leader-follower chain updates with decaying $c_1$, and integer discretization with bounds clipping.

2. **CloudSim Integration & Simulation (`broker/` & `simulation/`)**
   - [OBLHGASSABroker.java](src/main/java/org/cloudscheduler/broker/OBLHGASSABroker.java): Extends CloudSim's `DatacenterBroker` to bind each Cloudlet to the optimized VM mapping instead of CloudSim's default assignment.
   - [CloudEnvironment.java](src/main/java/org/cloudscheduler/simulation/CloudEnvironment.java): Provisions `Datacenter` with a 32-core Host (10,000 MIPS/core, 64 GB RAM, 10 TB storage, 50 Gbps bandwidth), heterogeneous VMs (`BASE_MIPS × processingPower`) using `CloudletSchedulerTimeShared`, and Cloudlets (`duration × BASE_MIPS`).
   - [MainSimulation.java](src/main/java/org/cloudscheduler/simulation/MainSimulation.java): Runs the primary 10-task / 5-VM multi-objective optimization, validates execution telemetry in CloudSim, computes `Gen Diff %` vs. baseline, and exports results.
   - [MultiScenarioSimulation.java](src/main/java/org/cloudscheduler/simulation/MultiScenarioSimulation.java): Benchmarks Baseline HGA-SSA vs. Proposed OBL-HGA-SSA across 3 workload scales (10, 25, and 50 tasks) under a makespan-focused objective ($\alpha=1.0, \beta=0.0, \gamma=0.0$).

3. **Models, Metrics, Configuration & Output (`model/`, `metrics/`, `config/`, `output/`)**
   - [SimulationConfig.java](src/main/java/org/cloudscheduler/config/SimulationConfig.java): Central constants (`NUM_TASKS=10`, `NUM_VMS=5`, `BASE_MIPS=1000`, `OBL_RANDOM_COUNT=50`, `OBL_ELITE_COUNT=30`, `ALPHA=0.4`, `BETA=0.3`, `GAMMA=0.3`, `POPULATION_SIZE=30`, `NUM_GENERATIONS=100`, `RANDOM_SEED=42`).
   - [Schedule.java](src/main/java/org/cloudscheduler/model/Schedule.java) & [VMInfo.java](src/main/java/org/cloudscheduler/model/VMInfo.java): Candidate task-to-VM mapping chromosome and VM power metadata.
   - [MetricsCalculator.java](src/main/java/org/cloudscheduler/metrics/MetricsCalculator.java): Computes VM execution times, makespan, energy, and degree of imbalance.
   - [ResultExporter.java](src/main/java/org/cloudscheduler/output/ResultExporter.java): Exports `convergence.csv`, `best_schedule.csv`, and `summary.txt`.

4. **Python Analysis (`analysis/`)**
   - [generate_tables.py](analysis/generate_tables.py): Prints task-to-VM allocation tables matching Tables II–IV of the ICTCS 2025 paper.
   - [compare_baseline.py](analysis/compare_baseline.py): Compares the achieved makespan against standalone GA (`134.76s`), standalone SSA (`172.20s`), and Baseline HGA-SSA (`115.60s`).
   - [plot_convergence.py](analysis/plot_convergence.py): Generates dual-axis fitness vs. makespan convergence curve (`results/convergence_curve.png`).
   - [plot_multi_scenario.py](analysis/plot_multi_scenario.py): Generates 3-panel trajectory comparison plots (`results/multi_scenario_convergence.png`).

5. **Unit Tests (`src/test/java/org/cloudscheduler/`)**
   - JUnit 5 tests in `algorithm/` ([FitnessEvaluatorTest.java](src/test/java/org/cloudscheduler/algorithm/FitnessEvaluatorTest.java), [SalpSwarmAlgorithmTest.java](src/test/java/org/cloudscheduler/algorithm/SalpSwarmAlgorithmTest.java)), `metrics/` ([MetricsCalculatorTest.java](src/test/java/org/cloudscheduler/metrics/MetricsCalculatorTest.java)), and `model/` ([ScheduleTest.java](src/test/java/org/cloudscheduler/model/ScheduleTest.java)).

---

## Prerequisites

- **Java Development Kit (JDK) 11 or higher** (`javac` and `java` available on `PATH`).
- **CloudSim 3.0.3**: Already bundled in [lib/cloudsim-3.0.3.jar](lib/cloudsim-3.0.3.jar) (no manual download needed).
- **Python 3** with dependencies from [analysis/requirements.txt](analysis/requirements.txt):
  ```bash
  python3 -m venv analysis/venv
  source analysis/venv/bin/activate   # On Windows: analysis\venv\Scripts\activate
  pip install -r analysis/requirements.txt
  ```

---

## How to Run

### Quick Start (Recommended)

- **Linux / macOS / WSL / Git Bash**:
  ```bash
  ./run_simulation.sh
  ```
  Compiles all 15 Java source files, runs `MainSimulation` and `MultiScenarioSimulation`, and executes `generate_tables.py` and `compare_baseline.py` for clean terminal tables (auto-detecting `analysis/venv` if present).

- **Windows PowerShell**:
  ```powershell
  .\run_simulation.ps1
  ```

- **Windows Command Prompt**:
  ```cmd
  run_simulation.bat
  ```

> **Note:** [run_simulation.sh](run_simulation.sh) outputs formatted console tables and CSV/text reports without invoking matplotlib plots by default, whereas `run_simulation.ps1` and `run_simulation.bat` also run the plot scripts. You can generate plots anytime using the Python commands below.

### Manual Execution

1. **Compile Java Sources**:
   ```bash
   # Linux / macOS
   mkdir -p target/classes
   javac -encoding UTF-8 -cp "lib/cloudsim-3.0.3.jar" -d target/classes $(find src/main/java -name '*.java')
   ```
   ```cmd
   REM Windows CMD
   if not exist target\classes mkdir target\classes
   dir /s /b src\main\java\*.java > sources.txt
   javac -encoding UTF-8 -cp "lib\cloudsim-3.0.3.jar" -d target\classes @sources.txt
   del sources.txt
   ```

2. **Run Primary Simulation & Multi-Scenario Benchmark**:
   ```bash
   # Linux / macOS (use ';' instead of ':' in classpath on Windows)
   java -Dfile.encoding=UTF-8 -cp "target/classes:lib/cloudsim-3.0.3.jar" org.cloudscheduler.simulation.MainSimulation
   java -Dfile.encoding=UTF-8 -cp "target/classes:lib/cloudsim-3.0.3.jar" org.cloudscheduler.simulation.MultiScenarioSimulation
   ```

3. **Run Python Tables & Optional Plot Generators**:
   ```bash
   python3 analysis/generate_tables.py
   python3 analysis/compare_baseline.py
   python3 analysis/plot_convergence.py
   python3 analysis/plot_multi_scenario.py
   ```

---

## Generated Outputs (`results/`)

All output artifacts are saved to the `results/` directory:

| File | Produced By | Description |
|---|---|---|
| `best_schedule.csv` | `MainSimulation` | Optimal task-to-VM assignment, VM power factors, individual task times, and total VM times. |
| `convergence.csv` | `MainSimulation` | Generation-by-generation fitness, makespan, energy, and imbalance from Gen 0 to Gen 100. |
| `summary.txt` | `MainSimulation` | Executive evaluation summary comparing metrics, baseline reduction, and `Gen Diff` speedup. |
| `multi_scenario_comparison.csv` | `MultiScenarioSimulation` | Comparative metrics across Small, Medium, and Large workloads including `gen_diff_pct`. |
| `multi_scenario_trajectories.csv` | `MultiScenarioSimulation` | Makespan trajectories across 101 points (Gen 0–100) for Baseline vs. OBL across all 3 scenarios. |
| `convergence_curve.png` | `plot_convergence.py` *(optional)* | Dual-axis convergence curve plotting Fitness $F(i)$ and Makespan vs. the 115.60s paper baseline. |
| `multi_scenario_convergence.png` | `plot_multi_scenario.py` *(optional)* | 3-panel scalability plot comparing Baseline HGA-SSA vs. OBL-HGA-SSA trajectories. |

---

## Verified Benchmark Results

### 1. Primary Simulation (`MainSimulation`: 10 Tasks, 5 Heterogeneous VMs, Seed 42)

- **Optimal Task-to-VM Mapping**: `[4, 4, 3, 3, 1, 2, 0, 3, 1, 2]`
- **Theoretical Makespan**: **`112.22 s`**
- **CloudSim 3.0.3 Actual Makespan**: **`112.32 s`** *(includes the 0.10 s CloudSim VM provisioning offset; all 10 Cloudlets start concurrently at `0.10 s` under `CloudletSchedulerTimeShared`)*
- **Energy Consumption**: `553.40` units | **Degree of Imbalance (DI)**: `0.0756`
- **Convergence Speed (`Gen Diff`)**: Surpassed the `115.60 s` ICTCS 2025 baseline by **Generation 21 / 100** (**`+79.00%`** speedup)

| Model | Makespan (s) | Reduction by Proposed OBL-HGA-SSA |
|---|---|---|
| Standalone GA *(Paper)* | `134.76` | **`+16.72%`** |
| Standalone SSA *(Paper)* | `172.20` | **`+34.83%`** |
| Baseline HGA-SSA *(Paper)* | `115.60` | **`+2.92%`** |
| **Proposed OBL-HGA-SSA (Ours)** | **`112.22`** | Reference (`0.00%`) |

### 2. Multi-Scenario Scalability Benchmark (`MultiScenarioSimulation`, Pure Makespan Mode $\alpha = 1.0$)

| Scenario | Workload | Gen 0 Makespan (Base / OBL) | Final Makespan (Base / OBL) | CloudSim Makespan (Base / OBL) | Makespan Gain | Target Speed (`OBL` vs `Base`) | `Gen Diff %` |
|---|---|---|---|---|---|---|---|
| **Small** | 10T / 5VM | `147.27s` / `136.67s` (`+7.2%`) | `123.64s` / `119.09s` | `123.74s` / `119.19s` | **`+3.68%`** | Gen `6` vs Gen `52` | **`+88.46%`** |
| **Medium** | 25T / 5VM | `478.75s` / `436.67s` (`+8.8%`) | `372.00s` / `372.00s` | `372.10s` / `372.10s` | `0.00%` | Gen `68` vs Gen `26` | `-161.54%` |
| **Large** | 50T / 10VM | `813.89s` / `813.89s` (`0.0%`) | `555.00s` / `537.78s` | `555.10s` / `537.88s` | **`+3.10%`** | Gen `42` vs Gen `80` | **`+47.50%`** |
