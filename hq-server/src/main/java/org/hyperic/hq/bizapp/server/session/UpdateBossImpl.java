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
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.zip.GZIPOutputStream;

import javax.sql.DataSource;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.PlatformManager;
import org.hyperic.hq.appdef.shared.ServerManager;
import org.hyperic.hq.appdef.shared.ServiceManager;
import org.hyperic.hq.auth.domain.AuthzSubject;
import org.hyperic.hq.auth.shared.SessionException;
import org.hyperic.hq.auth.shared.SessionManager;
import org.hyperic.hq.bizapp.shared.UpdateBoss;
import org.hyperic.hq.common.server.session.ServerConfigAuditFactory;
import org.hyperic.hq.common.shared.ProductProperties;
import org.hyperic.hq.common.shared.ServerConfigManager;
import org.hyperic.hq.hqu.server.session.UIPlugin;
import org.hyperic.hq.hqu.shared.UIPluginManager;
import org.hyperic.util.timer.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 */
@Service
@Transactional
public class UpdateBossImpl implements UpdateBoss {
    private final Log log = LogFactory.getLog(UpdateBossImpl.class.getName());

    private ServerConfigManager serverConfigManager;
    private PlatformManager platformManager;
    private ServerManager serverManager;
    private ServiceManager serviceManager;
    private UIPluginManager uiPluginManager;
    private UpdateStatusDAO updateDAO;
    private ServerConfigAuditFactory serverConfigAuditFactory;
    private String updateNotifyUrl;
    private static final int HTTP_TIMEOUT_MILLIS = 30000;
    private DataSource dataSource;
 

    @Autowired
    public UpdateBossImpl(
                          UpdateStatusDAO updateDAO,
                          ServerConfigManager serverConfigManager,
                          PlatformManager platformManager,
                          ServerManager serverManager,
                          ServiceManager serviceManager,
                          UIPluginManager uiPluginManager,
                          ServerConfigAuditFactory serverConfigAuditFactory,
                          DataSource dataSource,
                          @Value("#{tweakProperties['hq.updateNotify.url'] }") String updateNotifyUrl) {
        this.updateDAO = updateDAO;
        this.serverConfigManager = serverConfigManager;
        this.platformManager = platformManager;
        this.serverManager = serverManager;
        this.serviceManager = serviceManager;
        this.uiPluginManager = uiPluginManager;
        this.serverConfigAuditFactory = serverConfigAuditFactory;
        this.dataSource = dataSource;
        this.updateNotifyUrl = updateNotifyUrl;
    }

    protected Properties getRequestInfo(UpdateStatus status) {
        Properties req = new Properties();
        String guid = serverConfigManager.getGUID();

        req.setProperty("hq.updateStatusMode", "" + status.getMode().getCode());
        req.setProperty("hq.version", ProductProperties.getVersion());
        req.setProperty("hq.guid", guid);
        req.setProperty("hq.flavour", ProductProperties.getFlavour());
        req.setProperty("platform.time", "" + System.currentTimeMillis());
        req.setProperty("os.name", System.getProperty("os.name"));
        req.setProperty("os.arch", System.getProperty("os.arch"));
        req.setProperty("os.version", System.getProperty("os.version"));
        req.setProperty("java.version", System.getProperty("java.version"));
        req.setProperty("java.vendor", System.getProperty("java.vendor"));

        Map<String,Integer> plats = platformManager.getPlatformTypeCounts();
        Map<String,Integer> svrs = serverManager.getServerTypeCounts();
        Map<String,Integer> svcs = serviceManager.getServiceTypeCounts();

        addResourceProperties(req, plats, "hq.rsrc.plat.");
        addResourceProperties(req, svrs, "hq.rsrc.svr.");
        addResourceProperties(req, svcs, "hq.rsrc.svc.");

        req.putAll(SysStats.getCpuMemStats());
        try {
            req.putAll(SysStats.getDBStats(dataSource.getConnection()));
        } catch (SQLException e) {
            log.warn("Error obtaining DB Stats: " + e.getMessage(),e);
        }
        req.putAll(getHQUPlugins());
        return req;
    }

    private Properties getHQUPlugins() {
        Collection<UIPlugin> plugins = uiPluginManager.findAll();
        Properties res = new Properties();

        for (UIPlugin p : plugins) {
            res.setProperty("hqu.plugin." + p.getName(), p.getPluginVersion());
        }
        return res;
    }

    private void addResourceProperties(Properties p, Map<String,Integer> resCounts, String prefix) {
        for (Map.Entry<String, Integer> val : resCounts.entrySet()) {
            p.setProperty(prefix +val.getKey(), "" + val.getValue());
        }
    }

    /**
     * Meant to be called internally by the fetching thread
     * 
     * 
     */
    public void fetchReport() {
        final boolean debug = log.isDebugEnabled();
        final StopWatch watch = new StopWatch();
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
        } catch (IOException e) {
            log.warn("Error creating report request", e);
            return;
        }
        if(debug){
            log.debug("Generated report.  Size=" + reqBytes.length + " report:\n" + req);
        }

        PostMethod post = new PostMethod(this.updateNotifyUrl);
        post.addRequestHeader("x-hq-guid", req.getProperty("hq.guid"));
        HttpClient c = new HttpClient();
        c.setConnectionTimeout(HTTP_TIMEOUT_MILLIS);
        c.setTimeout(HTTP_TIMEOUT_MILLIS);

        ByteArrayInputStream bIs = new ByteArrayInputStream(reqBytes);

        post.setRequestBody(bIs);

        String response = null;
        int statusCode = -1;
        try {
            if (debug) watch.markTimeBegin("post");
            statusCode = c.executeMethod(post);
            if (debug) watch.markTimeEnd("post");

            response = post.getResponseBodyAsString();
        } catch (Exception e) {
            if(debug) {
                log.debug("Unable to get updates from " + updateNotifyUrl, e);
            }
            return;
        } finally {
            post.releaseConnection();
            if (debug) {
                log.debug("fetchReport: " + watch
                    + ", currentReport {" + status.getReport()
                    + "}, latestReport {url=" + updateNotifyUrl
                    + ", statusCode=" + statusCode
                    + ", response=" + response
                    + "}");
            }
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
    @Transactional(readOnly = true)
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
    public void setUpdateMode(int sess, UpdateStatusMode mode) throws SessionException {
        AuthzSubject subject = SessionManager.getInstance().getSubject(sess);
        UpdateStatus status = getOrCreateStatus();

        if (!status.getMode().equals(mode)) {
            serverConfigAuditFactory.updateAnnounce(subject, mode, status.getMode());
        }

        status.setMode(mode);

        if (mode.equals(UpdateStatusMode.NONE)) {
            status.setIgnored(true);
            status.setReport("");
        }
    }

    /**
     * 
     */
    @Transactional(readOnly = true)
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

}
