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

import java.util.Date;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.io.IOException;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * This class holds vital marshalling functions,
 * enum types and exceptions.
 *
 * @author XenSource Inc.
 */
public class Types {

    /**
     * Interface for all Record classes
     */
    public static interface Record {
        /**
         * Convert a Record to a Map
         */
        Map<String,Object> toMap();
    }

    /**
     * Base class for all XenAPI Exceptions
     */
    public static class XenAPIException extends IOException {
    }

    /**
     * Thrown if the response from the server contains an invalid status.
     */
    public static class BadServerResponse extends XenAPIException {
        public final Map response;
        public BadServerResponse(Map response) {
            this.response = response;
        }

        public String toString() {
            try {
                Object[] errDesc = (Object[]) 
                    this.response.get("ErrorDescription");
                return super.toString()+" "+java.util.Arrays.deepToString(errDesc);
            } catch (Exception e) {
                return super.toString()+" "+this.response.toString();
            }
        }
    }

    public static class BadAsyncResult extends XenAPIException {
        public final String result;
        public BadAsyncResult(String result) {
            this.result=result;
        }
        public String toString(){
            return result;
        }
    }

     /*
      * A call has been made which should not be made against this version of host.
      * Probably the host is out of date and cannot handle this call, or is
      * unable to comply with the details of the call. For instance SR.create
      * on "Miami" hosts takes an smConfig parameter, which must be an empty map 
      * when making this call on "Rio" hosts.
      */
     public static class VersionException extends XenAPIException {
         public final String result;
         public VersionException(String result) {
             this.result=result;
         }
         public String toString(){
             return result;
         }
     }

    private static String parseResult (String result) throws BadAsyncResult 
    {
        Pattern pattern = Pattern.compile("<value>(.*)</value>");
        Matcher matcher = pattern.matcher(result);
        matcher.find();

        if(matcher.groupCount()!=1)
        {
            throw new Types.BadAsyncResult("Can't interpret: "+result);
        }

        return matcher.group(1);
    }

    public enum ConsoleProtocol {
        /**
         * The value does not belong to this enumeration
         */
        UNRECOGNIZED,
        /**
         * VT100 terminal
         */
        VT100,
        /**
         * Remote FrameBuffer protocol (as used in VNC)
         */
        RFB,
        /**
         * Remote Desktop Protocol
         */
        RDP
    };

    public enum VbdMode {
        /**
         * The value does not belong to this enumeration
         */
        UNRECOGNIZED,
        /**
         * only read-only access will be allowed
         */
        RO,
        /**
         * read-write access will be allowed
         */
        RW
    };

    public enum IpConfigurationMode {
        /**
         * The value does not belong to this enumeration
         */
        UNRECOGNIZED,
        /**
         * Do not acquire an IP address
         */
        NONE,
        /**
         * Acquire an IP address by DHCP
         */
        DHCP,
        /**
         * Static IP address configuration
         */
        STATIC
    };

    public enum VmOperations {
        /**
         * The value does not belong to this enumeration
         */
        UNRECOGNIZED,
        /**
         * refers to the operation "clone"
         */
        CLONE,
        /**
         * refers to the operation "copy"
         */
        COPY,
        /**
         * refers to the operation "provision"
         */
        PROVISION,
        /**
         * refers to the operation "start"
         */
        START,
        /**
         * refers to the operation "start_on"
         */
        START_ON,
        /**
         * refers to the operation "pause"
         */
        PAUSE,
        /**
         * refers to the operation "unpause"
         */
        UNPAUSE,
        /**
         * refers to the operation "clean_shutdown"
         */
        CLEAN_SHUTDOWN,
        /**
         * refers to the operation "clean_reboot"
         */
        CLEAN_REBOOT,
        /**
         * refers to the operation "hard_shutdown"
         */
        HARD_SHUTDOWN,
        /**
         * refers to the operation "power_state_reset"
         */
        POWER_STATE_RESET,
        /**
         * refers to the operation "hard_reboot"
         */
        HARD_REBOOT,
        /**
         * refers to the operation "suspend"
         */
        SUSPEND,
        /**
         * refers to the operation "csvm"
         */
        CSVM,
        /**
         * refers to the operation "resume"
         */
        RESUME,
        /**
         * refers to the operation "resume_on"
         */
        RESUME_ON,
        /**
         * refers to the operation "pool_migrate"
         */
        POOL_MIGRATE,
        /**
         * refers to the operation "migrate"
         */
        MIGRATE,
        /**
         * refers to the operation "statistics"
         */
        STATISTICS,
        /**
         * refers to the operation "get_boot_record"
         */
        GET_BOOT_RECORD,
        /**
         * refers to the operation "send_sysrq"
         */
        SEND_SYSRQ,
        /**
         * refers to the operation "send_trigger"
         */
        SEND_TRIGGER,
        /**
         * Changing the memory settings
         */
        CHANGING_MEMORY_LIVE,
        /**
         * Changing the shadow memory settings
         */
        CHANGING_SHADOW_MEMORY_LIVE,
        /**
         * Changing either the VCPUs_number or VCPUs_params
         */
        CHANGING_VCPUS_LIVE,
        /**
         * 
         */
        ASSERT_OPERATION_VALID,
        /**
         * 
         */
        UPDATE_ALLOWED_OPERATIONS,
        /**
         * Turning this VM into a template
         */
        MAKE_INTO_TEMPLATE,
        /**
         * importing a VM from a network stream
         */
        IMPORT,
        /**
         * exporting a VM to a network stream
         */
        EXPORT,
        /**
         * refers to the act of uninstalling the VM
         */
        DESTROY
    };

    public enum VbdType {
        /**
         * The value does not belong to this enumeration
         */
        UNRECOGNIZED,
        /**
         * VBD will appear to guest as CD
         */
        CD,
        /**
         * VBD will appear to guest as disk
         */
        DISK
    };

    public enum NetworkOperations {
        /**
         * The value does not belong to this enumeration
         */
        UNRECOGNIZED,
        /**
         * Indicates this network is attaching to a VIF or PIF
         */
        ATTACHING
    };

    public enum VdiOperations {
        /**
         * The value does not belong to this enumeration
         */
        UNRECOGNIZED,
        /**
         * Scanning backends for new or deleted VDIs
         */
        SCAN,
        /**
         * Cloning the VDI
         */
        CLONE,
        /**
         * Copying the VDI
         */
        COPY,
        /**
         * Resizing the VDI
         */
        RESIZE,
        /**
         * Snapshotting the VDI
         */
        SNAPSHOT,
        /**
         * Destroying the VDI
         */
        DESTROY,
        /**
         * Forget about the VDI
         */
        FORGET,
        /**
         * Forcibly unlocking the VDI
         */
        FORCE_UNLOCK
    };

    public enum StorageOperations {
        /**
         * The value does not belong to this enumeration
         */
        UNRECOGNIZED,
        /**
         * Scanning backends for new or deleted VDIs
         */
        SCAN,
        /**
         * Destroying the SR
         */
        DESTROY,
        /**
         * Forgetting about SR
         */
        FORGET,
        /**
         * Plugging a PBD into this SR
         */
        PLUG,
        /**
         * Unplugging a PBD from this SR
         */
        UNPLUG,
        /**
         * Creating a new VDI
         */
        VDI_CREATE,
        /**
         * Introducing a new VDI
         */
        VDI_INTRODUCE,
        /**
         * Destroying a VDI
         */
        VDI_DESTROY,
        /**
         * Resizing a VDI
         */
        VDI_RESIZE,
        /**
         * Cloneing a VDI
         */
        VDI_CLONE,
        /**
         * Snapshotting a VDI
         */
        VDI_SNAPSHOT
    };

    public enum OnNormalExit {
        /**
         * The value does not belong to this enumeration
         */
        UNRECOGNIZED,
        /**
         * destroy the VM state
         */
        DESTROY,
        /**
         * restart the VM
         */
        RESTART
    };

    public enum OnCrashBehaviour {
        /**
         * The value does not belong to this enumeration
         */
        UNRECOGNIZED,
        /**
         * destroy the VM state
         */
        DESTROY,
        /**
         * record a coredump and then destroy the VM state
         */
        COREDUMP_AND_DESTROY,
        /**
         * restart the VM
         */
        RESTART,
        /**
         * record a coredump and then restart the VM
         */
        COREDUMP_AND_RESTART,
        /**
         * leave the crashed VM paused
         */
        PRESERVE,
        /**
         * rename the crashed VM and start a new copy
         */
        RENAME_RESTART
    };

    public enum VmPowerState {
        /**
         * The value does not belong to this enumeration
         */
        UNRECOGNIZED,
        /**
         * VM is offline and not using any resources
         */
        HALTED,
        /**
         * All resources have been allocated but the VM itself is paused and its vCPUs are not running
         */
        PAUSED,
        /**
         * Running
         */
        RUNNING,
        /**
         * VM state has been saved to disk and it is nolonger running. Note that disks remain in-use while the VM is suspended.
         */
        SUSPENDED,
        /**
         * Some other unknown state
         */
        UNKNOWN
    };

    public enum TaskStatusType {
        /**
         * The value does not belong to this enumeration
         */
        UNRECOGNIZED,
        /**
         * task is in progress
         */
        PENDING,
        /**
         * task was completed successfully
         */
        SUCCESS,
        /**
         * task has failed
         */
        FAILURE,
        /**
         * task is being cancelled
         */
        CANCELLING,
        /**
         * task has been cancelled
         */
        CANCELLED
    };

    public enum XenAPIObjects {
        /**
         * The value does not belong to this enumeration
         */
        UNRECOGNIZED,
        /**
         * A session
         */
        SESSION,
        /**
         * A long-running asynchronous task
         */
        TASK,
        /**
         * Asynchronous event registration and handling
         */
        EVENT,
        /**
         * Pool-wide information
         */
        POOL,
        /**
         * Pool-wide patches
         */
        POOL_PATCH,
        /**
         * A virtual machine (or 'guest').
         */
        VM,
        /**
         * The metrics associated with a VM
         */
        VM_METRICS,
        /**
         * The metrics reported by the guest (as opposed to inferred from outside)
         */
        VM_GUEST_METRICS,
        /**
         * A physical host
         */
        HOST,
        /**
         * Represents a host crash dump
         */
        HOST_CRASHDUMP,
        /**
         * Represents a patch stored on a server
         */
        HOST_PATCH,
        /**
         * The metrics associated with a host
         */
        HOST_METRICS,
        /**
         * A physical CPU
         */
        HOST_CPU,
        /**
         * A virtual network
         */
        NETWORK,
        /**
         * A virtual network interface
         */
        VIF,
        /**
         * The metrics associated with a virtual network device
         */
        VIF_METRICS,
        /**
         * A physical network interface (note separate VLANs are represented as several PIFs)
         */
        PIF,
        /**
         * The metrics associated with a physical network interface
         */
        PIF_METRICS,
        /**
         * 
         */
        BOND,
        /**
         * A VLAN mux/demux
         */
        VLAN,
        /**
         * A storage manager plugin
         */
        SM,
        /**
         * A storage repository
         */
        SR,
        /**
         * A virtual disk image
         */
        VDI,
        /**
         * A virtual block device
         */
        VBD,
        /**
         * The metrics associated with a virtual block device
         */
        VBD_METRICS,
        /**
         * The physical block devices through which hosts access SRs
         */
        PBD,
        /**
         * A VM crashdump
         */
        CRASHDUMP,
        /**
         * A virtual TPM device
         */
        VTPM,
        /**
         * A console
         */
        CONSOLE,
        /**
         * A user of the system
         */
        USER
    };

    public enum HostAllowedOperations {
        /**
         * The value does not belong to this enumeration
         */
        UNRECOGNIZED,
        /**
         * Indicates this host is able to provision another VM
         */
        PROVISION,
        /**
         * Indicates this host is evacuating
         */
        EVACUATE
    };

    public enum AfterApplyGuidance {
        /**
         * The value does not belong to this enumeration
         */
        UNRECOGNIZED,
        /**
         * This patch requires HVM guests to be restarted once applied.
         */
        RESTARTHVM,
        /**
         * This patch requires PV guests to be restarted once applied.
         */
        RESTARTPV,
        /**
         * This patch requires the host to be restarted once applied.
         */
        RESTARTHOST,
        /**
         * This patch requires XAPI to be restarted once applied.
         */
        RESTARTXAPI
    };

    public enum EventOperation {
        /**
         * The value does not belong to this enumeration
         */
        UNRECOGNIZED,
        /**
         * An object has been created
         */
        ADD,
        /**
         * An object has been deleted
         */
        DEL,
        /**
         * An object has been modified
         */
        MOD
    };

    public enum VbdOperations {
        /**
         * The value does not belong to this enumeration
         */
        UNRECOGNIZED,
        /**
         * Attempting to attach this VBD to a VM
         */
        ATTACH,
        /**
         * Attempting to eject the media from this VBD
         */
        EJECT,
        /**
         * Attempting to insert new media into this VBD
         */
        INSERT,
        /**
         * Attempting to hotplug this VBD
         */
        PLUG,
        /**
         * Attempting to hot unplug this VBD
         */
        UNPLUG,
        /**
         * Attempting to forcibly unplug this VBD
         */
        UNPLUG_FORCE
    };

    public enum TaskAllowedOperations {
        /**
         * The value does not belong to this enumeration
         */
        UNRECOGNIZED,
        /**
         * refers to the operation "cancel"
         */
        CANCEL
    };

    public enum VdiType {
        /**
         * The value does not belong to this enumeration
         */
        UNRECOGNIZED,
        /**
         * a disk that may be replaced on upgrade
         */
        SYSTEM,
        /**
         * a disk that is always preserved on upgrade
         */
        USER,
        /**
         * a disk that may be reformatted on upgrade
         */
        EPHEMERAL,
        /**
         * a disk that stores a suspend image
         */
        SUSPEND,
        /**
         * a disk that stores VM crashdump information
         */
        CRASHDUMP
    };

    public enum VifOperations {
        /**
         * The value does not belong to this enumeration
         */
        UNRECOGNIZED,
        /**
         * Attempting to attach this VIF to a VM
         */
        ATTACH,
        /**
         * Attempting to hotplug this VIF
         */
        PLUG,
        /**
         * Attempting to hot unplug this VIF
         */
        UNPLUG
    };


    /**
     * The patch precheck stage failed with an unknown error.  See attached info for more details.
     */
    public static class PatchPrecheckFailedUnknownError extends XenAPIException {
        public final String patch;
        public final String info;

        /**
         * Create a new PatchPrecheckFailedUnknownError
         *
         * @param patch
         * @param info
         */
        public PatchPrecheckFailedUnknownError(String patch, String info) {
            super();
            this.patch = patch;
            this.info = info;
        }

        public String toString() {
            return "The patch precheck stage failed with an unknown error.  See attached info for more details.";
        }
    }

    /**
     * The specified object no longer exists.
     */
    public static class ObjectNolongerExists extends XenAPIException {

        /**
         * Create a new ObjectNolongerExists
         */
        public ObjectNolongerExists() {
            super();
        }

        public String toString() {
            return "The specified object no longer exists.";
        }
    }

    /**
     * The operation attempted is not valid for a template VM
     */
    public static class VmIsTemplate extends XenAPIException {
        public final String vm;

        /**
         * Create a new VmIsTemplate
         *
         * @param vm
         */
        public VmIsTemplate(String vm) {
            super();
            this.vm = vm;
        }

        public String toString() {
            return "The operation attempted is not valid for a template VM";
        }
    }

    /**
     * The operation could not be performed while the host is still armed; it must be disarmed first
     */
    public static class HaHostIsArmed extends XenAPIException {
        public final String host;

        /**
         * Create a new HaHostIsArmed
         *
         * @param host
         */
        public HaHostIsArmed(String host) {
            super();
            this.host = host;
        }

        public String toString() {
            return "The operation could not be performed while the host is still armed; it must be disarmed first";
        }
    }

    /**
     * The uploaded patch file already exists
     */
    public static class PatchAlreadyExists extends XenAPIException {
        public final String uuid;

        /**
         * Create a new PatchAlreadyExists
         *
         * @param uuid
         */
        public PatchAlreadyExists(String uuid) {
            super();
            this.uuid = uuid;
        }

        public String toString() {
            return "The uploaded patch file already exists";
        }
    }

    /**
     * The requested update could to be obtained from the master.
     */
    public static class CannotFetchPatch extends XenAPIException {
        public final String uuid;

        /**
         * Create a new CannotFetchPatch
         *
         * @param uuid
         */
        public CannotFetchPatch(String uuid) {
            super();
            this.uuid = uuid;
        }

        public String toString() {
            return "The requested update could to be obtained from the master.";
        }
    }

    /**
     * Not enough host memory is available to perform this operation
     */
    public static class HostNotEnoughFreeMemory extends XenAPIException {

        /**
         * Create a new HostNotEnoughFreeMemory
         */
        public HostNotEnoughFreeMemory() {
            super();
        }

        public String toString() {
            return "Not enough host memory is available to perform this operation";
        }
    }

    /**
     * The function is not implemented
     */
    public static class NotImplemented extends XenAPIException {
        public final String function;

        /**
         * Create a new NotImplemented
         *
         * @param function
         */
        public NotImplemented(String function) {
            super();
            this.function = function;
        }

        public String toString() {
            return "The function is not implemented";
        }
    }

    /**
     * You tried to create a PIF, but it already exists.
     */
    public static class PifVlanExists extends XenAPIException {
        public final String PIF;

        /**
         * Create a new PifVlanExists
         *
         * @param PIF
         */
        public PifVlanExists(String PIF) {
            super();
            this.PIF = PIF;
        }

        public String toString() {
            return "You tried to create a PIF, but it already exists.";
        }
    }

    /**
     * You attempted an operation that was not allowed.
     */
    public static class OperationNotAllowed extends XenAPIException {
        public final String reason;

        /**
         * Create a new OperationNotAllowed
         *
         * @param reason
         */
        public OperationNotAllowed(String reason) {
            super();
            this.reason = reason;
        }

        public String toString() {
            return "You attempted an operation that was not allowed.";
        }
    }

    /**
     * Too many VCPUs to start this VM
     */
    public static class VmTooManyVcpus extends XenAPIException {
        public final String vm;

        /**
         * Create a new VmTooManyVcpus
         *
         * @param vm
         */
        public VmTooManyVcpus(String vm) {
            super();
            this.vm = vm;
        }

        public String toString() {
            return "Too many VCPUs to start this VM";
        }
    }

    /**
     * The SR operation cannot be performed because a device underlying the SR is in use by the host.
     */
    public static class SrDeviceInUse extends XenAPIException {

        /**
         * Create a new SrDeviceInUse
         */
        public SrDeviceInUse() {
            super();
        }

        public String toString() {
            return "The SR operation cannot be performed because a device underlying the SR is in use by the host.";
        }
    }

    /**
     * The operation required write access but this VDI is read-only
     */
    public static class VdiReadonly extends XenAPIException {
        public final String vdi;

        /**
         * Create a new VdiReadonly
         *
         * @param vdi
         */
        public VdiReadonly(String vdi) {
            super();
            this.vdi = vdi;
        }

        public String toString() {
            return "The operation required write access but this VDI is read-only";
        }
    }

    /**
     * This host failed in the middle of an automatic failover operation and needs to retry the failover action
     */
    public static class HostBroken extends XenAPIException {

        /**
         * Create a new HostBroken
         */
        public HostBroken() {
            super();
        }

        public String toString() {
            return "This host failed in the middle of an automatic failover operation and needs to retry the failover action";
        }
    }

    /**
     * HVM is required for this operation
     */
    public static class VmHvmRequired extends XenAPIException {
        public final String vm;

        /**
         * Create a new VmHvmRequired
         *
         * @param vm
         */
        public VmHvmRequired(String vm) {
            super();
            this.vm = vm;
        }

        public String toString() {
            return "HVM is required for this operation";
        }
    }

    /**
     * There was an error connecting to the host. the service contacted didn't reply properly.
     */
    public static class JoiningHostServiceFailed extends XenAPIException {

        /**
         * Create a new JoiningHostServiceFailed
         */
        public JoiningHostServiceFailed() {
            super();
        }

        public String toString() {
            return "There was an error connecting to the host. the service contacted didn't reply properly.";
        }
    }

    /**
     * The VM rejected the attempt to detach the device.
     */
    public static class DeviceDetachRejected extends XenAPIException {
        public final String type;
        public final String ref;
        public final String msg;

        /**
         * Create a new DeviceDetachRejected
         *
         * @param type
         * @param ref
         * @param msg
         */
        public DeviceDetachRejected(String type, String ref, String msg) {
            super();
            this.type = type;
            this.ref = ref;
            this.msg = msg;
        }

        public String toString() {
            return "The VM rejected the attempt to detach the device.";
        }
    }

    /**
     * A VDI with the specified location already exists within the SR
     */
    public static class LocationNotUnique extends XenAPIException {
        public final String SR;
        public final String location;

        /**
         * Create a new LocationNotUnique
         *
         * @param SR
         * @param location
         */
        public LocationNotUnique(String SR, String location) {
            super();
            this.SR = SR;
            this.location = location;
        }

        public String toString() {
            return "A VDI with the specified location already exists within the SR";
        }
    }

    /**
     * This message has been deprecated.
     */
    public static class MessageDeprecated extends XenAPIException {
        public final String message;

        /**
         * Create a new MessageDeprecated
         *
         * @param message
         */
        public MessageDeprecated(String message) {
            super();
            this.message = message;
        }

        public String toString() {
            return "This message has been deprecated.";
        }
    }

    /**
     * This operation can only be performed on CD VDIs (iso files or CDROM drives)
     */
    public static class VdiIsNotIso extends XenAPIException {
        public final String vdi;
        public final String type;

        /**
         * Create a new VdiIsNotIso
         *
         * @param vdi
         * @param type
         */
        public VdiIsNotIso(String vdi, String type) {
            super();
            this.vdi = vdi;
            this.type = type;
        }

        public String toString() {
            return "This operation can only be performed on CD VDIs (iso files or CDROM drives)";
        }
    }

    /**
     * A timeout happened while attempting to attach a device to a VM.
     */
    public static class DeviceAttachTimeout extends XenAPIException {
        public final String type;
        public final String ref;

        /**
         * Create a new DeviceAttachTimeout
         *
         * @param type
         * @param ref
         */
        public DeviceAttachTimeout(String type, String ref) {
            super();
            this.type = type;
            this.ref = ref;
        }

        public String toString() {
            return "A timeout happened while attempting to attach a device to a VM.";
        }
    }

    /**
     * Another operation involving the object is currently in progress
     */
    public static class OtherOperationInProgress extends XenAPIException {
        public final String clazz;
        public final String object;

        /**
         * Create a new OtherOperationInProgress
         *
         * @param clazz
         * @param object
         */
        public OtherOperationInProgress(String clazz, String object) {
            super();
            this.clazz = clazz;
            this.object = object;
        }

        public String toString() {
            return "Another operation involving the object is currently in progress";
        }
    }

    /**
     * The restore could not be performed because the restore script failed.  Is the file corrupt?
     */
    public static class RestoreScriptFailed extends XenAPIException {

        /**
         * Create a new RestoreScriptFailed
         */
        public RestoreScriptFailed() {
            super();
        }

        public String toString() {
            return "The restore could not be performed because the restore script failed.  Is the file corrupt?";
        }
    }

    /**
     * PIF is the management interface.
     */
    public static class PifIsManagementInterface extends XenAPIException {
        public final String PIF;

        /**
         * Create a new PifIsManagementInterface
         *
         * @param PIF
         */
        public PifIsManagementInterface(String PIF) {
            super();
            this.PIF = PIF;
        }

        public String toString() {
            return "PIF is the management interface.";
        }
    }

    /**
     * You need at least 1 VCPU to start a VM
     */
    public static class VmNoVcpus extends XenAPIException {
        public final String vm;

        /**
         * Create a new VmNoVcpus
         *
         * @param vm
         */
        public VmNoVcpus(String vm) {
            super();
            this.vm = vm;
        }

        public String toString() {
            return "You need at least 1 VCPU to start a VM";
        }
    }

    /**
     * The patch apply failed.  Please see attached output.
     */
    public static class PatchApplyFailed extends XenAPIException {
        public final String output;

        /**
         * Create a new PatchApplyFailed
         *
         * @param output
         */
        public PatchApplyFailed(String output) {
            super();
            this.output = output;
        }

        public String toString() {
            return "The patch apply failed.  Please see attached output.";
        }
    }

    /**
     * The host joining the pool cannot have any running or suspended VMs.
     */
    public static class JoiningHostCannotHaveRunningOrSuspendedVms extends XenAPIException {

        /**
         * Create a new JoiningHostCannotHaveRunningOrSuspendedVms
         */
        public JoiningHostCannotHaveRunningOrSuspendedVms() {
            super();
        }

        public String toString() {
            return "The host joining the pool cannot have any running or suspended VMs.";
        }
    }

    /**
     * There were no hosts available to complete the specified operation.
     */
    public static class NoHostsAvailable extends XenAPIException {

        /**
         * Create a new NoHostsAvailable
         */
        public NoHostsAvailable() {
            super();
        }

        public String toString() {
            return "There were no hosts available to complete the specified operation.";
        }
    }

    /**
     * The patch precheck stage failed: the server is of an incorrect version.
     */
    public static class PatchPrecheckFailedWrongServerVersion extends XenAPIException {
        public final String patch;
        public final String foundVersion;
        public final String requiredVersion;

        /**
         * Create a new PatchPrecheckFailedWrongServerVersion
         *
         * @param patch
         * @param foundVersion
         * @param requiredVersion
         */
        public PatchPrecheckFailedWrongServerVersion(String patch, String foundVersion, String requiredVersion) {
            super();
            this.patch = patch;
            this.foundVersion = foundVersion;
            this.requiredVersion = requiredVersion;
        }

        public String toString() {
            return "The patch precheck stage failed: the server is of an incorrect version.";
        }
    }

    /**
     * The backup partition to stream the updat to cannot be found
     */
    public static class CannotFindOemBackupPartition extends XenAPIException {

        /**
         * Create a new CannotFindOemBackupPartition
         */
        public CannotFindOemBackupPartition() {
            super();
        }

        public String toString() {
            return "The backup partition to stream the updat to cannot be found";
        }
    }

    /**
     * The value given is invalid
     */
    public static class InvalidValue extends XenAPIException {
        public final String field;
        public final String value;

        /**
         * Create a new InvalidValue
         *
         * @param field
         * @param value
         */
        public InvalidValue(String field, String value) {
            super();
            this.field = field;
            this.value = value;
        }

        public String toString() {
            return "The value given is invalid";
        }
    }

    /**
     * The default SR reference does not point to a valid SR
     */
    public static class DefaultSrNotFound extends XenAPIException {
        public final String sr;

        /**
         * Create a new DefaultSrNotFound
         *
         * @param sr
         */
        public DefaultSrNotFound(String sr) {
            super();
            this.sr = sr;
        }

        public String toString() {
            return "The default SR reference does not point to a valid SR";
        }
    }

    /**
     * The restore could not be performed because the host's current management interface is not in the backup. The interfaces mentioned in the backup are:
     */
    public static class RestoreTargetMgmtIfNotInBackup extends XenAPIException {

        /**
         * Create a new RestoreTargetMgmtIfNotInBackup
         */
        public RestoreTargetMgmtIfNotInBackup() {
            super();
        }

        public String toString() {
            return "The restore could not be performed because the host's current management interface is not in the backup. The interfaces mentioned in the backup are:";
        }
    }

    /**
     * The operation could not proceed because necessary VDIs were already locked at the storage level.
     */
    public static class SrVdiLockingFailed extends XenAPIException {

        /**
         * Create a new SrVdiLockingFailed
         */
        public SrVdiLockingFailed() {
            super();
        }

        public String toString() {
            return "The operation could not proceed because necessary VDIs were already locked at the storage level.";
        }
    }

    /**
     * The management interface on a slave cannot be disabled because the slave would enter emergency mode.
     */
    public static class SlaveRequiresManagementInterface extends XenAPIException {

        /**
         * Create a new SlaveRequiresManagementInterface
         */
        public SlaveRequiresManagementInterface() {
            super();
        }

        public String toString() {
            return "The management interface on a slave cannot be disabled because the slave would enter emergency mode.";
        }
    }

    /**
     * The value specified is of the wrong type
     */
    public static class FieldTypeError extends XenAPIException {
        public final String field;

        /**
         * Create a new FieldTypeError
         *
         * @param field
         */
        public FieldTypeError(String field) {
            super();
            this.field = field;
        }

        public String toString() {
            return "The value specified is of the wrong type";
        }
    }

    /**
     * The system rejected the password change request; perhaps the new password was too short?
     */
    public static class ChangePasswordRejected extends XenAPIException {
        public final String msg;

        /**
         * Create a new ChangePasswordRejected
         *
         * @param msg
         */
        public ChangePasswordRejected(String msg) {
            super();
            this.msg = msg;
        }

        public String toString() {
            return "The system rejected the password change request; perhaps the new password was too short?";
        }
    }

    /**
     * This host cannot accept the proposed new master setting at this time.
     */
    public static class HaAbortNewMaster extends XenAPIException {
        public final String reason;

        /**
         * Create a new HaAbortNewMaster
         *
         * @param reason
         */
        public HaAbortNewMaster(String reason) {
            super();
            this.reason = reason;
        }

        public String toString() {
            return "This host cannot accept the proposed new master setting at this time.";
        }
    }

    /**
     * You attempted to run a VM on a host which doesn't have access to an SR needed by the VM. The VM has at least one VBD attached to a VDI in the SR.
     */
    public static class VmRequiresSr extends XenAPIException {
        public final String vm;
        public final String sr;

        /**
         * Create a new VmRequiresSr
         *
         * @param vm
         * @param sr
         */
        public VmRequiresSr(String vm, String sr) {
            super();
            this.vm = vm;
            this.sr = sr;
        }

        public String toString() {
            return "You attempted to run a VM on a host which doesn't have access to an SR needed by the VM. The VM has at least one VBD attached to a VDI in the SR.";
        }
    }

    /**
     * You attempted an operation on a VM that was not in an appropriate power state at the time; for example, you attempted to start a VM that was already running.  The parameters returned are the VM's handle, and the expected and actual VM state at the time of the call.
     */
    public static class VmBadPowerState extends XenAPIException {
        public final String vm;
        public final String expected;
        public final String actual;

        /**
         * Create a new VmBadPowerState
         *
         * @param vm
         * @param expected
         * @param actual
         */
        public VmBadPowerState(String vm, String expected, String actual) {
            super();
            this.vm = vm;
            this.expected = expected;
            this.actual = actual;
        }

        public String toString() {
            return "You attempted an operation on a VM that was not in an appropriate power state at the time; for example, you attempted to start a VM that was already running.  The parameters returned are the VM's handle, and the expected and actual VM state at the time of the call.";
        }
    }

    /**
     * This host cannot destroy itself.
     */
    public static class HostCannotDestroySelf extends XenAPIException {

        /**
         * Create a new HostCannotDestroySelf
         */
        public HostCannotDestroySelf() {
            super();
        }

        public String toString() {
            return "This host cannot destroy itself.";
        }
    }

    /**
     * The SR is still connected to a host via a PBD. It cannot be destroyed.
     */
    public static class SrHasPbd extends XenAPIException {
        public final String sr;

        /**
         * Create a new SrHasPbd
         *
         * @param sr
         */
        public SrHasPbd(String sr) {
            super();
            this.sr = sr;
        }

        public String toString() {
            return "The SR is still connected to a host via a PBD. It cannot be destroyed.";
        }
    }

    /**
     * This host cannot join a pool because it's license does not support pooling
     */
    public static class LicenseDoesNotSupportPooling extends XenAPIException {

        /**
         * Create a new LicenseDoesNotSupportPooling
         */
        public LicenseDoesNotSupportPooling() {
            super();
        }

        public String toString() {
            return "This host cannot join a pool because it's license does not support pooling";
        }
    }

    /**
     * PIF has no IP configuration (mode curently set to 'none')
     */
    public static class PifHasNoNetworkConfiguration extends XenAPIException {

        /**
         * Create a new PifHasNoNetworkConfiguration
         */
        public PifHasNoNetworkConfiguration() {
            super();
        }

        public String toString() {
            return "PIF has no IP configuration (mode curently set to 'none')";
        }
    }

    /**
     * You tried to destroy a PIF, but it represents an aspect of the physical host configuration, and so cannot be destroyed.  The parameter echoes the PIF handle you gave.
     */
    public static class PifIsPhysical extends XenAPIException {
        public final String PIF;

        /**
         * Create a new PifIsPhysical
         *
         * @param PIF
         */
        public PifIsPhysical(String PIF) {
            super();
            this.PIF = PIF;
        }

        public String toString() {
            return "You tried to destroy a PIF, but it represents an aspect of the physical host configuration, and so cannot be destroyed.  The parameter echoes the PIF handle you gave.";
        }
    }

    /**
     * There was an SR backend failure.
     */
    public static class SrBackendFailure extends XenAPIException {
        public final String status;
        public final String stdout;
        public final String stderr;

        /**
         * Create a new SrBackendFailure
         *
         * @param status
         * @param stdout
         * @param stderr
         */
        public SrBackendFailure(String status, String stdout, String stderr) {
            super();
            this.status = status;
            this.stdout = stdout;
            this.stderr = stderr;
        }

        public String toString() {
            return "There was an SR backend failure.";
        }
    }

    /**
     * You gave an invalid object reference.  The object may have recently been deleted.  The class parameter gives the type of reference given, and the handle parameter echoes the bad value given.
     */
    public static class HandleInvalid extends XenAPIException {
        public final String clazz;
        public final String handle;

        /**
         * Create a new HandleInvalid
         *
         * @param clazz
         * @param handle
         */
        public HandleInvalid(String clazz, String handle) {
            super();
            this.clazz = clazz;
            this.handle = handle;
        }

        public String toString() {
            return "You gave an invalid object reference.  The object may have recently been deleted.  The class parameter gives the type of reference given, and the handle parameter echoes the bad value given.";
        }
    }

    /**
     * This command is not allowed on the OEM edition.
     */
    public static class NotAllowedOnOemEdition extends XenAPIException {
        public final String command;

        /**
         * Create a new NotAllowedOnOemEdition
         *
         * @param command
         */
        public NotAllowedOnOemEdition(String command) {
            super();
            this.command = command;
        }

        public String toString() {
            return "This command is not allowed on the OEM edition.";
        }
    }

    /**
     * This operation is not allowed under your license.  Please contact your support representative.
     */
    public static class LicenceRestriction extends XenAPIException {

        /**
         * Create a new LicenceRestriction
         */
        public LicenceRestriction() {
            super();
        }

        public String toString() {
            return "This operation is not allowed under your license.  Please contact your support representative.";
        }
    }

    /**
     * This patch has already been applied
     */
    public static class PatchAlreadyApplied extends XenAPIException {
        public final String patch;

        /**
         * Create a new PatchAlreadyApplied
         *
         * @param patch
         */
        public PatchAlreadyApplied(String patch) {
            super();
            this.patch = patch;
        }

        public String toString() {
            return "This patch has already been applied";
        }
    }

    /**
     * This operation cannot be performed because this VDI is in use by some other operation
     */
    public static class VdiInUse extends XenAPIException {
        public final String vdi;
        public final String operation;

        /**
         * Create a new VdiInUse
         *
         * @param vdi
         * @param operation
         */
        public VdiInUse(String vdi, String operation) {
            super();
            this.vdi = vdi;
            this.operation = operation;
        }

        public String toString() {
            return "This operation cannot be performed because this VDI is in use by some other operation";
        }
    }

    /**
     * VM cannot be started because it requires a VDI which cannot be attached
     */
    public static class VmRequiresVdi extends XenAPIException {
        public final String vm;
        public final String vdi;

        /**
         * Create a new VmRequiresVdi
         *
         * @param vm
         * @param vdi
         */
        public VmRequiresVdi(String vm, String vdi) {
            super();
            this.vm = vm;
            this.vdi = vdi;
        }

        public String toString() {
            return "VM cannot be started because it requires a VDI which cannot be attached";
        }
    }

    /**
     * The uuid you supplied was invalid.
     */
    public static class UuidInvalid extends XenAPIException {
        public final String type;
        public final String uuid;

        /**
         * Create a new UuidInvalid
         *
         * @param type
         * @param uuid
         */
        public UuidInvalid(String type, String uuid) {
            super();
            this.type = type;
            this.uuid = uuid;
        }

        public String toString() {
            return "The uuid you supplied was invalid.";
        }
    }

    /**
     * You tried to call a method with the incorrect number of parameters.  The fully-qualified method name that you used, and the number of received and expected parameters are returned.
     */
    public static class MessageParameterCountMismatch extends XenAPIException {
        public final String method;
        public final String expected;
        public final String received;

        /**
         * Create a new MessageParameterCountMismatch
         *
         * @param method
         * @param expected
         * @param received
         */
        public MessageParameterCountMismatch(String method, String expected, String received) {
            super();
            this.method = method;
            this.expected = expected;
            this.received = received;
        }

        public String toString() {
            return "You tried to call a method with the incorrect number of parameters.  The fully-qualified method name that you used, and the number of received and expected parameters are returned.";
        }
    }

    /**
     * This operation cannot be performed because the specified VDI could not be found in the specified SR
     */
    public static class VdiLocationMissing extends XenAPIException {
        public final String sr;
        public final String location;

        /**
         * Create a new VdiLocationMissing
         *
         * @param sr
         * @param location
         */
        public VdiLocationMissing(String sr, String location) {
            super();
            this.sr = sr;
            this.location = location;
        }

        public String toString() {
            return "This operation cannot be performed because the specified VDI could not be found in the specified SR";
        }
    }

    /**
     * You gave an invalid session reference.  It may have been invalidated by a server restart, or timed out.  You should get a new session handle, using one of the session.login_ calls.  This error does not invalidate the current connection.  The handle parameter echoes the bad value given.
     */
    public static class SessionInvalid extends XenAPIException {
        public final String handle;

        /**
         * Create a new SessionInvalid
         *
         * @param handle
         */
        public SessionInvalid(String handle) {
            super();
            this.handle = handle;
        }

        public String toString() {
            return "You gave an invalid session reference.  It may have been invalidated by a server restart, or timed out.  You should get a new session handle, using one of the session.login_ calls.  This error does not invalidate the current connection.  The handle parameter echoes the bad value given.";
        }
    }

    /**
     * This pool is not in emergency mode.
     */
    public static class NotInEmergencyMode extends XenAPIException {

        /**
         * Create a new NotInEmergencyMode
         */
        public NotInEmergencyMode() {
            super();
        }

        public String toString() {
            return "This pool is not in emergency mode.";
        }
    }

    /**
     * Retrieving system status from the host failed.  A diagnostic reason suitable for support organisations is also returned.
     */
    public static class SystemStatusRetrievalFailed extends XenAPIException {
        public final String reason;

        /**
         * Create a new SystemStatusRetrievalFailed
         *
         * @param reason
         */
        public SystemStatusRetrievalFailed(String reason) {
            super();
            this.reason = reason;
        }

        public String toString() {
            return "Retrieving system status from the host failed.  A diagnostic reason suitable for support organisations is also returned.";
        }
    }

    /**
     * Cannot downgrade license while in pool. Please disband the pool first, then downgrade licenses on hosts separately.
     */
    public static class LicenseCannotDowngradeWhileInPool extends XenAPIException {

        /**
         * Create a new LicenseCannotDowngradeWhileInPool
         */
        public LicenseCannotDowngradeWhileInPool() {
            super();
        }

        public String toString() {
            return "Cannot downgrade license while in pool. Please disband the pool first, then downgrade licenses on hosts separately.";
        }
    }

    /**
     * You tried to create a PIF, but the network you tried to attach it to is already attached to some other PIF, and so the creation failed.
     */
    public static class NetworkAlreadyConnected extends XenAPIException {
        public final String network;
        public final String connectedPIF;

        /**
         * Create a new NetworkAlreadyConnected
         *
         * @param network
         * @param connectedPIF
         */
        public NetworkAlreadyConnected(String network, String connectedPIF) {
            super();
            this.network = network;
            this.connectedPIF = connectedPIF;
        }

        public String toString() {
            return "You tried to create a PIF, but the network you tried to attach it to is already attached to some other PIF, and so the creation failed.";
        }
    }

    /**
     * You attempted an operation on a VM which requires PV drivers to be installed but the drivers were not detected.
     */
    public static class VmMissingPvDrivers extends XenAPIException {
        public final String vm;

        /**
         * Create a new VmMissingPvDrivers
         *
         * @param vm
         */
        public VmMissingPvDrivers(String vm) {
            super();
            this.vm = vm;
        }

        public String toString() {
            return "You attempted an operation on a VM which requires PV drivers to be installed but the drivers were not detected.";
        }
    }

    /**
     * You cannot make regular API calls directly on a slave. Please pass API calls via the master host.
     */
    public static class HostIsSlave extends XenAPIException {
        public final String masterIPAddress;

        /**
         * Create a new HostIsSlave
         *
         * @param masterIPAddress
         */
        public HostIsSlave(String masterIPAddress) {
            super();
            this.masterIPAddress = masterIPAddress;
        }

        public String toString() {
            return "You cannot make regular API calls directly on a slave. Please pass API calls via the master host.";
        }
    }

    /**
     * The host failed to acquire an IP address on its management interface and therefore cannot contact the master.
     */
    public static class HostHasNoManagementIp extends XenAPIException {

        /**
         * Create a new HostHasNoManagementIp
         */
        public HostHasNoManagementIp() {
            super();
        }

        public String toString() {
            return "The host failed to acquire an IP address on its management interface and therefore cannot contact the master.";
        }
    }

    /**
     * The operation cannot be performed on physical device
     */
    public static class VdiIsAPhysicalDevice extends XenAPIException {
        public final String vdi;

        /**
         * Create a new VdiIsAPhysicalDevice
         *
         * @param vdi
         */
        public VdiIsAPhysicalDevice(String vdi) {
            super();
            this.vdi = vdi;
        }

        public String toString() {
            return "The operation cannot be performed on physical device";
        }
    }

    /**
     * The patch precheck stage failed: prerequisite patches are missing.
     */
    public static class PatchPrecheckFailedPrerequisiteMissing extends XenAPIException {
        public final String patch;
        public final String prerequisitePatchUuidList;

        /**
         * Create a new PatchPrecheckFailedPrerequisiteMissing
         *
         * @param patch
         * @param prerequisitePatchUuidList
         */
        public PatchPrecheckFailedPrerequisiteMissing(String patch, String prerequisitePatchUuidList) {
            super();
            this.patch = patch;
            this.prerequisitePatchUuidList = prerequisitePatchUuidList;
        }

        public String toString() {
            return "The patch precheck stage failed: prerequisite patches are missing.";
        }
    }

    /**
     * This operation cannot be performed because the specified VDI could not be found on the storage substrate
     */
    public static class VdiMissing extends XenAPIException {
        public final String sr;
        public final String vdi;

        /**
         * Create a new VdiMissing
         *
         * @param sr
         * @param vdi
         */
        public VdiMissing(String sr, String vdi) {
            super();
            this.sr = sr;
            this.vdi = vdi;
        }

        public String toString() {
            return "This operation cannot be performed because the specified VDI could not be found on the storage substrate";
        }
    }

    /**
     * You attempted an operation which involves a host which could not be contacted.
     */
    public static class HostOffline extends XenAPIException {
        public final String host;

        /**
         * Create a new HostOffline
         *
         * @param host
         */
        public HostOffline(String host) {
            super();
            this.host = host;
        }

        public String toString() {
            return "You attempted an operation which involves a host which could not be contacted.";
        }
    }

    /**
     * The credentials given by the user are incorrect, so access has been denied, and you have not been issued a session handle.
     */
    public static class SessionAuthenticationFailed extends XenAPIException {

        /**
         * Create a new SessionAuthenticationFailed
         */
        public SessionAuthenticationFailed() {
            super();
        }

        public String toString() {
            return "The credentials given by the user are incorrect, so access has been denied, and you have not been issued a session handle.";
        }
    }

    /**
     * Host cannot rejoin pool because it should have fenced (it is not in the master's partition)
     */
    public static class HaShouldBeFenced extends XenAPIException {
        public final String host;

        /**
         * Create a new HaShouldBeFenced
         *
         * @param host
         */
        public HaShouldBeFenced(String host) {
            super();
            this.host = host;
        }

        public String toString() {
            return "Host cannot rejoin pool because it should have fenced (it is not in the master's partition)";
        }
    }

    /**
     * The device is not currently attached
     */
    public static class DeviceAlreadyDetached extends XenAPIException {
        public final String device;

        /**
         * Create a new DeviceAlreadyDetached
         *
         * @param device
         */
        public DeviceAlreadyDetached(String device) {
            super();
            this.device = device;
        }

        public String toString() {
            return "The device is not currently attached";
        }
    }

    /**
     * The master reports that it cannot talk back to the slave on the supplied management IP address.
     */
    public static class HostMasterCannotTalkBack extends XenAPIException {
        public final String ip;

        /**
         * Create a new HostMasterCannotTalkBack
         *
         * @param ip
         */
        public HostMasterCannotTalkBack(String ip) {
            super();
            this.ip = ip;
        }

        public String toString() {
            return "The master reports that it cannot talk back to the slave on the supplied management IP address.";
        }
    }

    /**
     * You attempted to set a value that is not supported by this implementation.  The fully-qualified field name and the value that you tried to set are returned.  Also returned is a developer-only diagnostic reason.
     */
    public static class ValueNotSupported extends XenAPIException {
        public final String field;
        public final String value;
        public final String reason;

        /**
         * Create a new ValueNotSupported
         *
         * @param field
         * @param value
         * @param reason
         */
        public ValueNotSupported(String field, String value, String reason) {
            super();
            this.field = field;
            this.value = value;
            this.reason = reason;
        }

        public String toString() {
            return "You attempted to set a value that is not supported by this implementation.  The fully-qualified field name and the value that you tried to set are returned.  Also returned is a developer-only diagnostic reason.";
        }
    }

    /**
     * Host cannot attach network (in the case of NIC bonding, this may be because attaching the network on this host would require other networks [that are currently active] to be taken down).
     */
    public static class HostCannotAttachNetwork extends XenAPIException {
        public final String host;
        public final String network;

        /**
         * Create a new HostCannotAttachNetwork
         *
         * @param host
         * @param network
         */
        public HostCannotAttachNetwork(String host, String network) {
            super();
            this.host = host;
            this.network = network;
        }

        public String toString() {
            return "Host cannot attach network (in the case of NIC bonding, this may be because attaching the network on this host would require other networks [that are currently active] to be taken down).";
        }
    }

    /**
     * This VM does not have a crashdump SR specified.
     */
    public static class VmNoCrashdumpSr extends XenAPIException {
        public final String vm;

        /**
         * Create a new VmNoCrashdumpSr
         *
         * @param vm
         */
        public VmNoCrashdumpSr(String vm) {
            super();
            this.vm = vm;
        }

        public String toString() {
            return "This VM does not have a crashdump SR specified.";
        }
    }

    /**
     * The specified VM has too little memory to be started.
     */
    public static class VmMemorySizeTooLow extends XenAPIException {
        public final String vm;

        /**
         * Create a new VmMemorySizeTooLow
         *
         * @param vm
         */
        public VmMemorySizeTooLow(String vm) {
            super();
            this.vm = vm;
        }

        public String toString() {
            return "The specified VM has too little memory to be started.";
        }
    }

    /**
     * The SR could not be connected because the driver was not recognised.
     */
    public static class SrUnknownDriver extends XenAPIException {
        public final String driver;

        /**
         * Create a new SrUnknownDriver
         *
         * @param driver
         */
        public SrUnknownDriver(String driver) {
            super();
            this.driver = driver;
        }

        public String toString() {
            return "The SR could not be connected because the driver was not recognised.";
        }
    }

    /**
     * You cannot delete the specified default template.
     */
    public static class VmCannotDeleteDefaultTemplate extends XenAPIException {
        public final String vm;

        /**
         * Create a new VmCannotDeleteDefaultTemplate
         *
         * @param vm
         */
        public VmCannotDeleteDefaultTemplate(String vm) {
            super();
            this.vm = vm;
        }

        public String toString() {
            return "You cannot delete the specified default template.";
        }
    }

    /**
     * This operation cannot be completed as the host is in use by (at least) the object of type and ref echoed below.
     */
    public static class HostInUse extends XenAPIException {
        public final String host;
        public final String type;
        public final String ref;

        /**
         * Create a new HostInUse
         *
         * @param host
         * @param type
         * @param ref
         */
        public HostInUse(String host, String type, String ref) {
            super();
            this.host = host;
            this.type = type;
            this.ref = ref;
        }

        public String toString() {
            return "This operation cannot be completed as the host is in use by (at least) the object of type and ref echoed below.";
        }
    }

    /**
     * This operation cannot be performed because the pif is bonded.
     */
    public static class PifAlreadyBonded extends XenAPIException {
        public final String PIF;

        /**
         * Create a new PifAlreadyBonded
         *
         * @param PIF
         */
        public PifAlreadyBonded(String PIF) {
            super();
            this.PIF = PIF;
        }

        public String toString() {
            return "This operation cannot be performed because the pif is bonded.";
        }
    }

    /**
     * The operation could not be performed because HA is enabled on the Pool
     */
    public static class HaIsEnabled extends XenAPIException {

        /**
         * Create a new HaIsEnabled
         */
        public HaIsEnabled() {
            super();
        }

        public String toString() {
            return "The operation could not be performed because HA is enabled on the Pool";
        }
    }

    /**
     * The specified patch is applied and cannot be destroyed.
     */
    public static class PatchIsApplied extends XenAPIException {

        /**
         * Create a new PatchIsApplied
         */
        public PatchIsApplied() {
            super();
        }

        public String toString() {
            return "The specified patch is applied and cannot be destroyed.";
        }
    }

    /**
     * The import failed because this export has been created by a different (incompatible) product verion
     */
    public static class ImportIncompatibleVersion extends XenAPIException {

        /**
         * Create a new ImportIncompatibleVersion
         */
        public ImportIncompatibleVersion() {
            super();
        }

        public String toString() {
            return "The import failed because this export has been created by a different (incompatible) product verion";
        }
    }

    /**
     * The SR backend does not support the operation (check the SR's allowed operations)
     */
    public static class SrOperationNotSupported extends XenAPIException {
        public final String sr;

        /**
         * Create a new SrOperationNotSupported
         *
         * @param sr
         */
        public SrOperationNotSupported(String sr) {
            super();
            this.sr = sr;
        }

        public String toString() {
            return "The SR backend does not support the operation (check the SR's allowed operations)";
        }
    }

    /**
     * Media could not be ejected because it is not removable
     */
    public static class VbdNotRemovableMedia extends XenAPIException {
        public final String vbd;

        /**
         * Create a new VbdNotRemovableMedia
         *
         * @param vbd
         */
        public VbdNotRemovableMedia(String vbd) {
            super();
            this.vbd = vbd;
        }

        public String toString() {
            return "Media could not be ejected because it is not removable";
        }
    }

    /**
     * Your license has expired.  Please contact your support representative.
     */
    public static class LicenseExpired extends XenAPIException {

        /**
         * Create a new LicenseExpired
         */
        public LicenseExpired() {
            super();
        }

        public String toString() {
            return "Your license has expired.  Please contact your support representative.";
        }
    }

    /**
     * The MAC address specified doesn't exist on this host.
     */
    public static class MacDoesNotExist extends XenAPIException {
        public final String MAC;

        /**
         * Create a new MacDoesNotExist
         *
         * @param MAC
         */
        public MacDoesNotExist(String MAC) {
            super();
            this.MAC = MAC;
        }

        public String toString() {
            return "The MAC address specified doesn't exist on this host.";
        }
    }

    /**
     * The specified interface cannot be used because it has no IP address
     */
    public static class InterfaceHasNoIp extends XenAPIException {
        public final String iface;

        /**
         * Create a new InterfaceHasNoIp
         *
         * @param iface
         */
        public InterfaceHasNoIp(String iface) {
            super();
            this.iface = iface;
        }

        public String toString() {
            return "The specified interface cannot be used because it has no IP address";
        }
    }

    /**
     * The network contains active VIFs and cannot be deleted.
     */
    public static class NetworkContainsVif extends XenAPIException {
        public final String vifs;

        /**
         * Create a new NetworkContainsVif
         *
         * @param vifs
         */
        public NetworkContainsVif(String vifs) {
            super();
            this.vifs = vifs;
        }

        public String toString() {
            return "The network contains active VIFs and cannot be deleted.";
        }
    }

    /**
     * The host joining the pool cannot contain any shared storage.
     */
    public static class JoiningHostCannotContainSharedSrs extends XenAPIException {

        /**
         * Create a new JoiningHostCannotContainSharedSrs
         */
        public JoiningHostCannotContainSharedSrs() {
            super();
        }

        public String toString() {
            return "The host joining the pool cannot contain any shared storage.";
        }
    }

    /**
     * The requested bootloader is unknown
     */
    public static class UnknownBootloader extends XenAPIException {
        public final String vm;
        public final String bootloader;

        /**
         * Create a new UnknownBootloader
         *
         * @param vm
         * @param bootloader
         */
        public UnknownBootloader(String vm, String bootloader) {
            super();
            this.vm = vm;
            this.bootloader = bootloader;
        }

        public String toString() {
            return "The requested bootloader is unknown";
        }
    }

    /**
     * Cannot restore this VM because it would create a duplicate
     */
    public static class DuplicateVm extends XenAPIException {
        public final String vm;

        /**
         * Create a new DuplicateVm
         *
         * @param vm
         */
        public DuplicateVm(String vm) {
            super();
            this.vm = vm;
        }

        public String toString() {
            return "Cannot restore this VM because it would create a duplicate";
        }
    }

    /**
     * Read/write CDs are not supported
     */
    public static class VbdCdsMustBeReadonly extends XenAPIException {

        /**
         * Create a new VbdCdsMustBeReadonly
         */
        public VbdCdsMustBeReadonly() {
            super();
        }

        public String toString() {
            return "Read/write CDs are not supported";
        }
    }

    /**
     * An unknown error occurred while attempting to configure an interface.
     */
    public static class PifConfigurationError extends XenAPIException {
        public final String PIF;
        public final String msg;

        /**
         * Create a new PifConfigurationError
         *
         * @param PIF
         * @param msg
         */
        public PifConfigurationError(String PIF, String msg) {
            super();
            this.PIF = PIF;
            this.msg = msg;
        }

        public String toString() {
            return "An unknown error occurred while attempting to configure an interface.";
        }
    }

    /**
     * You tried to create a VLAN, but the tag you gave was invalid -- it must be between 0 and 4095.  The parameter echoes the VLAN tag you gave.
     */
    public static class VlanTagInvalid extends XenAPIException {
        public final String VLAN;

        /**
         * Create a new VlanTagInvalid
         *
         * @param VLAN
         */
        public VlanTagInvalid(String VLAN) {
            super();
            this.VLAN = VLAN;
        }

        public String toString() {
            return "You tried to create a VLAN, but the tag you gave was invalid -- it must be between 0 and 4095.  The parameter echoes the VLAN tag you gave.";
        }
    }

    /**
     * The server failed to handle your request, due to an internal error.  The given message may give details useful for debugging the problem.
     */
    public static class InternalError extends XenAPIException {
        public final String message;

        /**
         * Create a new InternalError
         *
         * @param message
         */
        public InternalError(String message) {
            super();
            this.message = message;
        }

        public String toString() {
            return "The server failed to handle your request, due to an internal error.  The given message may give details useful for debugging the problem.";
        }
    }

    /**
     * The patch precheck stage failed: there are one or more VMs still running on the server.  All VMs must be suspended before the patch can be applied.
     */
    public static class PatchPrecheckFailedVmRunning extends XenAPIException {
        public final String patch;

        /**
         * Create a new PatchPrecheckFailedVmRunning
         *
         * @param patch
         */
        public PatchPrecheckFailedVmRunning(String patch) {
            super();
            this.patch = patch;
        }

        public String toString() {
            return "The patch precheck stage failed: there are one or more VMs still running on the server.  All VMs must be suspended before the patch can be applied.";
        }
    }

    /**
     * The bootloader returned an error
     */
    public static class BootloaderFailed extends XenAPIException {
        public final String vm;
        public final String msg;

        /**
         * Create a new BootloaderFailed
         *
         * @param vm
         * @param msg
         */
        public BootloaderFailed(String vm, String msg) {
            super();
            this.vm = vm;
            this.msg = msg;
        }

        public String toString() {
            return "The bootloader returned an error";
        }
    }

    /**
     * This operation is not supported during an upgrade
     */
    public static class NotSupportedDuringUpgrade extends XenAPIException {

        /**
         * Create a new NotSupportedDuringUpgrade
         */
        public NotSupportedDuringUpgrade() {
            super();
        }

        public String toString() {
            return "This operation is not supported during an upgrade";
        }
    }

    /**
     * This VM does not have a suspend SR specified.
     */
    public static class VmNoSuspendSr extends XenAPIException {
        public final String vm;

        /**
         * Create a new VmNoSuspendSr
         *
         * @param vm
         */
        public VmNoSuspendSr(String vm) {
            super();
            this.vm = vm;
        }

        public String toString() {
            return "This VM does not have a suspend SR specified.";
        }
    }

    /**
     * Cannot plug VIF
     */
    public static class CannotPlugVif extends XenAPIException {
        public final String VIF;

        /**
         * Create a new CannotPlugVif
         *
         * @param VIF
         */
        public CannotPlugVif(String VIF) {
            super();
            this.VIF = VIF;
        }

        public String toString() {
            return "Cannot plug VIF";
        }
    }

    /**
     * Some events have been lost from the queue and cannot be retrieved.
     */
    public static class EventsLost extends XenAPIException {

        /**
         * Create a new EventsLost
         */
        public EventsLost() {
            super();
        }

        public String toString() {
            return "Some events have been lost from the queue and cannot be retrieved.";
        }
    }

    /**
     * Operation could not be performed because the drive is empty
     */
    public static class VbdIsEmpty extends XenAPIException {
        public final String vbd;

        /**
         * Create a new VbdIsEmpty
         *
         * @param vbd
         */
        public VbdIsEmpty(String vbd) {
            super();
            this.vbd = vbd;
        }

        public String toString() {
            return "Operation could not be performed because the drive is empty";
        }
    }

    /**
     * You tried to add a key-value pair to a map, but that key is already there.
     */
    public static class MapDuplicateKey extends XenAPIException {
        public final String type;
        public final String paramName;
        public final String uuid;
        public final String key;

        /**
         * Create a new MapDuplicateKey
         *
         * @param type
         * @param paramName
         * @param uuid
         * @param key
         */
        public MapDuplicateKey(String type, String paramName, String uuid, String key) {
            super();
            this.type = type;
            this.paramName = paramName;
            this.uuid = uuid;
            this.key = key;
        }

        public String toString() {
            return "You tried to add a key-value pair to a map, but that key is already there.";
        }
    }

    /**
     * The metrics of this host could not be read.
     */
    public static class HostCannotReadMetrics extends XenAPIException {

        /**
         * Create a new HostCannotReadMetrics
         */
        public HostCannotReadMetrics() {
            super();
        }

        public String toString() {
            return "The metrics of this host could not be read.";
        }
    }

    /**
     * The operation could not be performed because HA is not enabled on the Pool
     */
    public static class HaNotEnabled extends XenAPIException {

        /**
         * Create a new HaNotEnabled
         */
        public HaNotEnabled() {
            super();
        }

        public String toString() {
            return "The operation could not be performed because HA is not enabled on the Pool";
        }
    }

    /**
     * This session is not registered to receive events.  You must call event.register before event.next.  The session handle you are using is echoed.
     */
    public static class SessionNotRegistered extends XenAPIException {
        public final String handle;

        /**
         * Create a new SessionNotRegistered
         *
         * @param handle
         */
        public SessionNotRegistered(String handle) {
            super();
            this.handle = handle;
        }

        public String toString() {
            return "This session is not registered to receive events.  You must call event.register before event.next.  The session handle you are using is echoed.";
        }
    }

    /**
     * You must use tar output to retrieve system status from an OEM host.
     */
    public static class SystemStatusMustUseTarOnOem extends XenAPIException {

        /**
         * Create a new SystemStatusMustUseTarOnOem
         */
        public SystemStatusMustUseTarOnOem() {
            super();
        }

        public String toString() {
            return "You must use tar output to retrieve system status from an OEM host.";
        }
    }

    /**
     * The PBD could not be plugged because the SR is in use by another host and is not marked as sharable.
     */
    public static class SrNotSharable extends XenAPIException {
        public final String sr;
        public final String host;

        /**
         * Create a new SrNotSharable
         *
         * @param sr
         * @param host
         */
        public SrNotSharable(String sr, String host) {
            super();
            this.sr = sr;
            this.host = host;
        }

        public String toString() {
            return "The PBD could not be plugged because the SR is in use by another host and is not marked as sharable.";
        }
    }

    /**
     * The network contains active PIFs and cannot be deleted.
     */
    public static class NetworkContainsPif extends XenAPIException {
        public final String pifs;

        /**
         * Create a new NetworkContainsPif
         *
         * @param pifs
         */
        public NetworkContainsPif(String pifs) {
            super();
            this.pifs = pifs;
        }

        public String toString() {
            return "The network contains active PIFs and cannot be deleted.";
        }
    }

    /**
     * The server failed to unmarshal the XMLRPC message; it was expecting one element and received something else.
     */
    public static class XmlrpcUnmarshalFailure extends XenAPIException {
        public final String expected;
        public final String received;

        /**
         * Create a new XmlrpcUnmarshalFailure
         *
         * @param expected
         * @param received
         */
        public XmlrpcUnmarshalFailure(String expected, String received) {
            super();
            this.expected = expected;
            this.received = received;
        }

        public String toString() {
            return "The server failed to unmarshal the XMLRPC message; it was expecting one element and received something else.";
        }
    }

    /**
     * XHA cannot be enabled because this host's license does not allow it
     */
    public static class LicenseDoesNotSupportXha extends XenAPIException {

        /**
         * Create a new LicenseDoesNotSupportXha
         */
        public LicenseDoesNotSupportXha() {
            super();
        }

        public String toString() {
            return "XHA cannot be enabled because this host's license does not allow it";
        }
    }

    /**
     * You cannot bond interfaces across different hosts.
     */
    public static class PifCannotBondCrossHost extends XenAPIException {

        /**
         * Create a new PifCannotBondCrossHost
         */
        public PifCannotBondCrossHost() {
            super();
        }

        public String toString() {
            return "You cannot bond interfaces across different hosts.";
        }
    }

    /**
     * The specified host is disabled.
     */
    public static class HostDisabled extends XenAPIException {
        public final String host;

        /**
         * Create a new HostDisabled
         *
         * @param host
         */
        public HostDisabled(String host) {
            super();
            this.host = host;
        }

        public String toString() {
            return "The specified host is disabled.";
        }
    }

    /**
     * A device with the name given already exists on the selected VM
     */
    public static class DeviceAlreadyExists extends XenAPIException {
        public final String device;

        /**
         * Create a new DeviceAlreadyExists
         *
         * @param device
         */
        public DeviceAlreadyExists(String device) {
            super();
            this.device = device;
        }

        public String toString() {
            return "A device with the name given already exists on the selected VM";
        }
    }

    /**
     * This operation cannot be performed because this VDI could not be properly attached to the VM.
     */
    public static class VdiNotAvailable extends XenAPIException {
        public final String vdi;

        /**
         * Create a new VdiNotAvailable
         *
         * @param vdi
         */
        public VdiNotAvailable(String vdi) {
            super();
            this.vdi = vdi;
        }

        public String toString() {
            return "This operation cannot be performed because this VDI could not be properly attached to the VM.";
        }
    }

    /**
     * The specified device was not found.
     */
    public static class PifDeviceNotFound extends XenAPIException {

        /**
         * Create a new PifDeviceNotFound
         */
        public PifDeviceNotFound() {
            super();
        }

        public String toString() {
            return "The specified device was not found.";
        }
    }

    /**
     * The operation could not be performed because the HA software is not installed on this host.
     */
    public static class HaNotInstalled extends XenAPIException {
        public final String host;

        /**
         * Create a new HaNotInstalled
         *
         * @param host
         */
        public HaNotInstalled(String host) {
            super();
            this.host = host;
        }

        public String toString() {
            return "The operation could not be performed because the HA software is not installed on this host.";
        }
    }

    /**
     * The request was rejected because the server is too busy.
     */
    public static class TooBusy extends XenAPIException {

        /**
         * Create a new TooBusy
         */
        public TooBusy() {
            super();
        }

        public String toString() {
            return "The request was rejected because the server is too busy.";
        }
    }

    /**
     * The request was asynchronously cancelled.
     */
    public static class TaskCancelled extends XenAPIException {
        public final String task;

        /**
         * Create a new TaskCancelled
         *
         * @param task
         */
        public TaskCancelled(String task) {
            super();
            this.task = task;
        }

        public String toString() {
            return "The request was asynchronously cancelled.";
        }
    }

    /**
     * This operation cannot be completed as the host is not live.
     */
    public static class HostNotLive extends XenAPIException {

        /**
         * Create a new HostNotLive
         */
        public HostNotLive() {
            super();
        }

        public String toString() {
            return "This operation cannot be completed as the host is not live.";
        }
    }

    /**
     * The specified VM has a duplicate VBD device and cannot be started.
     */
    public static class VmDuplicateVbdDevice extends XenAPIException {
        public final String vm;
        public final String vbd;
        public final String device;

        /**
         * Create a new VmDuplicateVbdDevice
         *
         * @param vm
         * @param vbd
         * @param device
         */
        public VmDuplicateVbdDevice(String vm, String vbd, String device) {
            super();
            this.vm = vm;
            this.vbd = vbd;
            this.device = device;
        }

        public String toString() {
            return "The specified VM has a duplicate VBD device and cannot be started.";
        }
    }

    /**
     * VM failed to shutdown before the timeout expired
     */
    public static class VmShutdownTimeout extends XenAPIException {
        public final String vm;
        public final String timeout;

        /**
         * Create a new VmShutdownTimeout
         *
         * @param vm
         * @param timeout
         */
        public VmShutdownTimeout(String vm, String timeout) {
            super();
            this.vm = vm;
            this.timeout = timeout;
        }

        public String toString() {
            return "VM failed to shutdown before the timeout expired";
        }
    }

    /**
     * The device name is invalid
     */
    public static class InvalidDevice extends XenAPIException {
        public final String device;

        /**
         * Create a new InvalidDevice
         *
         * @param device
         */
        public InvalidDevice(String device) {
            super();
            this.device = device;
        }

        public String toString() {
            return "The device name is invalid";
        }
    }

    /**
     * A required parameter contained an invalid IP address
     */
    public static class InvalidIpAddressSpecified extends XenAPIException {
        public final String parameter;

        /**
         * Create a new InvalidIpAddressSpecified
         *
         * @param parameter
         */
        public InvalidIpAddressSpecified(String parameter) {
            super();
            this.parameter = parameter;
        }

        public String toString() {
            return "A required parameter contained an invalid IP address";
        }
    }

    /**
     * There was an error processing your license.  Please contact your support representative.
     */
    public static class LicenseProcessingError extends XenAPIException {

        /**
         * Create a new LicenseProcessingError
         */
        public LicenseProcessingError() {
            super();
        }

        public String toString() {
            return "There was an error processing your license.  Please contact your support representative.";
        }
    }

    /**
     * The SR is full. Requested new size exceeds the maximum size
     */
    public static class SrFull extends XenAPIException {
        public final String requested;
        public final String maximum;

        /**
         * Create a new SrFull
         *
         * @param requested
         * @param maximum
         */
        public SrFull(String requested, String maximum) {
            super();
            this.requested = requested;
            this.maximum = maximum;
        }

        public String toString() {
            return "The SR is full. Requested new size exceeds the maximum size";
        }
    }

    /**
     * This operation cannot be performed because the host is not disabled.
     */
    public static class HostNotDisabled extends XenAPIException {

        /**
         * Create a new HostNotDisabled
         */
        public HostNotDisabled() {
            super();
        }

        public String toString() {
            return "This operation cannot be performed because the host is not disabled.";
        }
    }

    /**
     * You attempted an operation which would have resulted in duplicate keys in the database.
     */
    public static class DbUniquenessConstraintViolation extends XenAPIException {
        public final String table;
        public final String field;
        public final String value;

        /**
         * Create a new DbUniquenessConstraintViolation
         *
         * @param table
         * @param field
         * @param value
         */
        public DbUniquenessConstraintViolation(String table, String field, String value) {
            super();
            this.table = table;
            this.field = field;
            this.value = value;
        }

        public String toString() {
            return "You attempted an operation which would have resulted in duplicate keys in the database.";
        }
    }

    /**
     * Caller not allowed to perform this operation.
     */
    public static class PermissionDenied extends XenAPIException {
        public final String message;

        /**
         * Create a new PermissionDenied
         *
         * @param message
         */
        public PermissionDenied(String message) {
            super();
            this.message = message;
        }

        public String toString() {
            return "Caller not allowed to perform this operation.";
        }
    }

    /**
     * The request was rejected because there are too many pending tasks on the server.
     */
    public static class TooManyPendingTasks extends XenAPIException {

        /**
         * Create a new TooManyPendingTasks
         */
        public TooManyPendingTasks() {
            super();
        }

        public String toString() {
            return "The request was rejected because there are too many pending tasks on the server.";
        }
    }

    /**
     * The SR operation cannot be performed because the SR is not empty.
     */
    public static class SrNotEmpty extends XenAPIException {

        /**
         * Create a new SrNotEmpty
         */
        public SrNotEmpty() {
            super();
        }

        public String toString() {
            return "The SR operation cannot be performed because the SR is not empty.";
        }
    }

    /**
     * An SR with that uuid already exists.
     */
    public static class SrUuidExists extends XenAPIException {
        public final String uuid;

        /**
         * Create a new SrUuidExists
         *
         * @param uuid
         */
        public SrUuidExists(String uuid) {
            super();
            this.uuid = uuid;
        }

        public String toString() {
            return "An SR with that uuid already exists.";
        }
    }

    /**
     * You tried to call a method that does not exist.  The method name that you used is echoed.
     */
    public static class MessageMethodUnknown extends XenAPIException {
        public final String method;

        /**
         * Create a new MessageMethodUnknown
         *
         * @param method
         */
        public MessageMethodUnknown(String method) {
            super();
            this.method = method;
        }

        public String toString() {
            return "You tried to call a method that does not exist.  The method name that you used is echoed.";
        }
    }

    /**
     * Network has active VIFs
     */
    public static class VifInUse extends XenAPIException {
        public final String network;
        public final String VIF;

        /**
         * Create a new VifInUse
         *
         * @param network
         * @param VIF
         */
        public VifInUse(String network, String VIF) {
            super();
            this.network = network;
            this.VIF = VIF;
        }

        public String toString() {
            return "Network has active VIFs";
        }
    }

    /**
     * The requested update could not be found.  This can occur when you designate a new master or xe patch-clean.  Please upload the update again
     */
    public static class CannotFindPatch extends XenAPIException {

        /**
         * Create a new CannotFindPatch
         */
        public CannotFindPatch() {
            super();
        }

        public String toString() {
            return "The requested update could not be found.  This can occur when you designate a new master or xe patch-clean.  Please upload the update again";
        }
    }

    /**
     * The uploaded patch file is invalid.  See attached log for more details.
     */
    public static class InvalidPatchWithLog extends XenAPIException {
        public final String log;

        /**
         * Create a new InvalidPatchWithLog
         *
         * @param log
         */
        public InvalidPatchWithLog(String log) {
            super();
            this.log = log;
        }

        public String toString() {
            return "The uploaded patch file is invalid.  See attached log for more details.";
        }
    }

    /**
     * This operation could not be performed because the state partition could not be found
     */
    public static class CannotFindStatePartition extends XenAPIException {

        /**
         * Create a new CannotFindStatePartition
         */
        public CannotFindStatePartition() {
            super();
        }

        public String toString() {
            return "This operation could not be performed because the state partition could not be found";
        }
    }

    /**
     * A timeout happened while attempting to detach a device from a VM.
     */
    public static class DeviceDetachTimeout extends XenAPIException {
        public final String type;
        public final String ref;

        /**
         * Create a new DeviceDetachTimeout
         *
         * @param type
         * @param ref
         */
        public DeviceDetachTimeout(String type, String ref) {
            super();
            this.type = type;
            this.ref = ref;
        }

        public String toString() {
            return "A timeout happened while attempting to detach a device from a VM.";
        }
    }

    /**
     * This host cannot be evacuated.
     */
    public static class CannotEvacuateHost extends XenAPIException {
        public final String errors;

        /**
         * Create a new CannotEvacuateHost
         *
         * @param errors
         */
        public CannotEvacuateHost(String errors) {
            super();
            this.errors = errors;
        }

        public String toString() {
            return "This host cannot be evacuated.";
        }
    }

    /**
     * The provision call can only be invoked on templates, not regular VMs.
     */
    public static class ProvisionOnlyAllowedOnTemplate extends XenAPIException {

        /**
         * Create a new ProvisionOnlyAllowedOnTemplate
         */
        public ProvisionOnlyAllowedOnTemplate() {
            super();
        }

        public String toString() {
            return "The provision call can only be invoked on templates, not regular VMs.";
        }
    }

    /**
     * The SR has no attached PBDs
     */
    public static class SrHasNoPbds extends XenAPIException {
        public final String sr;

        /**
         * Create a new SrHasNoPbds
         *
         * @param sr
         */
        public SrHasNoPbds(String sr) {
            super();
            this.sr = sr;
        }

        public String toString() {
            return "The SR has no attached PBDs";
        }
    }

    /**
     * Cannot perform operation as the host is running in emergency mode.
     */
    public static class HostInEmergencyMode extends XenAPIException {

        /**
         * Create a new HostInEmergencyMode
         */
        public HostInEmergencyMode() {
            super();
        }

        public String toString() {
            return "Cannot perform operation as the host is running in emergency mode.";
        }
    }

    /**
     * HA can only be enabled for 2 hosts or more. Note that 2 hosts requires a pre-configured quorum tiebreak script.
     */
    public static class HaTooFewHosts extends XenAPIException {

        /**
         * Create a new HaTooFewHosts
         */
        public HaTooFewHosts() {
            super();
        }

        public String toString() {
            return "HA can only be enabled for 2 hosts or more. Note that 2 hosts requires a pre-configured quorum tiebreak script.";
        }
    }

    /**
     * The uploaded patch file is invalid
     */
    public static class InvalidPatch extends XenAPIException {

        /**
         * Create a new InvalidPatch
         */
        public InvalidPatch() {
            super();
        }

        public String toString() {
            return "The uploaded patch file is invalid";
        }
    }

    /**
     * Operation could not be performed because the drive is not empty
     */
    public static class VbdNotEmpty extends XenAPIException {
        public final String vbd;

        /**
         * Create a new VbdNotEmpty
         *
         * @param vbd
         */
        public VbdNotEmpty(String vbd) {
            super();
            this.vbd = vbd;
        }

        public String toString() {
            return "Operation could not be performed because the drive is not empty";
        }
    }

    /**
     * An error occurred during the migration process.
     */
    public static class VmMigrateFailed extends XenAPIException {
        public final String vm;
        public final String source;
        public final String destination;
        public final String msg;

        /**
         * Create a new VmMigrateFailed
         *
         * @param vm
         * @param source
         * @param destination
         * @param msg
         */
        public VmMigrateFailed(String vm, String source, String destination, String msg) {
            super();
            this.vm = vm;
            this.source = source;
            this.destination = destination;
            this.msg = msg;
        }

        public String toString() {
            return "An error occurred during the migration process.";
        }
    }

    /**
     * The hosts in this pool are not homogeneous.
     */
    public static class HostsNotHomogeneous extends XenAPIException {
        public final String reason;

        /**
         * Create a new HostsNotHomogeneous
         *
         * @param reason
         */
        public HostsNotHomogeneous(String reason) {
            super();
            this.reason = reason;
        }

        public String toString() {
            return "The hosts in this pool are not homogeneous.";
        }
    }

    /**
     * A bond must consist of at least two member interfaces
     */
    public static class PifBondNeedsMoreMembers extends XenAPIException {

        /**
         * Create a new PifBondNeedsMoreMembers
         */
        public PifBondNeedsMoreMembers() {
            super();
        }

        public String toString() {
            return "A bond must consist of at least two member interfaces";
        }
    }

    /**
     * The device is already attached to a VM
     */
    public static class DeviceAlreadyAttached extends XenAPIException {
        public final String device;

        /**
         * Create a new DeviceAlreadyAttached
         *
         * @param device
         */
        public DeviceAlreadyAttached(String device) {
            super();
            this.device = device;
        }

        public String toString() {
            return "The device is already attached to a VM";
        }
    }

    /**
     * Attaching this SR failed.
     */
    public static class SrAttachFailed extends XenAPIException {
        public final String sr;

        /**
         * Create a new SrAttachFailed
         *
         * @param sr
         */
        public SrAttachFailed(String sr) {
            super();
            this.sr = sr;
        }

        public String toString() {
            return "Attaching this SR failed.";
        }
    }

    /**
     * The MAC address specified is not valid.
     */
    public static class MacInvalid extends XenAPIException {
        public final String MAC;

        /**
         * Create a new MacInvalid
         *
         * @param MAC
         */
        public MacInvalid(String MAC) {
            super();
            this.MAC = MAC;
        }

        public String toString() {
            return "The MAC address specified is not valid.";
        }
    }

    /**
     * The restore could not be performed because this backup has been created by a different (incompatible) product verion
     */
    public static class RestoreIncompatibleVersion extends XenAPIException {

        /**
         * Create a new RestoreIncompatibleVersion
         */
        public RestoreIncompatibleVersion() {
            super();
        }

        public String toString() {
            return "The restore could not be performed because this backup has been created by a different (incompatible) product verion";
        }
    }

    /**
     * The host joining the pool cannot already be a master of another pool.
     */
    public static class JoiningHostCannotBeMasterOfOtherHosts extends XenAPIException {

        /**
         * Create a new JoiningHostCannotBeMasterOfOtherHosts
         */
        public JoiningHostCannotBeMasterOfOtherHosts() {
            super();
        }

        public String toString() {
            return "The host joining the pool cannot already be a master of another pool.";
        }
    }

    /**
     * The MAC address specified still exists on this host.
     */
    public static class MacStillExists extends XenAPIException {
        public final String MAC;

        /**
         * Create a new MacStillExists
         *
         * @param MAC
         */
        public MacStillExists(String MAC) {
            super();
            this.MAC = MAC;
        }

        public String toString() {
            return "The MAC address specified still exists on this host.";
        }
    }

    /**
     * This command is only allowed on the OEM edition.
     */
    public static class OnlyAllowedOnOemEdition extends XenAPIException {
        public final String command;

        /**
         * Create a new OnlyAllowedOnOemEdition
         *
         * @param command
         */
        public OnlyAllowedOnOemEdition(String command) {
            super();
            this.command = command;
        }

        public String toString() {
            return "This command is only allowed on the OEM edition.";
        }
    }

    /**
     * VM didn't acknowledge the need to shutdown.
     */
    public static class VmFailedShutdownAcknowledgment extends XenAPIException {

        /**
         * Create a new VmFailedShutdownAcknowledgment
         */
        public VmFailedShutdownAcknowledgment() {
            super();
        }

        public String toString() {
            return "VM didn't acknowledge the need to shutdown.";
        }
    }

    /**
     * The operation failed because the HA software on the specified host could not see a subset of other hosts. Check your network connectivity.
     */
    public static class HaHostCannotSeePeers extends XenAPIException {
        public final String host;
        public final String all;
        public final String subset;

        /**
         * Create a new HaHostCannotSeePeers
         *
         * @param host
         * @param all
         * @param subset
         */
        public HaHostCannotSeePeers(String host, String all, String subset) {
            super();
            this.host = host;
            this.all = all;
            this.subset = subset;
        }

        public String toString() {
            return "The operation failed because the HA software on the specified host could not see a subset of other hosts. Check your network connectivity.";
        }
    }

    /**
     * You attempted an operation on a VM that was judged to be unsafe by the server. This can happen if the VM would run on a CPU that has a potentially incompatable set of feature flags to those the VM requires. If you want to override this warning then use the 'force' option.
     */
    public static class VmUnsafeBoot extends XenAPIException {
        public final String vm;

        /**
         * Create a new VmUnsafeBoot
         *
         * @param vm
         */
        public VmUnsafeBoot(String vm) {
            super();
            this.vm = vm;
        }

        public String toString() {
            return "You attempted an operation on a VM that was judged to be unsafe by the server. This can happen if the VM would run on a CPU that has a potentially incompatable set of feature flags to those the VM requires. If you want to override this warning then use the 'force' option.";
        }
    }

    /**
     * A PBD already exists connecting the SR to the host
     */
    public static class PbdExists extends XenAPIException {
        public final String sr;
        public final String host;
        public final String pbd;

        /**
         * Create a new PbdExists
         *
         * @param sr
         * @param host
         * @param pbd
         */
        public PbdExists(String sr, String host, String pbd) {
            super();
            this.sr = sr;
            this.host = host;
            this.pbd = pbd;
        }

        public String toString() {
            return "A PBD already exists connecting the SR to the host";
        }
    }

    /**
     * Cannot find a plan for placement of VMs as there are no other hosts available.
     */
    public static class HaNoPlan extends XenAPIException {

        /**
         * Create a new HaNoPlan
         */
        public HaNoPlan() {
            super();
        }

        public String toString() {
            return "Cannot find a plan for placement of VMs as there are no other hosts available.";
        }
    }

    /**
     * There was an error connecting to the host while joining it in the pool.
     */
    public static class JoiningHostConnectionFailed extends XenAPIException {

        /**
         * Create a new JoiningHostConnectionFailed
         */
        public JoiningHostConnectionFailed() {
            super();
        }

        public String toString() {
            return "There was an error connecting to the host while joining it in the pool.";
        }
    }

    /**
     * This VM has locked the DVD drive tray, so the disk cannot be ejected
     */
    public static class VbdTrayLocked extends XenAPIException {
        public final String vbd;

        /**
         * Create a new VbdTrayLocked
         *
         * @param vbd
         */
        public VbdTrayLocked(String vbd) {
            super();
            this.vbd = vbd;
        }

        public String toString() {
            return "This VM has locked the DVD drive tray, so the disk cannot be ejected";
        }
    }

    /**
     * Operation cannot proceed while a VLAN exists on this interface.
     */
    public static class PifVlanStillExists extends XenAPIException {
        public final String PIF;

        /**
         * Create a new PifVlanStillExists
         *
         * @param PIF
         */
        public PifVlanStillExists(String PIF) {
            super();
            this.PIF = PIF;
        }

        public String toString() {
            return "Operation cannot proceed while a VLAN exists on this interface.";
        }
    }

    /**
     * You attempted an operation on a VM which requires a more recent version of the PV drivers. Please upgrade your PV drivers.
     */
    public static class VmOldPvDrivers extends XenAPIException {
        public final String vm;
        public final String major;
        public final String minor;

        /**
         * Create a new VmOldPvDrivers
         *
         * @param vm
         * @param major
         * @param minor
         */
        public VmOldPvDrivers(String vm, String major, String minor) {
            super();
            this.vm = vm;
            this.major = major;
            this.minor = minor;
        }

        public String toString() {
            return "You attempted an operation on a VM which requires a more recent version of the PV drivers. Please upgrade your PV drivers.";
        }
    }

    /**
     * Cannot forward messages because the host cannot be contacted.  The host may be switched off or there may be network connectivity problems.
     */
    public static class CannotContactHost extends XenAPIException {
        public final String host;

        /**
         * Create a new CannotContactHost
         *
         * @param host
         */
        public CannotContactHost(String host) {
            super();
            this.host = host;
        }

        public String toString() {
            return "Cannot forward messages because the host cannot be contacted.  The host may be switched off or there may be network connectivity problems.";
        }
    }

    /**
     * The master says the host is not known to it. Perhaps the Host was deleted from the master's database?
     */
    public static class HostUnknownToMaster extends XenAPIException {
        public final String host;

        /**
         * Create a new HostUnknownToMaster
         *
         * @param host
         */
        public HostUnknownToMaster(String host) {
            super();
            this.host = host;
        }

        public String toString() {
            return "The master says the host is not known to it. Perhaps the Host was deleted from the master's database?";
        }
    }

    /**
     * You attempted to run a VM on a host which doesn't have a PIF on a Network needed by the VM. The VM has at least one VIF attached to the Network.
     */
    public static class VmRequiresNetwork extends XenAPIException {
        public final String vm;
        public final String network;

        /**
         * Create a new VmRequiresNetwork
         *
         * @param vm
         * @param network
         */
        public VmRequiresNetwork(String vm, String network) {
            super();
            this.vm = vm;
            this.network = network;
        }

        public String toString() {
            return "You attempted to run a VM on a host which doesn't have a PIF on a Network needed by the VM. The VM has at least one VIF attached to the Network.";
        }
    }

    /**
     * The backup could not be performed because the backup script failed.
     */
    public static class BackupScriptFailed extends XenAPIException {

        /**
         * Create a new BackupScriptFailed
         */
        public BackupScriptFailed() {
            super();
        }

        public String toString() {
            return "The backup could not be performed because the backup script failed.";
        }
    }

    /**
     * Drive could not be hot-unplugged because it is not marked as unpluggable
     */
    public static class VbdNotUnpluggable extends XenAPIException {
        public final String vbd;

        /**
         * Create a new VbdNotUnpluggable
         *
         * @param vbd
         */
        public VbdNotUnpluggable(String vbd) {
            super();
            this.vbd = vbd;
        }

        public String toString() {
            return "Drive could not be hot-unplugged because it is not marked as unpluggable";
        }
    }

    /**
     * The restore could not be performed because a network interface is missing
     */
    public static class RestoreTargetMissingDevice extends XenAPIException {
        public final String device;

        /**
         * Create a new RestoreTargetMissingDevice
         *
         * @param device
         */
        public RestoreTargetMissingDevice(String device) {
            super();
            this.device = device;
        }

        public String toString() {
            return "The restore could not be performed because a network interface is missing";
        }
    }

    /**
     * The host is still booting.
     */
    public static class HostStillBooting extends XenAPIException {

        /**
         * Create a new HostStillBooting
         */
        public HostStillBooting() {
            super();
        }

        public String toString() {
            return "The host is still booting.";
        }
    }

    /**
     * The host joining the pool cannot have any VMs with active tasks.
     */
    public static class JoiningHostCannotHaveVmsWithCurrentOperations extends XenAPIException {

        /**
         * Create a new JoiningHostCannotHaveVmsWithCurrentOperations
         */
        public JoiningHostCannotHaveVmsWithCurrentOperations() {
            super();
        }

        public String toString() {
            return "The host joining the pool cannot have any VMs with active tasks.";
        }
    }

    /**
     * The VM could not be imported; is the file corrupt?
     */
    public static class ImportError extends XenAPIException {
        public final String msg;

        /**
         * Create a new ImportError
         *
         * @param msg
         */
        public ImportError(String msg) {
            super();
            this.msg = msg;
        }

        public String toString() {
            return "The VM could not be imported; is the file corrupt?";
        }
    }


    public static String toString(Object object) {
        try {
            return (String) object;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Long toLong(Object object) {
        try {
            return Long.valueOf((String) object);
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Double toDouble(Object object) {
        try {
            return (Double) object;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Boolean toBoolean(Object object) {
        try {
            return (Boolean) object;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Date toDate(Object object) {
        try {
            try{
                return (Date) object;
            } catch (ClassCastException e){
                    //Occasionally the date comes back as an ocaml float rather than 
                    //in the xmlrpc format! Catch this and convert. 
                    return (new Date((long) (1000*Double.parseDouble((String) object))));
            }
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Types.XenAPIObjects toXenAPIObjects(Object object) {
        try {
            try {
                return XenAPIObjects.valueOf(((String) object).toUpperCase());
            } catch (IllegalArgumentException ex) {
                return XenAPIObjects.UNRECOGNIZED;
            }
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Types.AfterApplyGuidance toAfterApplyGuidance(Object object) {
        try {
            try {
                return AfterApplyGuidance.valueOf(((String) object).toUpperCase());
            } catch (IllegalArgumentException ex) {
                return AfterApplyGuidance.UNRECOGNIZED;
            }
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Types.ConsoleProtocol toConsoleProtocol(Object object) {
        try {
            try {
                return ConsoleProtocol.valueOf(((String) object).toUpperCase());
            } catch (IllegalArgumentException ex) {
                return ConsoleProtocol.UNRECOGNIZED;
            }
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Types.EventOperation toEventOperation(Object object) {
        try {
            try {
                return EventOperation.valueOf(((String) object).toUpperCase());
            } catch (IllegalArgumentException ex) {
                return EventOperation.UNRECOGNIZED;
            }
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Types.HostAllowedOperations toHostAllowedOperations(Object object) {
        try {
            try {
                return HostAllowedOperations.valueOf(((String) object).toUpperCase());
            } catch (IllegalArgumentException ex) {
                return HostAllowedOperations.UNRECOGNIZED;
            }
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Types.IpConfigurationMode toIpConfigurationMode(Object object) {
        try {
            try {
                return IpConfigurationMode.valueOf(((String) object).toUpperCase());
            } catch (IllegalArgumentException ex) {
                return IpConfigurationMode.UNRECOGNIZED;
            }
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Types.NetworkOperations toNetworkOperations(Object object) {
        try {
            try {
                return NetworkOperations.valueOf(((String) object).toUpperCase());
            } catch (IllegalArgumentException ex) {
                return NetworkOperations.UNRECOGNIZED;
            }
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Types.OnCrashBehaviour toOnCrashBehaviour(Object object) {
        try {
            try {
                return OnCrashBehaviour.valueOf(((String) object).toUpperCase());
            } catch (IllegalArgumentException ex) {
                return OnCrashBehaviour.UNRECOGNIZED;
            }
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Types.OnNormalExit toOnNormalExit(Object object) {
        try {
            try {
                return OnNormalExit.valueOf(((String) object).toUpperCase());
            } catch (IllegalArgumentException ex) {
                return OnNormalExit.UNRECOGNIZED;
            }
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Types.StorageOperations toStorageOperations(Object object) {
        try {
            try {
                return StorageOperations.valueOf(((String) object).toUpperCase());
            } catch (IllegalArgumentException ex) {
                return StorageOperations.UNRECOGNIZED;
            }
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Types.TaskAllowedOperations toTaskAllowedOperations(Object object) {
        try {
            try {
                return TaskAllowedOperations.valueOf(((String) object).toUpperCase());
            } catch (IllegalArgumentException ex) {
                return TaskAllowedOperations.UNRECOGNIZED;
            }
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Types.TaskStatusType toTaskStatusType(Object object) {
        try {
            try {
                return TaskStatusType.valueOf(((String) object).toUpperCase());
            } catch (IllegalArgumentException ex) {
                return TaskStatusType.UNRECOGNIZED;
            }
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Types.VbdMode toVbdMode(Object object) {
        try {
            try {
                return VbdMode.valueOf(((String) object).toUpperCase());
            } catch (IllegalArgumentException ex) {
                return VbdMode.UNRECOGNIZED;
            }
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Types.VbdOperations toVbdOperations(Object object) {
        try {
            try {
                return VbdOperations.valueOf(((String) object).toUpperCase());
            } catch (IllegalArgumentException ex) {
                return VbdOperations.UNRECOGNIZED;
            }
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Types.VbdType toVbdType(Object object) {
        try {
            try {
                return VbdType.valueOf(((String) object).toUpperCase());
            } catch (IllegalArgumentException ex) {
                return VbdType.UNRECOGNIZED;
            }
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Types.VdiOperations toVdiOperations(Object object) {
        try {
            try {
                return VdiOperations.valueOf(((String) object).toUpperCase());
            } catch (IllegalArgumentException ex) {
                return VdiOperations.UNRECOGNIZED;
            }
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Types.VdiType toVdiType(Object object) {
        try {
            try {
                return VdiType.valueOf(((String) object).toUpperCase());
            } catch (IllegalArgumentException ex) {
                return VdiType.UNRECOGNIZED;
            }
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Types.VifOperations toVifOperations(Object object) {
        try {
            try {
                return VifOperations.valueOf(((String) object).toUpperCase());
            } catch (IllegalArgumentException ex) {
                return VifOperations.UNRECOGNIZED;
            }
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Types.VmOperations toVmOperations(Object object) {
        try {
            try {
                return VmOperations.valueOf(((String) object).toUpperCase());
            } catch (IllegalArgumentException ex) {
                return VmOperations.UNRECOGNIZED;
            }
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Types.VmPowerState toVmPowerState(Object object) {
        try {
            try {
                return VmPowerState.valueOf(((String) object).toUpperCase());
            } catch (IllegalArgumentException ex) {
                return VmPowerState.UNRECOGNIZED;
            }
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Set<String> toSetOfString(Object object) {
        try {
            Object[] items = (Object[]) object;
            Set<String> result = new HashSet<String>();
            for(Object item: items) {
                String typed = toString(item);
                result.add(typed);
            }
            return result;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Set<Types.AfterApplyGuidance> toSetOfAfterApplyGuidance(Object object) {
        try {
            Object[] items = (Object[]) object;
            Set<Types.AfterApplyGuidance> result = new HashSet<Types.AfterApplyGuidance>();
            for(Object item: items) {
                Types.AfterApplyGuidance typed = toAfterApplyGuidance(item);
                result.add(typed);
            }
            return result;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Set<Types.HostAllowedOperations> toSetOfHostAllowedOperations(Object object) {
        try {
            Object[] items = (Object[]) object;
            Set<Types.HostAllowedOperations> result = new HashSet<Types.HostAllowedOperations>();
            for(Object item: items) {
                Types.HostAllowedOperations typed = toHostAllowedOperations(item);
                result.add(typed);
            }
            return result;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Set<Types.NetworkOperations> toSetOfNetworkOperations(Object object) {
        try {
            Object[] items = (Object[]) object;
            Set<Types.NetworkOperations> result = new HashSet<Types.NetworkOperations>();
            for(Object item: items) {
                Types.NetworkOperations typed = toNetworkOperations(item);
                result.add(typed);
            }
            return result;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Set<Types.StorageOperations> toSetOfStorageOperations(Object object) {
        try {
            Object[] items = (Object[]) object;
            Set<Types.StorageOperations> result = new HashSet<Types.StorageOperations>();
            for(Object item: items) {
                Types.StorageOperations typed = toStorageOperations(item);
                result.add(typed);
            }
            return result;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Set<Types.TaskAllowedOperations> toSetOfTaskAllowedOperations(Object object) {
        try {
            Object[] items = (Object[]) object;
            Set<Types.TaskAllowedOperations> result = new HashSet<Types.TaskAllowedOperations>();
            for(Object item: items) {
                Types.TaskAllowedOperations typed = toTaskAllowedOperations(item);
                result.add(typed);
            }
            return result;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Set<Types.VbdOperations> toSetOfVbdOperations(Object object) {
        try {
            Object[] items = (Object[]) object;
            Set<Types.VbdOperations> result = new HashSet<Types.VbdOperations>();
            for(Object item: items) {
                Types.VbdOperations typed = toVbdOperations(item);
                result.add(typed);
            }
            return result;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Set<Types.VdiOperations> toSetOfVdiOperations(Object object) {
        try {
            Object[] items = (Object[]) object;
            Set<Types.VdiOperations> result = new HashSet<Types.VdiOperations>();
            for(Object item: items) {
                Types.VdiOperations typed = toVdiOperations(item);
                result.add(typed);
            }
            return result;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Set<Types.VifOperations> toSetOfVifOperations(Object object) {
        try {
            Object[] items = (Object[]) object;
            Set<Types.VifOperations> result = new HashSet<Types.VifOperations>();
            for(Object item: items) {
                Types.VifOperations typed = toVifOperations(item);
                result.add(typed);
            }
            return result;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Set<Types.VmOperations> toSetOfVmOperations(Object object) {
        try {
            Object[] items = (Object[]) object;
            Set<Types.VmOperations> result = new HashSet<Types.VmOperations>();
            for(Object item: items) {
                Types.VmOperations typed = toVmOperations(item);
                result.add(typed);
            }
            return result;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Set<Bond> toSetOfBond(Object object) {
        try {
            Object[] items = (Object[]) object;
            Set<Bond> result = new HashSet<Bond>();
            for(Object item: items) {
                Bond typed = toBond(item);
                result.add(typed);
            }
            return result;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Set<PBD> toSetOfPBD(Object object) {
        try {
            Object[] items = (Object[]) object;
            Set<PBD> result = new HashSet<PBD>();
            for(Object item: items) {
                PBD typed = toPBD(item);
                result.add(typed);
            }
            return result;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Set<PIF> toSetOfPIF(Object object) {
        try {
            Object[] items = (Object[]) object;
            Set<PIF> result = new HashSet<PIF>();
            for(Object item: items) {
                PIF typed = toPIF(item);
                result.add(typed);
            }
            return result;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Set<PIFMetrics> toSetOfPIFMetrics(Object object) {
        try {
            Object[] items = (Object[]) object;
            Set<PIFMetrics> result = new HashSet<PIFMetrics>();
            for(Object item: items) {
                PIFMetrics typed = toPIFMetrics(item);
                result.add(typed);
            }
            return result;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Set<SM> toSetOfSM(Object object) {
        try {
            Object[] items = (Object[]) object;
            Set<SM> result = new HashSet<SM>();
            for(Object item: items) {
                SM typed = toSM(item);
                result.add(typed);
            }
            return result;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Set<SR> toSetOfSR(Object object) {
        try {
            Object[] items = (Object[]) object;
            Set<SR> result = new HashSet<SR>();
            for(Object item: items) {
                SR typed = toSR(item);
                result.add(typed);
            }
            return result;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Set<VBD> toSetOfVBD(Object object) {
        try {
            Object[] items = (Object[]) object;
            Set<VBD> result = new HashSet<VBD>();
            for(Object item: items) {
                VBD typed = toVBD(item);
                result.add(typed);
            }
            return result;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Set<VBDMetrics> toSetOfVBDMetrics(Object object) {
        try {
            Object[] items = (Object[]) object;
            Set<VBDMetrics> result = new HashSet<VBDMetrics>();
            for(Object item: items) {
                VBDMetrics typed = toVBDMetrics(item);
                result.add(typed);
            }
            return result;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Set<VDI> toSetOfVDI(Object object) {
        try {
            Object[] items = (Object[]) object;
            Set<VDI> result = new HashSet<VDI>();
            for(Object item: items) {
                VDI typed = toVDI(item);
                result.add(typed);
            }
            return result;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Set<VIF> toSetOfVIF(Object object) {
        try {
            Object[] items = (Object[]) object;
            Set<VIF> result = new HashSet<VIF>();
            for(Object item: items) {
                VIF typed = toVIF(item);
                result.add(typed);
            }
            return result;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Set<VIFMetrics> toSetOfVIFMetrics(Object object) {
        try {
            Object[] items = (Object[]) object;
            Set<VIFMetrics> result = new HashSet<VIFMetrics>();
            for(Object item: items) {
                VIFMetrics typed = toVIFMetrics(item);
                result.add(typed);
            }
            return result;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Set<VLAN> toSetOfVLAN(Object object) {
        try {
            Object[] items = (Object[]) object;
            Set<VLAN> result = new HashSet<VLAN>();
            for(Object item: items) {
                VLAN typed = toVLAN(item);
                result.add(typed);
            }
            return result;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Set<VM> toSetOfVM(Object object) {
        try {
            Object[] items = (Object[]) object;
            Set<VM> result = new HashSet<VM>();
            for(Object item: items) {
                VM typed = toVM(item);
                result.add(typed);
            }
            return result;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Set<VMGuestMetrics> toSetOfVMGuestMetrics(Object object) {
        try {
            Object[] items = (Object[]) object;
            Set<VMGuestMetrics> result = new HashSet<VMGuestMetrics>();
            for(Object item: items) {
                VMGuestMetrics typed = toVMGuestMetrics(item);
                result.add(typed);
            }
            return result;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Set<VMMetrics> toSetOfVMMetrics(Object object) {
        try {
            Object[] items = (Object[]) object;
            Set<VMMetrics> result = new HashSet<VMMetrics>();
            for(Object item: items) {
                VMMetrics typed = toVMMetrics(item);
                result.add(typed);
            }
            return result;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Set<VTPM> toSetOfVTPM(Object object) {
        try {
            Object[] items = (Object[]) object;
            Set<VTPM> result = new HashSet<VTPM>();
            for(Object item: items) {
                VTPM typed = toVTPM(item);
                result.add(typed);
            }
            return result;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Set<Console> toSetOfConsole(Object object) {
        try {
            Object[] items = (Object[]) object;
            Set<Console> result = new HashSet<Console>();
            for(Object item: items) {
                Console typed = toConsole(item);
                result.add(typed);
            }
            return result;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Set<Crashdump> toSetOfCrashdump(Object object) {
        try {
            Object[] items = (Object[]) object;
            Set<Crashdump> result = new HashSet<Crashdump>();
            for(Object item: items) {
                Crashdump typed = toCrashdump(item);
                result.add(typed);
            }
            return result;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Set<Host> toSetOfHost(Object object) {
        try {
            Object[] items = (Object[]) object;
            Set<Host> result = new HashSet<Host>();
            for(Object item: items) {
                Host typed = toHost(item);
                result.add(typed);
            }
            return result;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Set<HostCpu> toSetOfHostCpu(Object object) {
        try {
            Object[] items = (Object[]) object;
            Set<HostCpu> result = new HashSet<HostCpu>();
            for(Object item: items) {
                HostCpu typed = toHostCpu(item);
                result.add(typed);
            }
            return result;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Set<HostCrashdump> toSetOfHostCrashdump(Object object) {
        try {
            Object[] items = (Object[]) object;
            Set<HostCrashdump> result = new HashSet<HostCrashdump>();
            for(Object item: items) {
                HostCrashdump typed = toHostCrashdump(item);
                result.add(typed);
            }
            return result;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Set<HostMetrics> toSetOfHostMetrics(Object object) {
        try {
            Object[] items = (Object[]) object;
            Set<HostMetrics> result = new HashSet<HostMetrics>();
            for(Object item: items) {
                HostMetrics typed = toHostMetrics(item);
                result.add(typed);
            }
            return result;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Set<HostPatch> toSetOfHostPatch(Object object) {
        try {
            Object[] items = (Object[]) object;
            Set<HostPatch> result = new HashSet<HostPatch>();
            for(Object item: items) {
                HostPatch typed = toHostPatch(item);
                result.add(typed);
            }
            return result;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Set<Network> toSetOfNetwork(Object object) {
        try {
            Object[] items = (Object[]) object;
            Set<Network> result = new HashSet<Network>();
            for(Object item: items) {
                Network typed = toNetwork(item);
                result.add(typed);
            }
            return result;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Set<Pool> toSetOfPool(Object object) {
        try {
            Object[] items = (Object[]) object;
            Set<Pool> result = new HashSet<Pool>();
            for(Object item: items) {
                Pool typed = toPool(item);
                result.add(typed);
            }
            return result;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Set<PoolPatch> toSetOfPoolPatch(Object object) {
        try {
            Object[] items = (Object[]) object;
            Set<PoolPatch> result = new HashSet<PoolPatch>();
            for(Object item: items) {
                PoolPatch typed = toPoolPatch(item);
                result.add(typed);
            }
            return result;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Set<Task> toSetOfTask(Object object) {
        try {
            Object[] items = (Object[]) object;
            Set<Task> result = new HashSet<Task>();
            for(Object item: items) {
                Task typed = toTask(item);
                result.add(typed);
            }
            return result;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Set<Event.Record> toSetOfEventRecord(Object object) {
        try {
            Object[] items = (Object[]) object;
            Set<Event.Record> result = new HashSet<Event.Record>();
            for(Object item: items) {
                Event.Record typed = toEventRecord(item);
                result.add(typed);
            }
            return result;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Map<String, String> toMapOfStringString(Object object) {
        try {
            Map map = (Map) object;
            Map<String,String> result = new HashMap<String,String>();
            Set<Map.Entry> entries = map.entrySet();
            for(Map.Entry entry: entries) {
                String key = toString(entry.getKey());
                String value = toString(entry.getValue());
                result.put(key, value);
            }
            return result;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Map<String, Types.HostAllowedOperations> toMapOfStringHostAllowedOperations(Object object) {
        try {
            Map map = (Map) object;
            Map<String,Types.HostAllowedOperations> result = new HashMap<String,Types.HostAllowedOperations>();
            Set<Map.Entry> entries = map.entrySet();
            for(Map.Entry entry: entries) {
                String key = toString(entry.getKey());
                Types.HostAllowedOperations value = toHostAllowedOperations(entry.getValue());
                result.put(key, value);
            }
            return result;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Map<String, Types.NetworkOperations> toMapOfStringNetworkOperations(Object object) {
        try {
            Map map = (Map) object;
            Map<String,Types.NetworkOperations> result = new HashMap<String,Types.NetworkOperations>();
            Set<Map.Entry> entries = map.entrySet();
            for(Map.Entry entry: entries) {
                String key = toString(entry.getKey());
                Types.NetworkOperations value = toNetworkOperations(entry.getValue());
                result.put(key, value);
            }
            return result;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Map<String, Types.StorageOperations> toMapOfStringStorageOperations(Object object) {
        try {
            Map map = (Map) object;
            Map<String,Types.StorageOperations> result = new HashMap<String,Types.StorageOperations>();
            Set<Map.Entry> entries = map.entrySet();
            for(Map.Entry entry: entries) {
                String key = toString(entry.getKey());
                Types.StorageOperations value = toStorageOperations(entry.getValue());
                result.put(key, value);
            }
            return result;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Map<String, Types.TaskAllowedOperations> toMapOfStringTaskAllowedOperations(Object object) {
        try {
            Map map = (Map) object;
            Map<String,Types.TaskAllowedOperations> result = new HashMap<String,Types.TaskAllowedOperations>();
            Set<Map.Entry> entries = map.entrySet();
            for(Map.Entry entry: entries) {
                String key = toString(entry.getKey());
                Types.TaskAllowedOperations value = toTaskAllowedOperations(entry.getValue());
                result.put(key, value);
            }
            return result;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Map<String, Types.VbdOperations> toMapOfStringVbdOperations(Object object) {
        try {
            Map map = (Map) object;
            Map<String,Types.VbdOperations> result = new HashMap<String,Types.VbdOperations>();
            Set<Map.Entry> entries = map.entrySet();
            for(Map.Entry entry: entries) {
                String key = toString(entry.getKey());
                Types.VbdOperations value = toVbdOperations(entry.getValue());
                result.put(key, value);
            }
            return result;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Map<String, Types.VdiOperations> toMapOfStringVdiOperations(Object object) {
        try {
            Map map = (Map) object;
            Map<String,Types.VdiOperations> result = new HashMap<String,Types.VdiOperations>();
            Set<Map.Entry> entries = map.entrySet();
            for(Map.Entry entry: entries) {
                String key = toString(entry.getKey());
                Types.VdiOperations value = toVdiOperations(entry.getValue());
                result.put(key, value);
            }
            return result;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Map<String, Types.VifOperations> toMapOfStringVifOperations(Object object) {
        try {
            Map map = (Map) object;
            Map<String,Types.VifOperations> result = new HashMap<String,Types.VifOperations>();
            Set<Map.Entry> entries = map.entrySet();
            for(Map.Entry entry: entries) {
                String key = toString(entry.getKey());
                Types.VifOperations value = toVifOperations(entry.getValue());
                result.put(key, value);
            }
            return result;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Map<String, Types.VmOperations> toMapOfStringVmOperations(Object object) {
        try {
            Map map = (Map) object;
            Map<String,Types.VmOperations> result = new HashMap<String,Types.VmOperations>();
            Set<Map.Entry> entries = map.entrySet();
            for(Map.Entry entry: entries) {
                String key = toString(entry.getKey());
                Types.VmOperations value = toVmOperations(entry.getValue());
                result.put(key, value);
            }
            return result;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Map<Long, Long> toMapOfLongLong(Object object) {
        try {
            Map map = (Map) object;
            Map<Long,Long> result = new HashMap<Long,Long>();
            Set<Map.Entry> entries = map.entrySet();
            for(Map.Entry entry: entries) {
                Long key = toLong(entry.getKey());
                Long value = toLong(entry.getValue());
                result.put(key, value);
            }
            return result;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Map<Long, Double> toMapOfLongDouble(Object object) {
        try {
            Map map = (Map) object;
            Map<Long,Double> result = new HashMap<Long,Double>();
            Set<Map.Entry> entries = map.entrySet();
            for(Map.Entry entry: entries) {
                Long key = toLong(entry.getKey());
                Double value = toDouble(entry.getValue());
                result.put(key, value);
            }
            return result;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Map<Long, Set<String>> toMapOfLongSetOfString(Object object) {
        try {
            Map map = (Map) object;
            Map<Long,Set<String>> result = new HashMap<Long,Set<String>>();
            Set<Map.Entry> entries = map.entrySet();
            for(Map.Entry entry: entries) {
                Long key = toLong(entry.getKey());
                Set<String> value = toSetOfString(entry.getValue());
                result.put(key, value);
            }
            return result;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Map<Bond, Bond.Record> toMapOfBondBondRecord(Object object) {
        try {
            Map map = (Map) object;
            Map<Bond,Bond.Record> result = new HashMap<Bond,Bond.Record>();
            Set<Map.Entry> entries = map.entrySet();
            for(Map.Entry entry: entries) {
                Bond key = toBond(entry.getKey());
                Bond.Record value = toBondRecord(entry.getValue());
                result.put(key, value);
            }
            return result;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Map<PBD, PBD.Record> toMapOfPBDPBDRecord(Object object) {
        try {
            Map map = (Map) object;
            Map<PBD,PBD.Record> result = new HashMap<PBD,PBD.Record>();
            Set<Map.Entry> entries = map.entrySet();
            for(Map.Entry entry: entries) {
                PBD key = toPBD(entry.getKey());
                PBD.Record value = toPBDRecord(entry.getValue());
                result.put(key, value);
            }
            return result;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Map<PIF, PIF.Record> toMapOfPIFPIFRecord(Object object) {
        try {
            Map map = (Map) object;
            Map<PIF,PIF.Record> result = new HashMap<PIF,PIF.Record>();
            Set<Map.Entry> entries = map.entrySet();
            for(Map.Entry entry: entries) {
                PIF key = toPIF(entry.getKey());
                PIF.Record value = toPIFRecord(entry.getValue());
                result.put(key, value);
            }
            return result;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Map<PIFMetrics, PIFMetrics.Record> toMapOfPIFMetricsPIFMetricsRecord(Object object) {
        try {
            Map map = (Map) object;
            Map<PIFMetrics,PIFMetrics.Record> result = new HashMap<PIFMetrics,PIFMetrics.Record>();
            Set<Map.Entry> entries = map.entrySet();
            for(Map.Entry entry: entries) {
                PIFMetrics key = toPIFMetrics(entry.getKey());
                PIFMetrics.Record value = toPIFMetricsRecord(entry.getValue());
                result.put(key, value);
            }
            return result;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Map<SM, SM.Record> toMapOfSMSMRecord(Object object) {
        try {
            Map map = (Map) object;
            Map<SM,SM.Record> result = new HashMap<SM,SM.Record>();
            Set<Map.Entry> entries = map.entrySet();
            for(Map.Entry entry: entries) {
                SM key = toSM(entry.getKey());
                SM.Record value = toSMRecord(entry.getValue());
                result.put(key, value);
            }
            return result;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Map<SR, SR.Record> toMapOfSRSRRecord(Object object) {
        try {
            Map map = (Map) object;
            Map<SR,SR.Record> result = new HashMap<SR,SR.Record>();
            Set<Map.Entry> entries = map.entrySet();
            for(Map.Entry entry: entries) {
                SR key = toSR(entry.getKey());
                SR.Record value = toSRRecord(entry.getValue());
                result.put(key, value);
            }
            return result;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Map<VBD, VBD.Record> toMapOfVBDVBDRecord(Object object) {
        try {
            Map map = (Map) object;
            Map<VBD,VBD.Record> result = new HashMap<VBD,VBD.Record>();
            Set<Map.Entry> entries = map.entrySet();
            for(Map.Entry entry: entries) {
                VBD key = toVBD(entry.getKey());
                VBD.Record value = toVBDRecord(entry.getValue());
                result.put(key, value);
            }
            return result;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Map<VBDMetrics, VBDMetrics.Record> toMapOfVBDMetricsVBDMetricsRecord(Object object) {
        try {
            Map map = (Map) object;
            Map<VBDMetrics,VBDMetrics.Record> result = new HashMap<VBDMetrics,VBDMetrics.Record>();
            Set<Map.Entry> entries = map.entrySet();
            for(Map.Entry entry: entries) {
                VBDMetrics key = toVBDMetrics(entry.getKey());
                VBDMetrics.Record value = toVBDMetricsRecord(entry.getValue());
                result.put(key, value);
            }
            return result;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Map<VDI, VDI.Record> toMapOfVDIVDIRecord(Object object) {
        try {
            Map map = (Map) object;
            Map<VDI,VDI.Record> result = new HashMap<VDI,VDI.Record>();
            Set<Map.Entry> entries = map.entrySet();
            for(Map.Entry entry: entries) {
                VDI key = toVDI(entry.getKey());
                VDI.Record value = toVDIRecord(entry.getValue());
                result.put(key, value);
            }
            return result;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Map<VIF, VIF.Record> toMapOfVIFVIFRecord(Object object) {
        try {
            Map map = (Map) object;
            Map<VIF,VIF.Record> result = new HashMap<VIF,VIF.Record>();
            Set<Map.Entry> entries = map.entrySet();
            for(Map.Entry entry: entries) {
                VIF key = toVIF(entry.getKey());
                VIF.Record value = toVIFRecord(entry.getValue());
                result.put(key, value);
            }
            return result;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Map<VIFMetrics, VIFMetrics.Record> toMapOfVIFMetricsVIFMetricsRecord(Object object) {
        try {
            Map map = (Map) object;
            Map<VIFMetrics,VIFMetrics.Record> result = new HashMap<VIFMetrics,VIFMetrics.Record>();
            Set<Map.Entry> entries = map.entrySet();
            for(Map.Entry entry: entries) {
                VIFMetrics key = toVIFMetrics(entry.getKey());
                VIFMetrics.Record value = toVIFMetricsRecord(entry.getValue());
                result.put(key, value);
            }
            return result;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Map<VLAN, VLAN.Record> toMapOfVLANVLANRecord(Object object) {
        try {
            Map map = (Map) object;
            Map<VLAN,VLAN.Record> result = new HashMap<VLAN,VLAN.Record>();
            Set<Map.Entry> entries = map.entrySet();
            for(Map.Entry entry: entries) {
                VLAN key = toVLAN(entry.getKey());
                VLAN.Record value = toVLANRecord(entry.getValue());
                result.put(key, value);
            }
            return result;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Map<VM, VM.Record> toMapOfVMVMRecord(Object object) {
        try {
            Map map = (Map) object;
            Map<VM,VM.Record> result = new HashMap<VM,VM.Record>();
            Set<Map.Entry> entries = map.entrySet();
            for(Map.Entry entry: entries) {
                VM key = toVM(entry.getKey());
                VM.Record value = toVMRecord(entry.getValue());
                result.put(key, value);
            }
            return result;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Map<VMGuestMetrics, VMGuestMetrics.Record> toMapOfVMGuestMetricsVMGuestMetricsRecord(Object object) {
        try {
            Map map = (Map) object;
            Map<VMGuestMetrics,VMGuestMetrics.Record> result = new HashMap<VMGuestMetrics,VMGuestMetrics.Record>();
            Set<Map.Entry> entries = map.entrySet();
            for(Map.Entry entry: entries) {
                VMGuestMetrics key = toVMGuestMetrics(entry.getKey());
                VMGuestMetrics.Record value = toVMGuestMetricsRecord(entry.getValue());
                result.put(key, value);
            }
            return result;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Map<VMMetrics, VMMetrics.Record> toMapOfVMMetricsVMMetricsRecord(Object object) {
        try {
            Map map = (Map) object;
            Map<VMMetrics,VMMetrics.Record> result = new HashMap<VMMetrics,VMMetrics.Record>();
            Set<Map.Entry> entries = map.entrySet();
            for(Map.Entry entry: entries) {
                VMMetrics key = toVMMetrics(entry.getKey());
                VMMetrics.Record value = toVMMetricsRecord(entry.getValue());
                result.put(key, value);
            }
            return result;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Map<Console, Console.Record> toMapOfConsoleConsoleRecord(Object object) {
        try {
            Map map = (Map) object;
            Map<Console,Console.Record> result = new HashMap<Console,Console.Record>();
            Set<Map.Entry> entries = map.entrySet();
            for(Map.Entry entry: entries) {
                Console key = toConsole(entry.getKey());
                Console.Record value = toConsoleRecord(entry.getValue());
                result.put(key, value);
            }
            return result;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Map<Crashdump, Crashdump.Record> toMapOfCrashdumpCrashdumpRecord(Object object) {
        try {
            Map map = (Map) object;
            Map<Crashdump,Crashdump.Record> result = new HashMap<Crashdump,Crashdump.Record>();
            Set<Map.Entry> entries = map.entrySet();
            for(Map.Entry entry: entries) {
                Crashdump key = toCrashdump(entry.getKey());
                Crashdump.Record value = toCrashdumpRecord(entry.getValue());
                result.put(key, value);
            }
            return result;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Map<Host, Host.Record> toMapOfHostHostRecord(Object object) {
        try {
            Map map = (Map) object;
            Map<Host,Host.Record> result = new HashMap<Host,Host.Record>();
            Set<Map.Entry> entries = map.entrySet();
            for(Map.Entry entry: entries) {
                Host key = toHost(entry.getKey());
                Host.Record value = toHostRecord(entry.getValue());
                result.put(key, value);
            }
            return result;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Map<HostCpu, HostCpu.Record> toMapOfHostCpuHostCpuRecord(Object object) {
        try {
            Map map = (Map) object;
            Map<HostCpu,HostCpu.Record> result = new HashMap<HostCpu,HostCpu.Record>();
            Set<Map.Entry> entries = map.entrySet();
            for(Map.Entry entry: entries) {
                HostCpu key = toHostCpu(entry.getKey());
                HostCpu.Record value = toHostCpuRecord(entry.getValue());
                result.put(key, value);
            }
            return result;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Map<HostCrashdump, HostCrashdump.Record> toMapOfHostCrashdumpHostCrashdumpRecord(Object object) {
        try {
            Map map = (Map) object;
            Map<HostCrashdump,HostCrashdump.Record> result = new HashMap<HostCrashdump,HostCrashdump.Record>();
            Set<Map.Entry> entries = map.entrySet();
            for(Map.Entry entry: entries) {
                HostCrashdump key = toHostCrashdump(entry.getKey());
                HostCrashdump.Record value = toHostCrashdumpRecord(entry.getValue());
                result.put(key, value);
            }
            return result;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Map<HostMetrics, HostMetrics.Record> toMapOfHostMetricsHostMetricsRecord(Object object) {
        try {
            Map map = (Map) object;
            Map<HostMetrics,HostMetrics.Record> result = new HashMap<HostMetrics,HostMetrics.Record>();
            Set<Map.Entry> entries = map.entrySet();
            for(Map.Entry entry: entries) {
                HostMetrics key = toHostMetrics(entry.getKey());
                HostMetrics.Record value = toHostMetricsRecord(entry.getValue());
                result.put(key, value);
            }
            return result;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Map<HostPatch, HostPatch.Record> toMapOfHostPatchHostPatchRecord(Object object) {
        try {
            Map map = (Map) object;
            Map<HostPatch,HostPatch.Record> result = new HashMap<HostPatch,HostPatch.Record>();
            Set<Map.Entry> entries = map.entrySet();
            for(Map.Entry entry: entries) {
                HostPatch key = toHostPatch(entry.getKey());
                HostPatch.Record value = toHostPatchRecord(entry.getValue());
                result.put(key, value);
            }
            return result;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Map<Network, Network.Record> toMapOfNetworkNetworkRecord(Object object) {
        try {
            Map map = (Map) object;
            Map<Network,Network.Record> result = new HashMap<Network,Network.Record>();
            Set<Map.Entry> entries = map.entrySet();
            for(Map.Entry entry: entries) {
                Network key = toNetwork(entry.getKey());
                Network.Record value = toNetworkRecord(entry.getValue());
                result.put(key, value);
            }
            return result;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Map<Pool, Pool.Record> toMapOfPoolPoolRecord(Object object) {
        try {
            Map map = (Map) object;
            Map<Pool,Pool.Record> result = new HashMap<Pool,Pool.Record>();
            Set<Map.Entry> entries = map.entrySet();
            for(Map.Entry entry: entries) {
                Pool key = toPool(entry.getKey());
                Pool.Record value = toPoolRecord(entry.getValue());
                result.put(key, value);
            }
            return result;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Map<PoolPatch, PoolPatch.Record> toMapOfPoolPatchPoolPatchRecord(Object object) {
        try {
            Map map = (Map) object;
            Map<PoolPatch,PoolPatch.Record> result = new HashMap<PoolPatch,PoolPatch.Record>();
            Set<Map.Entry> entries = map.entrySet();
            for(Map.Entry entry: entries) {
                PoolPatch key = toPoolPatch(entry.getKey());
                PoolPatch.Record value = toPoolPatchRecord(entry.getValue());
                result.put(key, value);
            }
            return result;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Map<Task, Task.Record> toMapOfTaskTaskRecord(Object object) {
        try {
            Map map = (Map) object;
            Map<Task,Task.Record> result = new HashMap<Task,Task.Record>();
            Set<Map.Entry> entries = map.entrySet();
            for(Map.Entry entry: entries) {
                Task key = toTask(entry.getKey());
                Task.Record value = toTaskRecord(entry.getValue());
                result.put(key, value);
            }
            return result;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Bond toBond(Object object) {
        try {
            return Bond.getInstFromRef((String) object);
        } catch (NullPointerException e){
            return null;
        }
    }

    public static PBD toPBD(Object object) {
        try {
            return PBD.getInstFromRef((String) object);
        } catch (NullPointerException e){
            return null;
        }
    }

    public static PIF toPIF(Object object) {
        try {
            return PIF.getInstFromRef((String) object);
        } catch (NullPointerException e){
            return null;
        }
    }

    public static PIFMetrics toPIFMetrics(Object object) {
        try {
            return PIFMetrics.getInstFromRef((String) object);
        } catch (NullPointerException e){
            return null;
        }
    }

    public static SM toSM(Object object) {
        try {
            return SM.getInstFromRef((String) object);
        } catch (NullPointerException e){
            return null;
        }
    }

    public static SR toSR(Object object) {
        try {
            return SR.getInstFromRef((String) object);
        } catch (NullPointerException e){
            return null;
        }
    }

    public static VBD toVBD(Object object) {
        try {
            return VBD.getInstFromRef((String) object);
        } catch (NullPointerException e){
            return null;
        }
    }

    public static VBDMetrics toVBDMetrics(Object object) {
        try {
            return VBDMetrics.getInstFromRef((String) object);
        } catch (NullPointerException e){
            return null;
        }
    }

    public static VDI toVDI(Object object) {
        try {
            return VDI.getInstFromRef((String) object);
        } catch (NullPointerException e){
            return null;
        }
    }

    public static VIF toVIF(Object object) {
        try {
            return VIF.getInstFromRef((String) object);
        } catch (NullPointerException e){
            return null;
        }
    }

    public static VIFMetrics toVIFMetrics(Object object) {
        try {
            return VIFMetrics.getInstFromRef((String) object);
        } catch (NullPointerException e){
            return null;
        }
    }

    public static VLAN toVLAN(Object object) {
        try {
            return VLAN.getInstFromRef((String) object);
        } catch (NullPointerException e){
            return null;
        }
    }

    public static VM toVM(Object object) {
        try {
            return VM.getInstFromRef((String) object);
        } catch (NullPointerException e){
            return null;
        }
    }

    public static VMGuestMetrics toVMGuestMetrics(Object object) {
        try {
            return VMGuestMetrics.getInstFromRef((String) object);
        } catch (NullPointerException e){
            return null;
        }
    }

    public static VMMetrics toVMMetrics(Object object) {
        try {
            return VMMetrics.getInstFromRef((String) object);
        } catch (NullPointerException e){
            return null;
        }
    }

    public static VTPM toVTPM(Object object) {
        try {
            return VTPM.getInstFromRef((String) object);
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Console toConsole(Object object) {
        try {
            return Console.getInstFromRef((String) object);
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Crashdump toCrashdump(Object object) {
        try {
            return Crashdump.getInstFromRef((String) object);
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Host toHost(Object object) {
        try {
            return Host.getInstFromRef((String) object);
        } catch (NullPointerException e){
            return null;
        }
    }

    public static HostCpu toHostCpu(Object object) {
        try {
            return HostCpu.getInstFromRef((String) object);
        } catch (NullPointerException e){
            return null;
        }
    }

    public static HostCrashdump toHostCrashdump(Object object) {
        try {
            return HostCrashdump.getInstFromRef((String) object);
        } catch (NullPointerException e){
            return null;
        }
    }

    public static HostMetrics toHostMetrics(Object object) {
        try {
            return HostMetrics.getInstFromRef((String) object);
        } catch (NullPointerException e){
            return null;
        }
    }

    public static HostPatch toHostPatch(Object object) {
        try {
            return HostPatch.getInstFromRef((String) object);
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Network toNetwork(Object object) {
        try {
            return Network.getInstFromRef((String) object);
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Pool toPool(Object object) {
        try {
            return Pool.getInstFromRef((String) object);
        } catch (NullPointerException e){
            return null;
        }
    }

    public static PoolPatch toPoolPatch(Object object) {
        try {
            return PoolPatch.getInstFromRef((String) object);
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Session toSession(Object object) {
        try {
            return Session.getInstFromRef((String) object);
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Task toTask(Object object) {
        try {
            return Task.getInstFromRef((String) object);
        } catch (NullPointerException e){
            return null;
        }
    }

    public static User toUser(Object object) {
        try {
            return User.getInstFromRef((String) object);
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Bond.Record toBondRecord(Object object) {
        try {
            Map<String,Object> map = (Map<String,Object>) object;
            Bond.Record record = new Bond.Record();
            record.uuid = toString(map.get("uuid"));
            record.master = toPIF(map.get("master"));
            record.slaves = toSetOfPIF(map.get("slaves"));
            record.otherConfig = toMapOfStringString(map.get("other_config"));
            return record;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static PBD.Record toPBDRecord(Object object) {
        try {
            Map<String,Object> map = (Map<String,Object>) object;
            PBD.Record record = new PBD.Record();
            record.uuid = toString(map.get("uuid"));
            record.host = toHost(map.get("host"));
            record.SR = toSR(map.get("SR"));
            record.deviceConfig = toMapOfStringString(map.get("device_config"));
            record.currentlyAttached = toBoolean(map.get("currently_attached"));
            record.otherConfig = toMapOfStringString(map.get("other_config"));
            return record;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static PIF.Record toPIFRecord(Object object) {
        try {
            Map<String,Object> map = (Map<String,Object>) object;
            PIF.Record record = new PIF.Record();
            record.uuid = toString(map.get("uuid"));
            record.device = toString(map.get("device"));
            record.network = toNetwork(map.get("network"));
            record.host = toHost(map.get("host"));
            record.MAC = toString(map.get("MAC"));
            record.MTU = toLong(map.get("MTU"));
            record.VLAN = toLong(map.get("VLAN"));
            record.metrics = toPIFMetrics(map.get("metrics"));
            record.physical = toBoolean(map.get("physical"));
            record.currentlyAttached = toBoolean(map.get("currently_attached"));
            record.ipConfigurationMode = toIpConfigurationMode(map.get("ip_configuration_mode"));
            record.IP = toString(map.get("IP"));
            record.netmask = toString(map.get("netmask"));
            record.gateway = toString(map.get("gateway"));
            record.DNS = toString(map.get("DNS"));
            record.bondSlaveOf = toBond(map.get("bond_slave_of"));
            record.bondMasterOf = toSetOfBond(map.get("bond_master_of"));
            record.VLANMasterOf = toVLAN(map.get("VLAN_master_of"));
            record.VLANSlaveOf = toSetOfVLAN(map.get("VLAN_slave_of"));
            record.management = toBoolean(map.get("management"));
            record.otherConfig = toMapOfStringString(map.get("other_config"));
            return record;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static PIFMetrics.Record toPIFMetricsRecord(Object object) {
        try {
            Map<String,Object> map = (Map<String,Object>) object;
            PIFMetrics.Record record = new PIFMetrics.Record();
            record.uuid = toString(map.get("uuid"));
            record.ioReadKbs = toDouble(map.get("io_read_kbs"));
            record.ioWriteKbs = toDouble(map.get("io_write_kbs"));
            record.carrier = toBoolean(map.get("carrier"));
            record.vendorId = toString(map.get("vendor_id"));
            record.vendorName = toString(map.get("vendor_name"));
            record.deviceId = toString(map.get("device_id"));
            record.deviceName = toString(map.get("device_name"));
            record.speed = toLong(map.get("speed"));
            record.duplex = toBoolean(map.get("duplex"));
            record.pciBusPath = toString(map.get("pci_bus_path"));
            record.lastUpdated = toDate(map.get("last_updated"));
            return record;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static SM.Record toSMRecord(Object object) {
        try {
            Map<String,Object> map = (Map<String,Object>) object;
            SM.Record record = new SM.Record();
            record.uuid = toString(map.get("uuid"));
            record.nameLabel = toString(map.get("name_label"));
            record.nameDescription = toString(map.get("name_description"));
            record.type = toString(map.get("type"));
            record.vendor = toString(map.get("vendor"));
            record.copyright = toString(map.get("copyright"));
            record.version = toString(map.get("version"));
            record.requiredApiVersion = toString(map.get("required_api_version"));
            record.configuration = toMapOfStringString(map.get("configuration"));
            record.capabilities = toSetOfString(map.get("capabilities"));
            record.otherConfig = toMapOfStringString(map.get("other_config"));
            return record;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static SR.Record toSRRecord(Object object) {
        try {
            Map<String,Object> map = (Map<String,Object>) object;
            SR.Record record = new SR.Record();
            record.uuid = toString(map.get("uuid"));
            record.nameLabel = toString(map.get("name_label"));
            record.nameDescription = toString(map.get("name_description"));
            record.allowedOperations = toSetOfStorageOperations(map.get("allowed_operations"));
            record.currentOperations = toMapOfStringStorageOperations(map.get("current_operations"));
            record.VDIs = toSetOfVDI(map.get("VDIs"));
            record.PBDs = toSetOfPBD(map.get("PBDs"));
            record.virtualAllocation = toLong(map.get("virtual_allocation"));
            record.physicalUtilisation = toLong(map.get("physical_utilisation"));
            record.physicalSize = toLong(map.get("physical_size"));
            record.type = toString(map.get("type"));
            record.contentType = toString(map.get("content_type"));
            record.shared = toBoolean(map.get("shared"));
            record.otherConfig = toMapOfStringString(map.get("other_config"));
            record.smConfig = toMapOfStringString(map.get("sm_config"));
            return record;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static VBD.Record toVBDRecord(Object object) {
        try {
            Map<String,Object> map = (Map<String,Object>) object;
            VBD.Record record = new VBD.Record();
            record.uuid = toString(map.get("uuid"));
            record.allowedOperations = toSetOfVbdOperations(map.get("allowed_operations"));
            record.currentOperations = toMapOfStringVbdOperations(map.get("current_operations"));
            record.VM = toVM(map.get("VM"));
            record.VDI = toVDI(map.get("VDI"));
            record.device = toString(map.get("device"));
            record.userdevice = toString(map.get("userdevice"));
            record.bootable = toBoolean(map.get("bootable"));
            record.mode = toVbdMode(map.get("mode"));
            record.type = toVbdType(map.get("type"));
            record.unpluggable = toBoolean(map.get("unpluggable"));
            record.storageLock = toBoolean(map.get("storage_lock"));
            record.empty = toBoolean(map.get("empty"));
            record.otherConfig = toMapOfStringString(map.get("other_config"));
            record.currentlyAttached = toBoolean(map.get("currently_attached"));
            record.statusCode = toLong(map.get("status_code"));
            record.statusDetail = toString(map.get("status_detail"));
            record.runtimeProperties = toMapOfStringString(map.get("runtime_properties"));
            record.qosAlgorithmType = toString(map.get("qos_algorithm_type"));
            record.qosAlgorithmParams = toMapOfStringString(map.get("qos_algorithm_params"));
            record.qosSupportedAlgorithms = toSetOfString(map.get("qos_supported_algorithms"));
            record.metrics = toVBDMetrics(map.get("metrics"));
            return record;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static VBDMetrics.Record toVBDMetricsRecord(Object object) {
        try {
            Map<String,Object> map = (Map<String,Object>) object;
            VBDMetrics.Record record = new VBDMetrics.Record();
            record.uuid = toString(map.get("uuid"));
            record.ioReadKbs = toDouble(map.get("io_read_kbs"));
            record.ioWriteKbs = toDouble(map.get("io_write_kbs"));
            record.lastUpdated = toDate(map.get("last_updated"));
            return record;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static VDI.Record toVDIRecord(Object object) {
        try {
            Map<String,Object> map = (Map<String,Object>) object;
            VDI.Record record = new VDI.Record();
            record.uuid = toString(map.get("uuid"));
            record.nameLabel = toString(map.get("name_label"));
            record.nameDescription = toString(map.get("name_description"));
            record.allowedOperations = toSetOfVdiOperations(map.get("allowed_operations"));
            record.currentOperations = toMapOfStringVdiOperations(map.get("current_operations"));
            record.SR = toSR(map.get("SR"));
            record.VBDs = toSetOfVBD(map.get("VBDs"));
            record.crashDumps = toSetOfCrashdump(map.get("crash_dumps"));
            record.virtualSize = toLong(map.get("virtual_size"));
            record.physicalUtilisation = toLong(map.get("physical_utilisation"));
            record.type = toVdiType(map.get("type"));
            record.sharable = toBoolean(map.get("sharable"));
            record.readOnly = toBoolean(map.get("read_only"));
            record.otherConfig = toMapOfStringString(map.get("other_config"));
            record.storageLock = toBoolean(map.get("storage_lock"));
            record.location = toString(map.get("location"));
            record.managed = toBoolean(map.get("managed"));
            record.missing = toBoolean(map.get("missing"));
            record.parent = toVDI(map.get("parent"));
            record.xenstoreData = toMapOfStringString(map.get("xenstore_data"));
            record.smConfig = toMapOfStringString(map.get("sm_config"));
            return record;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static VIF.Record toVIFRecord(Object object) {
        try {
            Map<String,Object> map = (Map<String,Object>) object;
            VIF.Record record = new VIF.Record();
            record.uuid = toString(map.get("uuid"));
            record.allowedOperations = toSetOfVifOperations(map.get("allowed_operations"));
            record.currentOperations = toMapOfStringVifOperations(map.get("current_operations"));
            record.device = toString(map.get("device"));
            record.network = toNetwork(map.get("network"));
            record.VM = toVM(map.get("VM"));
            record.MAC = toString(map.get("MAC"));
            record.MTU = toLong(map.get("MTU"));
            record.otherConfig = toMapOfStringString(map.get("other_config"));
            record.currentlyAttached = toBoolean(map.get("currently_attached"));
            record.statusCode = toLong(map.get("status_code"));
            record.statusDetail = toString(map.get("status_detail"));
            record.runtimeProperties = toMapOfStringString(map.get("runtime_properties"));
            record.qosAlgorithmType = toString(map.get("qos_algorithm_type"));
            record.qosAlgorithmParams = toMapOfStringString(map.get("qos_algorithm_params"));
            record.qosSupportedAlgorithms = toSetOfString(map.get("qos_supported_algorithms"));
            record.metrics = toVIFMetrics(map.get("metrics"));
            return record;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static VIFMetrics.Record toVIFMetricsRecord(Object object) {
        try {
            Map<String,Object> map = (Map<String,Object>) object;
            VIFMetrics.Record record = new VIFMetrics.Record();
            record.uuid = toString(map.get("uuid"));
            record.ioReadKbs = toDouble(map.get("io_read_kbs"));
            record.ioWriteKbs = toDouble(map.get("io_write_kbs"));
            record.lastUpdated = toDate(map.get("last_updated"));
            return record;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static VLAN.Record toVLANRecord(Object object) {
        try {
            Map<String,Object> map = (Map<String,Object>) object;
            VLAN.Record record = new VLAN.Record();
            record.uuid = toString(map.get("uuid"));
            record.taggedPIF = toPIF(map.get("tagged_PIF"));
            record.untaggedPIF = toPIF(map.get("untagged_PIF"));
            record.tag = toLong(map.get("tag"));
            record.otherConfig = toMapOfStringString(map.get("other_config"));
            return record;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static VM.Record toVMRecord(Object object) {
        try {
            Map<String,Object> map = (Map<String,Object>) object;
            VM.Record record = new VM.Record();
            record.uuid = toString(map.get("uuid"));
            record.allowedOperations = toSetOfVmOperations(map.get("allowed_operations"));
            record.currentOperations = toMapOfStringVmOperations(map.get("current_operations"));
            record.powerState = toVmPowerState(map.get("power_state"));
            record.nameLabel = toString(map.get("name_label"));
            record.nameDescription = toString(map.get("name_description"));
            record.userVersion = toLong(map.get("user_version"));
            record.isATemplate = toBoolean(map.get("is_a_template"));
            record.suspendVDI = toVDI(map.get("suspend_VDI"));
            record.residentOn = toHost(map.get("resident_on"));
            record.affinity = toHost(map.get("affinity"));
            record.memoryStaticMax = toLong(map.get("memory_static_max"));
            record.memoryDynamicMax = toLong(map.get("memory_dynamic_max"));
            record.memoryDynamicMin = toLong(map.get("memory_dynamic_min"));
            record.memoryStaticMin = toLong(map.get("memory_static_min"));
            record.VCPUsParams = toMapOfStringString(map.get("VCPUs_params"));
            record.VCPUsMax = toLong(map.get("VCPUs_max"));
            record.VCPUsAtStartup = toLong(map.get("VCPUs_at_startup"));
            record.actionsAfterShutdown = toOnNormalExit(map.get("actions_after_shutdown"));
            record.actionsAfterReboot = toOnNormalExit(map.get("actions_after_reboot"));
            record.actionsAfterCrash = toOnCrashBehaviour(map.get("actions_after_crash"));
            record.consoles = toSetOfConsole(map.get("consoles"));
            record.VIFs = toSetOfVIF(map.get("VIFs"));
            record.VBDs = toSetOfVBD(map.get("VBDs"));
            record.crashDumps = toSetOfCrashdump(map.get("crash_dumps"));
            record.VTPMs = toSetOfVTPM(map.get("VTPMs"));
            record.PVBootloader = toString(map.get("PV_bootloader"));
            record.PVKernel = toString(map.get("PV_kernel"));
            record.PVRamdisk = toString(map.get("PV_ramdisk"));
            record.PVArgs = toString(map.get("PV_args"));
            record.PVBootloaderArgs = toString(map.get("PV_bootloader_args"));
            record.PVLegacyArgs = toString(map.get("PV_legacy_args"));
            record.HVMBootPolicy = toString(map.get("HVM_boot_policy"));
            record.HVMBootParams = toMapOfStringString(map.get("HVM_boot_params"));
            record.HVMShadowMultiplier = toDouble(map.get("HVM_shadow_multiplier"));
            record.platform = toMapOfStringString(map.get("platform"));
            record.PCIBus = toString(map.get("PCI_bus"));
            record.otherConfig = toMapOfStringString(map.get("other_config"));
            record.domid = toLong(map.get("domid"));
            record.domarch = toString(map.get("domarch"));
            record.lastBootCPUFlags = toMapOfStringString(map.get("last_boot_CPU_flags"));
            record.isControlDomain = toBoolean(map.get("is_control_domain"));
            record.metrics = toVMMetrics(map.get("metrics"));
            record.guestMetrics = toVMGuestMetrics(map.get("guest_metrics"));
            record.lastBootedRecord = toString(map.get("last_booted_record"));
            record.recommendations = toString(map.get("recommendations"));
            record.xenstoreData = toMapOfStringString(map.get("xenstore_data"));
            return record;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static VMGuestMetrics.Record toVMGuestMetricsRecord(Object object) {
        try {
            Map<String,Object> map = (Map<String,Object>) object;
            VMGuestMetrics.Record record = new VMGuestMetrics.Record();
            record.uuid = toString(map.get("uuid"));
            record.osVersion = toMapOfStringString(map.get("os_version"));
            record.PVDriversVersion = toMapOfStringString(map.get("PV_drivers_version"));
            record.PVDriversUpToDate = toBoolean(map.get("PV_drivers_up_to_date"));
            record.memory = toMapOfStringString(map.get("memory"));
            record.disks = toMapOfStringString(map.get("disks"));
            record.networks = toMapOfStringString(map.get("networks"));
            record.other = toMapOfStringString(map.get("other"));
            record.lastUpdated = toDate(map.get("last_updated"));
            return record;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static VMMetrics.Record toVMMetricsRecord(Object object) {
        try {
            Map<String,Object> map = (Map<String,Object>) object;
            VMMetrics.Record record = new VMMetrics.Record();
            record.uuid = toString(map.get("uuid"));
            record.memoryActual = toLong(map.get("memory_actual"));
            record.VCPUsNumber = toLong(map.get("VCPUs_number"));
            record.VCPUsUtilisation = toMapOfLongDouble(map.get("VCPUs_utilisation"));
            record.VCPUsCPU = toMapOfLongLong(map.get("VCPUs_CPU"));
            record.VCPUsParams = toMapOfStringString(map.get("VCPUs_params"));
            record.VCPUsFlags = toMapOfLongSetOfString(map.get("VCPUs_flags"));
            record.state = toSetOfString(map.get("state"));
            record.startTime = toDate(map.get("start_time"));
            record.installTime = toDate(map.get("install_time"));
            record.lastUpdated = toDate(map.get("last_updated"));
            return record;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static VTPM.Record toVTPMRecord(Object object) {
        try {
            Map<String,Object> map = (Map<String,Object>) object;
            VTPM.Record record = new VTPM.Record();
            record.uuid = toString(map.get("uuid"));
            record.VM = toVM(map.get("VM"));
            record.backend = toVM(map.get("backend"));
            return record;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Console.Record toConsoleRecord(Object object) {
        try {
            Map<String,Object> map = (Map<String,Object>) object;
            Console.Record record = new Console.Record();
            record.uuid = toString(map.get("uuid"));
            record.protocol = toConsoleProtocol(map.get("protocol"));
            record.location = toString(map.get("location"));
            record.VM = toVM(map.get("VM"));
            record.otherConfig = toMapOfStringString(map.get("other_config"));
            return record;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Crashdump.Record toCrashdumpRecord(Object object) {
        try {
            Map<String,Object> map = (Map<String,Object>) object;
            Crashdump.Record record = new Crashdump.Record();
            record.uuid = toString(map.get("uuid"));
            record.VM = toVM(map.get("VM"));
            record.VDI = toVDI(map.get("VDI"));
            record.otherConfig = toMapOfStringString(map.get("other_config"));
            return record;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Event.Record toEventRecord(Object object) {
        try {
            Map<String,Object> map = (Map<String,Object>) object;
            Event.Record record = new Event.Record();
            record.id = toLong(map.get("id"));
            record.timestamp = toDate(map.get("timestamp"));
            record.clazz = toString(map.get("class"));
            record.operation = toEventOperation(map.get("operation"));
            record.ref = toString(map.get("ref"));
            record.objUuid = toString(map.get("obj_uuid"));


        Object a,b;
        a=map.get("snapshot");
        switch(toXenAPIObjects(record.clazz))
        {
                case           SESSION: b =           toSessionRecord(a); break;
                case              TASK: b =              toTaskRecord(a); break;
                case             EVENT: b =             toEventRecord(a); break;
                case              POOL: b =              toPoolRecord(a); break;
                case        POOL_PATCH: b =         toPoolPatchRecord(a); break;
                case                VM: b =                toVMRecord(a); break;
                case        VM_METRICS: b =         toVMMetricsRecord(a); break;
                case  VM_GUEST_METRICS: b =    toVMGuestMetricsRecord(a); break;
                case              HOST: b =              toHostRecord(a); break;
                case    HOST_CRASHDUMP: b =     toHostCrashdumpRecord(a); break;
                case        HOST_PATCH: b =         toHostPatchRecord(a); break;
                case      HOST_METRICS: b =       toHostMetricsRecord(a); break;
                case          HOST_CPU: b =           toHostCpuRecord(a); break;
                case           NETWORK: b =           toNetworkRecord(a); break;
                case               VIF: b =               toVIFRecord(a); break;
                case       VIF_METRICS: b =        toVIFMetricsRecord(a); break;
                case               PIF: b =               toPIFRecord(a); break;
                case       PIF_METRICS: b =        toPIFMetricsRecord(a); break;
                case              BOND: b =              toBondRecord(a); break;
                case              VLAN: b =              toVLANRecord(a); break;
                case                SM: b =                toSMRecord(a); break;
                case                SR: b =                toSRRecord(a); break;
                case               VDI: b =               toVDIRecord(a); break;
                case               VBD: b =               toVBDRecord(a); break;
                case       VBD_METRICS: b =        toVBDMetricsRecord(a); break;
                case               PBD: b =               toPBDRecord(a); break;
                case         CRASHDUMP: b =         toCrashdumpRecord(a); break;
                case              VTPM: b =              toVTPMRecord(a); break;
                case           CONSOLE: b =           toConsoleRecord(a); break;
                case              USER: b =              toUserRecord(a); break;
                default: throw new RuntimeException("Internal error in auto-generated code whilst unmarshalling event snapshot");
        }
        record.snapshot = b;
            return record;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Host.Record toHostRecord(Object object) {
        try {
            Map<String,Object> map = (Map<String,Object>) object;
            Host.Record record = new Host.Record();
            record.uuid = toString(map.get("uuid"));
            record.nameLabel = toString(map.get("name_label"));
            record.nameDescription = toString(map.get("name_description"));
            record.allowedOperations = toSetOfHostAllowedOperations(map.get("allowed_operations"));
            record.currentOperations = toMapOfStringHostAllowedOperations(map.get("current_operations"));
            record.APIVersionMajor = toLong(map.get("API_version_major"));
            record.APIVersionMinor = toLong(map.get("API_version_minor"));
            record.APIVersionVendor = toString(map.get("API_version_vendor"));
            record.APIVersionVendorImplementation = toMapOfStringString(map.get("API_version_vendor_implementation"));
            record.enabled = toBoolean(map.get("enabled"));
            record.softwareVersion = toMapOfStringString(map.get("software_version"));
            record.otherConfig = toMapOfStringString(map.get("other_config"));
            record.capabilities = toSetOfString(map.get("capabilities"));
            record.cpuConfiguration = toMapOfStringString(map.get("cpu_configuration"));
            record.schedPolicy = toString(map.get("sched_policy"));
            record.supportedBootloaders = toSetOfString(map.get("supported_bootloaders"));
            record.residentVMs = toSetOfVM(map.get("resident_VMs"));
            record.logging = toMapOfStringString(map.get("logging"));
            record.PIFs = toSetOfPIF(map.get("PIFs"));
            record.suspendImageSr = toSR(map.get("suspend_image_sr"));
            record.crashDumpSr = toSR(map.get("crash_dump_sr"));
            record.crashdumps = toSetOfHostCrashdump(map.get("crashdumps"));
            record.patches = toSetOfHostPatch(map.get("patches"));
            record.PBDs = toSetOfPBD(map.get("PBDs"));
            record.hostCPUs = toSetOfHostCpu(map.get("host_CPUs"));
            record.hostname = toString(map.get("hostname"));
            record.address = toString(map.get("address"));
            record.metrics = toHostMetrics(map.get("metrics"));
            record.licenseParams = toMapOfStringString(map.get("license_params"));
            return record;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static HostCpu.Record toHostCpuRecord(Object object) {
        try {
            Map<String,Object> map = (Map<String,Object>) object;
            HostCpu.Record record = new HostCpu.Record();
            record.uuid = toString(map.get("uuid"));
            record.host = toHost(map.get("host"));
            record.number = toLong(map.get("number"));
            record.vendor = toString(map.get("vendor"));
            record.speed = toLong(map.get("speed"));
            record.modelname = toString(map.get("modelname"));
            record.family = toLong(map.get("family"));
            record.model = toLong(map.get("model"));
            record.stepping = toString(map.get("stepping"));
            record.flags = toString(map.get("flags"));
            record.features = toString(map.get("features"));
            record.utilisation = toDouble(map.get("utilisation"));
            return record;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static HostCrashdump.Record toHostCrashdumpRecord(Object object) {
        try {
            Map<String,Object> map = (Map<String,Object>) object;
            HostCrashdump.Record record = new HostCrashdump.Record();
            record.uuid = toString(map.get("uuid"));
            record.host = toHost(map.get("host"));
            record.timestamp = toDate(map.get("timestamp"));
            record.size = toLong(map.get("size"));
            record.otherConfig = toMapOfStringString(map.get("other_config"));
            return record;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static HostMetrics.Record toHostMetricsRecord(Object object) {
        try {
            Map<String,Object> map = (Map<String,Object>) object;
            HostMetrics.Record record = new HostMetrics.Record();
            record.uuid = toString(map.get("uuid"));
            record.memoryTotal = toLong(map.get("memory_total"));
            record.memoryFree = toLong(map.get("memory_free"));
            record.live = toBoolean(map.get("live"));
            record.lastUpdated = toDate(map.get("last_updated"));
            return record;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static HostPatch.Record toHostPatchRecord(Object object) {
        try {
            Map<String,Object> map = (Map<String,Object>) object;
            HostPatch.Record record = new HostPatch.Record();
            record.uuid = toString(map.get("uuid"));
            record.nameLabel = toString(map.get("name_label"));
            record.nameDescription = toString(map.get("name_description"));
            record.version = toString(map.get("version"));
            record.host = toHost(map.get("host"));
            record.applied = toBoolean(map.get("applied"));
            record.timestampApplied = toDate(map.get("timestamp_applied"));
            record.size = toLong(map.get("size"));
            record.poolPatch = toPoolPatch(map.get("pool_patch"));
            record.otherConfig = toMapOfStringString(map.get("other_config"));
            return record;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Network.Record toNetworkRecord(Object object) {
        try {
            Map<String,Object> map = (Map<String,Object>) object;
            Network.Record record = new Network.Record();
            record.uuid = toString(map.get("uuid"));
            record.nameLabel = toString(map.get("name_label"));
            record.nameDescription = toString(map.get("name_description"));
            record.allowedOperations = toSetOfNetworkOperations(map.get("allowed_operations"));
            record.currentOperations = toMapOfStringNetworkOperations(map.get("current_operations"));
            record.VIFs = toSetOfVIF(map.get("VIFs"));
            record.PIFs = toSetOfPIF(map.get("PIFs"));
            record.otherConfig = toMapOfStringString(map.get("other_config"));
            record.bridge = toString(map.get("bridge"));
            return record;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Pool.Record toPoolRecord(Object object) {
        try {
            Map<String,Object> map = (Map<String,Object>) object;
            Pool.Record record = new Pool.Record();
            record.uuid = toString(map.get("uuid"));
            record.nameLabel = toString(map.get("name_label"));
            record.nameDescription = toString(map.get("name_description"));
            record.master = toHost(map.get("master"));
            record.defaultSR = toSR(map.get("default_SR"));
            record.suspendImageSR = toSR(map.get("suspend_image_SR"));
            record.crashDumpSR = toSR(map.get("crash_dump_SR"));
            record.otherConfig = toMapOfStringString(map.get("other_config"));
            return record;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static PoolPatch.Record toPoolPatchRecord(Object object) {
        try {
            Map<String,Object> map = (Map<String,Object>) object;
            PoolPatch.Record record = new PoolPatch.Record();
            record.uuid = toString(map.get("uuid"));
            record.nameLabel = toString(map.get("name_label"));
            record.nameDescription = toString(map.get("name_description"));
            record.version = toString(map.get("version"));
            record.size = toLong(map.get("size"));
            record.poolApplied = toBoolean(map.get("pool_applied"));
            record.hostPatches = toSetOfHostPatch(map.get("host_patches"));
            record.afterApplyGuidance = toSetOfAfterApplyGuidance(map.get("after_apply_guidance"));
            record.otherConfig = toMapOfStringString(map.get("other_config"));
            return record;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Session.Record toSessionRecord(Object object) {
        try {
            Map<String,Object> map = (Map<String,Object>) object;
            Session.Record record = new Session.Record();
            record.uuid = toString(map.get("uuid"));
            record.thisHost = toHost(map.get("this_host"));
            record.thisUser = toUser(map.get("this_user"));
            record.lastActive = toDate(map.get("last_active"));
            record.pool = toBoolean(map.get("pool"));
            record.otherConfig = toMapOfStringString(map.get("other_config"));
            return record;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static Task.Record toTaskRecord(Object object) {
        try {
            Map<String,Object> map = (Map<String,Object>) object;
            Task.Record record = new Task.Record();
            record.uuid = toString(map.get("uuid"));
            record.nameLabel = toString(map.get("name_label"));
            record.nameDescription = toString(map.get("name_description"));
            record.allowedOperations = toSetOfTaskAllowedOperations(map.get("allowed_operations"));
            record.currentOperations = toMapOfStringTaskAllowedOperations(map.get("current_operations"));
            record.created = toDate(map.get("created"));
            record.finished = toDate(map.get("finished"));
            record.status = toTaskStatusType(map.get("status"));
            record.residentOn = toHost(map.get("resident_on"));
            record.progress = toDouble(map.get("progress"));
            record.type = toString(map.get("type"));
            record.result = toString(map.get("result"));
            record.errorInfo = toSetOfString(map.get("error_info"));
            record.otherConfig = toMapOfStringString(map.get("other_config"));
            return record;
        } catch (NullPointerException e){
            return null;
        }
    }

    public static User.Record toUserRecord(Object object) {
        try {
            Map<String,Object> map = (Map<String,Object>) object;
            User.Record record = new User.Record();
            record.uuid = toString(map.get("uuid"));
            record.shortName = toString(map.get("short_name"));
            record.fullname = toString(map.get("fullname"));
            return record;
        } catch (NullPointerException e){
            return null;
        }
    }


   public static Bond toBond(Task task, Connection connection) throws BadServerResponse, org.apache.xmlrpc.XmlRpcException, BadAsyncResult{
               return Types.toBond(parseResult(task.getResult(connection)));
    }

   public static PBD toPBD(Task task, Connection connection) throws BadServerResponse, org.apache.xmlrpc.XmlRpcException, BadAsyncResult{
               return Types.toPBD(parseResult(task.getResult(connection)));
    }

   public static PIF toPIF(Task task, Connection connection) throws BadServerResponse, org.apache.xmlrpc.XmlRpcException, BadAsyncResult{
               return Types.toPIF(parseResult(task.getResult(connection)));
    }

   public static PIFMetrics toPIFMetrics(Task task, Connection connection) throws BadServerResponse, org.apache.xmlrpc.XmlRpcException, BadAsyncResult{
               return Types.toPIFMetrics(parseResult(task.getResult(connection)));
    }

   public static SM toSM(Task task, Connection connection) throws BadServerResponse, org.apache.xmlrpc.XmlRpcException, BadAsyncResult{
               return Types.toSM(parseResult(task.getResult(connection)));
    }

   public static SR toSR(Task task, Connection connection) throws BadServerResponse, org.apache.xmlrpc.XmlRpcException, BadAsyncResult{
               return Types.toSR(parseResult(task.getResult(connection)));
    }

   public static VBD toVBD(Task task, Connection connection) throws BadServerResponse, org.apache.xmlrpc.XmlRpcException, BadAsyncResult{
               return Types.toVBD(parseResult(task.getResult(connection)));
    }

   public static VBDMetrics toVBDMetrics(Task task, Connection connection) throws BadServerResponse, org.apache.xmlrpc.XmlRpcException, BadAsyncResult{
               return Types.toVBDMetrics(parseResult(task.getResult(connection)));
    }

   public static VDI toVDI(Task task, Connection connection) throws BadServerResponse, org.apache.xmlrpc.XmlRpcException, BadAsyncResult{
               return Types.toVDI(parseResult(task.getResult(connection)));
    }

   public static VIF toVIF(Task task, Connection connection) throws BadServerResponse, org.apache.xmlrpc.XmlRpcException, BadAsyncResult{
               return Types.toVIF(parseResult(task.getResult(connection)));
    }

   public static VIFMetrics toVIFMetrics(Task task, Connection connection) throws BadServerResponse, org.apache.xmlrpc.XmlRpcException, BadAsyncResult{
               return Types.toVIFMetrics(parseResult(task.getResult(connection)));
    }

   public static VLAN toVLAN(Task task, Connection connection) throws BadServerResponse, org.apache.xmlrpc.XmlRpcException, BadAsyncResult{
               return Types.toVLAN(parseResult(task.getResult(connection)));
    }

   public static VM toVM(Task task, Connection connection) throws BadServerResponse, org.apache.xmlrpc.XmlRpcException, BadAsyncResult{
               return Types.toVM(parseResult(task.getResult(connection)));
    }

   public static VMGuestMetrics toVMGuestMetrics(Task task, Connection connection) throws BadServerResponse, org.apache.xmlrpc.XmlRpcException, BadAsyncResult{
               return Types.toVMGuestMetrics(parseResult(task.getResult(connection)));
    }

   public static VMMetrics toVMMetrics(Task task, Connection connection) throws BadServerResponse, org.apache.xmlrpc.XmlRpcException, BadAsyncResult{
               return Types.toVMMetrics(parseResult(task.getResult(connection)));
    }

   public static VTPM toVTPM(Task task, Connection connection) throws BadServerResponse, org.apache.xmlrpc.XmlRpcException, BadAsyncResult{
               return Types.toVTPM(parseResult(task.getResult(connection)));
    }

   public static Console toConsole(Task task, Connection connection) throws BadServerResponse, org.apache.xmlrpc.XmlRpcException, BadAsyncResult{
               return Types.toConsole(parseResult(task.getResult(connection)));
    }

   public static Crashdump toCrashdump(Task task, Connection connection) throws BadServerResponse, org.apache.xmlrpc.XmlRpcException, BadAsyncResult{
               return Types.toCrashdump(parseResult(task.getResult(connection)));
    }

   public static Host toHost(Task task, Connection connection) throws BadServerResponse, org.apache.xmlrpc.XmlRpcException, BadAsyncResult{
               return Types.toHost(parseResult(task.getResult(connection)));
    }

   public static HostCpu toHostCpu(Task task, Connection connection) throws BadServerResponse, org.apache.xmlrpc.XmlRpcException, BadAsyncResult{
               return Types.toHostCpu(parseResult(task.getResult(connection)));
    }

   public static HostCrashdump toHostCrashdump(Task task, Connection connection) throws BadServerResponse, org.apache.xmlrpc.XmlRpcException, BadAsyncResult{
               return Types.toHostCrashdump(parseResult(task.getResult(connection)));
    }

   public static HostMetrics toHostMetrics(Task task, Connection connection) throws BadServerResponse, org.apache.xmlrpc.XmlRpcException, BadAsyncResult{
               return Types.toHostMetrics(parseResult(task.getResult(connection)));
    }

   public static HostPatch toHostPatch(Task task, Connection connection) throws BadServerResponse, org.apache.xmlrpc.XmlRpcException, BadAsyncResult{
               return Types.toHostPatch(parseResult(task.getResult(connection)));
    }

   public static Network toNetwork(Task task, Connection connection) throws BadServerResponse, org.apache.xmlrpc.XmlRpcException, BadAsyncResult{
               return Types.toNetwork(parseResult(task.getResult(connection)));
    }

   public static Pool toPool(Task task, Connection connection) throws BadServerResponse, org.apache.xmlrpc.XmlRpcException, BadAsyncResult{
               return Types.toPool(parseResult(task.getResult(connection)));
    }

   public static PoolPatch toPoolPatch(Task task, Connection connection) throws BadServerResponse, org.apache.xmlrpc.XmlRpcException, BadAsyncResult{
               return Types.toPoolPatch(parseResult(task.getResult(connection)));
    }

   public static Session toSession(Task task, Connection connection) throws BadServerResponse, org.apache.xmlrpc.XmlRpcException, BadAsyncResult{
               return Types.toSession(parseResult(task.getResult(connection)));
    }

   public static Task toTask(Task task, Connection connection) throws BadServerResponse, org.apache.xmlrpc.XmlRpcException, BadAsyncResult{
               return Types.toTask(parseResult(task.getResult(connection)));
    }

   public static User toUser(Task task, Connection connection) throws BadServerResponse, org.apache.xmlrpc.XmlRpcException, BadAsyncResult{
               return Types.toUser(parseResult(task.getResult(connection)));
    }

}
