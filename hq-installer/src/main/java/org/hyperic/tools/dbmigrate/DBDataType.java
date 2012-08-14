package org.hyperic.tools.dbmigrate;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

public enum DBDataType {
    
    BLOB(new int[] { 2004, -2, -3, -4 }) { 
        
        public final void serialize(final ObjectOutputStream ous, final ResultSet rs, final int columnIndex) throws Exception{
            byte blobContent[] = rs.getBytes(columnIndex);
            ous.writeObject(blobContent);
        }//EOM 

        /*public final void bindStatementParam(final int columnIndex, final ObjectInputStream ois, final PreparedStatement ps, 
                final int iSqlDataType) throws Throwable  {
            
            final byte blobContent[] = (byte[])(byte[])ois.readObject();
            if(blobContent == null) ps.setNull(columnIndex, 2004);
            else ps.setBytes(columnIndex, blobContent);
            
        }//EOM 
*/        
        @Override
        public final void bindStatementParam(final int columnIndex, final Object oValue, final PreparedStatement ps, final int iSqlDataType) throws Throwable  {
            
            final byte blobContent[] = (byte[]) oValue ; 
            if(blobContent == null) ps.setNull(columnIndex, 2004);
            else ps.setBytes(columnIndex, blobContent);
            
        }//EOM
        
    }, //EO BLOB
    VARCHAR(new int[] { 12 }){
    }, //EO VARCAHR
    DEFAULT{
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

    private DBDataType() {}//EOM 

    private DBDataType(int[] dataTypes) {
        this.sqlDataTypes = dataTypes;
    }//EOM 

    public void serialize(final ObjectOutputStream ous, final ResultSet rs, final int columnIndex) throws Exception {
        ous.writeObject(rs.getObject(columnIndex));
    }//EOM 

    /*public void bindStatementParam(final int columnIndex, final ObjectInputStream ois, final PreparedStatement ps, 
            final int iSqlDataType) throws Throwable {
        
        final Object oValue = ois.readObject();
        if (oValue == null) ps.setNull(columnIndex, iSqlDataType);
        else ps.setObject(columnIndex, oValue, iSqlDataType);
    }//EOM 
*/
    
    public void bindStatementParam(final int columnIndex, final Object oValue, final PreparedStatement ps, 
            final int iSqlDataType) throws Throwable {
        if (oValue == null) ps.setNull(columnIndex, iSqlDataType);
        else ps.setObject(columnIndex, oValue, iSqlDataType);
    }//EOM
    
    public static final DBDataType reverseValueOf(final int iSqlDataType) {
        DBDataType enumDataType = (DBDataType) mapReverseValues.get(Integer.valueOf(iSqlDataType));
        return enumDataType == null ? DEFAULT : enumDataType;
    }//EOM 

}//EOC 
