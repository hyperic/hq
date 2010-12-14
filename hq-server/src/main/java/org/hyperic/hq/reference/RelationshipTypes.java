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
    
    public static final String DEFINED_BY ="DEFINED_BY";
    
    public static final String HAS_OPERATION_TYPE ="HAS_OPERATION_TYPE";
    
    public static final String HAS_PROPERTY_TYPE ="HAS_PROPERTY_TYPE";
    
    public static final String OWNS ="OWNS";
    
    public static final String HAS_MEMBER="HAS_MEMBER";
    
    public static final String HAS_CONFIG="HAS_CONFIG";
    
    public static final String HAS_CONFIG_TYPE="HAS_CONFIG_TYPE";
    
    public static final String HAS_ROLE="HAS_ROLE";
    
    //The relationship types below will eventually be removed

    public static final String PLATFORM = "PLATFORM";

    public static final String SERVER = "SERVER";

    public static final String SERVICE = "SERVICE";

    public static final String PLATFORM_TYPE = "PLATFORM_TYPE";

    public static final String SERVER_TYPE = "SERVER_TYPE";

    public static final String SERVICE_TYPE = "SERVICE_TYPE";
    
    public static final String IP="IP";

}
