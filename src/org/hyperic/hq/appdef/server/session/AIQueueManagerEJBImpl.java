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

package org.hyperic.hq.appdef.server.session;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.ejb.CreateException;
import javax.ejb.FinderException;
import javax.ejb.RemoveException;
import javax.ejb.SessionBean;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.dao.AIPlatformDAO;
import org.hyperic.hq.dao.AIServerDAO;
import org.hyperic.hq.dao.AIIpDAO;
import org.hyperic.hq.appdef.shared.AIIpValue;
import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.hq.appdef.shared.AIQApprovalException;
import org.hyperic.hq.appdef.shared.AIQueueConstants;
import org.hyperic.hq.appdef.shared.AIQueueManagerLocal;
import org.hyperic.hq.appdef.shared.AIServerValue;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.CPropManagerLocal;
import org.hyperic.hq.appdef.shared.ConfigManagerLocal;
import org.hyperic.hq.appdef.shared.PlatformManagerLocal;
import org.hyperic.hq.appdef.shared.PlatformNotFoundException;
import org.hyperic.hq.appdef.shared.PlatformValue;
import org.hyperic.hq.appdef.shared.ServerManagerLocal;
import org.hyperic.hq.appdef.shared.ValidationException;
import org.hyperic.hq.appdef.shared.AIQueueManagerUtil;
import org.hyperic.hq.appdef.Ip;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.AuthzSubjectManagerEJBImpl;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.ResourceValue;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.server.session.AuditManagerEJBImpl;
import org.hyperic.hq.autoinventory.AIPlatform;
import org.hyperic.hq.autoinventory.AIServer;
import org.hyperic.hq.autoinventory.AIIp;
import org.hyperic.sigar.NetFlags;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.pager.Pager;
import org.hyperic.util.pager.SortAttribute;
import org.hyperic.dao.DAOFactory;
import org.hibernate.ObjectNotFoundException;

/**
 * This class is responsible for managing the various autoinventory
 * queues.
 * @ejb:bean name="AIQueueManager"
 *      jndi-name="ejb/appdef/AIQueueManager"
 *      local-jndi-name="LocalAIQueueManager"
 *      view-type="local"
 *      type="Stateless"
 * @ejb:util generate="physical"
 *
 */
public class AIQueueManagerEJBImpl
    extends AppdefSessionEJB implements SessionBean {
    protected final String AIPLATFORM_PROCESSOR
        = "org.hyperic.hq.appdef.server.session.PagerProcessor_aiplatform";
    protected final String AIPLATFORM_PROCESSOR_NOPLACEHOLDERS
        = "org.hyperic.hq.appdef.server.session.PagerProcessor_aiplatform_excludePlaceholders";
    private Pager aiplatformPager = null;
    private Pager aiplatformPager_noplaceholders = null;

    private final AI2AppdefDiff appdefDiffProcessor = new AI2AppdefDiff();
    private final AIQSynchronizer queueSynchronizer = new AIQSynchronizer();

    public AIQueueManagerEJBImpl () {}

    protected Log log = LogFactory.getLog(AIQueueManagerEJBImpl.class.getName());

    /**
     * Try to queue a candidate platform discovered via autoinventory.
     * @param aiplatform The platform that we got from the recent autoinventory
     * data that we are wanting to queue.  This may return null if the appdef
     * platform was removed because the AI platform had a qstat of "remove" that
     * was approved.
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public AIPlatformValue queue(AuthzSubjectValue subject,
                                 AIPlatformValue aiplatform,
                                 boolean updateServers,
                                 boolean isApproval,
                                 boolean isReport)
        throws NamingException, CreateException, RemoveException
    {
        AIPlatformDAO aiplatformLH = getAIPlatformDAO();
        PlatformManagerLocal pmLocal = getPlatformMgrLocal();
        AIQueueManagerLocal aiqLocal = getAIQManagerLocal();
        ConfigManagerLocal crmLocal = getConfigMgrLocal();
        CPropManagerLocal cpropMgr = getCPropMgrLocal();

        // First, calculate queuestatus and diff with respect to 
        // existing appdef data.
        AIPlatformValue revisedAIplatform
            = appdefDiffProcessor.diffAgainstAppdef(subject,
                                                    pmLocal, crmLocal, cpropMgr,
                                                    aiplatform);

        // A null return from diffAgainstAppdef means that 
        // the platform no longer exists in appdef, AND that the aiplatform
        // had status "removed", so everything is kosher we just need to
        // nuke the queue entry.
        if (revisedAIplatform == null) {
            // log.info("AIQmgr.queue (post appdef-diff): aiplatform=NULL");
            AIPlatform aiplatformLocal;
            aiplatformLocal =
                aiplatformLH.findById(aiplatform.getId());
            removeFromQueue(aiplatformLocal);
            return null;
        }

        // Synchronize current AI data into existing queue.
        revisedAIplatform = queueSynchronizer.sync(subject,
                                                   aiqLocal,
                                                   aiplatformLH, 
                                                   revisedAIplatform,
                                                   updateServers,
                                                   isApproval,
                                                   isReport);

        if (revisedAIplatform == null) {
            return null;
        }

        // OK, we do this the hard way: strip out "removed" servers
        // because we never want to show them
        AIServerValue[] servers = revisedAIplatform.getAIServerValues();
        revisedAIplatform.removeAllAIServerValues();
        for ( int i=0; i<servers.length; i++ ) {
            if (servers[i].getQueueStatus()!=AIQueueConstants.Q_STATUS_REMOVED){
                revisedAIplatform.addAIServerValue(servers[i]);
            }
        }

        return revisedAIplatform;
    }

    /**
     * Re-sync an existing queued platform against appdef.
     * @param aiplatform The platform that we got from the recent autoinventory
     *                   data that we are wanting to queue.
     * @param isApproval If true, the platform's servers will be updated as 
     *                   well.
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public AIPlatformValue syncQueue(AIPlatformValue aiplatform, 
                                     boolean isApproval) 
        throws NamingException, CreateException,  RemoveException,
               FinderException 
    {
        // Act as admin for now
        AuthzSubjectValue subject = 
            AuthzSubjectManagerEJBImpl.getOne().findOverlord();

        return queue(subject, aiplatform, true, isApproval, false);
    }

    /**
     * Retrieve the contents of the AI queue.
     * 
     * @param showIgnored
     *            If true, even resources in the AI queue that have the
     *            'ignored' flag set will be returned. By default, resources
     *            with the 'ignored' flag set are excluded when the queue is
     *            retrieved.
     * @param showPlaceholders
     *            If true, even resources in the AI queue that are unchanged
     *            with respect to appdef will be returned. By default, resources
     *            that are unchanged with respect to appdef are excluded when
     *            the queue is retrieved.
     * @return A List of AIPlatformValue objects representing the contents of
     *         the autoinventory queue.
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public PageList getQueue(AuthzSubjectValue subject, boolean showIgnored,
                             boolean showPlaceholders, PageControl pc) {
        return getQueue(subject, showIgnored, showPlaceholders, false, pc);
    }

    /**
     * Retrieve the contents of the AI queue.
     * @param showIgnored If true, even resources in the AI queue that have 
     * the 'ignored' flag set will be returned.  By default, resources with
     * the 'ignored' flag set are excluded when the queue is retrieved.
     * @param showPlaceholders If true, even resources in the AI queue that are 
     * unchanged with respect to appdef will be returned.  By default, resources
     * that are unchanged with respect to appdef are excluded when the queue is
     * retrieved.
     * @param showAlreadyProcessed If true, even resources that have already
     * been processed (approved or not approved) will be shown.
     * @return A List of AIPlatformValue objects representing the contents
     * of the autoinventory queue.
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public PageList getQueue(AuthzSubjectValue subject, boolean showIgnored,
                             boolean showPlaceholders,
                             boolean showAlreadyProcessed, PageControl pc)
    {
        Collection queue;
        PageList results;
        pc = PageControl.initDefaults(pc, SortAttribute.DEFAULT);

        try {
            if (showIgnored) {
                if (showAlreadyProcessed) {
                    queue = getAIPlatformDAO().findAllIncludingProcessed();
                } else {
                    queue = getAIPlatformDAO().findAll();
                }
            } else {
                if (showAlreadyProcessed) {
                    queue = getAIPlatformDAO().findAllNotIgnoredIncludingProcessed();
                } else {
                    queue = getAIPlatformDAO().findAllNotIgnored();
                }
            }

            boolean canCreatePlatforms;
            try {
                checkCreatePlatformPermission(subject);
                canCreatePlatforms = true;
            } catch (PermissionException pe) {
                canCreatePlatforms = false;
            }

            // Walk the collection.  If the aiplatform is "new", then only
            // keep it if the user has canCreatePlatforms permission.
            // If the aiplatform is not new, then make sure the user has 
            // view permissions on the platform that backs the aiplatform.
            Iterator iter = queue.iterator();
            AIPlatform aipLocal;
            PlatformValue pValue;
            AppdefEntityID aid;
            Integer ppk;
            while (iter.hasNext()) {
                aipLocal = (AIPlatform) iter.next();
                pValue = null;
                if (aipLocal.getQueueStatus() != AIQueueConstants.Q_STATUS_ADDED) {
                    try {
                        pValue = getPlatformByAI(subject, aipLocal);
                    } catch (Exception e) {
                        log.debug("Error finding platform for aiplatform: ", e);
                        pValue = null;
                    }
                }

                if (pValue == null && !canCreatePlatforms) {
                    if (log.isDebugEnabled()) {
                        log.debug("Removing platform because it doesn't exist"
                                  + " and the current user doesn't have the "
                                  + "'createPlatform' permission: " 
                                  + aipLocal.getId());
                    }
                    iter.remove();

                } else if (pValue != null) {
                    ppk = pValue.getId();
                    aid = AppdefEntityID.newPlatformID(ppk.intValue());

                    try {
                        checkModifyPermission(subject, aid);
                    } catch (PermissionException e) {
                        if (log.isDebugEnabled()) {
                            log.debug("Removing platform because the " +
                                      "current user doesn't have the " +
                                      "'modifyPlatform' permission." +
                                      " PlatformID=" + pValue.getId());
                        }
                        iter.remove();
                    }
                }
            }
            
            // Do paging here
            if (showPlaceholders) {
                results = aiplatformPager.seek(queue, 
                                               pc.getPagenum(), 
                                               pc.getPagesize());
            } else {
                results = aiplatformPager_noplaceholders.seek(queue, 
                                                              pc.getPagenum(), 
                                                              pc.getPagesize());
            }

        } catch (Exception e) {
            throw new SystemException(e);
        }

        return results; 
    }

    /**
     * Get details of a single ai platform by aiplatformID.
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public AIPlatformValue findAIPlatformById(AuthzSubjectValue subject,
                                              int aiplatformID)
        throws NamingException, CreateException, 
               FinderException, RemoveException {

        AIPlatform aiplatform;
        AIPlatformValue aiplatformValue;

        aiplatform = getAIPlatformDAO().findById(new Integer(aiplatformID));
        aiplatformValue = aiplatform.getAIPlatformValue();

        aiplatformValue = syncQueue(aiplatformValue, false);

        return aiplatformValue;
    }

    /**
     * Get details of a single ai platform by FQDN.
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public AIPlatformValue findAIPlatformByFqdn(AuthzSubjectValue subject,
                                                String fqdn)
        throws NamingException, CreateException, FinderException,
               RemoveException
    {
        Collection aiplatforms;
        AIPlatform aiplatform = null;
        AIPlatformValue aiplatformValue = null;

        // XXX Do authz check
        aiplatforms = getAIPlatformDAO().findByFQDN(fqdn);

        Iterator i = aiplatforms.iterator();
        while (i.hasNext()) {
            if (aiplatform != null) {
                throw new SystemException("Multiple platforms matched fqdn.");
            }
            aiplatform = (AIPlatform) i.next();
            aiplatformValue = aiplatform.getAIPlatformValue();
        }

        if (aiplatformValue == null)
            return null;
                        
        aiplatformValue = syncQueue(aiplatformValue, false);
        return aiplatformValue;
    }

    /**
     * Get details of a single ai server by serverID.
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public AIServerValue findAIServerById(AuthzSubjectValue subject,
                                          int serverID)
        throws FinderException {

        AIServer aiserver;
        AIServerValue aiserverValue;

        aiserver =
            getAIServerDAO().findById(new Integer(serverID));
        aiserverValue = aiserver.getAIServerValue();
        return aiserverValue;
    }

    /**
     * Get details of a single ai server by Name.
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public AIServerValue findAIServerByName(AuthzSubjectValue subject,
                                            String name )
        throws FinderException {

        // XXX Do authz check
        AIServer aiserver = getAIServerDAO().findByName(name);
        if (aiserver == null) {
            throw new FinderException("can't find server:" + name);
        }

        AIServerValue aiserverValue = aiserver.getAIServerValue();
        return aiserverValue;
    }

    /**
     * Get details of a single ai ip by ipID.
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public AIIpValue findAIIpById(AuthzSubjectValue subject,
                                  int ipID )
        throws FinderException {

        AIIp aiip = getAIIpDAO().findById(new Integer(ipID));
        AIIpValue aiipValue = aiip.getAIIpValue();
        return aiipValue;
    }

    /**
     * Get details of a single ai ip by Address.
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public AIIpValue findAIIpByAddress(AuthzSubjectValue subject,
                                       String address)
        throws FinderException {

        // XXX Do authz check
        List aiips = getAIIpDAO().findByAddress(address);
        if (aiips.size() == 0) {
            throw new FinderException("Can't find ip: " + address);
        }

        AIIpValue aiipValue = ((AIIp) aiips.get(0)).getAIIpValue();
        return aiipValue;
    }

    /**
     * Process resources in the AI queue.  This can be used to
     * approve resources for inclusion into appdef, to ignore or unignore
     * resources in the queue, or to purge resources from the queue.
     * @param platformList A List of aiplatform IDs.  This may be
     * null, in which case it is ignored.
     * @param ipList A List of aiip IDs.  This may be
     * null, in which case it is ignored.
     * @param serverList A List of aiserver IDs.  This may be
     * null, in which case it is ignored.
     * @param action One of the AIQueueConstants.Q_DECISION_XXX constants
     * indicating what to do with the platforms, ips and servers.
     *
     * @ejb:interface-method
     * @ejb:transaction type="Required"
     */
    public void processQueue(AuthzSubjectValue subject,
                             List platformList,
                             List serverList,
                             List ipList,
                             int action)
        throws CreateException, FinderException, NamingException,
               PermissionException, ValidationException,
               RemoveException, AIQApprovalException 
    {
        AuthzSubject s = 
            AuthzSubjectManagerEJBImpl.getOne().findSubjectById(subject.getId());
        boolean approved = false;
        
        try {
            if (action == AIQueueConstants.Q_DECISION_APPROVE) {
                approved = true;
                AuditManagerEJBImpl.getOne()
                                   .pushContainer(AIAudit.newImportAudit(s));
            }
            _processQueue(subject, platformList, serverList, ipList, action);
        } finally {
            if (approved)
                AuditManagerEJBImpl.getOne().popContainer(false);
        }
    } 
    
    private void _processQueue(AuthzSubjectValue subject,
                               List platformList,
                               List serverList,
                               List ipList,
                               int action)
        throws CreateException, FinderException, NamingException,
               PermissionException, ValidationException,
               RemoveException, AIQApprovalException
    { 
        boolean isApproveAction
            = (action == AIQueueConstants.Q_DECISION_APPROVE);
        boolean isPurgeAction
            = (action == AIQueueConstants.Q_DECISION_PURGE);
        int i;
        Iterator iter;
        Integer id;

        Map aiplatformsToResync = new HashMap();
        List aiserversToRemove = new ArrayList();
        Object marker = new Object();

        AIPlatformDAO aiplatformLH = getAIPlatformDAO();
        AIIpDAO aiipLH = getAIIpDAO();
        AIServerDAO aiserverLH = getAIServerDAO();

        PlatformManagerLocal pmLocal = getPlatformMgrLocal();
        ServerManagerLocal smLocal = getServerMgrLocal();
        ConfigManagerLocal configMgr = getConfigMgrLocal();
        CPropManagerLocal cpropMgr = getCPropMgrLocal();

        AIPlatform aiplatform = null;
        AIServer aiserver = null;
        AIIp aiip = null;
        List createdResources = new ArrayList();

        // Create our visitor based on the action
        AIQResourceVisitor visitor
            = AIQResourceVisitorFactory.getVisitor(action);

        if (platformList != null) {
            for ( i=0; i<platformList.size(); i++ ) {
                id = (Integer) platformList.get(i);
                if (id == null) {
                    log.error("processQueue: platform with ID=null");
                    continue;
                }
                try {
                    aiplatform =
                        aiplatformLH.findById(id);
                } catch ( ObjectNotFoundException e ) {
                    if (isPurgeAction) continue;
                    else throw e;
                }
                visitor.visitPlatform(aiplatform, 
                                      subject,
                                      pmLocal, configMgr, cpropMgr,
                                      createdResources);
                if (!isPurgeAction) aiplatformsToResync.put(id, marker);
            }
        }
        if (ipList != null) {
            for (i=0; i<ipList.size(); i++) {
                id = (Integer) ipList.get(i);
                if (id == null) {
                    log.error("processQueue: " + aiplatform.getName() +
                              " has an IP with ID=null");
                    continue;
                }
                try {
                    aiip = aiipLH.findById(id);
                } catch (ObjectNotFoundException e) {
                    if (isPurgeAction) continue;
                    else throw e;
                }
                visitor.visitIp(aiip, subject, pmLocal);
                if (!isPurgeAction) {
                    Integer pk =
                       aiip.getAIPlatform().getId();
                    aiplatformsToResync.put(pk, marker);
                }
            }
        }
        if (serverList != null) {
            for ( i=0; i<serverList.size(); i++ ) {
                id = (Integer) serverList.get(i);
                if (id == null) {
                    log.error("processQueue: " + aiplatform.getName() +
                              " has a Server with ID=null");
                    continue;
                }
                try {
                    aiserver = aiserverLH.findById(id);
                } catch (ObjectNotFoundException e) {
                    if (isPurgeAction) continue;
                    else throw e;
                }
                visitor.visitServer(aiserver, 
                                    subject,
                                    pmLocal, smLocal, configMgr,
                                    cpropMgr, createdResources);
                if (isApproveAction) {
                    // Approved servers are removed from the queue
                    aiserversToRemove.add(aiserver);
                } else if (!isPurgeAction) {
                    Integer pk =
                        aiserver.getAIPlatform().getId();
                    aiplatformsToResync.put(pk, marker);
                }
            }
        }

        // If the action was "approve", then resync queued platforms 
        // to appdef, now that appdef may have been updated.
        if (isApproveAction) {
            iter = aiplatformsToResync.keySet().iterator();
            while ( iter.hasNext() ) { 
                id = (Integer) iter.next();
                aiplatform =
                    aiplatformLH.findById(id);
                syncQueue(aiplatform.getAIPlatformValue(), isApproveAction);
            }
            
            if (aiplatform != null) {
                // See above note, now we remove approved servers from the queue
                Collection servers = aiplatform.getAIServers();
                if (servers != null) {
                    for (i=0; i<aiserversToRemove.size(); i++) {
                        servers.remove(aiserversToRemove.get(i));
                    }
                }
            }
        }
    }

    /**
     * Remove an AI platform from the queue.
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public void removeFromQueue(AIPlatform aiplatform ) throws RemoveException {
        // Remove the platform, this should recursively remove all queued 
        // servers and IPs
        DAOFactory.getDAOFactory().getAIPlatformDAO().remove(aiplatform);
    }

    /**
     * Find a platform given an AI platform id
     * @ejb:interface-method
     */
    public PlatformValue getPlatformByAI(AuthzSubjectValue subject,
                                         int aiPlatformID)
        throws FinderException, NamingException, CreateException,
               PermissionException,PlatformNotFoundException
    {
        AIPlatform aiplatform;

        // XXX Do authz check
        aiplatform = getAIPlatformDAO().findById(new Integer(aiPlatformID));
        return getPlatformByAI(subject, aiplatform);
    }

    /**
     * Find a platform given an AI platform 
     * @ejb:interface-method
     */
    public AIPlatformValue getAIPlatformByPlatformID(AuthzSubjectValue subject,
                                                     int platformID)
        throws FinderException, NamingException, PermissionException
    {
        Platform pLocal = getPlatformDAO().findById(new Integer(platformID));

        Collection ips = pLocal.getIps();
        // We can't use the FQDN to find a platform, because
        // the FQDN can change too easily.  Instead we use the
        // IP address now.  For now, if we get one IP address
        // match (and it isn't localhost), we assume that it is
        // the same platform.  In the future, we are probably going
        // to need to do better.
        for (Iterator i = ips.iterator(); i.hasNext(); ) {
            Ip qip = (Ip) i.next();
            
            String mac = qip.getMacAddress();
            
            if (mac != null && mac.length() > 0 &&
                !mac.equals(NetFlags.NULL_HWADDR)) {
                List addrs = getAIIpDAO().findByMACAddress(qip.getMacAddress());
                if (addrs.size() > 0) {
                    AIPlatform aiplatform =
                        ((AIIp) addrs.get(0)).getAIPlatform();
                    return aiplatform.getAIPlatformValue();
                }
            }

            String address = qip.getAddress();
            // XXX This is a hack that we need to get rid of
            // at some point.  The idea is simple.  Every platform
            // has the localhost address.  So, if we are looking
            // for a platform based on IP address, searching for
            // localhost doesn't give us any information.  Long
            // term, when we are trying to match all addresses,
            // this can go away.
            if ((address.equals(NetFlags.LOOPBACK_ADDRESS) ||
                 address.equals(NetFlags.ANY_ADDR)) && i.hasNext()) {
                continue;
            }

            List addrs = getAIIpDAO().findByAddress(address);
            if (addrs.size() > 0) {
                AIPlatform aiplatform = ((AIIp) addrs.get(0)).getAIPlatform();
                return aiplatform.getAIPlatformValue();
            }
        }

        return null;        
    }

    /**
     * Find an AI platform given an platform 
     * @ejb:interface-method
     */
    public PlatformValue getPlatformByAI(AuthzSubjectValue subject, 
                                         AIPlatform aipLocal)
        throws FinderException, CreateException, NamingException,
               PermissionException, PlatformNotFoundException
    {
        Collection ips;
        PlatformValue pValue;

        ips = aipLocal.getAIIps();
        // We can't use the FQDN to find a platform, because
        // the FQDN can change too easily.  Instead we use the
        // IP address now.  For now, if we get one IP address
        // match (and it isn't localhost), we assume that it is
        // the same platform.  In the future, we are probably going
        // to need to do better.
        PlatformManagerLocal pmLocal = getPlatformMgrLocal();
        for (Iterator i = ips.iterator(); i.hasNext(); ) {
            AIIp qip = (AIIp) i.next();
            
            String address = qip.getAddress();
            // XXX This is a hack that we need to get rid of
            // at some point.  The idea is simple.  Every platform
            // has the localhost address.  So, if we are looking
            // for a platform based on IP address, searching for
            // localhost doesn't give us any information.  Long
            // term, when we are trying to match all addresses,
            // this can go away.
            if (address.equals("127.0.0.1") && i.hasNext()) {
                continue;
            }
                
            PageList platforms = pmLocal.findPlatformsByIpAddr(subject, address,
                                                               null);
            if (!platforms.isEmpty()) {
                // If we got any platforms that match this
                // IP address, then we just take the first
                // one and assume that is the platform we
                // are looking for.  This should only fall
                // apart if we have multiple platforms defined
                // for the same IP address, which should be
                // a rarity.
                pValue = (PlatformValue)platforms.get(0);
                return pValue;
            }
        }
        throw new PlatformNotFoundException("platform not found for ai " +
                                            "platform: " +
                                            aipLocal.getId());
    }

    public static AIQueueManagerLocal getOne() {
        try {
            return AIQueueManagerUtil.getLocalHome().create();
        } catch (Exception e) {
            throw new SystemException(e);
        }
    }

    /**
     * Create an AI queue manager session bean.
     * @exception CreateException If an error occurs creating bean.
     */
    public void ejbCreate() throws CreateException {
        try {
            aiplatformPager = Pager.getPager(AIPLATFORM_PROCESSOR);
            aiplatformPager_noplaceholders = Pager.getPager(AIPLATFORM_PROCESSOR_NOPLACEHOLDERS);
        } catch ( Exception e ) {
            throw new CreateException("Could not create value pager:" + e);
        }
    }
    
    public void ejbRemove   () {}
    public void ejbActivate () {}
    public void ejbPassivate() {}

    protected AIPlatformDAO getAIPlatformDAO()
    {
        return DAOFactory.getDAOFactory().getAIPlatformDAO();
    }

    protected AIIpDAO getAIIpDAO()
    {
        return DAOFactory.getDAOFactory().getAIIpDAO();
    }
}
