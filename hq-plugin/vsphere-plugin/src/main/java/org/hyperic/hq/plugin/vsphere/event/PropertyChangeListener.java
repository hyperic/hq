package org.hyperic.hq.plugin.vsphere.event;

import com.vmware.vim25.*;
import com.vmware.vim25.mo.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * PropertyChangeListener
 * Detects property changes vs events.
 * Investigating.
 * 
 * @author Helena Edelson
 */
public class PropertyChangeListener {

    private static final Log logger = LogFactory.getLog(PropertyChangeListener.class.getName());

    /**
     * 
     * @param pc
     * @param rootFolder
     * @param vm
     * @param eventHandler
     */
    public void invoke(PropertyCollector pc, Folder rootFolder, ViewManager vm, EventHandler eventHandler) {
        if (pc != null && rootFolder != null && vm != null) {
            doInvoke(pc, rootFolder, vm, eventHandler);
        }
    }


    /**
     * Investigating
     *
     * @param propertyCollector
     * @param rootFolder
     * @param vm
     * @param eventHandler
     */
    private void doInvoke(PropertyCollector propertyCollector, Folder rootFolder, ViewManager vm, EventHandler eventHandler) {

        try {
            logger.debug("Waiting for new Updates...");

            /* create a ContainerView with all VirtualMachine objects, covered recursively */
            ContainerView cv = vm.createContainerView(rootFolder, new String[]{"VirtualMachine"}, true);

            /* create a PropertySpec with no property set for retrieving */
            PropertyFilterSpec pfs = new PropertyFilterSpec();
            pfs.setObjectSet(new ObjectSpec[]{createObjSpec(cv)});
            pfs.setPropSet(new PropertySpec[]{createPropSpec("VirtualMachine", new String[]{})});

            /* create a Property with partialUpdate is true */
            PropertyFilter pf = propertyCollector.createFilter(pfs, true);

            UpdateSet updates = propertyCollector.waitForUpdates(""); 
            String ver = updates.getVersion();
            handleUpdate(updates);
 
            ListView lv = vm.createListViewFromView(cv);

            lv.getServerConnection().getVimService().modifyListView(
                    lv.getMOR(),
                    new ManagedObjectReference[]{}, //don't remove anything
                    //remove the first VirtualMachine in the list
                    new ManagedObjectReference[]{
                            updates.getFilterSet()[0].getObjectSet()[0].getObj()
            });

            PropertyFilterSpec pfs1 = new PropertyFilterSpec();

            pfs1.setObjectSet(new ObjectSpec[]{createObjSpec(lv)});
            pfs1.setPropSet(new PropertySpec[]{
                    createPropSpec("VirtualMachine",
                            new String[]{"runtime.powerState"})});

            pf.destroyPropertyFilter();
            PropertyFilter pf1 = propertyCollector.createFilter(pfs1, true);

            while (true) {
                logger.debug("Waiting update from version: " + ver);
                try {
                    updates = propertyCollector.waitForUpdates(ver);
                }
                catch (Exception e) {
                    logger.error("exception:" + e);
                    continue;
                }
                                             
                handleUpdate(updates);  
            }
        }
        catch (Exception e) {
            if (!(e instanceof RequestCanceled)) {
                logger.error(e);
            }
        }
    }


    /**
     * @param view
     * @return
     */
    private static ObjectSpec createObjSpec(View view) {
        ObjectSpec oSpec = new ObjectSpec();
        oSpec.setSkip(true); //skip this ContainerView object
        oSpec.setObj(view.getMOR());

        TraversalSpec tSpec = new TraversalSpec();
        tSpec.setType(view.getMOR().getType());
        tSpec.setPath("view");
        oSpec.setSelectSet(new SelectionSpec[]{tSpec});

        return oSpec;
    }

    /**
     * Set up a PropertySpec to:
     * use the runtime.powerState attribute of a VirtualMachine.
     * Another example is to use the latestPage attribute of the EventHistoryCollector
     * </p>
     * VirtualMachineRuntimeInfo - execution state and history for a virtual machine.
     * The contents of this property change when:
     * <ul><li>the virtual machine's power state changes</li>
     * <li>an execution message is pending</li>
     * <li>an event occurs</li></ul>
     *
     * @param type
     * @param props
     * @return
     */
    private static PropertySpec createPropSpec(String type, String[] props) {
        PropertySpec pSpec = new PropertySpec();
        pSpec.setType(type); // VirtualMachine, etc
        pSpec.setAll(false);
        pSpec.setPathSet(props); //latestPage, runtime.powerState...

        /*
        propSpec.setPathSet(new String[] { "latestPage" });
        propSpec.setType(eventHistoryCollector.getType());*/

        return pSpec;
    }

    private void handleUpdate(UpdateSet uSet) {
            PropertyFilterUpdate[] propertyFilterUpdates = uSet.getFilterSet();

            if (propertyFilterUpdates != null) {
                for (PropertyFilterUpdate u : propertyFilterUpdates) {
                    logger.debug("Found updates: " + propertyFilterUpdates.length);
                    ObjectUpdate[] objectUpdates = u.getObjectSet();
                    if (objectUpdates != null) {
                        for (ObjectUpdate update : objectUpdates) {
                            if (update != null) {
                                StringBuilder builder = new StringBuilder().append("Update on ").append(update.getObj().getType()).append(":").append(update.getObj().get_value());
                                handleObjectUpdate(update, builder.toString());
                            }
                        }
                    }
                }
            }
        }

        /**
         *
         * @param update
         * @param status
         */
        private void handleObjectUpdate(ObjectUpdate update, String status) {
            PropertyChange[] changes = update.getChangeSet();
            String changeType = getChangeType(update);

            if (changes != null) {
                for (PropertyChange change : changes) {
                    if (change != null) {
                        logger.debug(status + ": " + changeType + change.getName() + "-->" + change.getVal());

                        extractEvents(change.getVal());
                    }
                }
            }
        }

        /**
         * @param obj
         */
        private void extractEvents(Object obj) {
            if (obj != null) {
                EventHandler handler = new DefaultEventHandler();
                if(obj instanceof VirtualMachinePowerState) {

                } else if (obj instanceof ArrayOfEvent) {
                    ArrayOfEvent aoe = (ArrayOfEvent) obj;
                    Event[] events = aoe.getEvent();

                    if (events != null && events.length > 0) {
                        handler.handleEvents(events);
                    }
                } else if (obj instanceof VmEvent) {
                    handler.handleEvent((VmEvent) obj);
                }
            }
        }

        /**
         * @param oUpdate
         * @return
         */
        private String getChangeType(ObjectUpdate oUpdate) {
            String type = null;
            if (oUpdate.getKind() == ObjectUpdateKind.enter) {
                type = "New Data: ";
            } else if (oUpdate.getKind() == ObjectUpdateKind.leave) {
                type = "Removed Data: ";
            } else if (oUpdate.getKind() == ObjectUpdateKind.modify) {
                type = "Changed Data: ";
            }

            return type;
        }

}
