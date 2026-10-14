package org.cloudscheduler.algorithm;

import org.cloudscheduler.model.Schedule;
import org.cloudscheduler.model.VMInfo;
import org.cloudscheduler.output.ResultExporter.ConvergencePoint;

import static org.cloudscheduler.config.SimulationConfig.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * HybridGASSA: Orchestrates the GA-SSA hybrid optimization pipeline with or without OBL.
 *
 * Modes:
 *   - With OBL: Generates 50 random + 50 opposite candidate schedules (100 total) -> Elite Seeding -> Gen 0
 *   - Without OBL (Standard Hybrid GA-SSA Baseline): 30 purely random schedules -> Gen 0
 *
 * Evolution Loop:
 *   - GA operators (Tournament, 2-point crossover, mutation, elitism)
 *   - SSA local refinement (30 salps exploring elite neighborhood)
 *   - Replaces weakest individuals
 *   - Records convergence generation & timing
 */
public class HybridGASSA {

    private final FitnessEvaluator fitnessEvaluator;
    private final GeneticAlgorithm geneticAlgorithm;
    private final SalpSwarmAlgorithm salpSwarmAlgorithm;
    private final List<ConvergencePoint> convergenceHistory;

    private int convergenceGeneration = 0;
    private long convergenceTimeMs = 0;
    private long totalTimeMs = 0;

    public HybridGASSA(FitnessEvaluator fitnessEvaluator) {
        this.fitnessEvaluator = fitnessEvaluator;
        this.geneticAlgorithm = new GeneticAlgorithm(fitnessEvaluator);
        this.salpSwarmAlgorithm = new SalpSwarmAlgorithm(fitnessEvaluator);
        this.convergenceHistory = new ArrayList<>();
    }

    public Schedule optimize(VMInfo[] vms, int[] taskDurations, Random rng) {
        return optimize(vms, taskDurations, rng, true);
    }

    public Schedule optimize(VMInfo[] vms, int[] taskDurations, Random rng, boolean useOBL) {
        convergenceHistory.clear();
        long startTime = System.currentTimeMillis();

        int numTasks = taskDurations.length;
        int numVMs = vms.length;

        Schedule[] population;

        if (useOBL) {
            // =====================================================================
            // Phase 1: OBL Initialization (50 random + 50 opposite = 100)
            // =====================================================================
            Schedule[] oblPool = OBLInitializer.generateOBLPoolAsSchedules(
                    OBL_RANDOM_COUNT, numTasks, 0, numVMs - 1, rng);
            fitnessEvaluator.evaluateAll(oblPool, vms, taskDurations);

            // Phase 2: Elite Seeding -> Generation 0 (Top 30)
            population = EliteSelector.selectGenerationZero(oblPool);
        } else {
            // =====================================================================
            // Baseline: Purely Random Initialization (Paper Baseline HGA-SSA)
            // =====================================================================
            population = new Schedule[POPULATION_SIZE];
            for (int i = 0; i < POPULATION_SIZE; i++) {
                population[i] = new Schedule(numTasks);
                for (int t = 0; t < numTasks; t++) {
                    population[i].taskToVM[t] = rng.nextInt(numVMs);
                }
            }
            fitnessEvaluator.evaluateAll(population, vms, taskDurations);
        }

        Arrays.sort(population, (a, b) -> Double.compare(a.fitness, b.fitness));

        // Record Generation 0
        convergenceHistory.add(new ConvergencePoint(
                0, population[0].fitness, population[0].makespan,
                population[0].energy, population[0].imbalance));

        double globalBestFitness = population[0].fitness;
        double globalBestMakespan = population[0].makespan;
        convergenceGeneration = 0;
        convergenceTimeMs = System.currentTimeMillis() - startTime;

        // =====================================================================
        // Phase 3: Hybrid GA-SSA Evolution Loop
        // =====================================================================
        for (int gen = 0; gen < NUM_GENERATIONS; gen++) {
            // Step A: GA Evolution
            population = geneticAlgorithm.evolve(population, vms, taskDurations, rng);
            Arrays.sort(population, (a, b) -> Double.compare(a.fitness, b.fitness));

            // Step B: SSA Refinement on top elites
            Schedule[] elites = new Schedule[] { population[0].copy(), population[1].copy() };
            Schedule[] refined = salpSwarmAlgorithm.refine(
                    elites, gen, NUM_GENERATIONS, vms, taskDurations, rng);

            // Step C: Merge SSA refined solutions
            int popSize = population.length;
            if (refined[0].fitness < population[popSize - 1].fitness) {
                population[popSize - 1] = refined[0];
            }
            if (refined.length > 1 && refined[1].fitness < population[popSize - 2].fitness) {
                population[popSize - 2] = refined[1];
            }

            Arrays.sort(population, (a, b) -> Double.compare(a.fitness, b.fitness));

            // Check if global best improved
            if (population[0].fitness < globalBestFitness - 1e-6 || population[0].makespan < globalBestMakespan - 1e-6) {
                globalBestFitness = population[0].fitness;
                globalBestMakespan = population[0].makespan;
                convergenceGeneration = gen + 1;
                convergenceTimeMs = System.currentTimeMillis() - startTime;
            }

            convergenceHistory.add(new ConvergencePoint(
                    gen + 1, population[0].fitness, population[0].makespan,
                    population[0].energy, population[0].imbalance));
        }

        totalTimeMs = System.currentTimeMillis() - startTime;
        return population[0];
    }

    public List<ConvergencePoint> getConvergenceHistory() {
        return convergenceHistory;
    }

    public int getConvergenceGeneration() {
        return convergenceGeneration;
    }

    public long getConvergenceTimeMs() {
        return convergenceTimeMs;
    }

    public long getTotalTimeMs() {
        return totalTimeMs;
    }
}
