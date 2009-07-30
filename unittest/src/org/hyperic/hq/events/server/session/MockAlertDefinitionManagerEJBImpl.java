package org.hyperic.hq.events.server.session;

import java.util.Collection;
import java.util.List;
import java.util.SortedMap;

import javax.ejb.EJBException;
import javax.ejb.EJBLocalHome;
import javax.ejb.EJBLocalObject;
import javax.ejb.FinderException;
import javax.ejb.RemoveException;

import org.hyperic.hibernate.PageInfo;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.escalation.server.session.Escalation;
import org.hyperic.hq.events.ActionCreateException;
import org.hyperic.hq.events.AlertConditionCreateException;
import org.hyperic.hq.events.AlertDefinitionCreateException;
import org.hyperic.hq.events.AlertSeverity;
import org.hyperic.hq.events.shared.AlertConditionValue;
import org.hyperic.hq.events.shared.AlertDefinitionManagerLocal;
import org.hyperic.hq.events.shared.AlertDefinitionValue;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.jmock.core.Verifiable;

public class MockAlertDefinitionManagerEJBImpl
implements AlertDefinitionManagerLocal, Verifiable {

	public void cleanupAlertDefinitions(AppdefEntityID aeid) {
		// TODO Auto-generated method stub

	}

	public AlertDefinitionValue createAlertDefinition(AuthzSubject subj,
			AlertDefinitionValue a) throws AlertDefinitionCreateException,
			PermissionException {
		// TODO Auto-generated method stub
		return null;
	}

	public void deleteAlertDefinitions(AuthzSubject subj, Integer[] ids)
	throws RemoveException, PermissionException {
		// TODO Auto-generated method stub

	}

	public void deleteAlertDefinitions(AuthzSubject subj, AppdefEntityID aeid)
	throws RemoveException, PermissionException {
		// TODO Auto-generated method stub

	}

	public void disassociateResource(Resource r) {
		// TODO Auto-generated method stub

	}

	public boolean isEnabled(Integer id) {
        // TODO Auto-generated method stub
        return false;
    }

    public PageList findAlertDefinitionChildren(Integer id) {
		// TODO Auto-generated method stub
		return null;
	}

	public SortedMap findAlertDefinitionNames(AuthzSubject subj, AppdefEntityID id,
			Integer parentId) throws PermissionException {
		// TODO Auto-generated method stub
		return null;
	}

	public List findAlertDefinitions(AuthzSubject subj, AlertSeverity minSeverity,
			Boolean enabled, boolean excludeTypeBased, PageInfo info) {
		// TODO Auto-generated method stub
		return null;
	}

	public List findAlertDefinitions(AuthzSubject subject, AppdefEntityID id)
	throws PermissionException {
		// TODO Auto-generated method stub
		return null;
	}

	public PageList findAlertDefinitions(AuthzSubject subj, AppdefEntityID id,
			PageControl pc) throws PermissionException {
		// TODO Auto-generated method stub
		return null;
	}

	public List findAlertDefinitions(AuthzSubject subject, Resource prototype)
	throws PermissionException {
		// TODO Auto-generated method stub
		return null;
	}

	public PageList findAlertDefinitions(AuthzSubject subj,
			AppdefEntityTypeID aetid, PageControl pc) throws PermissionException {
		// TODO Auto-generated method stub
		return null;
	}

	public PageList findAllAlertDefinitions(AuthzSubject subj) {
		// TODO Auto-generated method stub
		return null;
	}

	public Integer findChildAlertDefinitionId(AppdefEntityID aeid, Integer pid,
			boolean allowStale) {
		// TODO Auto-generated method stub
		return null;
	}

	public PageList findChildAlertDefinitions(Integer id) {
		// TODO Auto-generated method stub
		return null;
	}

	public List findRelatedAlertDefinitions(AuthzSubject subj, Resource res) {
		// TODO Auto-generated method stub
		return null;
	}

	public List findTypeBasedDefinitions(AuthzSubject subj, Boolean enabled,
			PageInfo info) throws PermissionException {
		// TODO Auto-generated method stub
		return null;
	}

	public AlertDefinitionValue getById(AuthzSubject subj, Integer id)
	throws PermissionException {
		// TODO Auto-generated method stub
		return null;
	}

	public AlertDefinition getByIdAndCheck(AuthzSubject subj, Integer id)
	throws PermissionException {
		// TODO Auto-generated method stub
		return null;
	}

	public AlertDefinition getByIdNoCheck(Integer id) {
		// TODO Auto-generated method stub
		return null;
	}

	public AlertConditionValue[] getConditionsById(Integer id)
	throws FinderException {
		// TODO Auto-generated method stub
		return null;
	}

	public Object[] getEnabledAndTriggerId(Integer id) {
		// TODO Auto-generated method stub
		return null;
	}

	public Integer getIdFromTrigger(Integer tid) {
		// TODO Auto-generated method stub
		return null;
	}

	public String getNameById(Integer id) throws FinderException {
		// TODO Auto-generated method stub
		return null;
	}

	public Collection getUsing(Escalation e) {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isAlertDefined(AppdefEntityID id, Integer parentId) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean isAvailability(AlertDefinition def, boolean up) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean isResourceTypeAlertDefinition(Integer id) {
		// TODO Auto-generated method stub
		return false;
	}

	public void setEscalation(AuthzSubject subj, Integer defId, Integer escId)
	throws PermissionException {
		// TODO Auto-generated method stub

	}

        public int getActiveCount() {
            // TODO Auto-generated method stub
            return 0;
        }

	public void startup() {
		// TODO Auto-generated method stub

	}

	public AlertDefinitionValue updateAlertDefinition(AlertDefinitionValue adval)
	throws AlertConditionCreateException, ActionCreateException,
	RemoveException {
		// TODO Auto-generated method stub
		return null;
	}

	public void updateAlertDefinitionActiveStatus(AuthzSubject subj,
			AlertDefinition def, boolean activate) throws PermissionException {
		// TODO Auto-generated method stub

	}

	public void updateAlertDefinitionBasic(AuthzSubject subj, Integer id,
			String name, String desc, int priority, boolean activate)
	throws PermissionException {
		// TODO Auto-generated method stub

	}

	public boolean updateAlertDefinitionInternalEnable(AuthzSubject subj,
			AlertDefinition def, boolean enable) throws PermissionException {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean updateAlertDefinitionInternalEnable(AuthzSubject subj,
			Integer defId, boolean enable) throws FinderException,
			PermissionException {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean updateAlertDefinitionInternalEnableForceNewTxn(
			AuthzSubject subj, Integer defId, boolean enable)
	throws PermissionException {
		// TODO Auto-generated method stub
		return false;
	}

	public void updateAlertDefinitionsActiveStatus(AuthzSubject subj,
			Integer[] ids, boolean activate) throws PermissionException {
		// TODO Auto-generated method stub

	}

	public EJBLocalHome getEJBLocalHome() throws EJBException {
		// TODO Auto-generated method stub
		return null;
	}

	public Object getPrimaryKey() throws EJBException {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isIdentical(EJBLocalObject arg0) throws EJBException {
		// TODO Auto-generated method stub
		return false;
	}

	public void remove() throws RemoveException, EJBException {
		// TODO Auto-generated method stub

	}

	public void verify() {
		// TODO Auto-generated method stub

	}

    public AlertDefinition findAlertDefinitionById(Integer id) {
        // TODO Auto-generated method stub
        return null;
    }
}
