/*                                                                 
 * NOTE: This copyright does *not* cover user programs that use HQ 
 * program services by normal system calls through the application 
 * program interfaces provided as part of the Hyperic Plug-in Development 
 * Kit or the Hyperic Client Development Kit - this is merely considered 
 * normal use of the program, and does *not* fall under the heading of 
 * "derived work". 
 *  
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc. 
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

package org.hyperic.hq.ui.json.action.escalation.crud;

import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.EventsBoss;
import org.hyperic.hq.bizapp.shared.action.EmailActionConfig;
import org.hyperic.hq.bizapp.shared.action.SnmpActionConfig;
import org.hyperic.hq.bizapp.shared.action.SyslogActionConfig;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.escalation.server.session.Escalation;
import org.hyperic.hq.events.ActionConfigInterface;
import org.hyperic.hq.events.NoOpAction;
import org.hyperic.hq.ui.json.action.JsonActionContext;
import org.hyperic.hq.ui.json.action.escalation.BaseAction;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.util.StringUtil;
import org.json.JSONException;



/**
 * Called when UI wants to create an escalation action
 */
public class SaveAction extends BaseAction {
    private final Log _log = LogFactory.getLog(SaveAction.class);
    
    public void execute(JsonActionContext context) 
        throws PermissionException, SessionTimeoutException,
               SessionNotFoundException, JSONException, RemoteException
    {
        ServletContext sctx = context.getServletContext();
        ActionConfigInterface cfg;
        
        Map        map    = context.getParameterMap();
        String     action = ((String[])map.get("action"))[0];
        Integer    escId  = Integer.valueOf(((String[])map.get("EscId"))[0]); 
        EventsBoss eBoss  = ContextUtils.getEventsBoss(sctx);
        int        sessId = context.getSessionId();
        Escalation e      = eBoss.findEscalationById(sessId, escId);
        long       wait   = Long.parseLong(((String[])map.get("waittime"))[0]);
        
        if (action.equalsIgnoreCase("Email")) {
            cfg = makeEmailActionCfg(e, map, false);
        } else if(action.equalsIgnoreCase("SMS")) {
            cfg = makeEmailActionCfg(e, map, true);
        } else if(action.equalsIgnoreCase("Syslog")) {
            cfg = makeSyslogActionCfg(e, map);
        } else if(action.equalsIgnoreCase("SNMP")) {
            cfg = makeSNMPActionCfg(e, map);
        } else if (action.equalsIgnoreCase("noop")) {
            cfg = new NoOpAction();  // Yow.
        } else {
            throw new SystemException("Unknown action type [" + action + "]");
        }
        
        eBoss.addAction(sessId, e, cfg, wait);
    }   
    
    private ActionConfigInterface
        makeSyslogActionCfg(Escalation e, Map p)
    {
        String meta    = ((String[])p.get("meta"))[0];
        String version = ((String[])p.get("version"))[0];
        String product = ((String[])p.get("product"))[0];
        
        return new SyslogActionConfig(meta, product, version);
    }
    
    private ActionConfigInterface
        makeSNMPActionCfg(Escalation e, Map p)
    {
        String address = ((String[])p.get("snmpIP"))[0];
        String oid     = ((String[])p.get("snmpOID"))[0];
        
        return new SnmpActionConfig(address, oid);
    }
    
    private ActionConfigInterface 
        makeEmailActionCfg(Escalation e, Map p, boolean sms) 
    { 
        EmailActionConfig cfg = new EmailActionConfig();
        String sType = ((String[])p.get("who"))[0];
        String nameVar;
        
        if (sType.equals("Users")) {
            cfg.setType(EmailActionConfig.TYPE_USERS);
            nameVar = "users";
        } else if (sType.equals("Others")) {
            cfg.setType(EmailActionConfig.TYPE_EMAILS);
            nameVar = "emailinput";
        } else if (sType.equals("Roles")) {
            cfg.setType(EmailActionConfig.TYPE_ROLES);
            nameVar = "roles";
        } else {
            throw new SystemException("Unknown email type [" + sType + "]");
        }
        
        String[] nameArr = (String[])p.get(nameVar);
        List nameList = Arrays.asList(nameArr);
        cfg.setNames(StringUtil.implode(nameList, ","));
        cfg.setSms(sms);
        return cfg;
    }
}
