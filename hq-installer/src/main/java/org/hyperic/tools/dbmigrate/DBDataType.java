/* **********************************************************************
/*
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2012], VMware, Inc.
 * This file is part of Hyperic.
 *
 * Hyperic is free software; you can redistribute it and/or modify
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
package org.hyperic.tools.dbmigrate;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

import org.postgresql.core.Oid;

public enum DBDataType {
    
    BLOB(new int[] { 2004, -2, -3, -4 }) { 
        
        @Override
        public final void serialize(final FileOutputStream ous, final ResultSet rs, final int columnIndex, final ValueHandlerType valueHandler) throws Exception{
            byte blobContent[] = rs.getBytes(columnIndex);
            ous.write(blobContent);
        }//EOM 
        
        @Override
        public final void bindStatementParam(final int columnIndex, final Object oValue, final PreparedStatement ps, final int iSqlDataType) throws Throwable  {
            
            final byte blobContent[] = (byte[]) oValue ; 
            if(blobContent == null) ps.setNull(columnIndex, 2004);
            else ps.setBytes(columnIndex, blobContent);
            
        }//EOM
        
    }, //EO BLOB
    INTEGER(new int[]{Types.INTEGER},Oid.INT4) { 
        
    },//EO INTEGER
    TINYINT(new int[]{Types.TINYINT, Types.SMALLINT},Oid.INT2) { 
        
    },//EO TINYINT
    BIGINT(new int[]{Types.BIGINT},Oid.INT8) { 
        
    },//EO BIGINT
    REAL(new int[]{Types.REAL},Oid.FLOAT4) { 
        
    },//REAL
    DOUBLE(new int[]{Types.DOUBLE, Types.FLOAT},Oid.FLOAT8) { 
    },//DOUBLE
    DECIMAL(new int[]{Types.DECIMAL, Types.NUMERIC},Oid.NUMERIC) { 
         
    },//EO DECIMAL
    CHAR(new int[]{Types.CHAR},Oid.BPCHAR) { 
         
    },//EO CHAR
    VARCHAR(new int[]{Types.VARCHAR, Types.LONGVARCHAR},Oid.VARCHAR) { 
        @Override
        protected void bindStatementParamInner(final int columnIndex, final Object oValue, final PreparedStatement ps, final int iSqlDataType) throws Throwable {
            ps.setString(columnIndex, (String)oValue) ;
        }//EOM
    },//EO VARCHAR
    BIT(new int[]{Types.BIT},Oid.BOOL){ 
        @Override
        protected void bindStatementParamInner(final int columnIndex, final Object oValue, final PreparedStatement ps, final int iSqlDataType) throws Throwable {
            ((org.postgresql.jdbc2.AbstractJdbc2Statement)ps).bindString(columnIndex, (String)oValue, this.oidDataType) ;
        }//EOM
    },//EO BIT
    DEFAULT(){
        @Override
        protected void bindStatementParamInner(final int columnIndex, final Object oValue, final PreparedStatement ps, final int iSqlDataType) throws Throwable {
            ps.setObject(columnIndex, oValue, iSqlDataType) ;
        }//EOM 
    };//EO DEFAULT 

    private static final Map<Integer, DBDataType> mapReverseValues;
    
    static {
        mapReverseValues = new HashMap<Integer,DBDataType>();

        for (DBDataType enumDBDataType : values()) {
            if (enumDBDataType.sqlDataTypes == null) continue;
            for (int sqlDataType : enumDBDataType.sqlDataTypes)
                mapReverseValues.put(sqlDataType, enumDBDataType);
        }//EO while there are more members 
    }//EO static block 
    
    private int[] sqlDataTypes;
    protected int oidDataType ; 

    private DBDataType() {}//EOM 

    private DBDataType(int[] dataTypes) {
        this.sqlDataTypes = dataTypes;
    }//EOM 
    
    private DBDataType(int[] dataTypes, int oidDataType) {
        this(dataTypes);
        this.oidDataType = oidDataType ;
    }//EOM 

    public void serialize(final FileOutputStream ous, final ResultSet rs, final int columnIndex, final ValueHandlerType valueHandler) throws Exception {
        String value = rs.getString(columnIndex) ; 
        if(value != null && valueHandler != null) value = valueHandler.handleValue(value) ; 
        ous.writeUTF(value) ; 
    }//EOM 

    public void bindStatementParam(final int columnIndex, final Object oValue, final PreparedStatement ps, final int iSqlDataType) throws Throwable {
        if (oValue == null) ps.setNull(columnIndex, iSqlDataType);
        else this.bindStatementParamInner(columnIndex, oValue, ps, iSqlDataType) ; 
    }//EOM
    
    protected void bindStatementParamInner(final int columnIndex, final Object oValue, final PreparedStatement ps, final int iSqlDataType) throws Throwable {
        ((org.postgresql.jdbc2.AbstractJdbc2Statement)ps).bindLiteral(columnIndex, (String)oValue, this.oidDataType) ;
    }//EOM
    
    public static final DBDataType reverseValueOf(final int iSqlDataType) {
        DBDataType enumDataType = (DBDataType) mapReverseValues.get(Integer.valueOf(iSqlDataType));
        return enumDataType == null ? DEFAULT : enumDataType;
    }//EOM 

}//EOC 
