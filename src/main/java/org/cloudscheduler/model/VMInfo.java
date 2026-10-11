package org.cloudscheduler.model;

/**
 * VM metadata — stores the VM's id and processing power.
 * Processing power ranges from 0.8 to 1.2 as per the paper.
 */
public class VMInfo {
    public int id;
    public double processingPower;  // 0.8 to 1.2

    public VMInfo(int id, double processingPower) {
        this.id = id;
        this.processingPower = processingPower;
    }

    public int getId() {
        return id;
    }

    public double getProcessingPower() {
        return processingPower;
    }

    @Override
    public String toString() {
        return String.format("VMInfo[id=%d, power=%.2f]", id, processingPower);
    }
}
