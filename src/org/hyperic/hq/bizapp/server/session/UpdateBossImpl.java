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

package org.hyperic.hq.bizapp.server.session;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.PlatformManagerLocal;
import org.hyperic.hq.appdef.shared.ServerManagerLocal;
import org.hyperic.hq.appdef.shared.ServiceManagerLocal;
import org.hyperic.hq.application.HQApp;
import org.hyperic.hq.auth.shared.SessionException;
import org.hyperic.hq.auth.shared.SessionManager;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.bizapp.shared.UpdateBoss;
import org.hyperic.hq.common.server.session.ServerConfigAudit;
import org.hyperic.hq.common.shared.ProductProperties;
import org.hyperic.hq.common.shared.ServerConfigManagerLocal;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.hqu.server.session.UIPlugin;
import org.hyperic.hq.hqu.shared.UIPluginManagerLocal;
import org.hyperic.util.thread.LoggingThreadGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


/**
 */
@Service
@Transactional
public class UpdateBossImpl
    extends BizappSessionEJB
    implements UpdateBoss
{
    private UpdateStatusDAO updateDAO;
    private static final String CHECK_URL =  "http://updates.hyperic.com/hq-updates";

    private final Log log = LogFactory.getLog(UpdateBossImpl.class);
    
    private ServerConfigManagerLocal serverConfigManager;
    
    private PlatformManagerLocal platformManager;
    
    private ServerManagerLocal serverManager;
    
    private ServiceManagerLocal serviceManager;
    
    private UIPluginManagerLocal uiPluginManager;
    
    
    @Autowired
    public UpdateBossImpl(UpdateStatusDAO updateDAO, ServerConfigManagerLocal serverConfigManager,
                          PlatformManagerLocal platformManager, ServerManagerLocal serverManager,
                          ServiceManagerLocal serviceManager, UIPluginManagerLocal uiPluginManager) {
        this.updateDAO = updateDAO;
        this.serverConfigManager = serverConfigManager;
        this.platformManager = platformManager;
        this.serverManager = serverManager;
        this.serviceManager = serviceManager;
        this.uiPluginManager = uiPluginManager;
    }

    private String getCheckURL() {
        try {
            Properties p = HQApp.getInstance().getTweakProperties();
            String res = p.getProperty("hq.updateNotify.url");
            if (res != null)
                return res;
        } catch(Exception e) {
            log.warn("Unable to get notification url", e);
        }
        return CHECK_URL;
    }

    /**
     * 
     */
    public void startup() {
        LoggingThreadGroup grp = new LoggingThreadGroup("Update Notifier");
        Thread t = new Thread(grp, new UpdateFetcher(), "Update Notifier");
        t.start();
    }

    @SuppressWarnings("unchecked")
    private Properties getRequestInfo(UpdateStatus status) {
        Properties req = new Properties();
        String guid = serverConfigManager.getGUID();

        req.setProperty("hq.updateStatusMode", "" + status.getMode().getCode());
        req.setProperty("hq.version", ProductProperties.getVersion());
        req.setProperty("hq.build", ProductProperties.getBuild());
        req.setProperty("hq.guid", guid);
        req.setProperty("hq.flavour", ProductProperties.getFlavour());
        req.setProperty("platform.time", "" + System.currentTimeMillis());
        req.setProperty("os.name", System.getProperty("os.name"));
        req.setProperty("os.arch", System.getProperty("os.arch"));
        req.setProperty("os.version", System.getProperty("os.version"));
        req.setProperty("java.version", System.getProperty("java.version"));
        req.setProperty("java.vendor", System.getProperty("java.vendor"));

        List<Object[]> plats = platformManager.getPlatformTypeCounts();
        List<Object[]> svrs  = serverManager.getServerTypeCounts();
        List<Object[]> svcs  = serviceManager.getServiceTypeCounts();

        addResourceProperties(req, plats, "hq.rsrc.plat.");
        addResourceProperties(req, svrs,  "hq.rsrc.svr.");
        addResourceProperties(req, svcs,  "hq.rsrc.svc.");

        req.putAll(SysStats.getCpuMemStats());
        req.putAll(SysStats.getDBStats());
        req.putAll(getHQUPlugins());
        BossStartupListener.getUpdateReportAppender().addProps(req);
        return req;
    }

    @SuppressWarnings("unchecked")
    private Properties getHQUPlugins() {
        Collection<UIPlugin> plugins = uiPluginManager.findAll();
        Properties res = new Properties();

        for (UIPlugin p: plugins ) {
            res.setProperty("hqu.plugin." + p.getName(),
                            p.getPluginVersion());
        }
        return res;
    }

    private void addResourceProperties(Properties p, List<Object[]> resCounts,
                                       String prefix)
    {
        for (Object[] val: resCounts) {
            p.setProperty(prefix + val[0], "" + val[1]);
        }
    }

    /**
     * Meant to be called internally by the fetching thread
     *
     * 
     */
    public void fetchReport() {
        UpdateStatus status = getOrCreateStatus();
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
            log.warn("Error creating report request", e);
            return;
        }

        log.debug("Generated report.  Size=" + reqBytes.length +
                   " report:\n" + req);

        PostMethod post = new PostMethod(getCheckURL());
        post.addRequestHeader("x-hq-guid", req.getProperty("hq.guid"));
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
            log.debug("Unable to get updates", e);
            return;
        } finally {
            post.releaseConnection();
        }

        processReport(statusCode, response);
    }

    private void processReport(int statusCode, String response) {
        UpdateStatus curStatus = getOrCreateStatus();
        String curReport;

        if (response.length() >= 4000) {
            log.warn("Update report exceeded 4k");
            return;
        }

        if (statusCode != 200) {
            log.debug("Bad status code returned: " + statusCode);
            return;
        }

        if (curStatus.getMode().equals(UpdateStatusMode.NONE))
            return;

        response = response.trim();

        curReport = curStatus.getReport() == null ? "" : curStatus.getReport();
        if (curReport.equals(response))
            return;

        curStatus.setReport(response);
        curStatus.setIgnored(response.trim().length() == 0);
    }

    /**
     * Returns null if there is no status report (or it's been ignored), else
     * the string status report
     *
     * 
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
     * 
     */
    public void setUpdateMode(int sess, UpdateStatusMode mode)
        throws SessionException
    {
        AuthzSubject subject =
            SessionManager.getInstance().getSubject(sess);
        UpdateStatus status = getOrCreateStatus();

        if (!status.getMode().equals(mode))
            ServerConfigAudit.updateAnnounce(subject, mode, status.getMode());

        status.setMode(mode);

        if (mode.equals(UpdateStatusMode.NONE)) {
            status.setIgnored(true);
            status.setReport("");
        }
    }

    /**
     * 
     */
    public UpdateStatusMode getUpdateMode() {
        return getOrCreateStatus().getMode();
    }

    /**
     * 
     */
    public void ignoreUpdate() {
        UpdateStatus status = getOrCreateStatus();

        status.setIgnored(true);
    }

    private UpdateStatus getOrCreateStatus() {
        UpdateStatus res = updateDAO.get();

        if (res == null) {
            res = new UpdateStatus("", UpdateStatusMode.MAJOR);
            updateDAO.save(res);
        }
        return res;
    }

    private static class UpdateFetcher implements Runnable {
        private static final int CHECK_INTERVAL = 1000 * 60 * 60 * 24;
        private static final Log _log = LogFactory.getLog(UpdateFetcher.class);

        public void run() {
            long interval = getCheckInterval();
            while(true) {
                try {
                    UpdateBossImpl.getOne().fetchReport();
                } catch(Exception e) {
                    _log.warn("Error getting update notification", e);
                }
                try {
                    Thread.sleep(interval);
                } catch(InterruptedException e) {
                    return;
                }
            }
        }

        private static long getCheckInterval() {
            try {
                Properties p = HQApp.getInstance().getTweakProperties();
                String res = p.getProperty("hq.updateNotify.interval");
                if (res != null)
                    return Long.parseLong(res);
            } catch(Exception e) {
                _log.warn("Unable to get notification interval", e);
            }
            return CHECK_INTERVAL;
        }
    }

    public static UpdateBoss getOne() {
       return Bootstrap.getBean(UpdateBoss.class);
    }

}
