package org.cloudscheduler.algorithm;

import java.util.Arrays;
import java.util.Random;

/**
 * Opposition-Based Learning (OBL) Initializer for Cloud Task Scheduling.
 * 
 * Generates an initial candidate pool of schedules by combining random
 * schedules with their mathematical opposite schedules:
 * x_tilde = Min_VM + Max_VM - x
 */

public class OBLInitializer {
    
    // Default configuration parameters according to the paper & project specs
    public static final int DEFAULT_RANDOM_COUNT = 50;  // 50 random schedules
    public static final int DEFAULT_NUM_TASKS = 10;     // 10 tasks (indices 0 to 9)
    public static final int DEFAULT_MIN_VM = 0;         // Minimum VM index
    public static final int DEFAULT_MAX_VM = 4;         // Maximum VM index (5 VMs: 0 to 4)

    // Generates a single random schedule mapping each task to a VM.
    public static int[] generateRandomSchedule(int numTasks, int minVM, int maxVM, Random random) {
        int [] schedule = new int[numTasks];
        int vmRange = (maxVM - minVM) + 1;      // +1 due to zero based indexing

        for(int taskId = 0; taskId < numTasks; taskId++) {
            schedule[taskId] = minVM + random.nextInt(vmRange);
        }
        return schedule;
    }

    // Calculates the "opposite schedule" using OBL
    // Formula : x_tilde = Min_VM + Max_VM - x
    public static int[] calculateOppositeSchedule(int[] schedule, int minVM, int maxVM) {
        int[] opposite = new int[schedule.length];

        for(int taskId = 0; taskId < schedule.length; taskId++) {
            opposite[taskId] = (minVM + maxVM) - schedule[taskId];
        }
        return opposite;
    }

    /* Generates the full OBL pool of 100 schedules:
       50 random schedules(index 0 to 49) and 50 Opposite schedule(index 50 to 99)
    */ 
    public static int[][] generateOBLPool(int randomCount, int numTasks, int minVM, int maxVM, Random random) {
        int totalCandidates = randomCount *2;
        int[][] pool = new int[totalCandidates][numTasks];

        for(int i=0; i<randomCount; i++) {
            // 1. generate random candidate x
            int[] randomSchedule = generateRandomSchedule(numTasks, minVM, maxVM, random);
            pool[i] = randomSchedule;

            // 2. generate opposite candidate x_tilde
            int[] oppositeSchedule = calculateOppositeSchedule(randomSchedule, minVM, maxVM);
            pool[randomCount + i] = oppositeSchedule;
        }

        return pool;
    }

    /*
      Generates the 100 OBL candidate schedules wrapped as Schedule objects.
     */
    public static org.cloudscheduler.model.Schedule[] generateOBLPoolAsSchedules(Random random) {
        int[][] rawPool = generateOBLPool(random);
        org.cloudscheduler.model.Schedule[] schedules = new org.cloudscheduler.model.Schedule[rawPool.length];
        for (int i = 0; i < rawPool.length; i++) {
            schedules[i] = new org.cloudscheduler.model.Schedule(rawPool[i]);
        }
        return schedules;
    }

    // Overloaded convenience method using project default parameters.
    public static int[][] generateOBLPool(Random random) {
        return generateOBLPool(DEFAULT_RANDOM_COUNT, DEFAULT_NUM_TASKS, DEFAULT_MIN_VM, DEFAULT_MAX_VM, random);
    }

        public static void main(String[] args) {
        Random rng = new Random(42);

        // 1. Synthetic dataset matching the paper (10 tasks between 10-100s, 5 VMs between 0.8-1.2)
        double[] taskDurations = {22, 99, 62, 21, 70, 98, 12, 54, 33, 43}; // 10 tasks
        org.cloudscheduler.model.VMInfo[] vms = new org.cloudscheduler.model.VMInfo[] {
            new org.cloudscheduler.model.VMInfo(0, 1.01),
            new org.cloudscheduler.model.VMInfo(1, 0.96),
            new org.cloudscheduler.model.VMInfo(2, 0.97),
            new org.cloudscheduler.model.VMInfo(3, 1.00),
            new org.cloudscheduler.model.VMInfo(4, 0.86)
        };

        System.out.println("=== 1. Step A: OBL Pool Generation ===");
        org.cloudscheduler.model.Schedule[] pool = generateOBLPoolAsSchedules(rng);
        System.out.println("Generated candidate schedules: " + pool.length);

        System.out.println("\n=== 2. Step B: Multi-Objective Fitness Evaluation ===");
        FitnessEvaluator evaluator = new FitnessEvaluator(0.4, 0.3, 0.3);
        evaluator.evaluateAll(pool, taskDurations, vms);
        System.out.println("All 100 schedules evaluated and normalized.");

        System.out.println("\n=== 3. Step C: Elite Seeding (Top 30 -> Generation 0) ===");
        org.cloudscheduler.model.Schedule[] genZero = EliteSelector.selectGenerationZero(pool);
        System.out.println("Elite Generation 0 size: " + genZero.length);

        System.out.println("\n--- Best Schedule in Generation 0 ---");
        System.out.println("Task-to-VM Mapping: " + java.util.Arrays.toString(genZero[0].getTaskToVM()));
        System.out.printf("Fitness F(i)       : %.4f\n", genZero[0].getFitness());
        System.out.printf("Makespan           : %.2f s\n", genZero[0].getMakespan());
        System.out.printf("Energy             : %.2f\n", genZero[0].getEnergy());
        System.out.printf("Imbalance          : %.4f\n", genZero[0].getImbalance());

        System.out.println("\n--- 30th Schedule (Cutoff for Gen 0) ---");
        System.out.printf("Fitness F(i)       : %.4f\n", genZero[29].getFitness());
    }
}