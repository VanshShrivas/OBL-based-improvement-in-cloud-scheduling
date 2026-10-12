package org.cloudscheduler.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ScheduleTest {

    @Test
    void constructor_initializesArrayAndDefaults() {
        Schedule s = new Schedule(5);

        assertEquals(5, s.taskToVM.length);
        for (int v : s.taskToVM) {
            assertEquals(0, v);
        }
        assertEquals(Double.MAX_VALUE, s.fitness);
        assertEquals(0.0, s.makespan);
        assertEquals(0.0, s.energy);
        assertEquals(0.0, s.imbalance);
    }

    @Test
    void copy_createsIndependentDeepCopy() {
        Schedule original = new Schedule(3);
        original.taskToVM = new int[]{2, 4, 1};
        original.fitness = 0.5;
        original.makespan = 100.0;
        original.energy = 200.0;
        original.imbalance = 1.5;

        Schedule copy = original.copy();

        // Values match
        assertArrayEquals(original.taskToVM, copy.taskToVM);
        assertEquals(original.fitness, copy.fitness);
        assertEquals(original.makespan, copy.makespan);
        assertEquals(original.energy, copy.energy);
        assertEquals(original.imbalance, copy.imbalance);

        // Mutation of copy does not affect original
        copy.taskToVM[0] = 99;
        copy.fitness = 999.0;
        assertEquals(2, original.taskToVM[0]);
        assertEquals(0.5, original.fitness);
    }
}
