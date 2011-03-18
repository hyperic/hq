package org.hyperic.hq.authz.server.session;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.common.VetoException;
import org.hyperic.hq.common.server.session.ResourceAuditFactory;
import org.hyperic.util.timer.StopWatch;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ResourceRemoverImpl implements ResourceRemover, ApplicationContextAware {

    private ApplicationContext applicationContext;
    private ResourceEdgeDAO edgeDao;
    private final Log log = LogFactory.getLog(ResourceRemoverImpl.class);
    private ResourceAuditFactory resourceAuditFactory;
    private ResourceDAO resourceDAO;

    @Autowired
    public ResourceRemoverImpl(ResourceEdgeDAO edgeDao, ResourceDAO resourceDAO,
                         ResourceAuditFactory resourceAuditFactory) {
        this.edgeDao = edgeDao;
        this.resourceDAO = resourceDAO;
        this.resourceAuditFactory = resourceAuditFactory;
    }

    public void removeEdges(Resource resource, ResourceRelation relation) {
        edgeDao.deleteEdges(resource, relation);
    }

    @SuppressWarnings("rawtypes")
    public void removeResource(AuthzSubject subject, Resource r) throws VetoException {
        if (r == null) {
            return;
        }
        applicationContext.publishEvent(new ResourceDeleteRequestedEvent(r));

        final long now = System.currentTimeMillis();
        resourceAuditFactory.deleteResource(resourceDAO.findById(AuthzConstants.authzHQSystem),
            subject, now, now);
        Collection groupBag = r.getGroupBag();
        if (groupBag != null) {
            groupBag.clear();
        }
        resourceDAO.remove(r);
    }
    
    public void removeResource(AuthzSubject subj, Resource r, boolean nullResourceType) {
        final boolean debug = log.isDebugEnabled();

        final StopWatch watch = new StopWatch();
        if (debug) {
            watch.markTimeBegin("removeResource.removeEdges");
        }
        // Delete the edges
        edgeDao.deleteEdges(r);
        if (debug) {
            watch.markTimeEnd("removeResource.removeEdges");
        }
        if (nullResourceType) {
            r.setResourceType(null);
        }
        final long now = System.currentTimeMillis();
        if (debug) {
            watch.markTimeBegin("removeResource.audit");
        }
        resourceAuditFactory.deleteResource(resourceDAO.findById(AuthzConstants.authzHQSystem),
            subj, now, now);
        if (debug) {
            watch.markTimeEnd("removeResource.audit");
            log.debug(watch);
        }
    }
    
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
