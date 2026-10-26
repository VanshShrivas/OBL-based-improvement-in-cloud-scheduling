package org.cloudscheduler.simulation;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

import static org.cloudscheduler.config.SimulationConfig.*;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * CloudEnvironment: Factory for CloudSim infrastructure objects.
 *
 * Creates:
 *   - 1 Datacenter with 1 Host (8 cores, 16 GB RAM, 1 TB storage)
 *   - 5 VMs with MIPS = BASE_MIPS × processing_power
 *   - 10 Cloudlets with length = task_duration × BASE_MIPS
 *
 *   execution_time = cloudlet_length / vm_mips = (duration × 1000) / (power × 1000) = duration / power
 */
public class CloudEnvironment {

    /**
     * Creates a Datacenter with a single powerful Host.
     * The Host has enough resources to run all 5 VMs simultaneously.
     */
    public static Datacenter createDatacenter(String name) {
        // --- Create Processing Elements (CPU cores) ---
        List<Pe> peList = new ArrayList<>();
        int hostMips = 10000; // Each core has 10000 MIPS
        for (int i = 0; i < 32; i++) { // 32 cores
            peList.add(new Pe(i, new PeProvisionerSimple(hostMips)));
        }

        // --- Create Host ---
        int hostId = 0;
        int ram = 65536;          // 64 GB RAM
        long storage = 10000000;  // 10 TB storage
        int bw = 50000;           // 50 Gbps bandwidth

        Host host = new Host(
                hostId,
                new RamProvisionerSimple(ram),
                new BwProvisionerSimple(bw),
                storage,
                peList,
                new VmSchedulerTimeShared(peList)
        );

        List<Host> hostList = new ArrayList<>();
        hostList.add(host);

        // --- Create Datacenter ---
        String arch = "x86";
        String os = "Linux";
        String vmm = "Xen";
        double timeZone = 5.5;    // IST
        double cost = 3.0;
        double costPerMem = 0.05;
        double costPerStorage = 0.001;
        double costPerBw = 0.0;

        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
                arch, os, vmm, hostList, timeZone, cost, costPerMem, costPerStorage, costPerBw
        );

        try {
            return new Datacenter(name, characteristics, new VmAllocationPolicySimple(hostList), new LinkedList<>(), 0);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Datacenter: " + e.getMessage(), e);
        }
    }

    /**
     * Creates X VMs with MIPS proportional to processing power.
     *
     * VM 0 → 800 MIPS  (power 0.8)
     * VM 1 → 900 MIPS  (power 0.9)
     * VM 2 → 1000 MIPS (power 1.0)
     * VM 3 → 1100 MIPS (power 1.1)
     * VM 4 → 1200 MIPS (power 1.2) ...
     *
     * @param brokerId The ID of the broker that owns these VMs
     * @param powers   Array of processing powers [0.8, 0.9, 1.0, 1.1, 1.2]
     * @return List of CloudSim Vm objects
     */
    public static List<Vm> createVMs(int brokerId, double[] powers) {
        List<Vm> vmList = new ArrayList<>();

        for (int i = 0; i < powers.length; i++) {
            int mips = (int) (BASE_MIPS * powers[i]); // e.g., 1000 × 0.8 = 800
            int pesNumber = 1;  // 1 CPU core per VM
            int ram = 2048;     // 2 GB RAM
            long bw = 1000;     // 1 Gbps
            long size = 10000;  // 10 GB image size
            String vmm = "Xen";

            Vm vm = new Vm(i, brokerId, mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerTimeShared());
            vmList.add(vm);
        }

        return vmList;
    }

    /**
     * Creates Y Cloudlets (tasks) with length = duration × BASE_MIPS.
     *
     * This ensures: execution_time = length / vm_mips = (duration × 1000) / (power × 1000) = duration / power
     *
     * @param brokerId      The ID of the broker that owns these Cloudlets
     * @param taskDurations Array of task durations in seconds (length = NUM_TASKS)
     * @return List of CloudSim Cloudlet objects
     */
    public static List<Cloudlet> createCloudlets(int brokerId, int[] taskDurations) {
        List<Cloudlet> cloudletList = new ArrayList<>();

        for (int i = 0; i < taskDurations.length; i++) {
            long length = (long) taskDurations[i] * BASE_MIPS; // e.g., 88s × 1000 = 88000 MI
            int pesNumber = 1;
            long fileSize = 300;
            long outputSize = 300;

            Cloudlet cloudlet = new Cloudlet(i, length, pesNumber, fileSize, outputSize,
                    new UtilizationModelFull(), new UtilizationModelFull(), new UtilizationModelFull());
            cloudlet.setUserId(brokerId);

            cloudletList.add(cloudlet);
        }

        return cloudletList;
    }
}
