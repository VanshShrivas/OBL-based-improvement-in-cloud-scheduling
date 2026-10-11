package org.cloudscheduler.metrics;

import org.cloudscheduler.model.Schedule;
import org.cloudscheduler.model.VMInfo;

import static org.cloudscheduler.config.SimulationConfig.*;

/**
 * Computes raw metrics for a schedule:
 *   - Makespan: max completion time across all VMs
 *   - Energy: total power consumed (Σ processing_power × time_on_vm)
 *   - Imbalance: (max_load - min_load) / avg_load
 */
public class MetricsCalculator {

    /**
     * Calculates the completion time for each VM given a schedule.
     * time_vm = Σ(task_duration) / processing_power_vm
     *
     * @return array of completion times, one per VM
     */
    public double[] computeVMTimes(Schedule schedule, VMInfo[] vms, int[] taskDurations) {
        double[] vmTimes = new double[NUM_VMS];

        // Sum task durations assigned to each VM
        for (int task = 0; task < NUM_TASKS; task++) {
            int vmId = schedule.taskToVM[task];
            vmTimes[vmId] += taskDurations[task];
        }

        // Divide by processing power to get actual execution time
        for (int vm = 0; vm < NUM_VMS; vm++) {
            vmTimes[vm] = vmTimes[vm] / vms[vm].processingPower;
        }

        return vmTimes;
    }

    /**
     * Makespan = max(time_vm for all VMs)
     * The time when the LAST VM finishes — determines total schedule length.
     */
    public double computeMakespan(double[] vmTimes) {
        double max = Double.MIN_VALUE;
        for (double t : vmTimes) {
            if (t > max) max = t;
        }
        return max;
    }

    /**
     * Energy = Σ(processing_power_vm × total_task_time_on_vm) for all VMs
     * Higher processing power VMs consume more energy per second.
     */
    public double computeEnergy(double[] vmTimes, VMInfo[] vms) {
        double energy = 0.0;
        for (int vm = 0; vm < NUM_VMS; vm++) {
            energy += vms[vm].processingPower * vmTimes[vm];
        }
        return energy;
    }

    /**
     * Imbalance = (max_load - min_load) / avg_load
     * 0.0 = perfectly balanced, higher = worse.
     */
    public double computeImbalance(double[] vmTimes) {
        double max = Double.MIN_VALUE;
        double min = Double.MAX_VALUE;
        double sum = 0.0;

        for (double t : vmTimes) {
            if (t > max) max = t;
            if (t < min) min = t;
            sum += t;
        }

        double avg = sum / vmTimes.length;
        if (avg == 0.0) return 0.0;  // avoid division by zero

        return (max - min) / avg;
    }
}
