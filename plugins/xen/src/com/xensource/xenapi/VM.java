/*
 *============================================================================
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of version 2.1 of the GNU Lesser General Public
 * License as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *============================================================================
 * Copyright (C) 2007 XenSource Inc.
 *============================================================================
 */
package com.xensource.xenapi;

import java.util.Map;
import java.util.Set;
import java.util.Date;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.apache.xmlrpc.XmlRpcException;

/**
 * A virtual machine (or 'guest').
 *
 * @author XenSource Inc.
 */
public class VM extends XenAPIObject {

    /**
     * The XenAPI reference to this object.
     */
    protected final String ref;

    private VM(String ref) {
       this.ref = ref;
    }

    public String toWireString() {
       return this.ref;
    }

    /**
     * This code helps ensure there is only one
     * VM instance per XenAPI reference.
     */
    private static final Map<String,SoftReference<VM>> cache = 
        new HashMap<String,SoftReference<VM>>();

    protected static synchronized VM getInstFromRef(String ref) {
        if(VM.cache.containsKey(ref)) {
            VM instance = 
                VM.cache.get(ref).get();
            if(instance != null) {
                return instance;
            }
        }

        VM instance = new VM(ref);
        VM.cache.put(ref, new SoftReference<VM>(instance));
        return instance;
    }

    /**
     * Represents all the fields in a VM
     */
    public static class Record  implements Types.Record{
        public String toString() {
            StringWriter writer = new StringWriter();
            PrintWriter print = new PrintWriter(writer);
            print.printf("%1$20s: %2$s\n", "uuid", this.uuid);
            print.printf("%1$20s: %2$s\n", "allowedOperations", this.allowedOperations);
            print.printf("%1$20s: %2$s\n", "currentOperations", this.currentOperations);
            print.printf("%1$20s: %2$s\n", "powerState", this.powerState);
            print.printf("%1$20s: %2$s\n", "nameLabel", this.nameLabel);
            print.printf("%1$20s: %2$s\n", "nameDescription", this.nameDescription);
            print.printf("%1$20s: %2$s\n", "userVersion", this.userVersion);
            print.printf("%1$20s: %2$s\n", "isATemplate", this.isATemplate);
            print.printf("%1$20s: %2$s\n", "suspendVDI", this.suspendVDI);
            print.printf("%1$20s: %2$s\n", "residentOn", this.residentOn);
            print.printf("%1$20s: %2$s\n", "affinity", this.affinity);
            print.printf("%1$20s: %2$s\n", "memoryStaticMax", this.memoryStaticMax);
            print.printf("%1$20s: %2$s\n", "memoryDynamicMax", this.memoryDynamicMax);
            print.printf("%1$20s: %2$s\n", "memoryDynamicMin", this.memoryDynamicMin);
            print.printf("%1$20s: %2$s\n", "memoryStaticMin", this.memoryStaticMin);
            print.printf("%1$20s: %2$s\n", "VCPUsParams", this.VCPUsParams);
            print.printf("%1$20s: %2$s\n", "VCPUsMax", this.VCPUsMax);
            print.printf("%1$20s: %2$s\n", "VCPUsAtStartup", this.VCPUsAtStartup);
            print.printf("%1$20s: %2$s\n", "actionsAfterShutdown", this.actionsAfterShutdown);
            print.printf("%1$20s: %2$s\n", "actionsAfterReboot", this.actionsAfterReboot);
            print.printf("%1$20s: %2$s\n", "actionsAfterCrash", this.actionsAfterCrash);
            print.printf("%1$20s: %2$s\n", "consoles", this.consoles);
            print.printf("%1$20s: %2$s\n", "VIFs", this.VIFs);
            print.printf("%1$20s: %2$s\n", "VBDs", this.VBDs);
            print.printf("%1$20s: %2$s\n", "crashDumps", this.crashDumps);
            print.printf("%1$20s: %2$s\n", "VTPMs", this.VTPMs);
            print.printf("%1$20s: %2$s\n", "PVBootloader", this.PVBootloader);
            print.printf("%1$20s: %2$s\n", "PVKernel", this.PVKernel);
            print.printf("%1$20s: %2$s\n", "PVRamdisk", this.PVRamdisk);
            print.printf("%1$20s: %2$s\n", "PVArgs", this.PVArgs);
            print.printf("%1$20s: %2$s\n", "PVBootloaderArgs", this.PVBootloaderArgs);
            print.printf("%1$20s: %2$s\n", "PVLegacyArgs", this.PVLegacyArgs);
            print.printf("%1$20s: %2$s\n", "HVMBootPolicy", this.HVMBootPolicy);
            print.printf("%1$20s: %2$s\n", "HVMBootParams", this.HVMBootParams);
            print.printf("%1$20s: %2$s\n", "HVMShadowMultiplier", this.HVMShadowMultiplier);
            print.printf("%1$20s: %2$s\n", "platform", this.platform);
            print.printf("%1$20s: %2$s\n", "PCIBus", this.PCIBus);
            print.printf("%1$20s: %2$s\n", "otherConfig", this.otherConfig);
            print.printf("%1$20s: %2$s\n", "domid", this.domid);
            print.printf("%1$20s: %2$s\n", "domarch", this.domarch);
            print.printf("%1$20s: %2$s\n", "lastBootCPUFlags", this.lastBootCPUFlags);
            print.printf("%1$20s: %2$s\n", "isControlDomain", this.isControlDomain);
            print.printf("%1$20s: %2$s\n", "metrics", this.metrics);
            print.printf("%1$20s: %2$s\n", "guestMetrics", this.guestMetrics);
            print.printf("%1$20s: %2$s\n", "recommendations", this.recommendations);
            print.printf("%1$20s: %2$s\n", "xenstoreData", this.xenstoreData);
            return writer.toString();
        }

        /**
         * Convert a VM.Record to a Map
         */
        public Map<String,Object> toMap() {
            Map<String,Object> map = new HashMap<String,Object>();
            map.put("uuid", this.uuid);
            map.put("allowed_operations", this.allowedOperations);
            map.put("current_operations", this.currentOperations);
            map.put("power_state", this.powerState);
            map.put("name_label", this.nameLabel);
            map.put("name_description", this.nameDescription);
            map.put("user_version", this.userVersion);
            map.put("is_a_template", this.isATemplate);
            map.put("suspend_VDI", this.suspendVDI);
            map.put("resident_on", this.residentOn);
            map.put("affinity", this.affinity);
            map.put("memory_static_max", this.memoryStaticMax);
            map.put("memory_dynamic_max", this.memoryDynamicMax);
            map.put("memory_dynamic_min", this.memoryDynamicMin);
            map.put("memory_static_min", this.memoryStaticMin);
            map.put("VCPUs_params", this.VCPUsParams);
            map.put("VCPUs_max", this.VCPUsMax);
            map.put("VCPUs_at_startup", this.VCPUsAtStartup);
            map.put("actions_after_shutdown", this.actionsAfterShutdown);
            map.put("actions_after_reboot", this.actionsAfterReboot);
            map.put("actions_after_crash", this.actionsAfterCrash);
            map.put("consoles", this.consoles);
            map.put("VIFs", this.VIFs);
            map.put("VBDs", this.VBDs);
            map.put("crash_dumps", this.crashDumps);
            map.put("VTPMs", this.VTPMs);
            map.put("PV_bootloader", this.PVBootloader);
            map.put("PV_kernel", this.PVKernel);
            map.put("PV_ramdisk", this.PVRamdisk);
            map.put("PV_args", this.PVArgs);
            map.put("PV_bootloader_args", this.PVBootloaderArgs);
            map.put("PV_legacy_args", this.PVLegacyArgs);
            map.put("HVM_boot_policy", this.HVMBootPolicy);
            map.put("HVM_boot_params", this.HVMBootParams);
            map.put("HVM_shadow_multiplier", this.HVMShadowMultiplier);
            map.put("platform", this.platform);
            map.put("PCI_bus", this.PCIBus);
            map.put("other_config", this.otherConfig);
            map.put("domid", this.domid);
            map.put("domarch", this.domarch);
            map.put("last_boot_CPU_flags", this.lastBootCPUFlags);
            map.put("is_control_domain", this.isControlDomain);
            map.put("metrics", this.metrics);
            map.put("guest_metrics", this.guestMetrics);
            map.put("recommendations", this.recommendations);
            map.put("xenstore_data", this.xenstoreData);
            return map;
        }

        /**
         * unique identifier/object reference
         */
        public String uuid;
        /**
         * list of the operations allowed in this state. This list is advisory only and the server state may have changed by the time this field is read by a client.
         */
        public Set<Types.VmOperations> allowedOperations;
        /**
         * links each of the running tasks using this object (by reference) to a current_operation enum which describes the nature of the task.
         */
        public Map<String, Types.VmOperations> currentOperations;
        /**
         * Current power state of the machine
         */
        public Types.VmPowerState powerState;
        /**
         * a human-readable name
         */
        public String nameLabel;
        /**
         * a notes field containg human-readable description
         */
        public String nameDescription;
        /**
         * a user version number for this machine
         */
        public Long userVersion;
        /**
         * true if this is a template. Template VMs can never be started, they are used only for cloning other VMs
         */
        public Boolean isATemplate;
        /**
         * The VDI that a suspend image is stored on. (Only has meaning if VM is currently suspended)
         */
        public VDI suspendVDI;
        /**
         * the host the VM is currently resident on
         */
        public Host residentOn;
        /**
         * a host which the VM has some affinity for (or NULL). This is used as a hint to the start call when it decides where to run the VM. Implementations are free to ignore this field.
         */
        public Host affinity;
        /**
         * Statically-set (i.e. absolute) maximum (bytes). The value of this field at VM start time acts as a hard limit of the amount of memory a guest can use. New values only take effect on reboot.
         */
        public Long memoryStaticMax;
        /**
         * Dynamic maximum (bytes)
         */
        public Long memoryDynamicMax;
        /**
         * Dynamic minimum (bytes)
         */
        public Long memoryDynamicMin;
        /**
         * Statically-set (i.e. absolute) mininum (bytes). The value of this field indicates the least amount of memory this VM can boot with without crashing.
         */
        public Long memoryStaticMin;
        /**
         * configuration parameters for the selected VCPU policy
         */
        public Map<String, String> VCPUsParams;
        /**
         * Max number of VCPUs
         */
        public Long VCPUsMax;
        /**
         * Boot number of VCPUs
         */
        public Long VCPUsAtStartup;
        /**
         * action to take after the guest has shutdown itself
         */
        public Types.OnNormalExit actionsAfterShutdown;
        /**
         * action to take after the guest has rebooted itself
         */
        public Types.OnNormalExit actionsAfterReboot;
        /**
         * action to take if the guest crashes
         */
        public Types.OnCrashBehaviour actionsAfterCrash;
        /**
         * virtual console devices
         */
        public Set<Console> consoles;
        /**
         * virtual network interfaces
         */
        public Set<VIF> VIFs;
        /**
         * virtual block devices
         */
        public Set<VBD> VBDs;
        /**
         * crash dumps associated with this VM
         */
        public Set<Crashdump> crashDumps;
        /**
         * virtual TPMs
         */
        public Set<VTPM> VTPMs;
        /**
         * name of or path to bootloader
         */
        public String PVBootloader;
        /**
         * path to the kernel
         */
        public String PVKernel;
        /**
         * path to the initrd
         */
        public String PVRamdisk;
        /**
         * kernel command-line arguments
         */
        public String PVArgs;
        /**
         * miscellaneous arguments for the bootloader
         */
        public String PVBootloaderArgs;
        /**
         * to make Zurich guests boot
         */
        public String PVLegacyArgs;
        /**
         * HVM boot policy
         */
        public String HVMBootPolicy;
        /**
         * HVM boot params
         */
        public Map<String, String> HVMBootParams;
        /**
         * multiplier applied to the amount of shadow that will be made available to the guest
         */
        public Double HVMShadowMultiplier;
        /**
         * platform-specific configuration
         */
        public Map<String, String> platform;
        /**
         * PCI bus path for pass-through devices
         */
        public String PCIBus;
        /**
         * additional configuration
         */
        public Map<String, String> otherConfig;
        /**
         * domain ID (if available, -1 otherwise)
         */
        public Long domid;
        /**
         * Domain architecture (if available, null string otherwise)
         */
        public String domarch;
        /**
         * describes the CPU flags on which the VM was last booted
         */
        public Map<String, String> lastBootCPUFlags;
        /**
         * true if this is a control domain (domain 0 or a driver domain)
         */
        public Boolean isControlDomain;
        /**
         * metrics associated with this VM
         */
        public VMMetrics metrics;
        /**
         * metrics associated with the running guest
         */
        public VMGuestMetrics guestMetrics;
        /**
         * An XML specification of recommended values and ranges for properties of this VM
         */
        public String recommendations;
        /**
         * data to be inserted into the xenstore tree (/local/domain/<domid>/vm-data) after the VM is created.
         */
        public Map<String, String> xenstoreData;
    }

    /**
     * Get a record containing the current state of the given VM.
     *
     * @return all fields from the object
     */
    public VM.Record getRecord(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.get_record";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toVMRecord(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get a reference to the VM instance with the specified UUID.
     *
     * @param uuid UUID of object to return
     * @return reference to the object
     */
    public static VM getByUuid(Connection c, String uuid) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.get_by_uuid";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(uuid)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toVM(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Create a new VM instance, and return its handle.
     *
     * @param record All constructor arguments
     * @return reference to the newly created object
     */
    public static VM create(Connection c, VM.Record record) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.create";
        String session = c.getSessionReference();
        Map<String, Object> record_map = record.toMap();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(record_map)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toVM(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Destroy the specified VM.  The VM is completely removed from the system.  This function can only be called when the VM is in the Halted State.
     *
     */
    public void destroy(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.destroy";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get all the VM instances with the given label.
     *
     * @param label label of object to return
     * @return references to objects with matching names
     */
    public static Set<VM> getByNameLabel(Connection c, String label) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.get_by_name_label";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(label)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toSetOfVM(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the uuid field of the given VM.
     *
     * @return value of the field
     */
    public String getUuid(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.get_uuid";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toString(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the allowed_operations field of the given VM.
     *
     * @return value of the field
     */
    public Set<Types.VmOperations> getAllowedOperations(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.get_allowed_operations";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toSetOfVmOperations(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the current_operations field of the given VM.
     *
     * @return value of the field
     */
    public Map<String, Types.VmOperations> getCurrentOperations(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.get_current_operations";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toMapOfStringVmOperations(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the power_state field of the given VM.
     *
     * @return value of the field
     */
    public Types.VmPowerState getPowerState(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.get_power_state";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toVmPowerState(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the name/label field of the given VM.
     *
     * @return value of the field
     */
    public String getNameLabel(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.get_name_label";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toString(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the name/description field of the given VM.
     *
     * @return value of the field
     */
    public String getNameDescription(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.get_name_description";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toString(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the user_version field of the given VM.
     *
     * @return value of the field
     */
    public Long getUserVersion(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.get_user_version";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toLong(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the is_a_template field of the given VM.
     *
     * @return value of the field
     */
    public Boolean getIsATemplate(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.get_is_a_template";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toBoolean(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the suspend_VDI field of the given VM.
     *
     * @return value of the field
     */
    public VDI getSuspendVDI(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.get_suspend_VDI";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toVDI(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the resident_on field of the given VM.
     *
     * @return value of the field
     */
    public Host getResidentOn(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.get_resident_on";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toHost(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the affinity field of the given VM.
     *
     * @return value of the field
     */
    public Host getAffinity(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.get_affinity";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toHost(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the memory/static_max field of the given VM.
     *
     * @return value of the field
     */
    public Long getMemoryStaticMax(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.get_memory_static_max";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toLong(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the memory/dynamic_max field of the given VM.
     *
     * @return value of the field
     */
    public Long getMemoryDynamicMax(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.get_memory_dynamic_max";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toLong(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the memory/dynamic_min field of the given VM.
     *
     * @return value of the field
     */
    public Long getMemoryDynamicMin(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.get_memory_dynamic_min";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toLong(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the memory/static_min field of the given VM.
     *
     * @return value of the field
     */
    public Long getMemoryStaticMin(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.get_memory_static_min";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toLong(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the VCPUs/params field of the given VM.
     *
     * @return value of the field
     */
    public Map<String, String> getVCPUsParams(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.get_VCPUs_params";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toMapOfStringString(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the VCPUs/max field of the given VM.
     *
     * @return value of the field
     */
    public Long getVCPUsMax(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.get_VCPUs_max";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toLong(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the VCPUs/at_startup field of the given VM.
     *
     * @return value of the field
     */
    public Long getVCPUsAtStartup(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.get_VCPUs_at_startup";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toLong(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the actions/after_shutdown field of the given VM.
     *
     * @return value of the field
     */
    public Types.OnNormalExit getActionsAfterShutdown(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.get_actions_after_shutdown";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toOnNormalExit(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the actions/after_reboot field of the given VM.
     *
     * @return value of the field
     */
    public Types.OnNormalExit getActionsAfterReboot(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.get_actions_after_reboot";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toOnNormalExit(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the actions/after_crash field of the given VM.
     *
     * @return value of the field
     */
    public Types.OnCrashBehaviour getActionsAfterCrash(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.get_actions_after_crash";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toOnCrashBehaviour(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the consoles field of the given VM.
     *
     * @return value of the field
     */
    public Set<Console> getConsoles(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.get_consoles";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toSetOfConsole(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the VIFs field of the given VM.
     *
     * @return value of the field
     */
    public Set<VIF> getVIFs(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.get_VIFs";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toSetOfVIF(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the VBDs field of the given VM.
     *
     * @return value of the field
     */
    public Set<VBD> getVBDs(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.get_VBDs";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toSetOfVBD(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the crash_dumps field of the given VM.
     *
     * @return value of the field
     */
    public Set<Crashdump> getCrashDumps(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.get_crash_dumps";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toSetOfCrashdump(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the VTPMs field of the given VM.
     *
     * @return value of the field
     */
    public Set<VTPM> getVTPMs(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.get_VTPMs";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toSetOfVTPM(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the PV/bootloader field of the given VM.
     *
     * @return value of the field
     */
    public String getPVBootloader(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.get_PV_bootloader";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toString(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the PV/kernel field of the given VM.
     *
     * @return value of the field
     */
    public String getPVKernel(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.get_PV_kernel";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toString(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the PV/ramdisk field of the given VM.
     *
     * @return value of the field
     */
    public String getPVRamdisk(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.get_PV_ramdisk";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toString(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the PV/args field of the given VM.
     *
     * @return value of the field
     */
    public String getPVArgs(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.get_PV_args";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toString(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the PV/bootloader_args field of the given VM.
     *
     * @return value of the field
     */
    public String getPVBootloaderArgs(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.get_PV_bootloader_args";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toString(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the PV/legacy_args field of the given VM.
     *
     * @return value of the field
     */
    public String getPVLegacyArgs(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.get_PV_legacy_args";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toString(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the HVM/boot_policy field of the given VM.
     *
     * @return value of the field
     */
    public String getHVMBootPolicy(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.get_HVM_boot_policy";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toString(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the HVM/boot_params field of the given VM.
     *
     * @return value of the field
     */
    public Map<String, String> getHVMBootParams(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.get_HVM_boot_params";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toMapOfStringString(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the HVM/shadow_multiplier field of the given VM.
     *
     * @return value of the field
     */
    public Double getHVMShadowMultiplier(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.get_HVM_shadow_multiplier";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toDouble(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the platform field of the given VM.
     *
     * @return value of the field
     */
    public Map<String, String> getPlatform(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.get_platform";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toMapOfStringString(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the PCI_bus field of the given VM.
     *
     * @return value of the field
     */
    public String getPCIBus(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.get_PCI_bus";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toString(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the other_config field of the given VM.
     *
     * @return value of the field
     */
    public Map<String, String> getOtherConfig(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.get_other_config";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toMapOfStringString(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the domid field of the given VM.
     *
     * @return value of the field
     */
    public Long getDomid(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.get_domid";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toLong(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the domarch field of the given VM.
     *
     * @return value of the field
     */
    public String getDomarch(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.get_domarch";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toString(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the last_boot_CPU_flags field of the given VM.
     *
     * @return value of the field
     */
    public Map<String, String> getLastBootCPUFlags(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.get_last_boot_CPU_flags";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toMapOfStringString(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the is_control_domain field of the given VM.
     *
     * @return value of the field
     */
    public Boolean getIsControlDomain(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.get_is_control_domain";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toBoolean(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the metrics field of the given VM.
     *
     * @return value of the field
     */
    public VMMetrics getMetrics(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.get_metrics";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toVMMetrics(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the guest_metrics field of the given VM.
     *
     * @return value of the field
     */
    public VMGuestMetrics getGuestMetrics(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.get_guest_metrics";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toVMGuestMetrics(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the recommendations field of the given VM.
     *
     * @return value of the field
     */
    public String getRecommendations(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.get_recommendations";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toString(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Get the xenstore_data field of the given VM.
     *
     * @return value of the field
     */
    public Map<String, String> getXenstoreData(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.get_xenstore_data";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toMapOfStringString(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Set the name/label field of the given VM.
     *
     * @param label New value to set
     */
    public void setNameLabel(Connection c, String label) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.set_name_label";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(label)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Set the name/description field of the given VM.
     *
     * @param description New value to set
     */
    public void setNameDescription(Connection c, String description) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.set_name_description";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(description)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Set the user_version field of the given VM.
     *
     * @param userVersion New value to set
     */
    public void setUserVersion(Connection c, Long userVersion) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.set_user_version";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(userVersion)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Set the is_a_template field of the given VM.
     *
     * @param isATemplate New value to set
     */
    public void setIsATemplate(Connection c, Boolean isATemplate) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.set_is_a_template";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(isATemplate)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Set the affinity field of the given VM.
     *
     * @param affinity New value to set
     */
    public void setAffinity(Connection c, Host affinity) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.set_affinity";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(affinity)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Set the memory/static_max field of the given VM.
     *
     * @param staticMax New value to set
     */
    public void setMemoryStaticMax(Connection c, Long staticMax) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.set_memory_static_max";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(staticMax)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Set the memory/dynamic_max field of the given VM.
     *
     * @param dynamicMax New value to set
     */
    public void setMemoryDynamicMax(Connection c, Long dynamicMax) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.set_memory_dynamic_max";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(dynamicMax)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Set the memory/dynamic_min field of the given VM.
     *
     * @param dynamicMin New value to set
     */
    public void setMemoryDynamicMin(Connection c, Long dynamicMin) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.set_memory_dynamic_min";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(dynamicMin)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Set the memory/static_min field of the given VM.
     *
     * @param staticMin New value to set
     */
    public void setMemoryStaticMin(Connection c, Long staticMin) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.set_memory_static_min";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(staticMin)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Set the VCPUs/params field of the given VM.
     *
     * @param params New value to set
     */
    public void setVCPUsParams(Connection c, Map<String, String> params) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.set_VCPUs_params";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(params)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Add the given key-value pair to the VCPUs/params field of the given VM.
     *
     * @param key Key to add
     * @param value Value to add
     */
    public void addToVCPUsParams(Connection c, String key, String value) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.add_to_VCPUs_params";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(key), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Remove the given key and its corresponding value from the VCPUs/params field of the given VM.  If the key is not in that Map, then do nothing.
     *
     * @param key Key to remove
     */
    public void removeFromVCPUsParams(Connection c, String key) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.remove_from_VCPUs_params";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(key)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Set the VCPUs/max field of the given VM.
     *
     * @param max New value to set
     */
    public void setVCPUsMax(Connection c, Long max) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.set_VCPUs_max";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(max)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Set the VCPUs/at_startup field of the given VM.
     *
     * @param atStartup New value to set
     */
    public void setVCPUsAtStartup(Connection c, Long atStartup) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.set_VCPUs_at_startup";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(atStartup)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Set the actions/after_shutdown field of the given VM.
     *
     * @param afterShutdown New value to set
     */
    public void setActionsAfterShutdown(Connection c, Types.OnNormalExit afterShutdown) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.set_actions_after_shutdown";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(afterShutdown)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Set the actions/after_reboot field of the given VM.
     *
     * @param afterReboot New value to set
     */
    public void setActionsAfterReboot(Connection c, Types.OnNormalExit afterReboot) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.set_actions_after_reboot";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(afterReboot)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Set the actions/after_crash field of the given VM.
     *
     * @param afterCrash New value to set
     */
    public void setActionsAfterCrash(Connection c, Types.OnCrashBehaviour afterCrash) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.set_actions_after_crash";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(afterCrash)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Set the PV/bootloader field of the given VM.
     *
     * @param bootloader New value to set
     */
    public void setPVBootloader(Connection c, String bootloader) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.set_PV_bootloader";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(bootloader)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Set the PV/kernel field of the given VM.
     *
     * @param kernel New value to set
     */
    public void setPVKernel(Connection c, String kernel) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.set_PV_kernel";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(kernel)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Set the PV/ramdisk field of the given VM.
     *
     * @param ramdisk New value to set
     */
    public void setPVRamdisk(Connection c, String ramdisk) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.set_PV_ramdisk";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(ramdisk)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Set the PV/args field of the given VM.
     *
     * @param args New value to set
     */
    public void setPVArgs(Connection c, String args) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.set_PV_args";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(args)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Set the PV/bootloader_args field of the given VM.
     *
     * @param bootloaderArgs New value to set
     */
    public void setPVBootloaderArgs(Connection c, String bootloaderArgs) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.set_PV_bootloader_args";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(bootloaderArgs)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Set the PV/legacy_args field of the given VM.
     *
     * @param legacyArgs New value to set
     */
    public void setPVLegacyArgs(Connection c, String legacyArgs) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.set_PV_legacy_args";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(legacyArgs)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Set the HVM/boot_policy field of the given VM.
     *
     * @param bootPolicy New value to set
     */
    public void setHVMBootPolicy(Connection c, String bootPolicy) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.set_HVM_boot_policy";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(bootPolicy)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Set the HVM/boot_params field of the given VM.
     *
     * @param bootParams New value to set
     */
    public void setHVMBootParams(Connection c, Map<String, String> bootParams) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.set_HVM_boot_params";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(bootParams)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Add the given key-value pair to the HVM/boot_params field of the given VM.
     *
     * @param key Key to add
     * @param value Value to add
     */
    public void addToHVMBootParams(Connection c, String key, String value) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.add_to_HVM_boot_params";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(key), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Remove the given key and its corresponding value from the HVM/boot_params field of the given VM.  If the key is not in that Map, then do nothing.
     *
     * @param key Key to remove
     */
    public void removeFromHVMBootParams(Connection c, String key) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.remove_from_HVM_boot_params";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(key)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Set the HVM/shadow_multiplier field of the given VM.
     *
     * @param shadowMultiplier New value to set
     */
    public void setHVMShadowMultiplier(Connection c, Double shadowMultiplier) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.set_HVM_shadow_multiplier";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(shadowMultiplier)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Set the platform field of the given VM.
     *
     * @param platform New value to set
     */
    public void setPlatform(Connection c, Map<String, String> platform) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.set_platform";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(platform)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Add the given key-value pair to the platform field of the given VM.
     *
     * @param key Key to add
     * @param value Value to add
     */
    public void addToPlatform(Connection c, String key, String value) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.add_to_platform";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(key), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Remove the given key and its corresponding value from the platform field of the given VM.  If the key is not in that Map, then do nothing.
     *
     * @param key Key to remove
     */
    public void removeFromPlatform(Connection c, String key) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.remove_from_platform";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(key)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Set the PCI_bus field of the given VM.
     *
     * @param PCIBus New value to set
     */
    public void setPCIBus(Connection c, String PCIBus) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.set_PCI_bus";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(PCIBus)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Set the other_config field of the given VM.
     *
     * @param otherConfig New value to set
     */
    public void setOtherConfig(Connection c, Map<String, String> otherConfig) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.set_other_config";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(otherConfig)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Add the given key-value pair to the other_config field of the given VM.
     *
     * @param key Key to add
     * @param value Value to add
     */
    public void addToOtherConfig(Connection c, String key, String value) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.add_to_other_config";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(key), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Remove the given key and its corresponding value from the other_config field of the given VM.  If the key is not in that Map, then do nothing.
     *
     * @param key Key to remove
     */
    public void removeFromOtherConfig(Connection c, String key) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.remove_from_other_config";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(key)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Set the recommendations field of the given VM.
     *
     * @param recommendations New value to set
     */
    public void setRecommendations(Connection c, String recommendations) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.set_recommendations";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(recommendations)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Set the xenstore_data field of the given VM.
     *
     * @param xenstoreData New value to set
     */
    public void setXenstoreData(Connection c, Map<String, String> xenstoreData) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.set_xenstore_data";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(xenstoreData)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Add the given key-value pair to the xenstore_data field of the given VM.
     *
     * @param key Key to add
     * @param value Value to add
     */
    public void addToXenstoreData(Connection c, String key, String value) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.add_to_xenstore_data";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(key), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Remove the given key and its corresponding value from the xenstore_data field of the given VM.  If the key is not in that Map, then do nothing.
     *
     * @param key Key to remove
     */
    public void removeFromXenstoreData(Connection c, String key) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.remove_from_xenstore_data";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(key)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Clones the specified VM, making a new VM. Clone automatically exploits the capabilities of the underlying storage repository in which the VM's disk images are stored (e.g. Copy on Write).   This function can only be called when the VM is in the Halted State.
     *
     * @param newName The name of the cloned VM
     * @return The reference of the newly created VM.
     */
    public VM createClone(Connection c, String newName) throws
       Types.BadServerResponse,
       XmlRpcException,
       Types.VmBadPowerState,
       Types.SrFull,
       Types.OperationNotAllowed {
        String method_call = "VM.clone";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(newName)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toVM(result);
        } else if(response.get("Status").equals("Failure")) {
            Object[] error = (Object[]) response.get("ErrorDescription");
            if(error[0].equals("VM_BAD_POWER_STATE")) {
                throw new Types.VmBadPowerState((String) error[1], (String) error[2], (String) error[3]);
            }
            if(error[0].equals("SR_FULL")) {
                throw new Types.SrFull((String) error[1], (String) error[2]);
            }
            if(error[0].equals("OPERATION_NOT_ALLOWED")) {
                throw new Types.OperationNotAllowed((String) error[1]);
            }
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Copied the specified VM, making a new VM. Unlike clone, copy does not exploits the capabilities of the underlying storage repository in which the VM's disk images are stored. Instead, copy guarantees that the disk images of the newly created VM will be 'full disks' - i.e. not part of a CoW chain.  This function can only be called when the VM is in the Halted State.
     *
     * @param newName The name of the copied VM
     * @param sr An SR to copy all the VM's disks into (if an invalid reference then it uses the existing SRs)
     * @return The reference of the newly created VM.
     */
    public VM copy(Connection c, String newName, SR sr) throws
       Types.BadServerResponse,
       XmlRpcException,
       Types.VmBadPowerState,
       Types.SrFull,
       Types.OperationNotAllowed {
        String method_call = "VM.copy";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(newName), Marshalling.toXMLRPC(sr)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toVM(result);
        } else if(response.get("Status").equals("Failure")) {
            Object[] error = (Object[]) response.get("ErrorDescription");
            if(error[0].equals("VM_BAD_POWER_STATE")) {
                throw new Types.VmBadPowerState((String) error[1], (String) error[2], (String) error[3]);
            }
            if(error[0].equals("SR_FULL")) {
                throw new Types.SrFull((String) error[1], (String) error[2]);
            }
            if(error[0].equals("OPERATION_NOT_ALLOWED")) {
                throw new Types.OperationNotAllowed((String) error[1]);
            }
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Inspects the disk configuration contained within the VM's other_config, creates VDIs and VBDs and then executes any applicable post-install script.
     *
     */
    public void provision(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException,
       Types.VmBadPowerState,
       Types.SrFull,
       Types.OperationNotAllowed {
        String method_call = "VM.provision";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        } else if(response.get("Status").equals("Failure")) {
            Object[] error = (Object[]) response.get("ErrorDescription");
            if(error[0].equals("VM_BAD_POWER_STATE")) {
                throw new Types.VmBadPowerState((String) error[1], (String) error[2], (String) error[3]);
            }
            if(error[0].equals("SR_FULL")) {
                throw new Types.SrFull((String) error[1], (String) error[2]);
            }
            if(error[0].equals("OPERATION_NOT_ALLOWED")) {
                throw new Types.OperationNotAllowed((String) error[1]);
            }
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Start the specified VM.  This function can only be called with the VM is in the Halted State.
     *
     * @param startPaused Instantiate VM in paused state if set to true.
     * @param force Attempt to force the VM to start. If this flag is false then the VM may fail pre-boot safety checks (e.g. if the CPU the VM last booted on looks substantially different to the current one)
     */
    public void start(Connection c, Boolean startPaused, Boolean force) throws
       Types.BadServerResponse,
       XmlRpcException,
       Types.VmBadPowerState,
       Types.VmHvmRequired,
       Types.VmIsTemplate,
       Types.OtherOperationInProgress,
       Types.OperationNotAllowed,
       Types.BootloaderFailed,
       Types.UnknownBootloader,
       Types.NoHostsAvailable,
       Types.LicenceRestriction {
        String method_call = "VM.start";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(startPaused), Marshalling.toXMLRPC(force)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        } else if(response.get("Status").equals("Failure")) {
            Object[] error = (Object[]) response.get("ErrorDescription");
            if(error[0].equals("VM_BAD_POWER_STATE")) {
                throw new Types.VmBadPowerState((String) error[1], (String) error[2], (String) error[3]);
            }
            if(error[0].equals("VM_HVM_REQUIRED")) {
                throw new Types.VmHvmRequired((String) error[1]);
            }
            if(error[0].equals("VM_IS_TEMPLATE")) {
                throw new Types.VmIsTemplate((String) error[1]);
            }
            if(error[0].equals("OTHER_OPERATION_IN_PROGRESS")) {
                throw new Types.OtherOperationInProgress((String) error[1], (String) error[2]);
            }
            if(error[0].equals("OPERATION_NOT_ALLOWED")) {
                throw new Types.OperationNotAllowed((String) error[1]);
            }
            if(error[0].equals("BOOTLOADER_FAILED")) {
                throw new Types.BootloaderFailed((String) error[1], (String) error[2]);
            }
            if(error[0].equals("UNKNOWN_BOOTLOADER")) {
                throw new Types.UnknownBootloader((String) error[1], (String) error[2]);
            }
            if(error[0].equals("NO_HOSTS_AVAILABLE")) {
                throw new Types.NoHostsAvailable();
            }
            if(error[0].equals("LICENCE_RESTRICTION")) {
                throw new Types.LicenceRestriction();
            }
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Start the specified VM on a particular host.  This function can only be called with the VM is in the Halted State.
     *
     * @param host The Host on which to start the VM
     * @param startPaused Instantiate VM in paused state if set to true.
     * @param force Attempt to force the VM to start. If this flag is false then the VM may fail pre-boot safety checks (e.g. if the CPU the VM last booted on looks substantially different to the current one)
     */
    public void startOn(Connection c, Host host, Boolean startPaused, Boolean force) throws
       Types.BadServerResponse,
       XmlRpcException,
       Types.VmBadPowerState,
       Types.VmIsTemplate,
       Types.OtherOperationInProgress,
       Types.OperationNotAllowed,
       Types.BootloaderFailed,
       Types.UnknownBootloader {
        String method_call = "VM.start_on";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(host), Marshalling.toXMLRPC(startPaused), Marshalling.toXMLRPC(force)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        } else if(response.get("Status").equals("Failure")) {
            Object[] error = (Object[]) response.get("ErrorDescription");
            if(error[0].equals("VM_BAD_POWER_STATE")) {
                throw new Types.VmBadPowerState((String) error[1], (String) error[2], (String) error[3]);
            }
            if(error[0].equals("VM_IS_TEMPLATE")) {
                throw new Types.VmIsTemplate((String) error[1]);
            }
            if(error[0].equals("OTHER_OPERATION_IN_PROGRESS")) {
                throw new Types.OtherOperationInProgress((String) error[1], (String) error[2]);
            }
            if(error[0].equals("OPERATION_NOT_ALLOWED")) {
                throw new Types.OperationNotAllowed((String) error[1]);
            }
            if(error[0].equals("BOOTLOADER_FAILED")) {
                throw new Types.BootloaderFailed((String) error[1], (String) error[2]);
            }
            if(error[0].equals("UNKNOWN_BOOTLOADER")) {
                throw new Types.UnknownBootloader((String) error[1], (String) error[2]);
            }
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Pause the specified VM. This can only be called when the specified VM is in the Running state.
     *
     */
    public void pause(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException,
       Types.VmBadPowerState,
       Types.OtherOperationInProgress,
       Types.OperationNotAllowed,
       Types.VmIsTemplate {
        String method_call = "VM.pause";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        } else if(response.get("Status").equals("Failure")) {
            Object[] error = (Object[]) response.get("ErrorDescription");
            if(error[0].equals("VM_BAD_POWER_STATE")) {
                throw new Types.VmBadPowerState((String) error[1], (String) error[2], (String) error[3]);
            }
            if(error[0].equals("OTHER_OPERATION_IN_PROGRESS")) {
                throw new Types.OtherOperationInProgress((String) error[1], (String) error[2]);
            }
            if(error[0].equals("OPERATION_NOT_ALLOWED")) {
                throw new Types.OperationNotAllowed((String) error[1]);
            }
            if(error[0].equals("VM_IS_TEMPLATE")) {
                throw new Types.VmIsTemplate((String) error[1]);
            }
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Resume the specified VM. This can only be called when the specified VM is in the Paused state.
     *
     */
    public void unpause(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException,
       Types.VmBadPowerState,
       Types.OperationNotAllowed,
       Types.VmIsTemplate {
        String method_call = "VM.unpause";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        } else if(response.get("Status").equals("Failure")) {
            Object[] error = (Object[]) response.get("ErrorDescription");
            if(error[0].equals("VM_BAD_POWER_STATE")) {
                throw new Types.VmBadPowerState((String) error[1], (String) error[2], (String) error[3]);
            }
            if(error[0].equals("OPERATION_NOT_ALLOWED")) {
                throw new Types.OperationNotAllowed((String) error[1]);
            }
            if(error[0].equals("VM_IS_TEMPLATE")) {
                throw new Types.VmIsTemplate((String) error[1]);
            }
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Attempt to cleanly shutdown the specified VM. (Note: this may not be supported---e.g. if a guest agent is not installed). This can only be called when the specified VM is in the Running state.
     *
     */
    public void cleanShutdown(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException,
       Types.VmBadPowerState,
       Types.OtherOperationInProgress,
       Types.OperationNotAllowed,
       Types.VmIsTemplate {
        String method_call = "VM.clean_shutdown";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        } else if(response.get("Status").equals("Failure")) {
            Object[] error = (Object[]) response.get("ErrorDescription");
            if(error[0].equals("VM_BAD_POWER_STATE")) {
                throw new Types.VmBadPowerState((String) error[1], (String) error[2], (String) error[3]);
            }
            if(error[0].equals("OTHER_OPERATION_IN_PROGRESS")) {
                throw new Types.OtherOperationInProgress((String) error[1], (String) error[2]);
            }
            if(error[0].equals("OPERATION_NOT_ALLOWED")) {
                throw new Types.OperationNotAllowed((String) error[1]);
            }
            if(error[0].equals("VM_IS_TEMPLATE")) {
                throw new Types.VmIsTemplate((String) error[1]);
            }
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Attempt to cleanly shutdown the specified VM (Note: this may not be supported---e.g. if a guest agent is not installed). This can only be called when the specified VM is in the Running state.
     *
     */
    public void cleanReboot(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException,
       Types.VmBadPowerState,
       Types.OtherOperationInProgress,
       Types.OperationNotAllowed,
       Types.VmIsTemplate {
        String method_call = "VM.clean_reboot";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        } else if(response.get("Status").equals("Failure")) {
            Object[] error = (Object[]) response.get("ErrorDescription");
            if(error[0].equals("VM_BAD_POWER_STATE")) {
                throw new Types.VmBadPowerState((String) error[1], (String) error[2], (String) error[3]);
            }
            if(error[0].equals("OTHER_OPERATION_IN_PROGRESS")) {
                throw new Types.OtherOperationInProgress((String) error[1], (String) error[2]);
            }
            if(error[0].equals("OPERATION_NOT_ALLOWED")) {
                throw new Types.OperationNotAllowed((String) error[1]);
            }
            if(error[0].equals("VM_IS_TEMPLATE")) {
                throw new Types.VmIsTemplate((String) error[1]);
            }
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Stop executing the specified VM without attempting a clean shutdown.
     *
     */
    public void hardShutdown(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException,
       Types.VmBadPowerState,
       Types.OtherOperationInProgress,
       Types.OperationNotAllowed,
       Types.VmIsTemplate {
        String method_call = "VM.hard_shutdown";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        } else if(response.get("Status").equals("Failure")) {
            Object[] error = (Object[]) response.get("ErrorDescription");
            if(error[0].equals("VM_BAD_POWER_STATE")) {
                throw new Types.VmBadPowerState((String) error[1], (String) error[2], (String) error[3]);
            }
            if(error[0].equals("OTHER_OPERATION_IN_PROGRESS")) {
                throw new Types.OtherOperationInProgress((String) error[1], (String) error[2]);
            }
            if(error[0].equals("OPERATION_NOT_ALLOWED")) {
                throw new Types.OperationNotAllowed((String) error[1]);
            }
            if(error[0].equals("VM_IS_TEMPLATE")) {
                throw new Types.VmIsTemplate((String) error[1]);
            }
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Reset the power-state of the VM to halted in the database only. (Used to recover from slave failures in pooling scenarios by resetting the power-states of VMs running on dead slaves to halted.) This is a potentially dangerous operation; use with care.
     *
     */
    public void powerStateReset(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.power_state_reset";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Stop executing the specified VM without attempting a clean shutdown and immediately restart the VM.
     *
     */
    public void hardReboot(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException,
       Types.VmBadPowerState,
       Types.OtherOperationInProgress,
       Types.OperationNotAllowed,
       Types.VmIsTemplate {
        String method_call = "VM.hard_reboot";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        } else if(response.get("Status").equals("Failure")) {
            Object[] error = (Object[]) response.get("ErrorDescription");
            if(error[0].equals("VM_BAD_POWER_STATE")) {
                throw new Types.VmBadPowerState((String) error[1], (String) error[2], (String) error[3]);
            }
            if(error[0].equals("OTHER_OPERATION_IN_PROGRESS")) {
                throw new Types.OtherOperationInProgress((String) error[1], (String) error[2]);
            }
            if(error[0].equals("OPERATION_NOT_ALLOWED")) {
                throw new Types.OperationNotAllowed((String) error[1]);
            }
            if(error[0].equals("VM_IS_TEMPLATE")) {
                throw new Types.VmIsTemplate((String) error[1]);
            }
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Suspend the specified VM to disk.  This can only be called when the specified VM is in the Running state.
     *
     */
    public void suspend(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException,
       Types.VmBadPowerState,
       Types.OtherOperationInProgress,
       Types.OperationNotAllowed,
       Types.VmIsTemplate {
        String method_call = "VM.suspend";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        } else if(response.get("Status").equals("Failure")) {
            Object[] error = (Object[]) response.get("ErrorDescription");
            if(error[0].equals("VM_BAD_POWER_STATE")) {
                throw new Types.VmBadPowerState((String) error[1], (String) error[2], (String) error[3]);
            }
            if(error[0].equals("OTHER_OPERATION_IN_PROGRESS")) {
                throw new Types.OtherOperationInProgress((String) error[1], (String) error[2]);
            }
            if(error[0].equals("OPERATION_NOT_ALLOWED")) {
                throw new Types.OperationNotAllowed((String) error[1]);
            }
            if(error[0].equals("VM_IS_TEMPLATE")) {
                throw new Types.VmIsTemplate((String) error[1]);
            }
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Awaken the specified VM and resume it.  This can only be called when the specified VM is in the Suspended state.
     *
     * @param startPaused Resume VM in paused state if set to true.
     * @param force Attempt to force the VM to resume. If this flag is false then the VM may fail pre-resume safety checks (e.g. if the CPU the VM was running on looks substantially different to the current one)
     */
    public void resume(Connection c, Boolean startPaused, Boolean force) throws
       Types.BadServerResponse,
       XmlRpcException,
       Types.VmBadPowerState,
       Types.OperationNotAllowed,
       Types.VmIsTemplate {
        String method_call = "VM.resume";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(startPaused), Marshalling.toXMLRPC(force)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        } else if(response.get("Status").equals("Failure")) {
            Object[] error = (Object[]) response.get("ErrorDescription");
            if(error[0].equals("VM_BAD_POWER_STATE")) {
                throw new Types.VmBadPowerState((String) error[1], (String) error[2], (String) error[3]);
            }
            if(error[0].equals("OPERATION_NOT_ALLOWED")) {
                throw new Types.OperationNotAllowed((String) error[1]);
            }
            if(error[0].equals("VM_IS_TEMPLATE")) {
                throw new Types.VmIsTemplate((String) error[1]);
            }
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Awaken the specified VM and resume it on a particular Host.  This can only be called when the specified VM is in the Suspended state.
     *
     * @param host The Host on which to resume the VM
     * @param startPaused Resume VM in paused state if set to true.
     * @param force Attempt to force the VM to resume. If this flag is false then the VM may fail pre-resume safety checks (e.g. if the CPU the VM was running on looks substantially different to the current one)
     */
    public void resumeOn(Connection c, Host host, Boolean startPaused, Boolean force) throws
       Types.BadServerResponse,
       XmlRpcException,
       Types.VmBadPowerState,
       Types.OperationNotAllowed,
       Types.VmIsTemplate {
        String method_call = "VM.resume_on";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(host), Marshalling.toXMLRPC(startPaused), Marshalling.toXMLRPC(force)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        } else if(response.get("Status").equals("Failure")) {
            Object[] error = (Object[]) response.get("ErrorDescription");
            if(error[0].equals("VM_BAD_POWER_STATE")) {
                throw new Types.VmBadPowerState((String) error[1], (String) error[2], (String) error[3]);
            }
            if(error[0].equals("OPERATION_NOT_ALLOWED")) {
                throw new Types.OperationNotAllowed((String) error[1]);
            }
            if(error[0].equals("VM_IS_TEMPLATE")) {
                throw new Types.VmIsTemplate((String) error[1]);
            }
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Migrate a VM to another Host. This can only be called when the specified VM is in the Running state.
     *
     * @param host The target host
     * @param options Extra configuration operations
     */
    public void poolMigrate(Connection c, Host host, Map<String, String> options) throws
       Types.BadServerResponse,
       XmlRpcException,
       Types.VmBadPowerState,
       Types.OtherOperationInProgress,
       Types.VmIsTemplate,
       Types.OperationNotAllowed,
       Types.VmMigrateFailed {
        String method_call = "VM.pool_migrate";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(host), Marshalling.toXMLRPC(options)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        } else if(response.get("Status").equals("Failure")) {
            Object[] error = (Object[]) response.get("ErrorDescription");
            if(error[0].equals("VM_BAD_POWER_STATE")) {
                throw new Types.VmBadPowerState((String) error[1], (String) error[2], (String) error[3]);
            }
            if(error[0].equals("OTHER_OPERATION_IN_PROGRESS")) {
                throw new Types.OtherOperationInProgress((String) error[1], (String) error[2]);
            }
            if(error[0].equals("VM_IS_TEMPLATE")) {
                throw new Types.VmIsTemplate((String) error[1]);
            }
            if(error[0].equals("OPERATION_NOT_ALLOWED")) {
                throw new Types.OperationNotAllowed((String) error[1]);
            }
            if(error[0].equals("VM_MIGRATE_FAILED")) {
                throw new Types.VmMigrateFailed((String) error[1], (String) error[2], (String) error[3], (String) error[4]);
            }
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Set this VM's VCPUs/at_startup value, and set the same value on the VM, if running
     *
     * @param nvcpu The number of VCPUs
     */
    public void setVCPUsNumberLive(Connection c, Long nvcpu) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.set_VCPUs_number_live";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(nvcpu)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Add the given key-value pair to VM.VCPUs_params, and apply that value on the running VM
     *
     * @param key The key
     * @param value The value
     */
    public void addToVCPUsParamsLive(Connection c, String key, String value) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.add_to_VCPUs_params_live";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(key), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Set the balloon driver's target on a running VM
     *
     * @param target The target in bytes
     */
    public void setMemoryTargetLive(Connection c, Long target) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.set_memory_target_live";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(target)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Set the shadow memory on a running VM with the new shadow multiplier
     *
     * @param multiplier The new shadow multiplier to set
     */
    public void setShadowMultiplierLive(Connection c, Double multiplier) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.set_shadow_multiplier_live";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(multiplier)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Send the given key as a sysrq to this VM.  The key is specified as a single character (a String of length 1).  This can only be called when the specified VM is in the Running state.
     *
     * @param key The key to send
     */
    public void sendSysrq(Connection c, String key) throws
       Types.BadServerResponse,
       XmlRpcException,
       Types.VmBadPowerState {
        String method_call = "VM.send_sysrq";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(key)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        } else if(response.get("Status").equals("Failure")) {
            Object[] error = (Object[]) response.get("ErrorDescription");
            if(error[0].equals("VM_BAD_POWER_STATE")) {
                throw new Types.VmBadPowerState((String) error[1], (String) error[2], (String) error[3]);
            }
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Send the named trigger to this VM.  This can only be called when the specified VM is in the Running state.
     *
     * @param trigger The trigger to send
     */
    public void sendTrigger(Connection c, String trigger) throws
       Types.BadServerResponse,
       XmlRpcException,
       Types.VmBadPowerState {
        String method_call = "VM.send_trigger";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(trigger)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        } else if(response.get("Status").equals("Failure")) {
            Object[] error = (Object[]) response.get("ErrorDescription");
            if(error[0].equals("VM_BAD_POWER_STATE")) {
                throw new Types.VmBadPowerState((String) error[1], (String) error[2], (String) error[3]);
            }
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Returns the maximum amount of guest memory which will fit, together with overheads, in the supplied amount of physical memory. If 'exact' is true then an exact calculation is performed using the VM's current settings. If 'exact' is false then a more conservative approximation is used
     *
     * @param total Total amount of physical RAM to fit within
     * @param approximate If false the limit is calculated with the guest's current exact configuration. Otherwise a more approximate calculation is performed
     * @return The maximum possible static-max
     */
    public Long maximiseMemory(Connection c, Long total, Boolean approximate) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.maximise_memory";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(total), Marshalling.toXMLRPC(approximate)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toLong(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Returns a record describing the VM's dynamic state, initialised when the VM boots and updated to reflect runtime configuration changes e.g. CPU hotplug
     *
     * @return A record describing the VM
     */
    public VM.Record getBootRecord(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.get_boot_record";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toVMRecord(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Check to see whether this operation is acceptable in the current state of the system, raising an error if the operation is invalid for some reason
     *
     * @param op proposed operation
     */
    public void assertOperationValid(Connection c, Types.VmOperations op) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.assert_operation_valid";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(op)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Recomputes the list of acceptable operations
     *
     */
    public void updateAllowedOperations(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.update_allowed_operations";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Returns a list of the allowed values that a VBD device field can take
     *
     * @return The allowed values
     */
    public Set<String> getAllowedVBDDevices(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.get_allowed_VBD_devices";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toSetOfString(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Returns a list of the allowed values that a VIF device field can take
     *
     * @return The allowed values
     */
    public Set<String> getAllowedVIFDevices(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.get_allowed_VIF_devices";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toSetOfString(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Return the list of hosts on which this VM may run.
     *
     * @return The possible hosts
     */
    public Set<Host> getPossibleHosts(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.get_possible_hosts";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toSetOfHost(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Returns an error if the VM could not boot on this host for some reason
     *
     * @param host The host
     */
    public void assertCanBootHere(Connection c, Host host) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.assert_can_boot_here";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(host)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return;
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Return a list of all the VMs known to the system.
     *
     * @return references to all objects
     */
    public static Set<VM> getAll(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.get_all";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toSetOfVM(result);
        }
        throw new Types.BadServerResponse(response);
    }

    /**
     * Return a map of VM references to VM records for all VMs known to the system.
     *
     * @return records of all objects
     */
    public static Map<VM, VM.Record> getAllRecords(Connection c) throws
       Types.BadServerResponse,
       XmlRpcException {
        String method_call = "VM.get_all_records";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session)};
        Map response = c.dispatch(method_call, method_params);
        if(response.get("Status").equals("Success")) {
            Object result = response.get("Value");
            return Types.toMapOfVMVMRecord(result);
        }
        throw new Types.BadServerResponse(response);
    }

}