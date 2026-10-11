package org.cloudscheduler.algorithm;

import org.cloudscheduler.metrics.MetricsCalculator;
import org.cloudscheduler.model.Schedule;
import org.cloudscheduler.model.VMInfo;

import static org.cloudscheduler.config.SimulationConfig.*;

/**
 * Multi-objective fitness evaluator.
 * 
 * For each schedule, computes:
 *   1. Raw metrics: makespan, energy, imbalance
 *   2. Normalizes each to [0, 1] using min-max scaling across the pool
 *   3. Weighted sum: F(i) = α×Makespan_norm + β×Energy_norm + γ×Imbalance_norm
 *                         = 0.4×M̂ + 0.3×Ê + 0.3×Î
 */
public class FitnessEvaluator {

    private final MetricsCalculator metricsCalc;

    public FitnessEvaluator() {
        this.metricsCalc = new MetricsCalculator();
    }

    /**
     * Evaluates all schedules in the pool.
     * Side effect: sets .fitness, .makespan, .energy, .imbalance on each Schedule.
     *
     * @param pool           Array of schedules to evaluate
     * @param vms            VM metadata
     * @param taskDurations  Duration of each task
     */
    public void evaluateAll(Schedule[] pool, VMInfo[] vms, int[] taskDurations) {
        int n = pool.length;

        // --- Step 1: Compute raw metrics for every schedule ---
        for (Schedule s : pool) {
            double[] vmTimes = metricsCalc.computeVMTimes(s, vms, taskDurations);
            s.makespan  = metricsCalc.computeMakespan(vmTimes);
            s.energy    = metricsCalc.computeEnergy(vmTimes, vms);
            s.imbalance = metricsCalc.computeImbalance(vmTimes);
        }

        // --- Step 2: Find min/max for normalization ---
        double minM = Double.MAX_VALUE, maxM = Double.MIN_VALUE;
        double minE = Double.MAX_VALUE, maxE = Double.MIN_VALUE;
        double minI = Double.MAX_VALUE, maxI = Double.MIN_VALUE;

        for (Schedule s : pool) {
            if (s.makespan < minM)  minM = s.makespan;
            if (s.makespan > maxM)  maxM = s.makespan;
            if (s.energy < minE)    minE = s.energy;
            if (s.energy > maxE)    maxE = s.energy;
            if (s.imbalance < minI) minI = s.imbalance;
            if (s.imbalance > maxI) maxI = s.imbalance;
        }

        // --- Step 3: Normalize and compute weighted fitness ---
        for (Schedule s : pool) {
            double normM = normalize(s.makespan, minM, maxM);
            double normE = normalize(s.energy, minE, maxE);
            double normI = normalize(s.imbalance, minI, maxI);

            s.fitness = ALPHA * normM + BETA * normE + GAMMA * normI;
        }
    }

    /**
     * Min-max normalization: maps value to [0, 1].
     * If all values are the same (max == min), returns 0.0.
     */
    private double normalize(double value, double min, double max) {
        if (max - min < 1e-9) return 0.0;  // avoid division by zero
        return (value - min) / (max - min);
    }
}
