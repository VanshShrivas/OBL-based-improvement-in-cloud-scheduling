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
        System.out.println("┌─────────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│          OBL-ENHANCED HGA-SSA CLOUD TASK SCHEDULER SIMULATION               │");
        System.out.println("│      Multi-Objective Optimization & Validation using CloudSim 3.0.3         │");
        System.out.println("└─────────────────────────────────────────────────────────────────────────────┘\n");

        long startTime = System.currentTimeMillis();

        // =====================================================================
        // Step 1: Setup — VMs and Tasks (same params for both algorithm & CloudSim)
        // =====================================================================
        Random rng = new Random(RANDOM_SEED);

        double[] powers = { 0.8, 0.9, 1.0, 1.1, 1.2 };
        VMInfo[] vms = new VMInfo[NUM_VMS];
        System.out.println("── Virtual Machine Profiles (5 Heterogeneous Nodes) ───────────────────────────");
        for (int i = 0; i < NUM_VMS; i++) {
            vms[i] = new VMInfo(i, powers[i]);
            System.out.printf("  VM %d -> Speed: %4d MIPS | Power Factor: %.1fx\n",
                    i, (int)(BASE_MIPS * powers[i]), powers[i]);
        }

        int[] taskDurations = new int[NUM_TASKS];
        int totalWorkload = 0;
        System.out.println("\n── Task Workloads (10 Tasks, Range: 10s–100s) ────────────────────────────────");
        for (int i = 0; i < NUM_TASKS; i++) {
            taskDurations[i] = MIN_TASK_DURATION + rng.nextInt(MAX_TASK_DURATION - MIN_TASK_DURATION + 1);
            totalWorkload += taskDurations[i];
            System.out.printf("  Task %-2d -> Workload: %3ds (Cloudlet Length: %6d MI)\n",
                    i, taskDurations[i], taskDurations[i] * BASE_MIPS);
        }
        System.out.printf("  [Total Task Workload Requirement: %ds / %d MI]\n",
                totalWorkload, totalWorkload * BASE_MIPS);

        // =====================================================================
        // Step 2: Run Optimization Algorithm (OBL + GA-SSA Hybrid)
        // =====================================================================
        System.out.println("\n┌─────────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│ OPTIMIZATION PHASE: OBL INITIALIZATION + HYBRID GA-SSA EVOLUTION            │");
        System.out.println("└─────────────────────────────────────────────────────────────────────────────┘");
        System.out.println("  • Phase 1 & 2 : OBL Initial Pool (100 candidates) & Multi-Objective Ranking");
        System.out.println("  • Phase 3     : Elite Seeding (Top 30 candidates seeded into Generation 0)");
        System.out.println("  • Phase 4     : Hybrid GA-SSA Evolution (100 Gens with 30-salp SSA refinement)\n");

        FitnessEvaluator fitnessEval = new FitnessEvaluator(ALPHA, BETA, GAMMA);
        HybridGASSA hybridGASSA = new HybridGASSA(fitnessEval);

        Schedule bestSchedule = hybridGASSA.optimize(vms, taskDurations, new Random(RANDOM_SEED));
        List<ConvergencePoint> convergenceHistory = hybridGASSA.getConvergenceHistory();

        System.out.println("  ┌────────┬──────────────┬──────────────┬─────────────┬─────────────┐");
        System.out.println("  │ Gen    │ Fitness F(i) │ Makespan (s) │ Energy (J)  │ Imbalance   │");
        System.out.println("  ├────────┼──────────────┼──────────────┼─────────────┼─────────────┤");
        for (ConvergencePoint cp : convergenceHistory) {
            if (cp.generation == 0 || cp.generation % 20 == 0 || cp.generation == NUM_GENERATIONS) {
                System.out.printf("  │ Gen %-3d│ %12.4f │ %12.2f │ %11.2f │ %11.4f │\n",
                        cp.generation, cp.fitness, cp.makespan, cp.energy, cp.imbalance);
            }
        }
        System.out.println("  └────────┴──────────────┴──────────────┴─────────────┴─────────────┘");

        long optimizationTime = System.currentTimeMillis() - startTime;

        System.out.println("\n  Optimal Schedule Found:");
        System.out.printf("  • Optimal Task-to-VM Mapping : %s\n", Arrays.toString(bestSchedule.taskToVM));
        System.out.printf("  • Minimized Makespan         : %.2f s\n", bestSchedule.makespan);
        System.out.printf("  • Energy Consumption         : %.2f J\n", bestSchedule.energy);
        System.out.printf("  • Degree of Imbalance (DI)   : %.4f\n", bestSchedule.imbalance);
        System.out.printf("  • Multi-Objective Cost Score : %.4f\n", bestSchedule.fitness);

        // =====================================================================
        // Step 3: CloudSim Simulation — Validate with actual cloud execution
        // =====================================================================
        System.out.println("\n┌─────────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│ CLOUDSIM SIMULATION VALIDATION PHASE                                        │");
        System.out.println("│ Deploying Datacenter, 5 Heterogeneous VMs & 10 Cloudlets in CloudSim 3.0.3  │");
        System.out.println("└─────────────────────────────────────────────────────────────────────────────┘\n");

        try {
            // Silence internal CloudSim event logging for clean presentation output
            Log.disable();

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
            System.out.println("  > Running CloudSim discrete-event execution engine...");
            CloudSim.startSimulation();
            CloudSim.stopSimulation();
            System.out.println("  [OK] CloudSim simulation execution finished successfully.\n");

            // Get results
            List<Cloudlet> results = broker.getCloudletReceivedList();

            // Print CloudSim output table
            System.out.println("  Cloudlet Execution Telemetry:");
            System.out.println("  ┌─────────────┬───────────┬────────┬──────────────┬──────────────┬──────────────┐");
            System.out.println("  │ Cloudlet ID │ Status    │ VM ID  │ Exec Time(s) │ Start Time(s)│ Finish Time  │");
            System.out.println("  ├─────────────┼───────────┼────────┼──────────────┼──────────────┼──────────────┤");

            double cloudsimMakespan = 0.0;
            for (Cloudlet cloudlet : results) {
                String status = (cloudlet.getStatus() == Cloudlet.SUCCESS) ? "SUCCESS" : "FAILED";
                double finishTime = cloudlet.getFinishTime();
                if (finishTime > cloudsimMakespan) {
                    cloudsimMakespan = finishTime;
                }

                System.out.printf("  │ Cloudlet %-2d │ %-9s │ VM %-3d │ %12.2f │ %12.2f │ %12.2f │\n",
                        cloudlet.getCloudletId(),
                        status,
                        cloudlet.getVmId(),
                        cloudlet.getActualCPUTime(),
                        cloudlet.getExecStartTime(),
                        finishTime);
            }
            System.out.println("  └─────────────┴───────────┴────────┴──────────────┴──────────────┴──────────────┘");

            // Compare our calculation vs CloudSim
            System.out.println("\n  Validation & Benchmark Comparison:");
            System.out.println("  ┌──────────────────────────────────────┬──────────────────────────────────────┐");
            System.out.printf("  │ Theoretical Calculated Makespan      │ %-36s │\n", String.format("%.2f s", bestSchedule.makespan));
            System.out.printf("  │ CloudSim 3.0.3 Actual Makespan       │ %-36s │\n", String.format("%.2f s", cloudsimMakespan));
            double diff = Math.abs(bestSchedule.makespan - cloudsimMakespan);
            if (diff < 1.0) {
                System.out.printf("  │ Simulation Fidelity                  │ %-36s │\n",
                        String.format("[MATCH] Diff: %.4fs", diff));
            } else {
                System.out.printf("  │ Simulation Fidelity                  │ %-36s │\n",
                        String.format("[MISMATCH] Diff: %.2fs", diff));
            }

            System.out.printf("  │ ICTCS 2025 Paper Baseline            │ %-36s │\n",
                    String.format("%.2f s", ResultExporter.BASELINE_MAKESPAN_SECONDS));
            double gain = ResultExporter.BASELINE_MAKESPAN_SECONDS - bestSchedule.makespan;
            double gainPct = (gain / ResultExporter.BASELINE_MAKESPAN_SECONDS) * 100.0;
            if (bestSchedule.makespan <= ResultExporter.BASELINE_MAKESPAN_SECONDS) {
                System.out.printf("  │ Benchmark Result                     │ %-36s │\n",
                        String.format("[PASS] Beats by %.2fs (+%.2f%%)", gain, gainPct));
            } else {
                System.out.printf("  │ Benchmark Result                     │ %-36s │\n",
                        String.format("Within %.2fs of baseline", -gain));
            }

            int genHitBaseline = -1;
            for (ConvergencePoint cp : convergenceHistory) {
                if (cp.makespan <= ResultExporter.BASELINE_MAKESPAN_SECONDS + 1e-4) {
                    genHitBaseline = cp.generation;
                    break;
                }
            }
            double genDiffPct = genHitBaseline >= 0 ?
                    ((double)(NUM_GENERATIONS - genHitBaseline) / NUM_GENERATIONS) * 100.0 : 0.0;
            if (genHitBaseline >= 0) {
                System.out.printf("  │ Convergence Gen Diff                 │ %-36s │\n",
                        String.format("Gen %d vs 100 (+%.2f%% speedup)", genHitBaseline, genDiffPct));
            }
            System.out.println("  └──────────────────────────────────────┴──────────────────────────────────────┘");

            // =====================================================================
            // Step 4: Export results to CSV for Python analysis
            // =====================================================================
            System.out.println("\n── Exporting Research Artifacts ───────────────────────────────────────────────");
            String resultsDir = ResultExporter.DEFAULT_RESULTS_DIR;
            ResultExporter.exportConvergence(convergenceHistory, resultsDir + "/convergence.csv");
            ResultExporter.exportBestSchedule(bestSchedule, vms, taskDurations, resultsDir + "/best_schedule.csv");
            ResultExporter.exportSummary(bestSchedule, optimizationTime, resultsDir + "/summary.txt", genHitBaseline, genDiffPct);

            System.out.println("  [OK] Exported: results/convergence.csv");
            System.out.println("  [OK] Exported: results/best_schedule.csv");
            System.out.println("  [OK] Exported: results/summary.txt");

        } catch (Exception e) {
            System.err.println("CloudSim simulation error: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("\n[OK] Primary Simulation Finished Successfully.\n");
    }
}
