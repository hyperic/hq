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

package org.hyperic.hq.bizapp.server.session;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.zip.GZIPOutputStream;

import javax.ejb.SessionBean;
import javax.ejb.SessionContext;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.appdef.server.session.PlatformManagerEJBImpl;
import org.hyperic.hq.appdef.server.session.ServerManagerEJBImpl;
import org.hyperic.hq.appdef.server.session.ServiceManagerEJBImpl;
import org.hyperic.hq.bizapp.shared.UpdateBossLocal;
import org.hyperic.hq.bizapp.server.session.UpdateStatusMode;
import org.hyperic.hq.bizapp.shared.UpdateBossUtil;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.server.session.ServerConfigManagerEJBImpl;
import org.hyperic.hq.common.shared.ProductProperties;
import org.hyperic.util.thread.LoggingThreadGroup;


/**
 * @ejb:bean name="UpdateBoss"
 *      jndi-name="ejb/bizapp/UpdateBoss"
 *      local-jndi-name="LocalUpdateBoss"
 *      view-type="both"
 *      type="Stateless"
 */
public class UpdateBossEJBImpl 
    extends BizappSessionEJB
    implements SessionBean 
{
    private static final UpdateStatusDAO _updateDAO = 
        new UpdateStatusDAO(DAOFactory.getDAOFactory());
    private static final int CHECK_INTERVAL = 1000 * 60 * 60 * 24; 
    private static final String CHECK_URL = "http://support.hyperic.com/uns"; 
        
    private final Log _log = LogFactory.getLog(UpdateBossEJBImpl.class);
    
    /**
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public void startup() {
        LoggingThreadGroup grp = new LoggingThreadGroup("Update Notifier");
        Thread t = new Thread(grp, new UpdateFetcher(), "Update Notifier");
        
        t.start();
    }
    
    private Properties getRequestInfo(UpdateStatus status) {
        Properties req = new Properties();
        String guid = ServerConfigManagerEJBImpl.getOne().getGUID();
        
        req.setProperty("hq.updateStatusMode", "" + status.getMode().getCode());
        req.setProperty("hq.version", ProductProperties.getVersion());
        req.setProperty("hq.build", ProductProperties.getBuild());
        req.setProperty("hq.guid", guid);
        req.setProperty("platform.time", "" + System.currentTimeMillis());
        req.setProperty("os.name", System.getProperty("os.name"));
        req.setProperty("os.arch", System.getProperty("os.arch"));
        req.setProperty("os.version", System.getProperty("os.version"));
        req.setProperty("java.version", System.getProperty("java.version"));
        req.setProperty("java.vendor", System.getProperty("java.vendor"));
        
        List plats = PlatformManagerEJBImpl.getOne().getPlatformTypeCounts();
        List svrs  = ServerManagerEJBImpl.getOne().getServerTypeCounts();
        List svcs  = ServiceManagerEJBImpl.getOne().getServiceTypeCounts();
        
        addResourceProperties(req, plats, "hq.rsrc.plat.");
        addResourceProperties(req, svrs,  "hq.rsrc.svr.");
        addResourceProperties(req, svcs,  "hq.rsrc.svc.");
        return req;
    }
    
    private void addResourceProperties(Properties p, List resCounts,
                                       String prefix) 
    {
        for (Iterator i=resCounts.iterator(); i.hasNext(); ) {
            Object[] val = (Object[])i.next();
            
            p.setProperty(prefix + val[0], "" + val[1]);
        }
    }

    /**
     * Meant to be called internally by the fetching thread
     * 
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public void fetchReport() {
        UpdateStatus status = _updateDAO.get();
        Properties req;
        byte[] reqBytes;
        
        if (status.getMode().equals(UpdateStatusMode.NONE))
            return;
        
        req = getRequestInfo(status);
        
        try {
            ByteArrayOutputStream bOs = new ByteArrayOutputStream(); 
            GZIPOutputStream gOs = new GZIPOutputStream(bOs);
            
            req.store(gOs, "");
            gOs.flush();
            gOs.close();
            bOs.flush();
            bOs.close();
            reqBytes = bOs.toByteArray();
        } catch(IOException e) {
            _log.warn("Error creating report request", e);
            return;
        }
        
        _log.debug("Generated report.  Size=" + reqBytes.length + 
                   " report:\n" + req);
        
        PostMethod post = new PostMethod(CHECK_URL);
        HttpClient c = new HttpClient();
        c.setTimeout(5 * 60 * 1000); 
        
        ByteArrayInputStream bIs = new ByteArrayInputStream(reqBytes);
        
        post.setRequestBody(bIs);

        String response;
        int statusCode;
        try {
            statusCode = c.executeMethod(post);
            
            response = post.getResponseBodyAsString();
        } catch(Exception e) {
            _log.warn("Unable to get updates", e);
            return;
        } finally {
            post.releaseConnection();
        }
        
        processReport(statusCode, response);
    }

    private void processReport(int statusCode, String response) {  
        UpdateStatus curStatus = getOrCreateStatus();
        
        if (response.length() >= 4000) { 
            _log.warn("Update report exceeded 4k");
            return;
        }

        // TODO:  Check status code so we only save valid stuffs
        if (curStatus.getReport() != null && 
            curStatus.getReport().equals(response))
        {
            return;
        }
        
        curStatus.setReport(response);
        curStatus.setIgnored(false);
    }
    
    /**
     * Returns null if there is no status report (or it's been ignored), else
     * the string status report
     * 
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public String getUpdateReport() {
        UpdateStatus status = getOrCreateStatus();
        
        if (status.isIgnored())
            return null;
        
        if (status.getReport() == null || status.getReport().equals("")) {
            return null;
        }
            
        return status.getReport(); 
    }
    
    /**
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public void setUpdateMode(UpdateStatusMode mode) {
        UpdateStatus status = getOrCreateStatus();
        
        status.setMode(mode);
    }

    /**
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public void ignoreUpdate() {
        UpdateStatus status = getOrCreateStatus();
        
        status.setIgnored(true);
    }
    
    private UpdateStatus getOrCreateStatus() {
        UpdateStatus res = _updateDAO.get();
        
        if (res == null) {
            res = new UpdateStatus("", UpdateStatusMode.MAJOR);
            _updateDAO.save(res);
        }
        return res;
    }
     
    private static class UpdateFetcher implements Runnable {
        public void run() {
            while(true) {
                UpdateBossEJBImpl.getOne().fetchReport();
                try {
                    Thread.sleep(CHECK_INTERVAL);
                } catch(InterruptedException e) {
                }
            }
        }
    }

    public static UpdateBossLocal getOne() {
        try {
            return UpdateBossUtil.getLocalHome().create();
        } catch(Exception e) {
            throw new SystemException(e);
        }
    }
    
    public void ejbCreate() { }
    public void ejbRemove() { }
    public void ejbActivate() { }
    public void ejbPassivate() { }
    public void setSessionContext(SessionContext c) {}
}
