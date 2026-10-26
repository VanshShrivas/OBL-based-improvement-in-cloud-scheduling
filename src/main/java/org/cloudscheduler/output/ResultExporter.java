package org.cloudscheduler.output;

import org.cloudscheduler.metrics.MetricsCalculator;
import org.cloudscheduler.model.Schedule;
import org.cloudscheduler.model.VMInfo;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Locale;

/**
 * ResultExporter handles persistent output generation for the cloud scheduler.
 * Exports:
 *   1. convergence.csv   -> Per-generation best fitness, makespan, energy, imbalance
 *   2. best_schedule.csv -> Task-to-VM assignments and VM execution times
 *   3. summary.txt       -> Human-readable performance report and comparison vs baseline
 */

public class ResultExporter {

    public static final String DEFAULT_RESULTS_DIR = "results";
    public static final double BASELINE_MAKESPAN_SECONDS = 115.60; // Baseline from ICTCS 2025 paper

    /**
     * Data carrier representing a single generation's best metrics.
     */
    public static class ConvergencePoint {
        public final int generation;
        public final double fitness;
        public final double makespan;
        public final double energy;
        public final double imbalance;

        public ConvergencePoint(int generation, double fitness, double makespan, double energy, double imbalance) {
            this.generation = generation;
            this.fitness = fitness;
            this.makespan = makespan;
            this.energy = energy;
            this.imbalance = imbalance;
        }
    }

    /**
     * Exports the convergence trajectory over generations to CSV.
     * Columns: generation,fitness,makespan,energy,imbalance
     *
     * @param history  List of ConvergencePoint per generation
     * @param filePath Target CSV file path (e.g., "results/convergence.csv")
     */
    public static void exportConvergence(List<ConvergencePoint> history, String filePath) throws IOException {
        ensureParentDirectoryExists(filePath);
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            writer.println("generation,fitness,makespan,energy,imbalance");
            for (ConvergencePoint point : history) {
                writer.printf(Locale.US, "%d,%.6f,%.4f,%.4f,%.6f%n",
                        point.generation,
                        point.fitness,
                        point.makespan,
                        point.energy,
                        point.imbalance);
            }
        }
    }

    /**
     * Exports task-to-VM schedule details to CSV.
     * Columns: task_id,vm_id,duration,vm_power,task_exec_time,vm_total_time
     *
     * @param bestSchedule  Best schedule found by optimization
     * @param vms           VM configuration array
     * @param taskDurations Task duration array
     * @param filePath      Target CSV file path (e.g., "results/best_schedule.csv")
     */
    public static void exportBestSchedule(Schedule bestSchedule, VMInfo[] vms, int[] taskDurations, String filePath) throws IOException {
        ensureParentDirectoryExists(filePath);
        // Convert int[] durations to double[] for load calculation
        double[] doubleDurations = new double[taskDurations.length];
        for (int i = 0; i < taskDurations.length; i++) {
            doubleDurations[i] = taskDurations[i];
        }
        double[] vmLoads = MetricsCalculator.calculateVMLoads(bestSchedule, doubleDurations, vms);
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            writer.println("task_id,vm_id,duration,vm_power,task_exec_time,vm_total_time");
            for (int taskId = 0; taskId < bestSchedule.getNumTasks(); taskId++) {
                int vmId = bestSchedule.getVMForTask(taskId);
                double duration = taskDurations[taskId];
                double power = vms[vmId].getProcessingPower();
                double taskExecTime = duration / power;
                double vmTotalTime = vmLoads[vmId];
                writer.printf(Locale.US, "%d,%d,%.2f,%.2f,%.4f,%.4f%n",
                        taskId, vmId, duration, power, taskExecTime, vmTotalTime);
            }
        }
    }

    /**
     * Exports a text summary comparing our results against the research paper baseline (115.60s).
     *
     * @param bestSchedule     Best schedule found
     * @param optimizationTimeMs Time taken by the algorithm in milliseconds
     * @param filePath         Target file path (e.g., "results/summary.txt")
     */
    public static void exportSummary(Schedule bestSchedule, long optimizationTimeMs, String filePath) throws IOException {
        exportSummary(bestSchedule, optimizationTimeMs, filePath, -1, 0.0);
    }

    public static void exportSummary(Schedule bestSchedule, long optimizationTimeMs, String filePath, int genHitBaseline, double genDiffPct) throws IOException {
        ensureParentDirectoryExists(filePath);
        double makespanReduction = ((BASELINE_MAKESPAN_SECONDS - bestSchedule.getMakespan()) / BASELINE_MAKESPAN_SECONDS) * 100.0;
        double optTimeSeconds = optimizationTimeMs / 1000.0;
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            writer.println("================================================================================");
            writer.println("   OBL-ENHANCED HGA-SSA CLOUD TASK SCHEDULER - SIMULATION SUMMARY");
            writer.println("================================================================================");
            writer.printf(Locale.US, "Status                   : COMPLETED%n");
            writer.printf(Locale.US, "Optimization Time        : %.3f seconds (%d ms)%n", optTimeSeconds, optimizationTimeMs);
            writer.println("--------------------------------------------------------------------------------");
            writer.println("PROPOSED MODEL METRICS:");
            writer.printf(Locale.US, "  - Multi-Objective Fitness F(i) : %.6f%n", bestSchedule.getFitness());
            writer.printf(Locale.US, "  - Makespan (Max Execution Time) : %.2f seconds%n", bestSchedule.getMakespan());
            writer.printf(Locale.US, "  - Energy Consumption            : %.2f units%n", bestSchedule.getEnergy());
            writer.printf(Locale.US, "  - Degree of Imbalance (DI)      : %.4f%n", bestSchedule.getImbalance());
            writer.println("--------------------------------------------------------------------------------");
            writer.println("BENCHMARK COMPARISON AGAINST PAPER (ICTCS 2025):");
            writer.printf(Locale.US, "  - Baseline HGA-SSA Makespan    : %.2f seconds%n", BASELINE_MAKESPAN_SECONDS);
            writer.printf(Locale.US, "  - Proposed OBL-HGA-SSA Makespan: %.2f seconds%n", bestSchedule.getMakespan());
            if (makespanReduction > 0) {
                writer.printf(Locale.US, "  - Makespan Improvement         : %.2f%% REDUCTION (Outperformed baseline)%n", makespanReduction);
            } else {
                writer.printf(Locale.US, "  - Makespan Difference          : %.2f%%%n", makespanReduction);
            }
            if (genHitBaseline >= 0) {
                writer.printf(Locale.US, "  - Convergence Target Reached   : Generation %d / 100 (Gen Diff: +%.2f%% speedup)%n",
                        genHitBaseline, genDiffPct);
            }
            writer.println("================================================================================");
        }
    }

    /**
     * Helper to create parent directories if they don't already exist.
     */
    private static void ensureParentDirectoryExists(String filePath) {
        File file = new File(filePath);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
    }

    /**
     * Self-contained verification runner to test CSV and summary generation.
     */
    public static void main(String[] args) {
        System.out.println("=== Verifying ResultExporter ===");
        try {
            // 1. Create mock VM and Task data
            VMInfo[] vms = new VMInfo[] {
                new VMInfo(0, 1.01),
                new VMInfo(1, 0.96),
                new VMInfo(2, 0.97),
                new VMInfo(3, 1.00),
                new VMInfo(4, 0.86)
            };
            int[] durations = { 22, 99, 62, 21, 70, 98, 12, 54, 33, 43 };
            // 2. Create mock schedule and calculate metrics
            Schedule mockSchedule = new Schedule(new int[] { 3, 4, 1, 3, 0, 2, 0, 3, 0, 1 });
            mockSchedule.setFitness(0.1876);
            MetricsCalculator.computeAndSetMetrics(mockSchedule, durations, vms);
            // 3. Create mock convergence history
            List<ConvergencePoint> mockHistory = List.of(
                new ConvergencePoint(0, 0.4500, 155.20, 580.0, 1.10),
                new ConvergencePoint(25, 0.3200, 134.50, 520.0, 0.85),
                new ConvergencePoint(50, 0.2400, 120.10, 495.0, 0.65),
                new ConvergencePoint(75, 0.1950, 114.80, 482.0, 0.55),
                new ConvergencePoint(100, 0.1876, mockSchedule.getMakespan(), mockSchedule.getEnergy(), mockSchedule.getImbalance())
            );
            // 4. Export files
            exportConvergence(mockHistory, DEFAULT_RESULTS_DIR + "/convergence.csv");
            exportBestSchedule(mockSchedule, vms, durations, DEFAULT_RESULTS_DIR + "/best_schedule.csv");
            exportSummary(mockSchedule, 180, DEFAULT_RESULTS_DIR + "/summary.txt");
            System.out.println("Successfully generated:");
            System.out.println("  - results/convergence.csv");
            System.out.println("  - results/best_schedule.csv");
            System.out.println("  - results/summary.txt");
            System.out.println("\nVerification PASSED!");
        } catch (Exception e) {
            System.err.println("Error during verification: " + e.getMessage());
            e.printStackTrace();
        }
    }

}