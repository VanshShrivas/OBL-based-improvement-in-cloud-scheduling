package org.cloudscheduler.algorithm;

import org.cloudscheduler.model.Schedule;
import org.cloudscheduler.model.VMInfo;

import static org.cloudscheduler.config.SimulationConfig.*;

import java.util.Arrays;
import java.util.Random;

/**
 * Genetic Algorithm operators for the HGA-SSA cloud task scheduler.
 * 
 * Implements:
 *   - Tournament selection (size 3)
 *   - 2-point crossover
 *   - Mutation (rate 0.1, random VM reassignment)
 *   - Elitism (top 2 preserved unchanged)
 * 
 * Reference: Section 4, Phase 4 of the Implementation Plan.
 */
public class GeneticAlgorithm {

    private final FitnessEvaluator fitnessEvaluator;

    public GeneticAlgorithm(FitnessEvaluator fitnessEvaluator) {
        this.fitnessEvaluator = fitnessEvaluator;
    }

    /**
     * Evolves the population through one generation of GA.
     * 
     * Steps:
     *   1. Sort population by fitness (ascending = better)
     *   2. Preserve top NUM_ELITES (2) individuals unchanged
     *   3. Fill remaining slots via tournament selection → crossover → mutation
     *   4. Re-evaluate fitness of the entire new population
     * 
     * @param population  Current generation (size = POPULATION_SIZE = 30)
     * @param vms         VM metadata array
     * @param taskDurations  Duration of each task
     * @param rng         Seeded random number generator
     * @return New generation of POPULATION_SIZE schedules
     */
    public Schedule[] evolve(Schedule[] population, VMInfo[] vms, int[] taskDurations, Random rng) {
        // Sort by fitness ascending (lower = better)
        Arrays.sort(population, (a, b) -> Double.compare(a.fitness, b.fitness));

        Schedule[] newPopulation = new Schedule[POPULATION_SIZE];

        // --- Step 1: Elitism — copy top NUM_ELITES (2) directly ---
        for (int i = 0; i < NUM_ELITES; i++) {
            newPopulation[i] = population[i].copy();
        }

        // --- Step 2: Fill remaining slots with offspring ---
        for (int i = NUM_ELITES; i < POPULATION_SIZE; i += 2) {
            // Select two parents via tournament selection
            Schedule parent1 = tournamentSelect(population, rng);
            Schedule parent2 = tournamentSelect(population, rng);

            // 2-point crossover → produce two children
            Schedule[] children = twoPointCrossover(parent1, parent2, rng);

            // Mutation on each child
            mutate(children[0], vms.length, rng);
            mutate(children[1], vms.length, rng);

            // Add children to new population
            newPopulation[i] = children[0];
            if (i + 1 < POPULATION_SIZE) {
                newPopulation[i + 1] = children[1];
            }
        }

        // --- Step 3: Re-evaluate fitness of the entire new population ---
        fitnessEvaluator.evaluateAll(newPopulation, vms, taskDurations);

        return newPopulation;
    }

    // ========================================================================
    // Tournament Selection
    // ========================================================================

    /**
     * Tournament selection: pick TOURNAMENT_SIZE (3) random individuals,
     * return the one with the lowest (best) fitness.
     */
    private Schedule tournamentSelect(Schedule[] population, Random rng) {
        Schedule best = null;

        for (int i = 0; i < TOURNAMENT_SIZE; i++) {
            int idx = rng.nextInt(population.length);
            Schedule candidate = population[idx];

            if (best == null || candidate.fitness < best.fitness) {
                best = candidate;
            }
        }

        return best.copy();
    }

    // ========================================================================
    // 2-Point Crossover
    // ========================================================================

    /**
     * 2-point crossover: select two random cut points, swap the middle segment
     * between parents to produce two children.
     */
    private Schedule[] twoPointCrossover(Schedule parent1, Schedule parent2, Random rng) {
        int numTasks = parent1.taskToVM.length;
        Schedule child1 = new Schedule(numTasks);
        Schedule child2 = new Schedule(numTasks);

        // Pick two distinct crossover points
        int point1 = rng.nextInt(numTasks);
        int point2 = rng.nextInt(numTasks);

        // Ensure point1 < point2
        if (point1 > point2) {
            int temp = point1;
            point1 = point2;
            point2 = temp;
        }
        // If same, just copy parents
        if (point1 == point2) {
            return new Schedule[] { parent1.copy(), parent2.copy() };
        }

        // Build children by swapping the segment [point1, point2)
        for (int i = 0; i < numTasks; i++) {
            if (i >= point1 && i < point2) {
                // Swap segment
                child1.taskToVM[i] = parent2.taskToVM[i];
                child2.taskToVM[i] = parent1.taskToVM[i];
            } else {
                // Keep from own parent
                child1.taskToVM[i] = parent1.taskToVM[i];
                child2.taskToVM[i] = parent2.taskToVM[i];
            }
        }

        return new Schedule[] { child1, child2 };
    }

    // ========================================================================
    // Mutation
    // ========================================================================

    /**
     * Mutation: for each task in the schedule, with probability MUTATION_RATE (0.1),
     * reassign it to a random VM in [0, numVMs - 1].
     */
    private void mutate(Schedule schedule, int numVMs, Random rng) {
        int numTasks = schedule.taskToVM.length;
        for (int i = 0; i < numTasks; i++) {
            if (rng.nextDouble() < MUTATION_RATE) {
                schedule.taskToVM[i] = rng.nextInt(numVMs);
            }
        }
    }
}
