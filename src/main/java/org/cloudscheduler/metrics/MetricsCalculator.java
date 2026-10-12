package org.cloudscheduler.metrics;

import org.cloudscheduler.model.Schedule;
import org.cloudscheduler.model.VMInfo;

import static org.cloudscheduler.config.SimulationConfig.*;

/**
 * Computes raw metrics for a schedule:
 *   - Makespan: max completion time across all VMs
 *   - Energy: total power consumed (processing_power * time_on_vm)
 *   - Imbalance: (max_load - min_load) / avg_load
 */
public class MetricsCalculator {

    /**
     * Calculates the completion time for each VM given a schedule (using int[] durations).
     * time_vm = sum(task_duration) / processing_power_vm
     */
    public double[] computeVMTimes(Schedule schedule, VMInfo[] vms, int[] taskDurations) {
        double[] vmTimes = new double[vms.length];

        for (int task = 0; task < schedule.taskToVM.length; task++) {
            int vmId = schedule.taskToVM[task];
            vmTimes[vmId] += taskDurations[task];
        }

        for (int vm = 0; vm < vms.length; vm++) {
            vmTimes[vm] = vmTimes[vm] / vms[vm].processingPower;
        }

        return vmTimes;
    }

    /**
     * Overloaded method for double[] taskDurations.
     */
    public static double[] calculateVMLoads(Schedule schedule, double[] taskDurations, VMInfo[] vms) {
        double[] vmLoads = new double[vms.length];
        for (int taskId = 0; taskId < schedule.getNumTasks(); taskId++) {
            int vmId = schedule.getVMForTask(taskId);
            double duration = taskDurations[taskId];
            double power = vms[vmId].getProcessingPower();
            vmLoads[vmId] += duration / power;
        }
        return vmLoads;
    }

    /**
     * Metric 1: Makespan (Maximum execution time among all VMs).
     */
    public double computeMakespan(double[] vmTimes) {
        return calculateMakespan(vmTimes);
    }

    public static double calculateMakespan(double[] vmLoads) {
        double max = Double.MIN_VALUE;
        for (double t : vmLoads) {
            if (t > max) max = t;
        }
        return max;
    }

    /**
     * Metric 2: Energy Consumption.
     */
    public double computeEnergy(double[] vmTimes, VMInfo[] vms) {
        double energy = 0.0;
        for (int vm = 0; vm < vms.length; vm++) {
            energy += vms[vm].processingPower * vmTimes[vm];
        }
        return energy;
    }

    public static double calculateEnergy(Schedule schedule, double[] taskDurations, VMInfo[] vms) {
        double[] rawWorkloadPerVM = new double[vms.length];
        for (int taskId = 0; taskId < schedule.getNumTasks(); taskId++) {
            int vmId = schedule.getVMForTask(taskId);
            rawWorkloadPerVM[vmId] += taskDurations[taskId];
        }
        double totalEnergy = 0.0;
        for (int i = 0; i < vms.length; i++) {
            totalEnergy += vms[i].getProcessingPower() * rawWorkloadPerVM[i];
        }
        return totalEnergy;
    }

    /**
     * Metric 3: Degree of Imbalance (DI).
     * (max_load - min_load) / avg_load
     */
    public double computeImbalance(double[] vmTimes) {
        return calculateImbalance(vmTimes);
    }

    public static double calculateImbalance(double[] vmLoads) {
        double max = Double.MIN_VALUE;
        double min = Double.MAX_VALUE;
        double sum = 0.0;

        for (double t : vmLoads) {
            if (t > max) max = t;
            if (t < min) min = t;
            sum += t;
        }

        double avg = sum / vmLoads.length;
        if (avg == 0.0) return 0.0;

        return (max - min) / avg;
    }

    /**
     * Convenience method to compute and populate all three raw metrics into the Schedule.
     */
    public static void computeAndSetMetrics(Schedule schedule, double[] taskDurations, VMInfo[] vms) {
        double[] vmLoads = calculateVMLoads(schedule, taskDurations, vms);
        schedule.setMakespan(calculateMakespan(vmLoads));
        schedule.setEnergy(calculateEnergy(schedule, taskDurations, vms));
        schedule.setImbalance(calculateImbalance(vmLoads));
    }

    public static void computeAndSetMetrics(Schedule schedule, int[] taskDurations, VMInfo[] vms) {
        double[] doubleDurations = new double[taskDurations.length];
        for (int i = 0; i < taskDurations.length; i++) {
            doubleDurations[i] = taskDurations[i];
        }
        computeAndSetMetrics(schedule, doubleDurations, vms);
    }
}
