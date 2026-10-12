package org.cloudscheduler.algorithm;

import org.cloudscheduler.model.Schedule;
import org.cloudscheduler.model.VMInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class SalpSwarmAlgorithmTest {

    private SalpSwarmAlgorithm ssa;
    private VMInfo[] vms;
    private int[] taskDurations;
    private Schedule[] elites;

    @BeforeEach
    void setUp() {
        FitnessEvaluator evaluator = new FitnessEvaluator();
        ssa = new SalpSwarmAlgorithm(evaluator);

        vms = new VMInfo[]{
            new VMInfo(0, 1.0),
            new VMInfo(1, 0.9),
            new VMInfo(2, 1.1),
            new VMInfo(3, 0.8),
            new VMInfo(4, 1.2)
        };
        taskDurations = new int[]{ 50, 30, 70, 20, 60, 40, 80, 10, 90, 45 };

        Schedule elite1 = new Schedule(10);
        elite1.taskToVM = new int[]{ 0, 1, 2, 3, 4, 0, 1, 2, 3, 4 };
        elite1.fitness = 0.3;

        Schedule elite2 = new Schedule(10);
        elite2.taskToVM = new int[]{ 4, 3, 2, 1, 0, 4, 3, 2, 1, 0 };
        elite2.fitness = 0.4;

        elites = new Schedule[]{ elite1, elite2 };
    }

    // ── c1 coefficient ──

    @Test
    void computeC1_atGenZero_returnsTwo() {
        // c1 = 2 × e^(-(4×0/100)²) = 2 × e^0 = 2.0
        assertEquals(2.0, ssa.computeC1(0, 100), 1e-9);
    }

    @Test
    void computeC1_atMaxGen_returnsNearZero() {
        // c1 = 2 × e^(-(4×100/100)²) = 2 × e^(-16) ≈ 2.24e-7
        double c1 = ssa.computeC1(100, 100);
        assertTrue(c1 < 0.001, "c1 at max gen should be near zero, got: " + c1);
        assertTrue(c1 > 0.0, "c1 should be positive");
    }

    @Test
    void computeC1_decreasesMonotonically() {
        double prev = ssa.computeC1(0, 100);
        for (int gen = 10; gen <= 100; gen += 10) {
            double current = ssa.computeC1(gen, 100);
            assertTrue(current < prev,
                "c1 should decrease: gen " + gen + " (" + current
                + ") >= gen " + (gen - 10) + " (" + prev + ")");
            prev = current;
        }
    }

    // ── refine() ──

    @Test
    void refine_returnsExactlyTwoSchedules() {
        Schedule[] result = ssa.refine(elites, 0, 100, vms, taskDurations, new Random(42));
        assertEquals(2, result.length);
    }

    @Test
    void refine_allAssignmentsInValidVMRange() {
        Schedule[] result = ssa.refine(elites, 0, 100, vms, taskDurations, new Random(42));
        for (Schedule s : result) {
            assertEquals(10, s.taskToVM.length, "Should have 10 task assignments");
            for (int j = 0; j < s.taskToVM.length; j++) {
                assertTrue(s.taskToVM[j] >= 0 && s.taskToVM[j] <= 4,
                    "Task " + j + " assigned to invalid VM: " + s.taskToVM[j]);
            }
        }
    }

    @Test
    void refine_deterministicWithSameSeed() {
        Schedule[] result1 = ssa.refine(elites, 50, 100, vms, taskDurations, new Random(42));
        Schedule[] result2 = ssa.refine(elites, 50, 100, vms, taskDurations, new Random(42));

        assertArrayEquals(result1[0].taskToVM, result2[0].taskToVM);
        assertArrayEquals(result1[1].taskToVM, result2[1].taskToVM);
        assertEquals(result1[0].fitness, result2[0].fitness, 1e-9);
        assertEquals(result1[1].fitness, result2[1].fitness, 1e-9);
    }

    @Test
    void refine_fitnessIsEvaluatedAndSortedAscending() {
        Schedule[] result = ssa.refine(elites, 0, 100, vms, taskDurations, new Random(42));

        // Fitness should be set (not default MAX_VALUE)
        for (Schedule s : result) {
            assertTrue(s.fitness < Double.MAX_VALUE,
                "Fitness should be evaluated, not default MAX_VALUE");
        }

        // Results sorted by fitness ascending (lower = better)
        assertTrue(result[0].fitness <= result[1].fitness,
            "Results should be sorted: " + result[0].fitness + " > " + result[1].fitness);
    }

    @Test
    void refine_appliesLeaderFollowerEquationsAndClipsToVmBounds() {
        CapturingFitnessEvaluator evaluator = new CapturingFitnessEvaluator();
        SalpSwarmAlgorithm algorithm = new SalpSwarmAlgorithm(evaluator);
        Random rng = new SequenceRandom(
            0.999999, 0.9,  // task 0: leader adds large step (upper clipping)
            0.999999, 0.1,  // task 1: leader subtracts large step (lower clipping)
            0.1,      0.9,  // task 2: leader adds moderate step (no clipping)
            0.0,      0.9,
            0.0,      0.9,
            0.0,      0.9,
            0.0,      0.9,
            0.0,      0.9,
            0.0,      0.9,
            0.0,      0.9
        );

        algorithm.refine(elites, 0, 100, vms, taskDurations, rng);

        int[] leader = evaluator.capturedAssignments[0];
        int[] follower = evaluator.capturedAssignments[1];

        assertEquals(4, leader[0], "Leader should be clipped to upper VM bound");
        assertEquals(0, leader[1], "Leader should be clipped to lower VM bound");
        assertEquals(3, leader[2], "Leader should follow c1/c2/c3 update equation");

        assertEquals(4, follower[0], "Follower average should be clipped to upper VM bound");
        assertEquals(0, follower[1], "Follower average should be clipped to lower VM bound");
        assertEquals(2, follower[2], "Follower should average with preceding salp");
    }

    private static final class CapturingFitnessEvaluator extends FitnessEvaluator {
        private int[][] capturedAssignments;

        @Override
        public void evaluateAll(Schedule[] pool, VMInfo[] vms, int[] taskDurations) {
            capturedAssignments = new int[pool.length][];
            for (int i = 0; i < pool.length; i++) {
                capturedAssignments[i] = pool[i].taskToVM.clone();
                pool[i].fitness = i;
            }
        }
    }

    private static final class SequenceRandom extends Random {
        private final double[] values;
        private int index = 0;

        private SequenceRandom(double... values) {
            this.values = values;
        }

        @Override
        public double nextDouble() {
            if (index >= values.length) {
                return 0.0;
            }
            return values[index++];
        }
    }
}
