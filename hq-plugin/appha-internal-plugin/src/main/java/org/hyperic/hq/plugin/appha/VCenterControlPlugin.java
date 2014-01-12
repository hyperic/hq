package org.hyperic.hq.plugin.appha;

import java.util.Calendar;
import java.util.Properties;

import org.hyperic.hq.bizapp.shared.lather.ControlSendCommandResult_args;
import org.hyperic.hq.product.ControlPlugin;
import org.hyperic.hq.product.PluginException;
import org.hyperic.util.config.ConfigResponse;

import com.vmware.vim25.EventEx;
import com.vmware.vim25.KeyAnyValue;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.VmEventArgument;
import com.vmware.vim25.mo.EventManager;

public class VCenterControlPlugin extends ControlPlugin {
    public void configure(ConfigResponse config)
        throws PluginException {
        super.configure(config);
    }
    
    public void doAction(String action, String[] args) throws PluginException {
        setResult(ControlPlugin.RESULT_FAILURE);
        // We expect to get the vm's moid and the event type. Without these parameters we can't continue
        if(args.length < 3) {
            setMessage("Expected at least 3 arguments for this action - 'VM moid', 'event type' and 'service name'");
            return;
        }
       
        // Extract the arguments
        String vmMoid = args[0];
        String eventType = args[1];
        String serviceName = args[2];
        String policyName = args[3];
        
        VSphereUtil vim = null;
        try {
            vim = VSphereUtil.getInstance(getConfig());
        } catch (PluginException ex) {
            setMessage("Unable to access VC: " + ex.getMessage());
            throw ex;
        }
        
        final ManagedObjectReference morVM = new ManagedObjectReference();
        morVM.setVal(vmMoid);
        morVM.setType("VirtualMachine");
        
        EventManager eventManager = vim.getEventManager();
        final Calendar calendar = Calendar.getInstance();
        final EventEx eventEx = new EventEx();
        eventEx.setCreatedTime(calendar);
        eventEx.setEventTypeId(eventType);
        VmEventArgument vmEventArgument = new VmEventArgument();
        vmEventArgument.setName("vm");
        vmEventArgument.setVm(morVM);
        eventEx.setUserName("");
        eventEx.setVm(vmEventArgument);
        
        final KeyAnyValue[] eventArgs = new KeyAnyValue[2];
        eventArgs[0] = new KeyAnyValue();
        eventArgs[0].setKey("serviceName");
        eventArgs[0].setValue(serviceName);
       
        eventArgs[1] = new KeyAnyValue();
        eventArgs[1].setKey("policyName");
        eventArgs[1].setValue(policyName);
        
        eventEx.setArguments(eventArgs);
        
    
        
        try {
            eventManager.postEvent(eventEx, null);
            setResult(ControlPlugin.RESULT_SUCCESS);
        } catch (Exception ex) {
            setMessage("Failed to post VC event: " + ex.getMessage());
            throw new PluginException(ex);
        }
        
    }
}
