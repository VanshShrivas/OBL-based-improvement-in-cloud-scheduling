package org.cloudscheduler.model;

/**
 * Represents a single candidate schedule — a mapping of tasks to VMs.
 * Each element taskToVM[i] = j means task i is assigned to VM j.
 */
public class Schedule {
    public int[] taskToVM;       // length = NUM_TASKS, values ∈ [0, NUM_VMS-1]
    public double fitness;       // set by FitnessEvaluator
    public double makespan;      // raw makespan value
    public double energy;        // raw energy value
    public double imbalance;     // raw imbalance value

    public Schedule(int numTasks) {
        this.taskToVM = new int[numTasks];
        this.fitness = Double.MAX_VALUE;
        this.makespan = 0.0;
        this.energy = 0.0;
        this.imbalance = 0.0;
    }

    /**
     * Deep copy of this schedule.
     */
    public Schedule copy() {
        Schedule clone = new Schedule(this.taskToVM.length);
        System.arraycopy(this.taskToVM, 0, clone.taskToVM, 0, this.taskToVM.length);
        clone.fitness = this.fitness;
        clone.makespan = this.makespan;
        clone.energy = this.energy;
        clone.imbalance = this.imbalance;
        return clone;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Schedule[fitness=");
        sb.append(String.format("%.4f", fitness));
        sb.append(", makespan=").append(String.format("%.2f", makespan));
        sb.append(", mapping={");
        for (int i = 0; i < taskToVM.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(taskToVM[i]);
        }
        sb.append("}]");
        return sb.toString();
    }
}
