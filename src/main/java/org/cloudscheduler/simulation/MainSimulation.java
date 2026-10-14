package org.cloudscheduler.simulation;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;

import org.cloudscheduler.algorithm.*;
import org.cloudscheduler.broker.OBLHGASSABroker;
import org.cloudscheduler.model.Schedule;
import org.cloudscheduler.model.VMInfo;
import org.cloudscheduler.output.ResultExporter;
import org.cloudscheduler.output.ResultExporter.ConvergencePoint;

import static org.cloudscheduler.config.SimulationConfig.*;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

/**
 * MainSimulation: Entry point for the OBL-Enhanced HGA-SSA Cloud Task Scheduler.
 *
 * Workflow:
 *   1. Run our optimization algorithm (OBL → GA) to find the best schedule mapping.
 *   2. Initialize CloudSim environment (Datacenter, VMs, Cloudlets).
 *   3. Feed the best mapping into OBLHGASSABroker.
 *   4. Start CloudSim simulation.
 *   5. Print and compare: our calculated metrics vs CloudSim's actual execution.
 *   6. Export results to CSV for Python visualization.
 */
public class MainSimulation {

    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════════════════════════════╗");
        System.out.println("║   OBL-Enhanced HGA-SSA Cloud Task Scheduler Simulation   ║");
        System.out.println("╚═══════════════════════════════════════════════════════════╝\n");

        long startTime = System.currentTimeMillis();

        // =====================================================================
        // Step 1: Setup — VMs and Tasks (same params for both algorithm & CloudSim)
        // =====================================================================
        Random rng = new Random(RANDOM_SEED);

        double[] powers = { 0.8, 0.9, 1.0, 1.1, 1.2 };
        VMInfo[] vms = new VMInfo[NUM_VMS];
        System.out.println("--- Virtual Machines ---");
        for (int i = 0; i < NUM_VMS; i++) {
            vms[i] = new VMInfo(i, powers[i]);
            System.out.printf("  VM %d -> MIPS: %d (power: %.1f)\n", i, (int)(BASE_MIPS * powers[i]), powers[i]);
        }

        int[] taskDurations = new int[NUM_TASKS];
        System.out.println("\n--- Task Workloads ---");
        for (int i = 0; i < NUM_TASKS; i++) {
            taskDurations[i] = MIN_TASK_DURATION + rng.nextInt(MAX_TASK_DURATION - MIN_TASK_DURATION + 1);
            System.out.printf("  Task %d -> duration: %ds (Cloudlet length: %d MI)\n",
                    i, taskDurations[i], taskDurations[i] * BASE_MIPS);
        }

        // =====================================================================
        // Step 2: Run Optimization Algorithm (OBL + GA-SSA Hybrid)
        // =====================================================================
        System.out.println("\n========== OPTIMIZATION PHASE (OBL + GA-SSA HYBRID) ==========\n");

        FitnessEvaluator fitnessEval = new FitnessEvaluator(ALPHA, BETA, GAMMA);
        HybridGASSA hybridGASSA = new HybridGASSA(fitnessEval);

        System.out.println("[Phase 1 & 2] OBL Pool Generation (100 candidates) & Multi-Objective Evaluation...");
        System.out.println("[Phase 3] Elite Seeding (Top 30 -> Generation 0)...");
        System.out.println("[Phase 4] Hybrid GA-SSA Evolution (100 generations with 30-salp SSA refinement)...");

        Schedule bestSchedule = hybridGASSA.optimize(vms, taskDurations, new Random(RANDOM_SEED));
        List<ConvergencePoint> convergenceHistory = hybridGASSA.getConvergenceHistory();

        for (ConvergencePoint cp : convergenceHistory) {
            if (cp.generation == 0 || cp.generation % 20 == 0 || cp.generation == NUM_GENERATIONS) {
                System.out.printf("  Gen %3d -> Fitness: %.4f | Makespan: %7.2fs | Energy: %7.2f | Imbalance: %.4f\n",
                        cp.generation, cp.fitness, cp.makespan, cp.energy, cp.imbalance);
            }
        }

        long optimizationTime = System.currentTimeMillis() - startTime;

        System.out.println("\n--- Algorithm Result ---");
        System.out.println("  Best Mapping : " + Arrays.toString(bestSchedule.taskToVM));
        System.out.printf("  Makespan     : %.2fs\n", bestSchedule.makespan);
        System.out.printf("  Energy       : %.2f\n", bestSchedule.energy);
        System.out.printf("  Imbalance    : %.4f\n", bestSchedule.imbalance);
        System.out.printf("  Fitness      : %.4f\n", bestSchedule.fitness);

        // =====================================================================
        // Step 3: CloudSim Simulation — Validate with actual cloud execution
        // =====================================================================
        System.out.println("\n========== CLOUDSIM VALIDATION PHASE ==========\n");

        try {
            // Initialize CloudSim
            int numUsers = 1;
            Calendar calendar = Calendar.getInstance();
            boolean traceFlag = false;
            CloudSim.init(numUsers, calendar, traceFlag);

            // Create Datacenter
            CloudEnvironment.createDatacenter("Datacenter_0");

            // Create Broker with our optimized mapping
            OBLHGASSABroker broker = new OBLHGASSABroker("OBL_HGA_SSA_Broker");
            broker.setScheduleMapping(bestSchedule.taskToVM);

            // Create VMs and submit to broker
            List<Vm> vmList = CloudEnvironment.createVMs(broker.getId(), powers);
            broker.submitVmList(vmList);

            // Create Cloudlets and submit to broker
            List<Cloudlet> cloudletList = CloudEnvironment.createCloudlets(broker.getId(), taskDurations);
            broker.submitCloudletList(cloudletList);

            // Run the simulation
            System.out.println("Starting CloudSim simulation...");
            CloudSim.startSimulation();
            CloudSim.stopSimulation();

            // Get results
            List<Cloudlet> results = broker.getCloudletReceivedList();

            // Print CloudSim output table
            System.out.println("\n========== CLOUDSIM EXECUTION RESULTS ==========");
            System.out.println(String.format("%-12s %-10s %-8s %-12s %-12s %-12s",
                    "Cloudlet ID", "Status", "VM ID", "Exec Time", "Start Time", "Finish Time"));
            System.out.println("----------------------------------------------------------------------");

            double cloudsimMakespan = 0.0;
            for (Cloudlet cloudlet : results) {
                String status = (cloudlet.getStatus() == Cloudlet.SUCCESS) ? "SUCCESS" : "FAILED";
                double finishTime = cloudlet.getFinishTime();
                if (finishTime > cloudsimMakespan) {
                    cloudsimMakespan = finishTime;
                }

                System.out.println(String.format("%-12d %-10s %-8d %-12.2f %-12.2f %-12.2f",
                        cloudlet.getCloudletId(),
                        status,
                        cloudlet.getVmId(),
                        cloudlet.getActualCPUTime(),
                        cloudlet.getExecStartTime(),
                        finishTime));
            }

            // Compare our calculation vs CloudSim
            System.out.println("\n========== VALIDATION COMPARISON ==========");
            System.out.printf("  Our Calculated Makespan  : %.2fs\n", bestSchedule.makespan);
            System.out.printf("  CloudSim Actual Makespan : %.2fs\n", cloudsimMakespan);
            double diff = Math.abs(bestSchedule.makespan - cloudsimMakespan);
            if (diff < 1.0) {
                System.out.printf("  [MATCH] Difference: %.4fs (within tolerance)\n", diff);
            } else {
                System.out.printf("  [MISMATCH] Difference: %.2fs\n", diff);
            }

            System.out.printf("\n  Paper Baseline Makespan  : %.2fs\n", ResultExporter.BASELINE_MAKESPAN_SECONDS);
            if (bestSchedule.makespan <= ResultExporter.BASELINE_MAKESPAN_SECONDS) {
                System.out.printf("  [SUCCESS] Beats baseline by %.2fs!\n",
                        ResultExporter.BASELINE_MAKESPAN_SECONDS - bestSchedule.makespan);
            } else {
                System.out.printf("  [INFO] Within %.2fs of baseline.\n",
                        bestSchedule.makespan - ResultExporter.BASELINE_MAKESPAN_SECONDS);
            }

            // =====================================================================
            // Step 4: Export results to CSV for Python analysis
            // =====================================================================
            System.out.println("\n========== EXPORTING RESULTS ==========");
            String resultsDir = ResultExporter.DEFAULT_RESULTS_DIR;
            ResultExporter.exportConvergence(convergenceHistory, resultsDir + "/convergence.csv");
            ResultExporter.exportBestSchedule(bestSchedule, vms, taskDurations, resultsDir + "/best_schedule.csv");
            ResultExporter.exportSummary(bestSchedule, optimizationTime, resultsDir + "/summary.txt");

            System.out.println("  Exported: results/convergence.csv");
            System.out.println("  Exported: results/best_schedule.csv");
            System.out.println("  Exported: results/summary.txt");
            System.out.println("\n  Run Python analysis scripts:");
            System.out.println("    python analysis/plot_convergence.py");
            System.out.println("    python analysis/generate_tables.py");
            System.out.println("    python analysis/compare_baseline.py");

        } catch (Exception e) {
            System.err.println("CloudSim simulation error: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("\n========== SIMULATION COMPLETE ==========\n");
    }
}
