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

    // Overloaded convenience method using project default parameters.
    public static int[][] generateOBLPool(Random random) {
        return generateOBLPool(DEFAULT_RANDOM_COUNT, DEFAULT_NUM_TASKS, DEFAULT_MIN_VM, DEFAULT_MAX_VM, random);
    }

    public static void main(String[] args) {
        // Set fixed seed (42) for reproducible demonstration
        Random rng = new Random(42);
        System.out.println("=== Running OBL Initializer Verification ===");
        int[][] pool = generateOBLPool(rng);
        System.out.println("Total schedules generated: " + pool.length);
        System.out.println("Tasks per schedule: " + pool[0].length);
        // Verification: display first random schedule and its exact opposite
        int[] firstRandom = pool[0];
        int[] firstOpposite = pool[DEFAULT_RANDOM_COUNT];
        System.out.println("\nSample Random Schedule   (Candidate 0) : " + Arrays.toString(firstRandom));
        System.out.println("Sample Opposite Schedule (Candidate 50): " + Arrays.toString(firstOpposite));
        // Sanity check: sum of each gene pair must equal (Min_VM + Max_VM = 4)
        boolean isMathValid = true;
        for (int j = 0; j < firstRandom.length; j++) {
            if (firstRandom[j] + firstOpposite[j] != (DEFAULT_MIN_VM + DEFAULT_MAX_VM)) {
                isMathValid = false;
                break;
            }
        }
        System.out.println("\nMathematical Validation: " + (isMathValid ? "PASSED (all pairs sum to 4)" : "FAILED"));
    }
}