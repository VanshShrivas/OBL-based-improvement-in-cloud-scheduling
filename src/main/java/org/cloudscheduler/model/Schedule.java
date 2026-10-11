package org.cloudscheduler.model;

import java.util.Arrays;

/**
 * Represents a single candidate schedule — a mapping of tasks to VMs.
 * Each element taskToVM[i] = j means task i is assigned to VM j.
 * 
 * Implements Comparable so schedules can be sorted by fitness (lower is better).
 */
public class Schedule implements Comparable<Schedule> {

    // Chromosome representation: taskToVM[i] is the VM ID assigned to Task i
    public int[] taskToVM;       // length = NUM_TASKS, values in [0, NUM_VMS-1]

    // Raw performance metrics
    public double makespan;      // raw makespan value
    public double energy;        // raw energy value
    public double imbalance;     // raw imbalance value

    // Combined multi-objective fitness score: F(i) = alpha*M_norm + beta*E_norm + gamma*I_norm
    public double fitness;

    /**
     * Constructor initializing a schedule with a given task count.
     */
    public Schedule(int numTasks) {
        this.taskToVM = new int[numTasks];
        this.fitness = Double.MAX_VALUE;
        this.makespan = 0.0;
        this.energy = 0.0;
        this.imbalance = 0.0;
    }

    /**
     * Constructor initializing from an existing task-to-VM assignment array.
     */
    public Schedule(int[] taskToVM) {
        this.taskToVM = Arrays.copyOf(taskToVM, taskToVM.length);
        this.fitness = Double.MAX_VALUE;
        this.makespan = 0.0;
        this.energy = 0.0;
        this.imbalance = 0.0;
    }

    /**
     * Deep copy of this schedule.
     * Essential for GA and SSA so mutations don't alter elite parents.
     */
    public Schedule copy() {
        Schedule clone = new Schedule(this.taskToVM);
        clone.makespan = this.makespan;
        clone.energy = this.energy;
        clone.imbalance = this.imbalance;
        clone.fitness = this.fitness;
        return clone;
    }

    /**
     * Comparison method for sorting.
     * Sorts in ascending order by fitness score (minimization problem: lower is better).
     */
    @Override
    public int compareTo(Schedule other) {
        return Double.compare(this.fitness, other.fitness);
    }

    // --- Getters and Setters ---
    public int[] getTaskToVM() {
        return taskToVM;
    }

    public void setTaskToVM(int[] taskToVM) {
        this.taskToVM = Arrays.copyOf(taskToVM, taskToVM.length);
    }

    public int getVMForTask(int taskId) {
        return this.taskToVM[taskId];
    }

    public void setVMForTask(int taskId, int vmId) {
        this.taskToVM[taskId] = vmId;
    }

    public int getNumTasks() {
        return this.taskToVM.length;
    }

    public double getMakespan() {
        return makespan;
    }

    public void setMakespan(double makespan) {
        this.makespan = makespan;
    }

    public double getEnergy() {
        return energy;
    }

    public void setEnergy(double energy) {
        this.energy = energy;
    }

    public double getImbalance() {
        return imbalance;
    }

    public void setImbalance(double imbalance) {
        this.imbalance = imbalance;
    }

    public double getFitness() {
        return fitness;
    }

    public void setFitness(double fitness) {
        this.fitness = fitness;
    }

    @Override
    public String toString() {
        return String.format("Schedule{mapping=%s, F(i)=%.4f, Makespan=%.2fs, Energy=%.2f, Imbalance=%.4f}",
                Arrays.toString(taskToVM), fitness, makespan, energy, imbalance);
    }
}
