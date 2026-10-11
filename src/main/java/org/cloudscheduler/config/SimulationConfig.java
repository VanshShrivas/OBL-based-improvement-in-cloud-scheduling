package org.cloudscheduler.config;

public class SimulationConfig {
    // === Problem Parameters ===
    public static final int    NUM_TASKS           = 10;
    public static final int    NUM_VMS             = 5;
    public static final int    MIN_TASK_DURATION    = 10;    // seconds
    public static final int    MAX_TASK_DURATION    = 100;   // seconds
    public static final double MIN_VM_POWER         = 0.8;
    public static final double MAX_VM_POWER         = 1.2;
    public static final int    BASE_MIPS            = 1000;  // MIPS = BASE_MIPS × processing_power

    // === OBL Parameters ===
    public static final int    OBL_RANDOM_COUNT     = 50;    // generates 50 random + 50 opposite = 100 total
    public static final int    OBL_ELITE_COUNT      = 30;    // top 30 from 100 → Generation 0 (matches paper)
    public static final int    MIN_VM_INDEX         = 0;
    public static final int    MAX_VM_INDEX         = NUM_VMS - 1;  // 4

    // === Fitness Weights ===
    public static final double ALPHA                = 0.4;   // Makespan weight
    public static final double BETA                 = 0.3;   // Energy weight
    public static final double GAMMA                = 0.3;   // Imbalance weight

    // === GA Parameters ===
    public static final int    POPULATION_SIZE      = 30;    // matches paper's population of 30
    public static final int    TOURNAMENT_SIZE      = 3;
    public static final double MUTATION_RATE        = 0.1;
    public static final int    NUM_ELITES           = 2;     // retained per generation, passed to SSA
    public static final int    NUM_CROSSOVER_POINTS = 2;

    // === SSA Parameters ===
    public static final int    NUM_SALPS            = 30;    // full 30-salp swarm spawned around elites

    // === Hybrid Loop ===
    public static final int    NUM_GENERATIONS      = 100;

    // === Reproducibility ===
    public static final long   RANDOM_SEED          = 42;
}
