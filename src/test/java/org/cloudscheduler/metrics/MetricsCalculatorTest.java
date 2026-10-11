package org.cloudscheduler.metrics;

import org.cloudscheduler.model.Schedule;
import org.cloudscheduler.model.VMInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MetricsCalculatorTest {

    private MetricsCalculator calc;

    @BeforeEach
    void setUp() {
        calc = new MetricsCalculator();
    }

    // ── Makespan ──

    @Test
    void makespan_allTasksOnOneVM() {
        // 5 VMs (standard NUM_VMS=5), 2 tasks [100, 50], all on VM 0
        // VM0 time = (100+50)/1.0 = 150, others 0
        // Makespan = 150
        VMInfo[] vms = new VMInfo[5];
        for (int i = 0; i < 5; i++) vms[i] = new VMInfo(i, 1.0);
        int[] durations = new int[]{ 100, 50, 0, 0, 0, 0, 0, 0, 0, 0 };
        Schedule s = new Schedule(10);
        s.taskToVM = new int[]{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

        double[] vmTimes = calc.computeVMTimes(s, vms, durations);
        assertEquals(150.0, calc.computeMakespan(vmTimes), 1e-9);
    }

    @Test
    void makespan_distributedTasks() {
        // 5 VMs (power 1.0), 2 tasks [100, 50], one on VM0, one on VM1
        // VM0=100, VM1=50 → makespan=100
        VMInfo[] vms = new VMInfo[5];
        for (int i = 0; i < 5; i++) vms[i] = new VMInfo(i, 1.0);
        int[] durations = new int[]{ 100, 50, 0, 0, 0, 0, 0, 0, 0, 0 };
        Schedule s = new Schedule(10);
        s.taskToVM = new int[]{ 0, 1, 0, 0, 0, 0, 0, 0, 0, 0 };

        double[] vmTimes = calc.computeVMTimes(s, vms, durations);
        assertEquals(100.0, calc.computeMakespan(vmTimes), 1e-9);
    }

    @Test
    void makespan_processingPowerAffectsTime() {
        // VM0 power=0.5 → time = 100/0.5 = 200
        // VM1 power=2.0 → time = 100/2.0 = 50
        // Makespan = 200
        VMInfo[] vms = new VMInfo[5];
        for (int i = 0; i < 5; i++) vms[i] = new VMInfo(i, 1.0);
        vms[0] = new VMInfo(0, 0.5);
        vms[1] = new VMInfo(1, 2.0);
        int[] durations = new int[]{ 100, 100, 0, 0, 0, 0, 0, 0, 0, 0 };
        Schedule s = new Schedule(10);
        s.taskToVM = new int[]{ 0, 1, 0, 0, 0, 0, 0, 0, 0, 0 };

        double[] vmTimes = calc.computeVMTimes(s, vms, durations);
        assertEquals(200.0, calc.computeMakespan(vmTimes), 1e-9);
    }

    // ── Energy ──

    @Test
    void energy_sumsProductOfPowerAndExecutionTime() {
        // VM0 power=0.8, tasks [50, 30] → time = 80/0.8 = 100 → energy = 0.8 * 100 = 80
        // VM1 power=1.2, tasks [20]     → time = 20/1.2 = 16.666... → energy = 1.2 * (20/1.2) = 20
        // Total energy = 100
        VMInfo[] vms = new VMInfo[5];
        for (int i = 0; i < 5; i++) vms[i] = new VMInfo(i, 1.0);
        vms[0] = new VMInfo(0, 0.8);
        vms[1] = new VMInfo(1, 1.2);
        int[] durations = new int[]{ 50, 30, 20, 0, 0, 0, 0, 0, 0, 0 };
        Schedule s = new Schedule(10);
        s.taskToVM = new int[]{ 0, 0, 1, 0, 0, 0, 0, 0, 0, 0 };

        double[] vmTimes = calc.computeVMTimes(s, vms, durations);
        assertEquals(100.0, calc.computeEnergy(vmTimes, vms), 1e-9);
    }

    // ── Imbalance ──

    @Test
    void imbalance_unevenDistribution() {
        // VM times: [80, 20, 0, 0, 0], max=80, min=0, avg=100/5 = 20
        // Imbalance = (80 - 0) / 20 = 4.0
        VMInfo[] vms = new VMInfo[5];
        for (int i = 0; i < 5; i++) vms[i] = new VMInfo(i, 1.0);
        int[] durations = new int[]{ 80, 20, 0, 0, 0, 0, 0, 0, 0, 0 };
        Schedule s = new Schedule(10);
        s.taskToVM = new int[]{ 0, 1, 0, 0, 0, 0, 0, 0, 0, 0 };

        double[] vmTimes = calc.computeVMTimes(s, vms, durations);
        assertEquals(4.0, calc.computeImbalance(vmTimes), 1e-9);
    }

    @Test
    void imbalance_perfectBalance_returnsZero() {
        // 5 VMs all time = 20, max=min=20 → imbalance = 0
        double[] vmTimes = new double[]{ 20.0, 20.0, 20.0, 20.0, 20.0 };
        assertEquals(0.0, calc.computeImbalance(vmTimes), 1e-9);
    }
}
