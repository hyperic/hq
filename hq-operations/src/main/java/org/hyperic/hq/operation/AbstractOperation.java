package org.hyperic.hq.operation;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;

import java.io.Serializable;
import java.util.*;


public abstract class AbstractOperation implements OperationData, Serializable {

    private static final long serialVersionUID = 6991306796752066389L;

    private Map stringVals;

    private Map intVals;

    private Map doubleVals;

    private Map longVals;

    private Map byteaVals;

    private Map objectVals;

    private Map stringLists;

    private Map intLists;

    private Map doubleLists;

    private Map byteaLists;

    private Map objectLists;

    @JsonIgnore
    private boolean ensureOrder;

    @JsonProperty("operationName")
    private String operationName;


    /**
     * TODO add tag=
     * @return
     */
    @Override
    public String toString() {
        return new StringBuilder(this.operationName).append(this.stringVals).append(this.intVals).append(this.doubleVals).append(this.longVals)
                .append(this.byteaVals).append(this.objectVals).append(this.stringLists).append(this.intLists)
                .append(this.doubleLists).append(this.byteaLists).append(this.objectLists).append(this.ensureOrder).toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (this.toString() == null ? 0 : this.toString().hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj != null && getClass() == obj.getClass()) {
            AbstractOperation other = (AbstractOperation) obj;
            if (this.toString() != null && other.toString() != null) {
                return this.toString().equals(other.toString());
            }
        }
        return false;
    }

    public AbstractOperation() {
        this(false);
    }

    /**
     * Using this constructor guarantees that encoding the exact
     * same Object 2 different times generates the same byte
     * order, as the maps are traversed in the same order.
     * @param ensureOrder
     */
    public AbstractOperation(boolean ensureOrder) {
        this.setup(ensureOrder);
        this.operationName = this.getClass().getSimpleName();
    }

    public String getOperationName() {
        return operationName;
    }

    public boolean isEnsureOrder() {
        return ensureOrder;
    }

    public Map getByteaLists() {
        return byteaLists;
    }

    public Map getByteaVals() {
        return byteaVals;
    }

    /* Everything from here down is legacy and should be re-evaluated */
    
    /**
     * Get a map used for storing the different types of values (i.e.
     * setStringVals, etc.)
     */
    private Map createStorageMap() {
        return this.ensureOrder ? new TreeMap() : new HashMap();
    }

    private void setup(boolean ensureOrder) {
        this.ensureOrder = ensureOrder;

        this.stringVals = this.createStorageMap();
        this.intVals = this.createStorageMap();
        this.doubleVals = this.createStorageMap();
        this.longVals = this.createStorageMap();
        this.byteaVals = this.createStorageMap();
        this.objectVals = this.createStorageMap();

        this.stringLists = this.createStorageMap();
        this.intLists = this.createStorageMap();
        this.doubleLists = this.createStorageMap();
        this.byteaLists = this.createStorageMap();
        this.objectLists = this.createStorageMap();
    }

    private void checkArg(Object arg) {
        if (arg == null) {
            throw new IllegalArgumentException("Argument cannot be null");
        }
    }

    protected String getStringValue(String key) {
        String res;

        if ((res = (String) this.stringVals.get(key)) == null) {
            //throw new Exception(key);
        }

        return res;
    }

    protected void setStringValue(String key, String value) {
        this.checkArg(value);
        this.stringVals.put(key, value);
    }

    protected int getIntValue(String key) {
        Integer res;

        if ((res = (Integer) this.intVals.get(key)) == null) {
            //throw new Exception(key);
        }

        return res.intValue();
    }

    protected void setIntValue(String key, int value) {
        this.intVals.put(key, new Integer(value));
    }

    protected double getDoubleValue(String key) {
        Double res;

        if ((res = (Double) this.doubleVals.get(key)) == null) {
            //throw new Exception(key);
        }

        return res.doubleValue();
    }

    protected void setDoubleValue(String key, double value) {
        this.doubleVals.put(key, new Double(value));
    }

    protected long getLongValue(String key) {
        Long res;

        if ((res = (Long) this.longVals.get(key)) == null) {
            //throw new Exception(key);
        }

        return res.longValue();
    }

    protected void setLongValue(String key, long value) {
        this.longVals.put(key, new Long(value));
    }

    protected byte[] getByteAValue(String key) {
        byte[] res;

        if ((res = (byte[]) this.byteaVals.get(key)) == null) {
            //throw new Exception(key);
        }

        return res;
    }

    protected void setObjectValue(String key, AbstractOperation value) {
        this.checkArg(value);
        this.objectVals.put(key, value);
    }

    protected AbstractOperation getObjectValue(String key) {
        AbstractOperation res;

        if ((res = (AbstractOperation) this.objectVals.get(key)) == null) {
            //throw new Exception(key);
        }

        return res;
    }

    protected void setByteAValue(String key, byte[] value) {
        this.checkArg(value);
        this.byteaVals.put(key, value);
    }

    private List getListValueForAdd(Map map, String listName) {
        List res = (List) map.get(listName);

        if (res == null) {
            res = new ArrayList();
            map.put(listName, res);
        }

        return res;
    }

    private List getListValueForGet(Map map, String listName) {
        List res = (List) map.get(listName);

        if (res == null) {
            //throw new Exception(listName);
        }

        return res;
    }

    protected void addStringToList(String listName, String value) {
        List list = this.getListValueForAdd(this.stringLists, listName);

        this.checkArg(value);
        list.add(value);
    }

    protected String[] getStringList(String listName) {
        List list = this.getListValueForGet(this.stringLists, listName);

        return (String[]) list.toArray(new String[0]);
    }

    protected void addIntToList(String listName, int value) {
        List list = this.getListValueForAdd(this.intLists, listName);

        list.add(new Integer(value));
    }

    protected int[] getIntList(String listName) {
        List list = this.getListValueForGet(this.intLists, listName);
        int[] res;
        int idx;

        res = new int[list.size()];
        idx = 0;
        for (Iterator i = list.iterator(); i.hasNext();) {
            Integer val = (Integer) i.next();

            res[idx++] = val.intValue();
        }

        return res;
    }

    protected void addDoubleToList(String listName, double value) {
        List list = this.getListValueForAdd(this.doubleLists, listName);

        list.add(new Double(value));
    }

    protected double[] getDoubleList(String listName) {
        List list = this.getListValueForGet(this.doubleLists, listName);
        double[] res;
        int idx;

        res = new double[list.size()];
        idx = 0;
        for (Iterator i = list.iterator(); i.hasNext();) {
            Double val = (Double) i.next();

            res[idx++] = val.doubleValue();
        }

        return res;
    }

    protected void addByteAToList(String listName, byte[] value) {
        List list = this.getListValueForAdd(this.byteaLists, listName);

        this.checkArg(value);
        list.add(value);
    }

    protected byte[][] getByteAList(String listName) {
        List list = this.getListValueForGet(this.byteaLists, listName);

        return (byte[][]) list.toArray(new byte[0][]);
    }

    protected void addObjectToList(String listName, AbstractOperation value) {
        List list = this.getListValueForAdd(this.objectLists, listName);

        this.checkArg(value);
        list.add(value);
    }

    protected Object[] getObjectList(String listName) {
        List list = this.getListValueForGet(this.objectLists, listName);

        return (Object[]) list.toArray(new Object[0]);
    }

    public Map getStringVals() {
        return this.stringVals;
    }

    public Map getIntVals() {
        return this.intVals;
    }

    public Map getDoubleVals() {
        return this.doubleVals;
    }

    public Map getLongVals() {
        return this.longVals;
    }

    public Map getByteAVals() {
        return this.byteaVals;
    }

    public Map getObjectVals() {
        return this.objectVals;
    }

    public Map getStringLists() {
        return this.stringLists;
    }

    public Map getIntLists() {
        return this.intLists;
    }

    public Map getDoubleLists() {
        return this.doubleLists;
    }

    public Map getByteALists() {
        return this.byteaLists;
    }

    public Map getObjectLists() {
        return this.objectLists;
    }

    /**
     * This method is called to verify that an Operation object
     * has all the appropriate contents.  It is invoked after decode.
     * @throws IllegalStateException if the obj state is not valid.
     */
    @JsonIgnore
    public abstract void validate() throws IllegalStateException;
}
