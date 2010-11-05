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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Expression;
import org.hibernate.criterion.Order;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityValue;
import org.hyperic.hq.appdef.shared.CPropKeyNotFoundException;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.dao.HibernateDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class CpropDAO
    extends HibernateDAO<Cprop> {

    private JdbcTemplate jdbcTemplate;
    private static final int CHUNKSIZE = 1000; // Max size for each row
    private static final String CPROP_TABLE = "EAM_CPROP";
    private static final String CPROPKEY_TABLE = "EAM_CPROP_KEY";
    private CpropKeyDAO cPropKeyDAO;

    @Autowired
    public CpropDAO(SessionFactory f, JdbcTemplate jdbcTemplate, CpropKeyDAO cPropKeyDAO) {
        super(Cprop.class, f);
        this.jdbcTemplate = jdbcTemplate;
        this.cPropKeyDAO = cPropKeyDAO;
    }

    @SuppressWarnings("unchecked")
    public List<Cprop> findByKeyName(CpropKey key, boolean asc) {
        Criteria c = createCriteria().add(Expression.eq("key", key)).addOrder(
            asc ? Order.asc("propValue") : Order.desc("propValue"));
        return c.list();
    }

    public void deleteValues(final int appdefType, final int id) {
        jdbcTemplate.update(new PreparedStatementCreator() {
            public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
                PreparedStatement stmt = con.prepareStatement("DELETE FROM " + CPROP_TABLE +
                                                              " WHERE keyid IN " +
                                                              "(SELECT id FROM " + CPROPKEY_TABLE +
                                                              " WHERE appdef_type = ?) " +
                                                              "AND appdef_id = ?");
                stmt.setInt(1, appdefType);
                stmt.setInt(2, id);
                return stmt;
            }
        });
    }

    public String setValue(final AppdefEntityID aID, int typeId, String key, String val)
        throws CPropKeyNotFoundException, AppdefEntityNotFoundException, PermissionException {

        CpropKey propKey = getKey(aID, typeId, key);

        Integer pk = propKey.getId();
        final int keyId = pk.intValue();

        // no need to grab the for update since we are in a transaction
        // and therefore automatically get a shared lock
        String sql = new StringBuilder().append("SELECT PROPVALUE FROM ").append(CPROP_TABLE)
            .append(" WHERE KEYID=").append(keyId).append(" AND APPDEF_ID=").append(aID.getID())
            .toString();

        List<String> oldvals = this.jdbcTemplate.queryForList(sql,String.class);
        
        String oldval = null;
        if(! oldvals.isEmpty()) {
            oldval = oldvals.get(0);
        }

        if (val == null && oldval == null) {
            //We don't need to change anything here
            return null;
        } else if (val != null && val.equals(oldval)) {
            return val;
        }

        if (oldval != null) {
            String deleteSql = new StringBuilder().append("DELETE FROM ").append(CPROP_TABLE)
                .append(" WHERE KEYID=").append(keyId).append(" AND APPDEF_ID=")
                .append(aID.getID()).toString();
            jdbcTemplate.update(deleteSql);
        }

        // Optionally add new values
        if (val != null) {
            final String[] chunks = chunk(val, CHUNKSIZE);

            StringBuilder insertSQL = new StringBuilder().append("INSERT INTO ")
                .append(CPROP_TABLE);

            final Cprop nprop = new Cprop();

            insertSQL.append(" (id,keyid,appdef_id,value_idx,PROPVALUE) VALUES ").append(
                "(?, ?, ?, ?, ?)");

            jdbcTemplate.batchUpdate(insertSQL.toString(), new BatchPreparedStatementSetter() {

                public void setValues(PreparedStatement pstmt, int i) throws SQLException {
                    pstmt.setInt(2, keyId);
                    pstmt.setInt(3, aID.getID());
                    int id = generateId("org.hyperic.hq.appdef.server.session.Cprop", nprop)
                        .intValue();
                    pstmt.setInt(1, id);
                    pstmt.setInt(4, i);
                    pstmt.setString(5, chunks[i]);
                }

                public int getBatchSize() {
                    return chunks.length;
                }
            });
        }
        return oldval;
    }
    
    /**
     * Generate a new ID for a class of the given type.
     * 
     * @param className the persisted class name, as per the .hbm descriptor:
     *                  e.g. org.hyperic.hq.appdef.server.session.CpropKey
     * @param o         The object which will be getting the new ID
     * 
     * @return an Integer id for the new object.  If your class uses Long IDs
     *         then that's too bad ... we'll have to write another method.
     */
    private Integer generateId(String className, Object o) {
       
        IdentifierGenerator gen = getIdentifierGenerator(className); 
        SessionImplementor sessImpl = (SessionImplementor)
            getSession();
        return (Integer)gen.generate(sessImpl, o);
    }

    public String getValue(AppdefEntityValue aVal, String key) throws CPropKeyNotFoundException,
        AppdefEntityNotFoundException, PermissionException {

        final AppdefEntityID aID = aVal.getID();
        AppdefResourceType recType = aVal.getAppdefResourceType();
        int typeId = recType.getId().intValue();
        CpropKey propKey = this.getKey(aID, typeId, key);

        Integer pk = propKey.getId();
        final int keyId = pk.intValue();
        StringBuffer buf = new StringBuffer();

        List<String> propvals = this.jdbcTemplate.query(new PreparedStatementCreator() {
            public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
                PreparedStatement stmt = con.prepareStatement("SELECT PROPVALUE FROM " +
                                                              CPROP_TABLE +
                                                              " WHERE KEYID=? AND APPDEF_ID=? " +
                                                              "ORDER BY VALUE_IDX");

                stmt.setInt(1, keyId);
                stmt.setInt(2, aID.getID());
                return stmt;
            }
        }, new RowMapper<String>() {
            public String mapRow(ResultSet rs, int rowNum) throws SQLException {
                return rs.getString(1);
            }
        });

        if (propvals.isEmpty()) {
            return null;
        }
        for (String propval : propvals) {
            buf.append(propval);
        }

        return buf.toString();
    }

    public Properties getEntries(AppdefEntityID aID, String column) {
        Properties res = new Properties();
       
        List<Map<String, Object>> props = jdbcTemplate.queryForList("SELECT A." + column + ", B.propvalue FROM " + CPROPKEY_TABLE +
            " A, " + CPROP_TABLE + " B WHERE " +
            "B.keyid=A.id AND A.appdef_type=? " + "AND B.appdef_id=? " +
            "ORDER BY B.value_idx",aID.getType(),aID.getId());
  
        //Props share the same value_idx when chunked, so there is no guarantee that 
        //you don't end up with the ordered set being propA.chunk0,propB.chunk0,propA.chunk1
        Map<String,StringBuilder> propChunks = new HashMap<String,StringBuilder>();
        for(Map<String,Object> prop: props) {
            String keyName = (String)prop.get(column);
            String valChunk = (String)prop.get("propvalue");
            StringBuilder fullVal = propChunks.get(keyName);
            if(fullVal == null) {
                fullVal =  new StringBuilder();
            }
            fullVal.append(valChunk);
            propChunks.put(keyName, fullVal);
        }
        for(Map.Entry<String,StringBuilder> propChunk :propChunks.entrySet()) {
            res.setProperty(propChunk.getKey(), propChunk.getValue().toString());
        }

        return res;
    }

    /**
     * Split a string into a list of same sized chunks, and a chunk of
     * potentially different size at the end, which contains the remainder.
     * 
     * e.g. chunk("11223", 2) -> { "11", "22", "3" }
     * 
     * @param src String to chunk
     * @param chunkSize The max size of any chunk
     * 
     * @return an array containing the chunked string
     */
    private String[] chunk(String src, int chunkSize) {
        String[] res;
        int strLen, nAlloc;

        if (chunkSize <= 0) {
            throw new IllegalArgumentException("chunkSize must be >= 1");
        }

        strLen = src.length();
        nAlloc = strLen / chunkSize;

        if ((strLen % chunkSize) != 0) {
            nAlloc++;
        }

        res = new String[nAlloc];

        for (int i = 0; i < nAlloc; i++) {
            int begIdx, endIdx;

            begIdx = i * chunkSize;
            endIdx = (i + 1) * chunkSize;
            if (endIdx > strLen)
                endIdx = strLen;

            res[i] = src.substring(begIdx, endIdx);
        }

        return res;
    }

    private CpropKey getKey(AppdefEntityID aID, int typeId, String key)
        throws CPropKeyNotFoundException, AppdefEntityNotFoundException, PermissionException {
        CpropKey res = cPropKeyDAO.findByKey(aID.getType(), typeId, key);

        if (res == null) {
            String msg = "Key, '" + key + "', does " + "not exist for aID=" + aID + ", typeId=" +
                         typeId;

            throw new CPropKeyNotFoundException(msg);
        }

        return res;
    }
}
