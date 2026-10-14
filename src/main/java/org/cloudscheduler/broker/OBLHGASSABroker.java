package org.cloudscheduler.broker;

import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Vm;

import java.util.List;

/**
 * Custom DatacenterBroker that applies a pre-computed task-to-VM mapping.
 *
 * Instead of CloudSim's default round-robin or random assignment,
 * this broker uses the schedule from our optimization algorithm
 * (OBL, GA, SSA, or any combination) to bind each Cloudlet to a specific VM.
 *
 * Usage:
 *   broker.setScheduleMapping(new int[]{4, 0, 0, 1, 2, 4, 3, 3, 1, 2});
 *   broker.submitVmList(vmList);
 *   broker.submitCloudletList(cloudletList);
 *   // CloudSim will now assign Cloudlet 0 → VM 4, Cloudlet 1 → VM 0, etc.
 */
public class OBLHGASSABroker extends DatacenterBroker {

    /** The optimized task-to-VM mapping: scheduleMapping[taskId] = vmId */
    private int[] scheduleMapping;

    /**
     * Creates a new broker.
     *
     * @param name Broker name for CloudSim
     * @throws Exception If CloudSim initialization fails
     */
    public OBLHGASSABroker(String name) throws Exception {
        super(name);
    }

    /**
     * Sets the pre-computed schedule mapping from the optimization algorithm.
     *
     * @param mapping int[] where mapping[i] = VM ID for Cloudlet i
     */
    public void setScheduleMapping(int[] mapping) {
        this.scheduleMapping = mapping;
    }

    /**
     * Override CloudSim's default cloudlet-to-VM binding.
     *
     * This method is called by CloudSim internally before starting the simulation.
     * Instead of letting CloudSim decide, we explicitly bind each Cloudlet
     * to the VM specified by our algorithm's mapping.
     */
    @Override
    protected void submitCloudlets() {
        List<Cloudlet> cloudletList = getCloudletList();
        List<Vm> vmList = getVmsCreatedList();

        if (scheduleMapping != null) {
            for (Cloudlet cloudlet : cloudletList) {
                int taskId = cloudlet.getCloudletId();

                if (taskId < scheduleMapping.length) {
                    int targetVmId = scheduleMapping[taskId];

                    // Find the Vm object with matching ID
                    for (Vm vm : vmList) {
                        if (vm.getId() == targetVmId) {
                            cloudlet.setVmId(vm.getId());
                            break;
                        }
                    }
                }
            }
        }

        // Call parent to actually submit the cloudlets to CloudSim
        super.submitCloudlets();
    }
}