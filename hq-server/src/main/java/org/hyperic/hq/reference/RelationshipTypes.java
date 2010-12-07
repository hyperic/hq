package org.hyperic.hq.reference;

abstract public class RelationshipTypes {

    // TODO this is used for parent/child and ResourceGroup membership. Probably
    // OK
    public static final String CONTAINS = "CONTAINS";

    // This is used for Resource->ResourceType
    // ex "VM1.eng.vmware.com IS_A VirtualMachine"
    public static final String IS_A = "IS_A";

    // This is used for ResourceType->ResourceType
    // ex. "vApp EXTENDS ResourcePool
    public static final String EXTENDS = "EXTENDS";

    public static final String PLATFORM = "PLATFORM";

    public static final String SERVER = "SERVER";

    public static final String SERVICE = "SERVICE";

    public static final String PLATFORM_TYPE = "PLATFORM_TYPE";

    public static final String SERVER_TYPE = "SERVER_TYPE";

    public static final String SERVICE_TYPE = "SERVICE_TYPE";
    
    public static final String IP="IP";

}
