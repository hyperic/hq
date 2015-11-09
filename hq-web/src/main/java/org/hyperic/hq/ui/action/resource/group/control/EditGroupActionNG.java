package org.hyperic.hq.ui.action.resource.group.control;

import javax.annotation.Resource;

import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.bizapp.shared.ControlBoss;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.PluginNotFoundException;
import org.hyperic.hq.scheduler.ScheduleValue;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.resource.common.control.EditActionNG;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.hq.ui.util.SessionUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;


@Component("editGroupControlActionNG")
@Scope(value="prototype")
public class EditGroupActionNG extends EditActionNG {
	 @Resource
		private ControlBoss controlBoss;
	
	public EditGroupActionNG () {
		this.cForm = new GroupControlFormNG();
		this.log = LogFactory.getLog(EditGroupActionNG.class.getName());
		this.runBackendSave=false;
	}
	
	public String save() throws Exception {
		request = getServletRequest();
		int sessionId = RequestUtils.getSessionId(request).intValue();
		AppdefEntityID appdefId = RequestUtils.getEntityId(request);
		
		String outcome = super.save();
		if (outcome.equals(INPUT)) {
			return outcome;
		}
		try{
	     
		ScheduleValue sv = this.convertToScheduleValue();
	     Integer[] orderSpec = null;
         int[] newOrderSpec = null;
         if (((GroupControlFormNG) cForm).getInParallel().booleanValue() == GroupControlFormNG.IN_ORDER.booleanValue()) {
             orderSpec = ((GroupControlFormNG) cForm).getResourceOrdering();
             newOrderSpec = new int[orderSpec.length];
             for (int i = 0; i < orderSpec.length; i++) {
                 newOrderSpec[i] = orderSpec[i].intValue();
             }
         }

         Integer[] bids = new Integer[] { RequestUtils.getIntParameter(request, Constants.CONTROL_BATCH_ID_PARAM), };
         controlBoss.deleteControlJob(sessionId, bids);
         try{
         if (cForm.getStartTime().equals(GroupControlFormNG.START_NOW)) {
             controlBoss.doGroupAction(sessionId, appdefId, cForm.getControlAction(), (String) null, newOrderSpec);
         } else {
             controlBoss.doGroupAction(sessionId, appdefId, cForm.getControlAction(), newOrderSpec, sv);
         }

		 }catch (Exception e){
			    addActionError(getText("resource.common.error.ControlNotEnabled"));
	        	return INPUT;
	        }
         // set confirmation message
         

         return outcome;

     } catch (PluginNotFoundException pnfe) {
         log.trace("no plugin available", pnfe);
         this.addActionError(getText("resource.common.error.PluginNotFound"));
         return INPUT;
     } catch (PluginException cpe) {
         log.trace("control not enabled", cpe);
         this.addActionError(getText("resource.common.error.ControlNotEnabled"));
         return INPUT;
     }

 }
		
	}
	

