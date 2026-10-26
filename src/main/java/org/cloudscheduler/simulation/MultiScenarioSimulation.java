package org.cloudscheduler.simulation;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudscheduler.algorithm.FitnessEvaluator;
import org.cloudscheduler.algorithm.HybridGASSA;
import org.cloudscheduler.broker.OBLHGASSABroker;
import org.cloudscheduler.model.Schedule;
import org.cloudscheduler.model.VMInfo;
import org.cloudscheduler.output.ResultExporter.ConvergencePoint;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.*;

/**
 * MultiScenarioSimulation:
 * Benchmarks Standard Hybrid GA-SSA vs OBL-Enhanced Hybrid GA-SSA across
 * 3 distinct VM and Task configurations focusing on Makespan.
 */
public class MultiScenarioSimulation {

    public static class ScenarioResult {
        public String scenarioName;
        public int numTasks;
        public int numVMs;
        
        // Gen 0 Initial Quality
        public double gaSsaGen0Makespan;
        public double oblGen0Makespan;
        public double initialImprovementPct;

        // Final Results
        public double gaSsaMakespanCalc;
        public double gaSsaMakespanCloudSim;
        public double oblMakespanCalc;
        public double oblMakespanCloudSim;
        public double makespanReductionPct;

        // Convergence Speed
        public int genOBLHitsBaseline;   // Generation where OBL achieves what Baseline took 100 gens to get
        public int gaSsaConvGen;
        public int oblConvGen;
        public double genDiffPct;
        public long gaSsaTotalTimeMs;
        public long oblTotalTimeMs;

        // Trajectories
        public List<ConvergencePoint> baseHistory;
        public List<ConvergencePoint> oblHistory;
    }

    public static void main(String[] args) {
        System.out.println("┌─────────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│          MULTI-SCENARIO SCALABILITY BENCHMARK (3 WORKLOAD TIERS)            │");
        System.out.println("│      Comparing Baseline Hybrid GA-SSA vs. Proposed OBL + Hybrid GA-SSA      │");
        System.out.println("└─────────────────────────────────────────────────────────────────────────────┘\n");

        List<ScenarioResult> results = new ArrayList<>();

        // ---------------------------------------------------------------------
        // Scenario 1: Standard Paper Scale (10 Tasks, 5 Heterogeneous VMs)
        // ---------------------------------------------------------------------
        double[] powers1 = { 0.8, 0.9, 1.0, 1.1, 1.2 };
        int[] tasks1 = { 88, 43, 58, 48, 36, 14, 88, 13, 65, 90 };
        results.add(runScenario("Small (10T/5VM)", powers1, tasks1, 42L));

        // ---------------------------------------------------------------------
        // Scenario 2: Medium Workload (25 Tasks, 5 Heterogeneous VMs)
        // ---------------------------------------------------------------------
        double[] powers2 = { 0.6, 0.8, 1.0, 1.2, 1.5 };
        int[] tasks2 = generateTasks(25, 20, 150, new Random(101));
        results.add(runScenario("Medium (25T/5VM)", powers2, tasks2, 101L));

        // ---------------------------------------------------------------------
        // Scenario 3: Large Workload (50 Tasks, 10 Heterogeneous VMs)
        // ---------------------------------------------------------------------
        double[] powers3 = { 0.5, 0.7, 0.8, 1.0, 1.0, 1.2, 1.4, 1.5, 1.8, 2.0 };
        int[] tasks3 = generateTasks(50, 15, 200, new Random(202));
        results.add(runScenario("Large (50T/10VM)", powers3, tasks3, 202L));

        // ---------------------------------------------------------------------
        // Print Summary Table
        // ---------------------------------------------------------------------
        printSummary(results);

        // Export CSVs
        System.out.println("\n── Exporting Multi-Scenario Research Artifacts ───────────────────────────────");
        exportResultsCSV(results, "results/multi_scenario_comparison.csv");
        exportTrajectoriesCSV(results, "results/multi_scenario_trajectories.csv");
        System.out.println("\n[OK] Multi-Scenario Benchmark Completed Successfully.\n");
    }

    private static ScenarioResult runScenario(String name, double[] powers, int[] taskDurations, long seed) {
        int totalDuration = Arrays.stream(taskDurations).sum();
        System.out.println("┌─────────────────────────────────────────────────────────────────────────────┐");
        System.out.printf("│ %-75s │\n", name);
        System.out.printf("│ Workload: %-2d Tasks | %-2d Heterogeneous VMs | Total Workload: %-6d s         │\n",
                taskDurations.length, powers.length, totalDuration);
        System.out.println("└─────────────────────────────────────────────────────────────────────────────┘");

        VMInfo[] vms = new VMInfo[powers.length];
        for (int i = 0; i < powers.length; i++) {
            vms[i] = new VMInfo(i, powers[i]);
        }

        // Pure Makespan-focused fitness: alpha = 1.0, beta = 0.0, gamma = 0.0
        FitnessEvaluator fitnessEval = new FitnessEvaluator(1.0, 0.0, 0.0);

        // 1. Run Baseline: Hybrid GA-SSA (NO OBL)
        System.out.print("  > Simulating Baseline HGA-SSA (Random Initialization)... ");
        HybridGASSA gaSsa = new HybridGASSA(fitnessEval);
        Schedule bestGaSsa = gaSsa.optimize(vms, taskDurations, new Random(seed), false);
        double gaSsaCloudSimMakespan = runCloudSim("GA_SSA_Broker", powers, taskDurations, bestGaSsa.taskToVM);
        System.out.println("Complete.");

        // 2. Run Proposed: OBL + Hybrid GA-SSA (WITH OBL)
        System.out.print("  > Simulating Proposed OBL-Enhanced HGA-SSA (Opposition Seeding)... ");
        HybridGASSA oblGaSsa = new HybridGASSA(fitnessEval);
        Schedule bestObl = oblGaSsa.optimize(vms, taskDurations, new Random(seed), true);
        double oblCloudSimMakespan = runCloudSim("OBL_GA_SSA_Broker", powers, taskDurations, bestObl.taskToVM);
        System.out.println("Complete.");

        List<ConvergencePoint> baseHist = gaSsa.getConvergenceHistory();
        List<ConvergencePoint> oblHist = oblGaSsa.getConvergenceHistory();

        ScenarioResult r = new ScenarioResult();
        r.scenarioName = name;
        r.numTasks = taskDurations.length;
        r.numVMs = powers.length;
        r.baseHistory = baseHist;
        r.oblHistory = oblHist;

        r.gaSsaGen0Makespan = baseHist.get(0).makespan;
        r.oblGen0Makespan = oblHist.get(0).makespan;
        r.initialImprovementPct = ((r.gaSsaGen0Makespan - r.oblGen0Makespan) / r.gaSsaGen0Makespan) * 100.0;

        r.gaSsaMakespanCalc = bestGaSsa.makespan;
        r.gaSsaMakespanCloudSim = gaSsaCloudSimMakespan;
        r.gaSsaConvGen = gaSsa.getConvergenceGeneration();
        r.gaSsaTotalTimeMs = gaSsa.getTotalTimeMs();

        r.oblMakespanCalc = bestObl.makespan;
        r.oblMakespanCloudSim = oblCloudSimMakespan;
        r.oblConvGen = oblGaSsa.getConvergenceGeneration();
        r.oblTotalTimeMs = oblGaSsa.getTotalTimeMs();

        r.makespanReductionPct = ((r.gaSsaMakespanCalc - r.oblMakespanCalc) / r.gaSsaMakespanCalc) * 100.0;

        // Find earliest generation where OBL matches or beats baseline final makespan
        r.genOBLHitsBaseline = -1;
        for (int g = 0; g < oblHist.size(); g++) {
            if (oblHist.get(g).makespan <= r.gaSsaMakespanCalc + 1e-4) {
                r.genOBLHitsBaseline = g;
                break;
            }
        }
        if (r.genOBLHitsBaseline == -1) r.genOBLHitsBaseline = oblHist.size() - 1;

        int genDiff = r.gaSsaConvGen - r.genOBLHitsBaseline;
        r.genDiffPct = r.gaSsaConvGen > 0 ? ((double) genDiff / r.gaSsaConvGen) * 100.0 : 0.0;

        System.out.printf("  • Gen 0 Initial Quality : Baseline %6.1fs vs OBL %6.1fs (OBL starts %+.1f%% better)\n",
                r.gaSsaGen0Makespan, r.oblGen0Makespan, r.initialImprovementPct);
        System.out.printf("  • Final Makespan Target : Baseline %6.2fs vs OBL %6.2fs (Makespan Gain: %+.2f%%)\n",
                r.gaSsaMakespanCalc, r.oblMakespanCalc, r.makespanReductionPct);
        System.out.printf("  • Convergence Speedup   : OBL reached Baseline target in Gen %d vs Gen %d (Gen Diff: %+d gens, %+.2f%%)\n\n",
                r.genOBLHitsBaseline, r.gaSsaConvGen, genDiff, r.genDiffPct);

        return r;
    }

    private static double runCloudSim(String brokerName, double[] powers, int[] taskDurations, int[] scheduleMapping) {
        try {
            // Silence internal CloudSim traces for clean presentation
            Log.disable();

            int numUsers = 1;
            Calendar calendar = Calendar.getInstance();
            CloudSim.init(numUsers, calendar, false);

            CloudEnvironment.createDatacenter("Datacenter_" + brokerName);
            OBLHGASSABroker broker = new OBLHGASSABroker(brokerName);
            broker.setScheduleMapping(scheduleMapping);

            List<Vm> vms = CloudEnvironment.createVMs(broker.getId(), powers);
            broker.submitVmList(vms);

            List<Cloudlet> cloudlets = CloudEnvironment.createCloudlets(broker.getId(), taskDurations);
            broker.submitCloudletList(cloudlets);

            CloudSim.startSimulation();
            CloudSim.stopSimulation();

            List<Cloudlet> finished = broker.getCloudletReceivedList();
            double actualMakespan = 0.0;
            for (Cloudlet c : finished) {
                if (c.getFinishTime() > actualMakespan) {
                    actualMakespan = c.getFinishTime();
                }
            }
            return actualMakespan;
        } catch (Exception e) {
            System.err.println("CloudSim error: " + e.getMessage());
            return -1.0;
        }
    }

    private static int[] generateTasks(int count, int minDur, int maxDur, Random rng) {
        int[] tasks = new int[count];
        for (int i = 0; i < count; i++) {
            tasks[i] = minDur + rng.nextInt(maxDur - minDur + 1);
        }
        return tasks;
    }

    private static void printSummary(List<ScenarioResult> list) {
        System.out.println("\n  FINAL BENCHMARK SUMMARY: Baseline HGA-SSA vs. Proposed OBL+HGA-SSA");
        System.out.println("  ┌──────────────────┬───────────┬─────────────────────┬────────────┬───────────┐");
        System.out.println("  │ Scenario         │ Scale     │ Final Makespan (s)  │   Makespan │ Gen Diff% │");
        System.out.println("  │ Configuration    │           │ Baseline / OBL      │   Gain (%) │ (Speedup) │");
        System.out.println("  ├──────────────────┼───────────┼─────────────────────┼────────────┼───────────┤");

        for (ScenarioResult r : list) {
            String scaleStr = String.format("%dT / %dVM", r.numTasks, r.numVMs);
            String finalStr = String.format("%7.2f / %7.2f", r.gaSsaMakespanCalc, r.oblMakespanCalc);
            String gainStr  = String.format("%+.2f%%", r.makespanReductionPct);
            String speedStr = String.format("%+.2f%%", r.genDiffPct);

            System.out.printf("  │ %-16s │ %-9s │ %-19s │ %10s │ %9s │\n",
                    r.scenarioName, scaleStr, finalStr, gainStr, speedStr);
        }
        System.out.println("  └──────────────────┴───────────┴─────────────────────┴────────────┴───────────┘");
    }

    private static void exportResultsCSV(List<ScenarioResult> list, String filename) {
        try {
            File f = new File(filename);
            f.getParentFile().mkdirs();
            try (PrintWriter pw = new PrintWriter(new FileWriter(f))) {
                pw.println("scenario,tasks,vms,base_gen0_makespan,obl_gen0_makespan,base_final_makespan,obl_final_makespan,base_cloudsim_makespan,obl_cloudsim_makespan,makespan_reduction_pct,gen_obl_hits_baseline,base_conv_gen,gen_diff_pct");
                for (ScenarioResult r : list) {
                    pw.printf(Locale.US, "%s,%d,%d,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%d,%d,%.2f\n",
                            r.scenarioName, r.numTasks, r.numVMs,
                            r.gaSsaGen0Makespan, r.oblGen0Makespan,
                            r.gaSsaMakespanCalc, r.oblMakespanCalc,
                            r.gaSsaMakespanCloudSim, r.oblMakespanCloudSim,
                            r.makespanReductionPct, r.genOBLHitsBaseline, r.gaSsaConvGen, r.genDiffPct);
                }
            }
            System.out.println("  [OK] Exported summary: " + filename);
        } catch (Exception e) {
            System.err.println("Error saving summary CSV: " + e.getMessage());
        }
    }

    private static void exportTrajectoriesCSV(List<ScenarioResult> list, String filename) {
        try {
            File f = new File(filename);
            f.getParentFile().mkdirs();
            try (PrintWriter pw = new PrintWriter(new FileWriter(f))) {
                pw.println("scenario,generation,base_makespan,obl_makespan");
                for (ScenarioResult r : list) {
                    int gens = Math.min(r.baseHistory.size(), r.oblHistory.size());
                    for (int g = 0; g < gens; g++) {
                        pw.printf(Locale.US, "%s,%d,%.2f,%.2f\n",
                                r.scenarioName, g,
                                r.baseHistory.get(g).makespan,
                                r.oblHistory.get(g).makespan);
                    }
                }
            }
            System.out.println("  [OK] Exported trajectories: " + filename);
        } catch (Exception e) {
            System.err.println("Error saving trajectories CSV: " + e.getMessage());
        }
    }
}
