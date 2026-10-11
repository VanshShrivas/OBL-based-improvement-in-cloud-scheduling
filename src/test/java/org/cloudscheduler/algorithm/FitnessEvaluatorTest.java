package org.cloudscheduler.algorithm;

import org.cloudscheduler.model.Schedule;
import org.cloudscheduler.model.VMInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FitnessEvaluatorTest {

    private FitnessEvaluator evaluator;
    private VMInfo[] vms;
    private int[] durations;

    @BeforeEach
    void setUp() {
        evaluator = new FitnessEvaluator();
        vms = new VMInfo[5];
        for (int i = 0; i < 5; i++) {
            vms[i] = new VMInfo(i, 1.0);
        }
        // 10 tasks as expected by NUM_TASKS in SimulationConfig
        durations = new int[]{ 80, 20, 0, 0, 0, 0, 0, 0, 0, 0 };
    }

    @Test
    void evaluateAll_setsRawMetrics() {
        Schedule a = new Schedule(10);
        a.taskToVM = new int[]{ 0, 1, 0, 0, 0, 0, 0, 0, 0, 0 };

        evaluator.evaluateAll(new Schedule[]{ a }, vms, durations);

        assertEquals(80.0, a.makespan, 1e-9);
        assertEquals(100.0, a.energy, 1e-9);
        assertEquals(4.0, a.imbalance, 1e-9);
    }

    @Test
    void evaluateAll_normalizesToZeroOneRange() {
        Schedule a = new Schedule(10);
        a.taskToVM = new int[]{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
        Schedule b = new Schedule(10);
        b.taskToVM = new int[]{ 0, 1, 0, 0, 0, 0, 0, 0, 0, 0 };

        evaluator.evaluateAll(new Schedule[]{ a, b }, vms, durations);

        assertTrue(a.fitness >= 0.0 && a.fitness <= 1.0,
            "fitness A out of range: " + a.fitness);
        assertTrue(b.fitness >= 0.0 && b.fitness <= 1.0,
            "fitness B out of range: " + b.fitness);
    }

    @Test
    void evaluateAll_correctWeightedFitness() {
        // A: tasks all on VM0 -> makespan=100, energy=100, imbalance=(100-0)/20 = 5.0
        // B: tasks on VM0 and VM1 -> makespan=80, energy=100, imbalance=(80-0)/20 = 4.0
        //
        // Makespan norm: A=1.0, B=0.0 (min=80, max=100)
        // Energy norm: A=0.0, B=0.0 (both 100 -> min=max -> 0.0)
        // Imbalance norm: A=1.0, B=0.0 (min=4.0, max=5.0)
        //
        // F(A) = 0.4*1.0 + 0.3*0.0 + 0.3*1.0 = 0.7
        // F(B) = 0.4*0.0 + 0.3*0.0 + 0.3*0.0 = 0.0
        Schedule a = new Schedule(10);
        a.taskToVM = new int[]{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
        Schedule b = new Schedule(10);
        b.taskToVM = new int[]{ 0, 1, 0, 0, 0, 0, 0, 0, 0, 0 };

        evaluator.evaluateAll(new Schedule[]{ a, b }, vms, durations);

        assertEquals(0.7, a.fitness, 1e-9);
        assertEquals(0.0, b.fitness, 1e-9);
    }

    @Test
    void evaluateAll_identicalSchedules_allGetZeroFitness() {
        Schedule a = new Schedule(10);
        a.taskToVM = new int[]{ 0, 1, 0, 0, 0, 0, 0, 0, 0, 0 };
        Schedule b = new Schedule(10);
        b.taskToVM = new int[]{ 0, 1, 0, 0, 0, 0, 0, 0, 0, 0 };

        evaluator.evaluateAll(new Schedule[]{ a, b }, vms, durations);

        assertEquals(0.0, a.fitness, 1e-9);
        assertEquals(0.0, b.fitness, 1e-9);
    }
}
