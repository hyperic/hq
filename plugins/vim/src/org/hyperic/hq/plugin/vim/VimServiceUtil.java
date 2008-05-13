/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2008], Hyperic, Inc.
 * This file is part of HQ.
 *
 * HQ is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

package org.hyperic.hq.plugin.vim;

import java.lang.reflect.Method;
import java.util.*;
import com.vmware.vim.*;

//derived from SDK/samples/Axis/java/com/vmware/apputils/vim/ServiceUtil.java
/**
 * Utility wrapper methods for the vimService methods
 */
public class VimServiceUtil {

    private VimServiceConnection _connection;

    public VimServiceUtil(VimServiceConnection connection) {
        _connection = connection;
    }

    private static String[] meTree = {
        "ManagedEntity",
        "ComputeResource",
        "ClusterComputeResource",
        "Datacenter",
        "Folder",
        "HostSystem",
        "ResourcePool",
        "VirtualMachine"
    };

    private static String[] crTree = {
        "ComputeResource",
        "ClusterComputeResource"
    };

    private static String[] hcTree = {
        "HistoryCollector",
        "EventHistoryCollector",
        "TaskHistoryCollector"
    };

    private boolean typeIsA(String searchType, String foundType) {
        if (searchType.equals(foundType)) {
            return true;
        }
        else if (searchType.equals("ManagedEntity")) {
            for (int i=0; i<meTree.length; i++) {
                if (meTree[i].equals(foundType)) {
                    return true;
                }
            }
        }
        else if (searchType.equals("ComputeResource")) {
            for (int i=0; i<crTree.length; i++) {
                if (crTree[i].equals(foundType)) {
                    return true;
                }
            }
        }
        else if (searchType.equals("HistoryCollector")) {
            for (int i=0; i<hcTree.length; i++) {
                if (hcTree[i].equals(foundType)) {
                    return true;
                }
            }
        }
        return false;
    }
    /**
     * Get the ManagedObjectReference for an item under the
     * specified root folder that has the type and name specified.
     *
     * @param root a root folder if available, or null for default
     * @param type type of the managed object
     * @param name name to match
     *
     * @return First ManagedObjectReference of the type / name pair found
     */
    public ManagedObjectReference getDecendentMoRef(ManagedObjectReference root,
                                                    String type, String name)
        throws Exception {
        if (name == null || name.length() == 0) {
            return null;
        }

        String[][] typeinfo = new String[][] {
            new String[] { type,  "name" }
        };

        ObjectContent[] ocary =
            getContentsRecursively(null, root, typeinfo, true);

        if (ocary == null || ocary.length == 0) {
            return null;
        }

        ObjectContent oc = null;
        ManagedObjectReference mor = null;
        DynamicProperty[] propary = null;
        String propval = null;
        boolean found = false;
        for (int oci = 0; oci < ocary.length && !found; oci++) {
            oc = ocary[oci];
            mor = oc.getObj();
            propary = oc.getPropSet();

            propval = null;
            if (type == null || typeIsA(type, mor.getType())) {
                if (propary.length > 0) {
                    propval = (String)propary[0].getVal();
                }
                found = propval != null && name.equals(propval);
            }
        }
        if (!found) {
            mor = null;
        }
        return mor;
    }

    /**
     * Get the first ManagedObjectReference from a root of
     * the specified type
     *
     * @param root a root folder if available, or null for default
     * @param type the type of the entity - e.g. VirtualMachine
     * @return managed object reference available
     */
    public ManagedObjectReference getFirstDecendentMoRef(ManagedObjectReference root,
                                                         String type)
        throws Exception {
        ArrayList morlist = getDecendentMoRefs(root, type);
        ManagedObjectReference mor = null;
        if (morlist.size() > 0) {
            mor = (ManagedObjectReference)morlist.get(0);
        }
        return mor;
    }

    /**
     * Retrieve all the ManagedObjectReferences of the type specified.
     *
     * @param root a root folder if available, or null for default
     * @param type type of container refs to retrieve
     *
     * @return List of MORefs
     */
    public ArrayList getDecendentMoRefs(ManagedObjectReference root,
                                        String type)
        throws Exception {
        ArrayList mors = getDecendentMoRefs(root,type,null);
        return mors;
    }

    public ArrayList getDecendentMoRefs(ManagedObjectReference root,
                                        String type,
                                        String [][] filter)
        throws Exception {
        String[][] typeinfo = new String[][] {
            new String[] { type, "name" }
        };
        ObjectContent[] ocary =
            getContentsRecursively(null, root, typeinfo, true);
        ArrayList refs = new ArrayList();
        if (ocary == null || ocary.length == 0) {
            return refs;
        }
        for (int oci = 0; oci < ocary.length; oci++) {
            refs.add(ocary[oci].getObj());
        }
        if (filter != null) {
            ArrayList filtermors = filterMOR(refs, filter);
            return filtermors;
        }
        else {
            return refs;
        }
    }

    private ArrayList filterMOR(ArrayList mors, String [][] filter)
        throws Exception {

        ArrayList filteredmors = new ArrayList();
        for (int i=0; i<mors.size(); i++){
            boolean flag = true;

            for (int k=0; k<filter.length; k++) {
                String prop = filter[k][0];
                String reqVal = filter[k][1];
                String value = getProp(((ManagedObjectReference)mors.get(i)),prop);
                if (reqVal == null) {
                    continue;
                }
                if (value == null && reqVal == null) {
                    continue;
                }
                if (value == null && reqVal != null) {
                    flag = false;
                    k = filter.length+1;
                }
                else if (value.equalsIgnoreCase(reqVal)) {
                }
                else {
                    flag = false;
                    k = filter.length+1;
                }
            }
            if (flag) {
                filteredmors.add(mors.get(i));
            }
        }
        return filteredmors;
    }

    private String getProp(ManagedObjectReference obj,String prop) {
        String propVal = null;
        try {
            propVal = (String)getDynamicProperty(obj,prop);
        }
        catch(Exception e) {}
        return propVal;
    }

    /**
     * Retrieve Container contents for all containers recursively from root
     *
     * @return retrieved object contents
     */
    public ObjectContent[] getAllContainerContents() throws Exception {
        ObjectContent[] ocary =
            getContentsRecursively(null, true);
        return ocary;
    }

    /**
     * Retrieve container contents from specified root recursively if requested.
     *
     * @param root a root folder if available, or null for default
     * @param recurse retrieve contents recursively from the root down
     *
     * @return retrieved object contents
     */
    public ObjectContent[] getContentsRecursively(ManagedObjectReference root,
                                                  boolean recurse)
        throws Exception {
        String[][] typeinfo = new String[][] {
            new String[] { "ManagedEntity" }
        };
        ObjectContent[] ocary =
            getContentsRecursively(null, root, typeinfo, recurse);
        return ocary;
    }

    /**
     * Retrieve content recursively with multiple properties.
     * the typeinfo array contains typename + properties to retrieve.
     *
     * @param collector a property collector if available or null for default
     * @param root a root folder if available, or null for default
     * @param typeinfo 2D array of properties for each typename
     * @param recurse retrieve contents recursively from the root down
     *
     * @return retrieved object contents
     */
    public ObjectContent[] getContentsRecursively(ManagedObjectReference collector,
                                                  ManagedObjectReference root,
                                                  String[][] typeinfo,
                                                  boolean recurse)
        throws Exception {
        if (typeinfo == null || typeinfo.length == 0) {
            return null;
        }
        ManagedObjectReference usecoll = collector;
        if (usecoll == null) {
            usecoll = _connection.getPropCol();
        }
        ManagedObjectReference useroot = root;
        if (useroot == null) {
            useroot = _connection.getRootFolder();
        }
        SelectionSpec[] selectionSpecs = null;
        if (recurse) {
            selectionSpecs = buildFullTraversal();
        }
        PropertySpec[] propspecary = buildPropertySpecArray(typeinfo);
        PropertyFilterSpec spec =
            new PropertyFilterSpec(null,null,
                                   propspecary,
                                   new ObjectSpec[] {
                                       new ObjectSpec(null,null,
                                                      useroot,
                                                      Boolean.FALSE,
                                                      selectionSpecs
                                                      )
                                   }
                                   );
        ObjectContent[] retoc =
            _connection.getService().retrieveProperties(usecoll,
                                                        new PropertyFilterSpec[] { spec });
        return retoc;
    }

    /**
     * Get a MORef from the property returned.
     *
     * @param objMor Object to get a reference property from
     * @param propName name of the property that is the MORef
     * @return the ManagedObjectReference for that property.
     */
    public ManagedObjectReference getMoRefProp(ManagedObjectReference objMor,
                                               String propName)
        throws Exception {
        Object props = getDynamicProperty(objMor, propName);
        ManagedObjectReference propmor = null;
        if (!props.getClass().isArray()) {
            propmor = (ManagedObjectReference)props;
        }
        return propmor;
    }

    /**
     * Retrieve contents for a single object based on the property collector
     * registered with the service.
     *
     * @param collector Property collector registered with service
     * @param mobj Managed Object Reference to get contents for
     * @param properties names of properties of object to retrieve
     *
     * @return retrieved object contents
     */
    public ObjectContent[] getObjectProperties(ManagedObjectReference collector,
                                               ManagedObjectReference mobj,
                                               String[] properties)
        throws Exception {
        if (mobj == null) {
            return null;
        }
        ManagedObjectReference usecoll = collector;
        if (usecoll == null) {
            usecoll = _connection.getPropCol();
        }
        PropertyFilterSpec spec = new PropertyFilterSpec();
        spec.setPropSet(new PropertySpec[] { new PropertySpec() });
        spec.getPropSet(0).setAll(new Boolean (properties == null || properties.length == 0));
        spec.getPropSet(0).setType(mobj.getType());
        spec.getPropSet(0).setPathSet(properties);
        spec.setObjectSet(new ObjectSpec[] { new ObjectSpec() });
        spec.getObjectSet(0).setObj(mobj);
        spec.getObjectSet(0).setSkip(Boolean.FALSE);
        return _connection.getService().retrieveProperties(usecoll, new PropertyFilterSpec[] { spec });
    }

    /**
     * Retrieve a single object
     *
     * @param mor Managed Object Reference to get contents for
     * @param propertyName of the object to retrieve
     *
     * @return retrieved object
     */
    public Object getDynamicProperty(ManagedObjectReference mor,
                                     String propertyName)
        throws Exception {
        ObjectContent[] objContent =
            getObjectProperties(null, mor,
                                new String[] { propertyName });

        Object propertyValue = null;
        if (objContent != null) {
            DynamicProperty[] dynamicProperty = objContent[0].getPropSet();
            if (dynamicProperty != null) {
                /*
                 * Check the dynamic propery for ArrayOfXXX object
                 */
                Object dynamicPropertyVal = dynamicProperty[0].getVal();
                String dynamicPropertyName = dynamicPropertyVal.getClass().getName();
                if (dynamicPropertyName.indexOf("ArrayOf") != -1) {
                    String methodName =
                        dynamicPropertyName.substring(dynamicPropertyName.indexOf("ArrayOf")
                                                      + "ArrayOf".length(),
                                                      dynamicPropertyName.length());
                    /*
                     * If object is ArrayOfXXX object, then get the XXX[] by
                     * invoking getXXX() on the object.
                     * For Ex:
                     * ArrayOfManagedObjectReference.getManagedObjectReference()
                     * returns ManagedObjectReference[] array.
                     */
                    if (methodExists(dynamicPropertyVal,
                                     "get" + methodName, null))
                    {
                        methodName = "get" + methodName;
                    }
                    else {
                        /*
                         * Construct methodName for ArrayOf primitive types
                         * Ex: For ArrayOfInt, methodName is get_int
                         */
                        methodName = "get_" +
                            methodName.toLowerCase();
                    }
                    Method getMorMethod =
                        dynamicPropertyVal.getClass().
                            getDeclaredMethod(methodName,  (Class[])null);
                    propertyValue = getMorMethod.invoke(dynamicPropertyVal, (Object[])null);
                }
                else if (dynamicPropertyVal.getClass().isArray()) {
                    /*
                     * Handle the case of an unwrapped array being deserialized.
                     */
                    propertyValue = dynamicPropertyVal;
                }
                else {
                    propertyValue = dynamicPropertyVal;
                }
            }
        }
        return propertyValue;
    }

    public String waitForTask(ManagedObjectReference taskmor) throws Exception {
        Object[] result =
            waitForValues(taskmor,
                          new String[] { "info.state", "info.error" },
                          new String[] { "state" },
                          new Object[][] { new Object[] { TaskInfoState.success, TaskInfoState.error } });

        if (result[0].equals(TaskInfoState.success)) {
            return "sucess";
        }
        else {
            TaskInfo tinfo = (TaskInfo)getDynamicProperty(taskmor, "info");
            LocalizedMethodFault fault = tinfo.getError();
            String error = "Error Occured";
            if (fault != null) {
                error = fault.getFault().getFaultReason();
                System.out.println("Fault " + fault.getFault().getFaultCode());
                System.out.println("Message " + fault.getLocalizedMessage());
            }
            return error;
        }
    }

    /**
     *  Handle Updates for a single object.
     *  waits till expected values of properties to check are reached
     *  Destroys the ObjectFilter when done.
     *  @param objmor MOR of the Object to wait for</param>
     *  @param filterProps Properties list to filter
     *  @param endWaitProps
     *    Properties list to check for expected values
     *    these be properties of a property in the filter properties list
     *  @param expectedVals values for properties to end the wait
     *  @return true indicating expected values were met, and false otherwise
     */
    public Object[] waitForValues(ManagedObjectReference objmor,
                                  String[] filterProps,
                                  String[] endWaitProps,
                                  Object[][] expectedVals)
        throws Exception {
        // version string is initially null
        String version = "";
        Object[] endVals = new Object[endWaitProps.length];
        Object[] filterVals = new Object[filterProps.length];

        PropertyFilterSpec spec = new PropertyFilterSpec();
        spec.setObjectSet(new ObjectSpec[] { new ObjectSpec() });
        spec.getObjectSet(0).setObj(objmor);

        spec.setPropSet(new PropertySpec[] { new PropertySpec() });
        spec.getPropSet(0).setPathSet(filterProps);
        spec.getPropSet(0).setType(objmor.getType());

        spec.getObjectSet(0).setSelectSet(null);
        spec.getObjectSet(0).setSkip(Boolean.FALSE);

        ManagedObjectReference filterSpecRef =
            _connection.getService().createFilter(_connection.getPropCol(),
                                                  spec, true);

        boolean reached = false;

        UpdateSet updateset = null;
        PropertyFilterUpdate[] filtupary = null;
        PropertyFilterUpdate filtup = null;
        ObjectUpdate[] objupary = null;
        ObjectUpdate objup = null;
        PropertyChange[] propchgary = null;
        PropertyChange propchg = null;
        while (!reached) {
            boolean retry = true;
            while(retry) {
                try {
                    updateset =
                        _connection.getService().waitForUpdates(
                        _connection.getPropCol(), version);
                    retry = false;
                }
                catch(Exception e) {
                    if(e instanceof org.apache.axis.AxisFault) {
                        org.apache.axis.AxisFault fault =
                            (org.apache.axis.AxisFault)e;
                        org.w3c.dom.Element [] errors =
                            fault.getFaultDetails();
                        String faultString = fault.getFaultString();
                        if(faultString.indexOf("java.net.SocketTimeoutException") != -1) {
                            System.out.println("Retrying2........");
                            retry = true;
                        }
                        else {
                            throw e;
                        }
                    }
                }
            }
            version = updateset.getVersion();
            if (updateset == null || updateset.getFilterSet() == null) {
                continue;
            }
            // Make this code more general purpose when PropCol changes later.
            filtupary = updateset.getFilterSet();
            filtup = null;
            for (int fi = 0; fi < filtupary.length; fi++) {
                filtup = filtupary[fi];
                objupary = filtup.getObjectSet();
                objup = null;
                propchgary = null;
                for (int oi = 0; oi < objupary.length; oi++) {
                    objup = objupary[oi];
                    // TODO: Handle all "kind"s of updates.
                    if (objup.getKind() == ObjectUpdateKind.modify ||
                        objup.getKind() == ObjectUpdateKind.enter ||
                        objup.getKind() == ObjectUpdateKind.leave)
                    {
                        propchgary = objup.getChangeSet();
                        for (int ci = 0; ci < propchgary.length; ci++) {
                            propchg = propchgary[ci];
                            updateValues(endWaitProps, endVals, propchg);
                            updateValues(filterProps, filterVals, propchg);
                        }
                    }
                }
            }
            Object expctdval = null;
            // Check if the expected values have been reached and exit the loop if done.
            // Also exit the WaitForUpdates loop if this is the case.
            for (int chgi = 0; chgi < endVals.length && !reached; chgi++) {
                for (int vali = 0; vali < expectedVals[chgi].length && !reached; vali++) {
                    expctdval = expectedVals[chgi][vali];
                    reached = expctdval.equals(endVals[chgi]) || reached;
                }
            }
        }

        // Destroy the filter when we are done.
        _connection.getService().destroyPropertyFilter(filterSpecRef);

        return filterVals;
    }

    protected void updateValues(String[] props, Object[] vals, PropertyChange propchg) {
        for (int findi = 0; findi < props.length; findi++) {
            if (propchg.getName().lastIndexOf(props[findi]) >= 0) {
                if (propchg.getOp() == PropertyChangeOp.remove) {
                    vals[findi] = "";
                } else {
                    vals[findi] = propchg.getVal();
                }
            }
        }
    }


    /**
     * This method creates a SelectionSpec[] to traverses the entire
     * inventory tree starting at a Folder
     * @return The SelectionSpec[]
     */
    public SelectionSpec [] buildFullTraversal() {

        // Recurse through all ResourcePools
        TraversalSpec rpToRp =
            new TraversalSpec(null, null, null,
                              "ResourcePool",
                              "resourcePool",
                              Boolean.FALSE,
                              new SelectionSpec[] {new SelectionSpec(null, null, "rpToRp"),
                                                   new SelectionSpec(null, null, "rpToVm")});

        rpToRp.setName("rpToRp");

        // Recurse through all ResourcePools
        TraversalSpec rpToVm =
            new TraversalSpec(null,null,null,
                              "ResourcePool",
                              "vm",
                              Boolean.FALSE,
                              new SelectionSpec[] {});

        rpToVm.setName("rpToVm");

        // Traversal through ResourcePool branch
        TraversalSpec crToRp =
            new TraversalSpec(null, null, null,
                              "ComputeResource",
                              "resourcePool",
                              Boolean.FALSE,
                              new SelectionSpec[] {new SelectionSpec(null, null, "rpToRp"),
                                                   new SelectionSpec(null, null, "rpToVm")});

        crToRp.setName("crToRp");

        // Traversal through host branch
        TraversalSpec crToH =
            new TraversalSpec(null, null, null,
                              "ComputeResource",
                              "host",
                              Boolean.FALSE,
                              new SelectionSpec[] {});

        crToH.setName("crToH");
        // Traversal through hostFolder branch
        TraversalSpec dcToHf =
            new TraversalSpec(null, null, null,
                              "Datacenter",
                              "hostFolder",
                              Boolean.FALSE,
                              new SelectionSpec[] {
                                  new SelectionSpec(null, null, "visitFolders")
                              });

        dcToHf.setName("dcToHf");

        // Traversal through vmFolder branch
        TraversalSpec dcToVmf =
            new TraversalSpec(null, null, null,
                              "Datacenter",
                              "vmFolder",
                              Boolean.FALSE,
                              new SelectionSpec[] {
                                  new SelectionSpec(null, null, "visitFolders")
                              });

        dcToVmf.setName("dcToVmf");

        // Recurse through all Hosts
        TraversalSpec HToVm =
            new TraversalSpec(null, null ,null,
                              "HostSystem",
                              "vm",
                              Boolean.FALSE,
                              new SelectionSpec[] {
                                  new SelectionSpec(null, null, "visitFolders")
                              });

        HToVm.setName("HToVm");

        // Recurse thriugh the folders
        TraversalSpec visitFolders =
            new TraversalSpec(null, null, null,
                              "Folder",
                              "childEntity",
                              Boolean.FALSE,
                              new SelectionSpec[] {
                                  new SelectionSpec(null, null, "visitFolders"),
                                  new SelectionSpec(null, null, "dcToHf"),
                                  new SelectionSpec(null, null, "dcToVmf"),
                                  new SelectionSpec(null, null, "crToH"),
                                  new SelectionSpec(null, null, "crToRp"),
                                  new SelectionSpec(null, null, "HToVm"),
                                  new SelectionSpec(null, null, "rpToVm"),
                              });

        visitFolders.setName("visitFolders");
        return new SelectionSpec[] {
            visitFolders,
            dcToVmf,
            dcToHf,
            crToH,
            crToRp,
            rpToRp,
            HToVm,
            rpToVm
        };
    }

    /**
     * This code takes an array of [typename, property, property, ...]
     * and converts it into a PropertySpec[].
     * handles case where multiple references to the same typename
     * are specified.
     *
     * @param typeinfo 2D array of type and properties to retrieve
     *
     * @return Array of container filter specs
     */
    public PropertySpec[] buildPropertySpecArray(String[][] typeinfo) {
        // Eliminate duplicates
        HashMap tInfo = new HashMap();
        for (int ti = 0; ti < typeinfo.length; ++ti) {
            Set props = (Set)tInfo.get(typeinfo[ti][0]);
            if(props == null) {
                props = new HashSet();
                tInfo.put(typeinfo[ti][0], props);
            }
            boolean typeSkipped = false;
            for (int pi=0; pi<typeinfo[ti].length; ++pi) {
                String prop = typeinfo[ti][pi];
                if (typeSkipped) {
                    props.add(prop);
                }
                else {
                    typeSkipped = true;
                }
            }
        }

        // Create PropertySpecs
        ArrayList pSpecs = new ArrayList();
        for (Iterator ki = tInfo.keySet().iterator(); ki.hasNext();) {
            String type = (String)ki.next();
            PropertySpec pSpec = new PropertySpec();
            Set props = (Set)tInfo.get(type);
            pSpec.setType(type);
            pSpec.setAll(props.isEmpty() ? Boolean.TRUE : Boolean.FALSE);
            pSpec.setPathSet(new String[props.size()]);
            int index = 0;
            for(Iterator pi = props.iterator(); pi.hasNext();) {
                String prop = (String)pi.next();
                pSpec.setPathSet(index++, prop);
            }
            pSpecs.add(pSpec);
        }

        return (PropertySpec[])pSpecs.toArray(new PropertySpec[0]);
    }

    /**
     * Determines of a method 'methodName' exists for the Object 'obj'
     * @param obj The Object to check
     * @param methodName The method name
     * @param parameterTypes Array of Class objects for the parameter types
     * @return true if the method exists, false otherwise
     */
    boolean methodExists(Object obj,
                         String methodName,
                         Class[] parameterTypes) {
        boolean exists = false;
        try {
            Method method = obj.getClass().getMethod(methodName, parameterTypes);
            if (method != null) {
                exists = true;
            }
        } catch(Exception e){}
        return exists;
    }
}
