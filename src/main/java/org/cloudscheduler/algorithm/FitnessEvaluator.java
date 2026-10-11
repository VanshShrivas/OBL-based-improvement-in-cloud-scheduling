package org.cloudscheduler.algorithm;

import org.cloudscheduler.metrics.MetricsCalculator;
import org.cloudscheduler.model.Schedule;
import org.cloudscheduler.model.VMInfo;

import static org.cloudscheduler.config.SimulationConfig.*;

/**
 * Step B: Multi-Objective Fitness Evaluator.
 * 
 * Computes raw metrics (Makespan, Energy, Imbalance) for all candidate schedules,
 * applies Min-Max Normalization (scaling to [0, 1]), and calculates the combined score:
 * F(i) = alpha * Makespan_norm + beta * Energy_norm + gamma * Imbalance_norm
 * 
 * Minimization problem: Lower F(i) score represents a better schedule.
 */
public class FitnessEvaluator {

    // Default weights from SimulationConfig
    public static final double DEFAULT_ALPHA = ALPHA;
    public static final double DEFAULT_BETA  = BETA;
    public static final double DEFAULT_GAMMA = GAMMA;

    private final double alpha;
    private final double beta;
    private final double gamma;
    private final MetricsCalculator metricsCalc;

    /**
     * Constructor with default weights (from SimulationConfig).
     */
    public FitnessEvaluator() {
        this(DEFAULT_ALPHA, DEFAULT_BETA, DEFAULT_GAMMA);
    }

    /**
     * Constructor allowing user-defined custom weights.
     */
    public FitnessEvaluator(double alpha, double beta, double gamma) {
        double sum = alpha + beta + gamma;
        if (Math.abs(sum - 1.0) > 1e-4) {
            throw new IllegalArgumentException("Weights (alpha + beta + gamma) must sum to 1.0. Current sum: " + sum);
        }
        this.alpha = alpha;
        this.beta = beta;
        this.gamma = gamma;
        this.metricsCalc = new MetricsCalculator();
    }

    /**
     * Evaluates all candidate schedules in the pool (using int[] taskDurations).
     *
     * @param pool           Array of schedules to evaluate
     * @param vms            VM metadata
     * @param taskDurations  Duration of each task
     */
    public void evaluateAll(Schedule[] pool, VMInfo[] vms, int[] taskDurations) {
        if (pool == null || pool.length == 0) return;

        double[] doubleDurations = new double[taskDurations.length];
        for (int i = 0; i < taskDurations.length; i++) {
            doubleDurations[i] = taskDurations[i];
        }
        evaluateAll(pool, doubleDurations, vms);
    }

    /**
     * Evaluates all candidate schedules in the pool (using double[] taskDurations).
     */
    public void evaluateAll(Schedule[] schedules, double[] taskDurations, VMInfo[] vms) {
        if (schedules == null || schedules.length == 0) {
            return;
        }

        // --- Step 1: Compute raw metrics for every schedule ---
        for (Schedule schedule : schedules) {
            MetricsCalculator.computeAndSetMetrics(schedule, taskDurations, vms);
        }

        // --- Step 2: Find Min and Max across the entire pool ---
        double minMakespan = Double.MAX_VALUE, maxMakespan = Double.MIN_VALUE;
        double minEnergy   = Double.MAX_VALUE, maxEnergy   = Double.MIN_VALUE;
        double minImb      = Double.MAX_VALUE, maxImb      = Double.MIN_VALUE;

        for (Schedule s : schedules) {
            if (s.makespan < minMakespan) minMakespan = s.makespan;
            if (s.makespan > maxMakespan) maxMakespan = s.makespan;

            if (s.energy < minEnergy) minEnergy = s.energy;
            if (s.energy > maxEnergy) maxEnergy = s.energy;

            if (s.imbalance < minImb) minImb = s.imbalance;
            if (s.imbalance > maxImb) maxImb = s.imbalance;
        }

        // --- Step 3: Min-Max Normalization & Weighted Sum F(i) ---
        for (Schedule s : schedules) {
            double normMakespan  = normalize(s.makespan, minMakespan, maxMakespan);
            double normEnergy    = normalize(s.energy, minEnergy, maxEnergy);
            double normImbalance = normalize(s.imbalance, minImb, maxImb);

            s.fitness = (this.alpha * normMakespan)
                      + (this.beta  * normEnergy)
                      + (this.gamma * normImbalance);
        }
    }

    /**
     * Min-Max normalization helper: scales value to [0.0, 1.0].
     * If all values are the same (max == min), returns 0.0.
     */
    private double normalize(double val, double min, double max) {
        if (max - min < 1e-9) {
            return 0.0;
        }
        return (val - min) / (max - min);
    }

    // --- Getters for Weights ---
    public double getAlpha() { return alpha; }
    public double getBeta()  { return beta; }
    public double getGamma() { return gamma; }
}
