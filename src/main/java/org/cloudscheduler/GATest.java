package org.cloudscheduler;

import org.cloudscheduler.algorithm.FitnessEvaluator;
import org.cloudscheduler.algorithm.GeneticAlgorithm;
import org.cloudscheduler.model.Schedule;
import org.cloudscheduler.model.VMInfo;

import static org.cloudscheduler.config.SimulationConfig.*;

import java.util.Arrays;
import java.util.Random;

/**
 * Quick test to verify the Genetic Algorithm works correctly.
 */
public class GATest {

    public static void main(String[] args) {
        System.out.println("=== Genetic Algorithm Test ===\n");

        Random rng = new Random(RANDOM_SEED); // seed = 42 for reproducibility

        // --- Setup VMs (5 VMs with processing power 0.8 to 1.2) ---
        VMInfo[] vms = new VMInfo[NUM_VMS];
        double[] powers = { 0.8, 0.9, 1.0, 1.1, 1.2 };
        System.out.println("VMs:");
        for (int i = 0; i < NUM_VMS; i++) {
            vms[i] = new VMInfo(i, powers[i]);
            System.out.println("  VM " + i + " → power = " + powers[i]);
        }

        // --- Setup Tasks (10 tasks with random durations 10-100s) ---
        int[] taskDurations = new int[NUM_TASKS];
        System.out.println("\nTasks:");
        for (int i = 0; i < NUM_TASKS; i++) {
            taskDurations[i] = MIN_TASK_DURATION + rng.nextInt(MAX_TASK_DURATION - MIN_TASK_DURATION + 1);
            System.out.println("  Task " + i + " → duration = " + taskDurations[i] + "s");
        }

        // --- Create initial random population (30 schedules) ---
        FitnessEvaluator fitnessEval = new FitnessEvaluator();
        Schedule[] population = new Schedule[POPULATION_SIZE];
        for (int i = 0; i < POPULATION_SIZE; i++) {
            population[i] = new Schedule(NUM_TASKS);
            for (int j = 0; j < NUM_TASKS; j++) {
                population[i].taskToVM[j] = rng.nextInt(NUM_VMS);
            }
        }

        // Evaluate initial population
        fitnessEval.evaluateAll(population, vms, taskDurations);
        Arrays.sort(population, (a, b) -> Double.compare(a.fitness, b.fitness));

        System.out.println("\n--- Initial Population (Gen 0) ---");
        System.out.printf("  Best fitness:  %.4f  |  Makespan: %.2fs\n",
                population[0].fitness, population[0].makespan);
        System.out.printf("  Worst fitness: %.4f  |  Makespan: %.2fs\n",
                population[POPULATION_SIZE - 1].fitness, population[POPULATION_SIZE - 1].makespan);
        System.out.println("  Best mapping:  " + Arrays.toString(population[0].taskToVM));

        // --- Run GA for NUM_GENERATIONS (100) generations ---
        GeneticAlgorithm ga = new GeneticAlgorithm(fitnessEval);

        System.out.println("\n--- Running GA for " + NUM_GENERATIONS + " generations ---");
        for (int gen = 0; gen < NUM_GENERATIONS; gen++) {
            population = ga.evolve(population, vms, taskDurations, rng);
            Arrays.sort(population, (a, b) -> Double.compare(a.fitness, b.fitness));

            // Print progress every 10 generations
            if (gen % 10 == 0 || gen == NUM_GENERATIONS - 1) {
                System.out.printf(
                        "  Gen %3d → Best fitness: %.4f | Makespan: %7.2fs | Energy: %7.2f | Imbalance: %.4f\n",
                        gen, population[0].fitness, population[0].makespan,
                        population[0].energy, population[0].imbalance);
            }
        }

        // --- Final results ---
        Schedule best = population[0];
        System.out.println("\n=== FINAL RESULT ===");
        System.out.println("  Best mapping:  " + Arrays.toString(best.taskToVM));
        System.out.printf("  Makespan:      %.2fs\n", best.makespan);
        System.out.printf("  Energy:        %.2f\n", best.energy);
        System.out.printf("  Imbalance:     %.4f\n", best.imbalance);
        System.out.printf("  Fitness:       %.4f\n", best.fitness);
        System.out.println("\n  (Paper's baseline makespan: 115.60s)");

        // --- Verify constraints ---
        System.out.println("\n--- Validation Checks ---");
        boolean valid = true;
        for (int i = 0; i < NUM_TASKS; i++) {
            if (best.taskToVM[i] < 0 || best.taskToVM[i] >= NUM_VMS) {
                System.out.println("  FAIL: Task " + i + " assigned to invalid VM " + best.taskToVM[i]);
                valid = false;
            }
        }
        if (valid)
            System.out.println("  ✓ All tasks assigned to valid VMs [0, " + (NUM_VMS - 1) + "]");
        if (best.fitness >= 0 && best.fitness <= 1)
            System.out.println("  ✓ Fitness in valid range [0, 1]");
        System.out.println("\n=== Test Complete ===");
    }
}
