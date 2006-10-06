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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.ejb.CreateException;
import javax.ejb.FinderException;
import javax.ejb.RemoveException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityValue;
import org.hyperic.hq.appdef.shared.AppdefResourceTypeValue;
import org.hyperic.hq.appdef.shared.CPropChangeEvent;
import org.hyperic.hq.appdef.shared.CPropKeyExistsException;
import org.hyperic.hq.appdef.shared.CPropKeyLocal;
import org.hyperic.hq.appdef.shared.CPropKeyLocalHome;
import org.hyperic.hq.appdef.shared.CPropKeyNotFoundException;
import org.hyperic.hq.appdef.shared.CPropKeyPK;
import org.hyperic.hq.appdef.shared.CPropKeyUtil;
import org.hyperic.hq.appdef.shared.CPropKeyValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.common.util.Messenger;
import org.hyperic.hq.events.EventConstants;
import org.hyperic.hq.product.TypeInfo;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.EncodingException;
import org.hyperic.util.jdbc.DBUtil;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @ejb:bean name="CPropManager"
 *      jndi-name="ejb/appdef/CPropManager"
 *      local-jndi-name="LocalCPropManager"
 *      view-type="local"
 *      type="Stateless"
 * @ejb:util generate="physical"
 * @ejb:transaction type="REQUIRED"
 */
public class CPropManagerEJBImpl
    extends AppdefSessionUtil
    implements SessionBean
{
    private static final int    CHUNKSIZE      = 1000; // Max size for each row
    private static final String DATASOURCE     = HQConstants.DATASOURCE;
    private static final String CPROP_TABLE    = "EAM_CPROP";
    private static final String CPROPKEY_TABLE = "EAM_CPROP_KEY";
    private static final String CPROP_SEQUENCE = "EAM_CPROP_ID_SEQ";

    private CPropKeyLocalHome cpLocalHome;
    private InitialContext    initialContext;
    private Log               log = 
        LogFactory.getLog(CPropManagerEJBImpl.class.getName());

    private Messenger sender = new Messenger();
    
    /**
     * Get all the keys associated with an appdef resource type.
     * 
     * @param appdefType   One of AppdefEntityConstants.APPDEF_TYPE_*
     * @param appdefTypeId The ID of the appdef resource type
     *
     * @return a List of CPropKeyValue objects
     *
     * @ejb:interface-method
     */
    public List getKeys(int appdefType, int appdefTypeId){
        CPropKeyLocalHome cpHome;
        Collection keys;
        ArrayList res = new ArrayList();

        try {
            keys = this.getCPropKeyLocalHome().findByAppdefType(appdefType,
                                                                appdefTypeId);
        } catch(FinderException exc){
            return res;
        }

        for(Iterator i=keys.iterator(); i.hasNext(); ){
            CPropKeyLocal key = (CPropKeyLocal)i.next();

            res.add(key.getCPropKeyValue());
        }
        return res;
    }

    /**
     * Add a key to a resource type based on a TypeInfo object.
     *
     * @throw AppdefEntityNotFoundException if the appdef resource type
     *        that the key references could not be found
     * @throw CPropKeyExistsException if the key already exists
     * @ejb:interface-method
     */

    public void addKey(TypeInfo info, String key, String description)
        throws AppdefEntityNotFoundException, CPropKeyExistsException
    {
        AppdefResourceTypeValue typeValue = 
            this.findResourceType(info);

        CPropKeyValue cPropKey
            = new CPropKeyValue(null, typeValue.getAppdefTypeId(),
                                typeValue.getId().intValue(),
                                key, description);

        addKey(cPropKey);
    }

    /**
     * Add a key to a resource type.  The key's 'appdefType' and
     * 'appdefTypeId' fields are used to locate the resource -- if
     * that resource does not exist, an AppdefEntityNotFoundException
     * will be thrown.
     *
     * @param key Key to create
     * @throw AppdefEntityNotFoundException if the appdef resource type
     *        that the key references could not be found
     * @throw CPropKeyExistsException if the key already exists
     * @ejb:interface-method
     */
    public void addKey(CPropKeyValue key)
        throws AppdefEntityNotFoundException, CPropKeyExistsException
    {
        AppdefResourceTypeValue recValue;
        CPropKeyLocalHome cpHome;
        CPropKeyLocal cpKey;

        // Insure that the resource type exists
        recValue = this.findResourceType(key.getAppdefType(),
                                         key.getAppdefTypeId());
        cpHome = this.getCPropKeyLocalHome();

        try {
            cpKey = cpHome.findByKey(key.getAppdefType(), 
                                     key.getAppdefTypeId(), key.getKey());
        } catch(FinderException exc){
            cpKey = null;
        }

        if(cpKey != null){
            throw new CPropKeyExistsException("Key, '" + key.getKey() + "', " +
               "already exists for " + 
               AppdefEntityConstants.typeToString(recValue.getAppdefTypeId()) +
               " type, '" + recValue.getName() + "'");
        }

        try {
            cpHome.create(key.getAppdefType(), key.getAppdefTypeId(),
                          key.getKey(), key.getDescription());
        } catch(CreateException exc){
            throw new SystemException(exc);
        }
    }

    /**
     * Remove a key from a resource type.
     *
     * @param appdefType   One of AppdefEntityConstants.APPDEF_TYPE_*
     * @param appdefTypeId The ID of the resource type
     * @param key          Key to remove
     *
     * @throw CPropKeyNotFoundException if the CPropKey could not be found
     * @ejb:interface-method
     */
    public void deleteKey(int appdefType, int appdefTypeId, String key)
        throws CPropKeyNotFoundException
    {
        PreparedStatement stmt = null;
        CPropKeyLocalHome cpHome;
        CPropKeyLocal cpKey;
        Connection conn = null;
        int keyId;

        cpHome = this.getCPropKeyLocalHome();

        try {
            cpKey = cpHome.findByKey(appdefType, appdefTypeId, key);
        } catch(FinderException exc){
            throw new CPropKeyNotFoundException("Key, '" + key + "', does not"+
                             " exist for " +
                             AppdefEntityConstants.typeToString(appdefType) +
                             " " + appdefTypeId);
        }

        CPropKeyPK pk = (CPropKeyPK)cpKey.getPrimaryKey();
        keyId = pk.getId().intValue();

        try {
            cpKey.remove();
        } catch(RemoveException exc){
            throw new SystemException(exc);
        }

        try {
            conn = this.getDBConn();
            stmt = conn.prepareStatement("DELETE FROM " + CPROP_TABLE +
                                         " WHERE KEYID=?");
            stmt.setInt(1, keyId);
            stmt.executeUpdate();
        } catch(SQLException exc){
            this.log.error("Unable to delete CPropKey values: " +
                           exc.getMessage(), exc);
            throw new SystemException(exc);
        } finally {
            DBUtil.closeStatement("CPropManager", stmt);
            DBUtil.closeConnection("CPropManager", conn);
        }
    }

    /**
     * Set (or delete) a custom property for a resource.  If the 
     * property already exists, it will be overwritten.
     *
     * @param aID Appdef entity id to set the value for
     * @param typeId Resource type id
     * @param key  Key to associate the value with
     * @param val  Value to assicate with the key.  If the value is null,
     *             then the value will simply be removed.
     *
     * @throw CPropKeyNotFoundException if the key has not been created
     *        for the resource's associated type
     * @throw AppdefEntityNotFoundException if id for 'aVal' specifies
     *        a resource which does not exist
     * @ejb:interface-method
     */
    public void setValue(AppdefEntityID aID, int typeId, String key, String val)
        throws CPropKeyNotFoundException, AppdefEntityNotFoundException,
               PermissionException
    {
        PreparedStatement selStmt, delStmt, addStmt;
        CPropKeyLocalHome cpHome;
        CPropKeyLocal propKey;
        Connection conn = null;
        ResultSet rs = null;
        
        propKey = this.getKey(aID, typeId, key);

        selStmt = null;
        delStmt = null;
        addStmt = null;
        try {
            CPropKeyPK pk = (CPropKeyPK)propKey.getPrimaryKey();
            final int keyId = pk.getId().intValue();

            conn = this.getDBConn();
                                    
            // Lock the rows we want to trash
            selStmt = conn.prepareStatement("SELECT PROPVALUE FROM " +
                                            CPROP_TABLE +
                                            " WHERE KEYID=? AND APPDEF_ID=? " +
                                            "FOR UPDATE");
            selStmt.setInt(1, keyId);
            selStmt.setInt(2, aID.getID());
            rs = selStmt.executeQuery();
            
            String oldval = null;
            if (rs.next()) {
                // See if value has changed
                oldval = rs.getString(1);
                if (oldval.equals(val))
                    return;
            }

            if (oldval != null) {
                // Nuke the old values
                delStmt = conn.prepareStatement("DELETE FROM " + CPROP_TABLE + 
                                                " WHERE KEYID=? AND APPDEF_ID=?");
                delStmt.setInt(1, keyId);
                delStmt.setInt(2, aID.getID());
                delStmt.executeUpdate();
            }
            
            // Optionally add new values
            if(val != null){
                String[] chunks = chunk(val, CHUNKSIZE);
                StringBuffer sql = new StringBuffer()
                    .append("INSERT INTO ")
                    .append(CPROP_TABLE);
                if (DBUtil.isOracle(conn)) {
                    sql.append(" (id,keyid,appdef_id,value_idx,PROPVALUE) VALUES (")
                        .append(CPROP_SEQUENCE)
                        .append(".NEXTVAL,")
                        .append("?, ?, ?, ?)");
                } else {
                    // assume sequence is generated in the db layer
                    sql.append(" (keyid,appdef_id,value_idx,PROPVALUE) VALUES ")
                        .append("(?, ?, ?, ?)");
                }
                addStmt = conn.prepareStatement(sql.toString());
                addStmt.setInt(1, keyId);
                addStmt.setInt(2, aID.getID());
                for(int i=0; i<chunks.length; i++){
                    addStmt.setInt(3, i);
                    addStmt.setString(4, chunks[i]);

                    // According to the javadocs, executeQuery should
                    // never return null -- it has been for me under
                    // pointbase.
                    addStmt.executeUpdate();
                }
            }

            if (log.isDebugEnabled())
                log.debug("Entity " + aID.getAppdefKey() + " " + key +
                          " changed from " + oldval + " to " + val);
            
            // Send cprop value changed event
            CPropChangeEvent event =
                new CPropChangeEvent(aID, key, oldval, val);
            
            // Now publish the event
            sender.publishMessage(EventConstants.EVENTS_TOPIC, event);
        } catch(SQLException exc){
            this.log.error("Unable to update CPropKey values: " + 
                           exc.getMessage(), exc);
            throw new SystemException(exc);
        } finally {
            DBUtil.closeJDBCObjects("CPropManager", null, selStmt, rs);
            DBUtil.closeStatement("CPropManager", delStmt);
            DBUtil.closeStatement("CPropManager", addStmt);
            DBUtil.closeConnection("CPropManager", conn);
        }
    }

    /**
     * Get a custom property for a resource.  
     *
     * @param aVal Appdef entity to get the value for
     * @param key  Key of the value to get
     *
     * @return The value associated with 'key' if found, else null
     *
     * @throw CPropKeyNotFoundException if the key for the associated
     *        resource is not found
     * @throw AppdefEntityNotFoundException if the passed entity is
     *        not found
     *
     * @ejb:interface-method
     */
    public String getValue(AppdefEntityValue aVal, String key)
        throws CPropKeyNotFoundException, AppdefEntityNotFoundException,
               PermissionException
    {
        CPropKeyLocalHome cpHome;
        CPropKeyLocal propKey;
        PreparedStatement stmt = null;
        Connection conn = null;
        ResultSet rs = null;
        AppdefEntityID aID = aVal.getID();
        AppdefResourceTypeValue recType = aVal.getResourceTypeValue();
        int typeId  = recType.getId().intValue();
        
        propKey = this.getKey(aID, typeId, key);
        try {
            CPropKeyPK pk = (CPropKeyPK)propKey.getPrimaryKey();
            final int keyId = pk.getId().intValue();
            StringBuffer buf = new StringBuffer();
            boolean didSomething;

            conn = this.getDBConn();
            stmt = conn.prepareStatement("SELECT PROPVALUE FROM " + 
                                         CPROP_TABLE +
                                         " WHERE KEYID=? AND APPDEF_ID=? " +
                                         "ORDER BY VALUE_IDX");
            stmt.setInt(1, keyId);
            stmt.setInt(2, aID.getID());
            rs = stmt.executeQuery();
            
            didSomething = false;
            while(rs.next()){
                didSomething = true;
                buf.append(rs.getString(1));
            }

            if(didSomething)
                return buf.toString();
            else
                return null;
        } catch(SQLException exc){
            this.log.error("Unable to get CPropKey values: " +
                           exc.getMessage(), exc);
            throw new SystemException(exc);
        } finally {
            DBUtil.closeJDBCObjects("CPropManager", conn, stmt, rs);
        }
    }

    private Properties getEntries(AppdefEntityID aID, String column) {
        AppdefResourceTypeValue recType;
        PreparedStatement stmt = null;
        Connection conn = null;
        Properties res = new Properties();
        ResultSet rs = null;

        try {
            StringBuffer buf;
            String lastKey;

            conn = this.getDBConn();
            stmt = conn.prepareStatement("SELECT A." + column +
                                         ", B.propvalue FROM " + 
                                         CPROPKEY_TABLE + " A, " +
                                         CPROP_TABLE + " B WHERE " +
                                         "B.keyid=A.id AND A.appdef_type=? " +
                                         "AND B.appdef_id=? " + 
                                         "ORDER BY B.value_idx");
            stmt.setInt(1, aID.getType());
            stmt.setInt(2, aID.getID());
            rs = stmt.executeQuery();

            lastKey = null;
            buf     = null;
            while(rs.next()){
                String keyName = rs.getString(1);
                String valChunk = rs.getString(2);
                
                if(lastKey == null || lastKey.equals(keyName) == false){
                    if(lastKey != null){
                        res.setProperty(lastKey, buf.toString());
                    }

                    buf     = new StringBuffer();
                    lastKey = keyName;
                }
                
                buf.append(valChunk);
            }

            // Have one at the end to add
            if(buf != null && buf.length() != 0){
                res.setProperty(lastKey, buf.toString());
            }
        } catch(SQLException exc){
            this.log.error("Unable to get CPropKey values: " +
                           exc.getMessage(), exc);
            throw new SystemException(exc);
        } finally {
            DBUtil.closeJDBCObjects("CPropManager", conn, stmt, rs);
        }

        return res;
    }
    
    /**
     * Get a map which holds the keys & their associated values
     * for an appdef entity.
     *
     * @param aID Appdef entity id to get the custom properties for
     *
     * @return The properties stored for a specific entity ID. 
     *         An empty Properties object will be returned if there are
     *         no custom properties defined for the resource
     *
     * @ejb:interface-method
     */
    public Properties getEntries(AppdefEntityID aID)
        throws PermissionException, AppdefEntityNotFoundException
    {
        return getEntries(aID, "propkey");
    }

    /**
     * Get a map which holds the descriptions & their associated values
     * for an appdef entity.
     *
     * @param aID Appdef entity id to get the custom properties for
     *
     * @return The properties stored for a specific entity ID
     *
     * @ejb:interface-method
     */
    public Properties getDescEntries(AppdefEntityID aID)
        throws PermissionException, AppdefEntityNotFoundException
    {
        return getEntries(aID, "description");
    }

    /**
     * Set custom properties for a resource.  If the 
     * property already exists, it will be overwritten.
     *
     * @param aID Appdef entity id to set the value for
     * @param typeId Resource type id
     * @param data Encoded ConfigResponse
     *
     * @ejb:interface-method
     */
    public void setConfigResponse(AppdefEntityID aID,
                                  int typeId,
                                  byte[] data)
        throws PermissionException, AppdefEntityNotFoundException {

        if (data == null) {
            return;
        }

        ConfigResponse cprops;
        try {
            cprops = ConfigResponse.decode(data);
        } catch (EncodingException e) {
            throw new SystemException(e);
        }
        
        log.debug("cprops=" + cprops);
        log.debug("aID=" + aID.toString() + ", typeId=" + typeId);
        for (Iterator it=cprops.getKeys().iterator(); it.hasNext();) {
            String key = (String)it.next();
            String val = cprops.getValue(key);
            try {
                setValue(aID, typeId, key, val);
            } catch (CPropKeyNotFoundException e) {
                log.error(e.getMessage());
            }
        }
    }

    /**
     * Remove custom properties for a given resource.
     *
     * @ejb:interface-method
     */
    public void deleteValues(int appdefType, int id)
    {
        PreparedStatement stmt = null;
        Connection conn = null;

        try {
            conn = this.getDBConn();
            stmt = conn.prepareStatement("DELETE FROM " + CPROP_TABLE + " " +
                                         "WHERE ID IN " +
                                         "(SELECT prop.id " +
                                         "FROM EAM_CPROP prop, " +
                                         "EAM_CPROP_KEY key " +
                                         "WHERE prop.keyid = key.id " +
                                         "AND key.appdef_type = ? " +
                                         "AND prop.appdef_id = ?)");
            stmt.setInt(1, appdefType);
            stmt.setInt(2, id);
                                         
            stmt.executeUpdate();
        } catch(SQLException exc){
            this.log.error("Unable to delete CProp values: " +
                           exc.getMessage(), exc);
            throw new SystemException(exc);
        } finally {
            DBUtil.closeStatement("CPropManager", stmt);
            DBUtil.closeConnection("CPropManager", conn);
        }
    }

    private CPropKeyLocalHome getCPropKeyLocalHome(){
        if(this.cpLocalHome == null) {
            try {
                this.cpLocalHome = CPropKeyUtil.getLocalHome();
            } catch(NamingException exc){
                throw new SystemException(exc);
            }
        }
        return this.cpLocalHome;
    }

    private InitialContext getInitialContext(){
        if(this.initialContext == null){
            try {
                this.initialContext = new InitialContext();
            } catch(NamingException exc){
                throw new SystemException(exc);
            }
        }
        return this.initialContext;
    }

    private Connection getDBConn()
        throws SQLException
    {
        Connection conn;

        try {
            conn = DBUtil.getConnByContext(this.getInitialContext(), 
                                           DATASOURCE);
        } catch(NamingException exc){
            throw new SystemException("Unable to get database context: " +
                                         exc.getMessage(), exc);
        }

        return conn;
    }

    private CPropKeyLocal getKey(AppdefEntityID aID, int typeId, String key)
        throws CPropKeyNotFoundException, AppdefEntityNotFoundException,
               PermissionException
    {
        CPropKeyLocal res;

        try {
            res = this.getCPropKeyLocalHome().findByKey(aID.getType(), typeId, key);
        } catch(FinderException exc){
            res = null;
        }

        if(res == null){
            String msg =
                "Key, '" + key + "', does " +
                "not exist for aID=" + aID + ", typeId=" + typeId;
            throw new CPropKeyNotFoundException(msg);
        }
        return res;
    }

    public void ejbCreate() throws CreateException {}
    public void ejbRemove() {}
    public void ejbActivate() {}
    public void ejbPassivate() {}
    public void setSessionContext(SessionContext ctx) {}

    /**
     * Split a string into a list of same sized chunks, and 
     * a chunk of potentially different size at the end, 
     * which contains the remainder.
     *
     * e.g. chunk("11223", 2) -> { "11", "22", "3" }
     *
     * @param src       String to chunk
     * @param chunkSize The max size of any chunk
     *
     * @return an array containing the chunked string
     */
    private static String[] chunk(String src, int chunkSize){
        String[] res;
        int strLen, nAlloc;
    
        if(chunkSize <= 0){
            throw new IllegalArgumentException("chunkSize must be >= 1");
        }
    
        strLen = src.length();
        nAlloc = strLen / chunkSize;
        if((strLen % chunkSize) != 0)
            nAlloc++;
    
        res = new String[nAlloc];
        for(int i=0; i<nAlloc; i++){
            int begIdx, endIdx;
    
            begIdx = i * chunkSize;
            endIdx = (i + 1) * chunkSize;
            if(endIdx > strLen)
                endIdx = strLen;
    
            res[i] = src.substring(begIdx, endIdx);
        }
        return res;
    }
}
