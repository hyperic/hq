package org.hyperic.hq.measurement.server.session;

import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.measurement.shared.DataManager_testUtil;
import org.hyperic.hq.measurement.shared.DataManager_testLocal;
import org.hyperic.hq.measurement.shared.DataManagerLocal;

import javax.ejb.SessionBean;
import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.SessionContext;
import java.rmi.RemoteException;
import java.util.List;
import java.util.ArrayList;

/**
 * The session bean implementing the in-container unit tests for the
 * DataManager.
 *
 * @ejb:bean name="DataManager_test"
 *      jndi-name="ejb/measurement/DataManager_test"
 *      local-jndi-name="DataManager_test"
 *      view-type="local"
 *      type="Stateless"
 *
 * @ejb:util generate="physical"
 * @ejb:transaction type="SUPPORTS"
 */
public class DataManager_testEJBImpl implements SessionBean {

    public static DataManager_testLocal getOne() {
        try {
            return DataManager_testUtil.getLocalHome().create();
        } catch(Exception e) {
            throw new SystemException(e);
        }
    }

    final double INSERT_VAL = 1;
    final double UPDATE_VAL = 2;
    /**
     * Test inserting duplicate data points ensuring they are updated.
     *
     * @ejb:interface-method
     */
    public void testDuplicateData() throws Exception {

        DataManagerLocal dman = DataManagerEJBImpl.getOne();

        List data = new ArrayList();
        long ts = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            DataPoint dp = new DataPoint(i, INSERT_VAL, ts);
            data.add(dp);
        }

        dman.addData(data);

        data.clear();
        for (int i = 0; i < 100; i++) {
            DataPoint dp = new DataPoint(i, UPDATE_VAL, ts);
            data.add(dp);
        }

        dman.addData(data);
    }

    public void ejbCreate() throws CreateException {}
    public void ejbActivate() throws EJBException, RemoteException {}
    public void ejbPassivate() throws EJBException, RemoteException {}
    public void ejbRemove() throws EJBException, RemoteException {}
    public void setSessionContext(SessionContext arg0) {}
}
