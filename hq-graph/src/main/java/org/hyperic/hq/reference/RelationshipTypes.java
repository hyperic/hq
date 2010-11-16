package org.hyperic.hq.reference;


abstract public class RelationshipTypes  {
    
    // TODO this is used for parent/child and ResourceGroup membership. Probably
    // OK
    public static final String CONTAINS="CONTAINS";
    
    // This is used for Resource->ResourceType and ResourceType->ResourceType
    // ex "VM1.eng.vmware.com IS_A VirtualMachine", "vApp IS_A ResourcePool"
    public static final String IS_A="IS_A";
   
}
