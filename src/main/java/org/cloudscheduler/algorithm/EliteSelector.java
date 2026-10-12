package org.cloudscheduler.algorithm;

import org.cloudscheduler.model.Schedule;

import java.util.Arrays;

/**
 * Step C: Elite Seeding and Selection.
 * 
 * 1. Takes the pool of evaluated schedules.
 * 2. Sorts them in ascending order based on F(i) fitness score (lower is better).
 * 3. Keeps the top N elite schedules (e.g., top 30 for Generation 0, top 2 for GA/SSA elitism).
 */
public class EliteSelector {

    public static final int DEFAULT_GEN_ZERO_SIZE = 30; // 30 elite schedules form Generation 0
    public static final int DEFAULT_ELITE_COUNT    = 2;  // Top 2 elites passed to SSA per generation

    /**
     * Sorts the candidate schedules by fitness (ascending) and extracts the top N elites.
     * Uses deep copies (schedule.copy()) so subsequent GA mutations do not alter elite solutions.
     *
     * @param pool Array of evaluated Schedule objects
     * @param topN Number of elite schedules to retain
     * @return Schedule[] array of the top N schedules
     */
    public static Schedule[] selectElite(Schedule[] pool, int topN) {
        if (pool == null || pool.length == 0) {
            throw new IllegalArgumentException("Schedule pool cannot be null or empty.");
        }
        if (topN <= 0 || topN > pool.length) {
            throw new IllegalArgumentException("topN must be between 1 and " + pool.length + ". Requested: " + topN);
        }

        // Defensive copy before sorting so we don't unexpectedly alter the original array order
        Schedule[] sorted = Arrays.copyOf(pool, pool.length);

        // Sorts using Schedule.compareTo() -> ascending order by fitness score
        Arrays.sort(sorted);

        // Extract the top N solutions using deep copy
        Schedule[] elites = new Schedule[topN];
        for (int i = 0; i < topN; i++) {
            elites[i] = sorted[i].copy();
        }

        return elites;
    }

    /**
     * Convenience method to extract Generation 0 (top 30 from the 100 OBL pool).
     *
     * @param oblPool 100 evaluated candidate schedules
     * @return 30 elite schedules ready for GA
     */
    public static Schedule[] selectGenerationZero(Schedule[] oblPool) {
        return selectElite(oblPool, DEFAULT_GEN_ZERO_SIZE);
    }
}