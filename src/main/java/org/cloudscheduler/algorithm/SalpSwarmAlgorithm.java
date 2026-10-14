package org.cloudscheduler.algorithm;

import org.cloudscheduler.config.SimulationConfig;
import org.cloudscheduler.model.Schedule;
import org.cloudscheduler.model.VMInfo;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

/**
 * Salp Swarm Algorithm (SSA) for refining elite schedules.
 *
 * Spawns NUM_SALPS salps around elite positions, updates them using
 * leader/follower equations, discretizes to valid VM indices,
 * evaluates fitness, and returns the best 2 refined schedules.
 *
 * Key equations (from spec):
 *   c1 = 2 × e^(-(4t/T)²)                      — exploration coefficient (decays over time)
 *   Leader:   x = F ± c1 × ((ub-lb)×c2 + lb)    — moves toward food source F
 *   Follower: x = (x_i + x_{i-1}) / 2            — follows the salp ahead in the chain
 */
public class SalpSwarmAlgorithm {

    private final FitnessEvaluator fitnessEvaluator;

    public SalpSwarmAlgorithm(FitnessEvaluator fitnessEvaluator) {
        this.fitnessEvaluator = fitnessEvaluator;
    }

    /**
     * Refine elite schedules using SSA.
     *
     * @param elites        top elites from GA phase (elites[0] = food source / best)
     * @param currentGen    current generation index (0-based)
     * @param maxGen        total number of generations
     * @param vms           VM metadata array
     * @param taskDurations task duration array
     * @param rng           seeded Random for reproducibility
     * @return best 2 refined schedules, sorted by fitness ascending (lower = better)
     */
    public Schedule[] refine(Schedule[] elites, int currentGen, int maxGen,
                             VMInfo[] vms, int[] taskDurations, Random rng) {
        int numTasks = taskDurations.length;
        int numSalps = SimulationConfig.NUM_SALPS;
        int lb = 0;
        int ub = vms.length - 1;

        // Food source = best elite (elites[0])
        Schedule foodSource = elites[0];

        // Initialize salp positions (continuous) from elites, alternating
        double[][] positions = new double[numSalps][numTasks];
        for (int i = 0; i < numSalps; i++) {
            Schedule base = elites[i % elites.length];
            for (int j = 0; j < numTasks; j++) {
                positions[i][j] = base.taskToVM[j];
            }
        }

        // Compute exploration coefficient c1
        double c1 = computeC1(currentGen, maxGen);

        // Update salp positions
        for (int i = 0; i < numSalps; i++) {
            if (i == 0) {
                // Leader salp: moves toward food source
                for (int j = 0; j < numTasks; j++) {
                    double c2 = rng.nextDouble(); // random weight in [0, 1)
                    double c3 = rng.nextDouble(); // direction selector
                    if (c3 >= 0.5) {
                        positions[i][j] = foodSource.taskToVM[j]
                            + c1 * ((ub - lb) * c2 + lb);
                    } else {
                        positions[i][j] = foodSource.taskToVM[j]
                            - c1 * ((ub - lb) * c2 + lb);
                    }
                }
            } else {
                // Follower salp: averages with the salp ahead in the chain
                for (int j = 0; j < numTasks; j++) {
                    positions[i][j] = (positions[i][j] + positions[i - 1][j]) / 2.0;
                }
            }
        }

        // Discretize: round to nearest integer and clip to [lb, ub]
        Schedule[] salps = new Schedule[numSalps];
        for (int i = 0; i < numSalps; i++) {
            salps[i] = new Schedule(numTasks);
            for (int j = 0; j < numTasks; j++) {
                int val = (int) Math.round(positions[i][j]);
                salps[i].taskToVM[j] = Math.max(lb, Math.min(ub, val));
            }
        }

        // Evaluate fitness of all salps
        fitnessEvaluator.evaluateAll(salps, vms, taskDurations);

        // Sort by fitness ascending (lower = better)
        Arrays.sort(salps, Comparator.comparingDouble(s -> s.fitness));

        // Return independent copies of best 2
        return new Schedule[]{ salps[0].copy(), salps[1].copy() };
    }

    /**
     * Compute SSA exploration coefficient c1.
     * c1 = 2 × e^(-(4t/T)²)
     * Starts at 2.0 (maximum exploration at gen 0), decays toward 0 (exploitation at max gen).
     */
    double computeC1(int currentGen, int maxGen) {
        double ratio = 4.0 * currentGen / maxGen;
        return 2.0 * Math.exp(-(ratio * ratio));
    }
}
