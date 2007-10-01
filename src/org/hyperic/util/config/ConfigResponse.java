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

package org.hyperic.util.config;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;

import org.hyperic.util.GenericValueMap;
import org.hyperic.util.StringUtil;

public class ConfigResponse implements GenericValueMap, Serializable  {

            Map          attributes;
    private Map          schemaOptionsMap;
    private ConfigSchema schema = null;

    /**
     * Empty, encoded ConfigResponse.
     */
    public static final byte[] EMPTY_CONFIG;

    static {
        try {
            EMPTY_CONFIG = new ConfigResponse().encode();
        } catch (EncodingException e) {
            //aint gonna happen.
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    /**
     * Create a ConfigResponse that will be validated against 
     * the specified schema.  Any default values in the Schema will
     * be set in the ConfigResponse
     *
     * @param schema The schema to validate option settings against.
     * If this is null then no schema validation will be done, this is
     * equivalent to using the no-argument constructor.
     */
    public ConfigResponse ( ConfigSchema schema ) {

        this.attributes = new HashMap();
        this.schema = schema;

        if ( this.schema != null ) {

            this.schemaOptionsMap = new HashMap();
            List options = schema.getOptions();
            ConfigOption opt = null;

            for ( int i=0; i<options.size(); i++ ) {
                opt = (ConfigOption) options.get(i);
                this.schemaOptionsMap.put(opt.getName(), opt);
                
                // Fill in any default values
                String def = opt.getDefault();
                if (def != null) {
                    this.attributes.put(opt.getName(), def);
                }
            }
        }
    }
    
    /**
     * Create a ConfigResponse that will
     * not be validated against any schema.
     */
    public ConfigResponse () {
        this.attributes = new HashMap();
        this.schema = null;
    }

    /**
     * Create a ConfigResponse that will not be validated
     * against any schema.
     * @param attributes The config properties normally
     * populated by setValue().
     */
    public ConfigResponse (Map attributes) {
        this.attributes = attributes;
        this.schema = null;
    }

    public void setValue(String key, boolean value) {
        setValue(key, String.valueOf(value));
    }

    public void setValue(String key, int value) {
        setValue(key, String.valueOf(value));
    }

    public void setValue(String key, long value) {
        setValue(key, String.valueOf(value));
    }

    /**
     * Set the value for an option.
     * @param key The name of the option to set
     * @param value The value to set the option to.
     * @exception InvalidOptionException If this ConfigResponse does
     * not support the specified option.
     * @exception InvalidOptionValueException If the value supplied
     * is not a legal/valid value for the option.
     */
    public void setValue(String key, String value) 
        throws InvalidOptionException, InvalidOptionValueException {
        
        if ( this.schema != null ) {
            
            ConfigOption option = (ConfigOption) schemaOptionsMap.get(key);
            if ( option == null ) throw new InvalidOptionException(key);

            option.checkOptionIsValid(value);
        }
        this.attributes.put(key, value);
    }

    /**
     * Set the value for an option.
     * @param key The name of the option to set
     * @exception InvalidOptionException If this ConfigResponse does
     * not support the specified option.
     * @exception InvalidOptionValueException If the value supplied
     * is not a legal/valid value for the option.
     */
    public void unsetValue(String key) 
        throws InvalidOptionException, InvalidOptionValueException {
        
        if ( this.schema != null ) {
            
            ConfigOption option = (ConfigOption) schemaOptionsMap.get(key);
            if ( option == null )
                throw new InvalidOptionException(key);
        }

        this.attributes.remove(key);
    }

    public String getValue(String key){
        return (String)this.attributes.get(key);
    }

    public String getValue(String key, String defaultValue){
        String val = (String)this.attributes.get(key);
        return (val != null) ? val : defaultValue;
    }

    public Set getKeys() {
        return this.attributes.keySet();
    }

    /**
     * Decode a ConfigResponse from a byte array with
     * no schema validation.
     * @param data The response data to decode.
     * @exception EncodingException If the encoding is incorrect.
     */
    public static ConfigResponse decode(byte[] data)
        throws EncodingException
    {
        try {
            return decode(null, data);

        } catch ( InvalidOptionException ioe ) {
            // XXX should never happen
            throw new EncodingException(ioe);

        } catch ( InvalidOptionValueException iove ) {
            throw new EncodingException(iove);
        }
    }
    /**
     * Decode a ConfigResponse from a byte array according
     * to the specified schema.
     * @param schema The schema to validate against.
     * @param data The response data to decode.
     * @exception EncodingException If the encoding is incorrect.
     * @exception InvalidOptionException If the data specifies as option
     * that is not valid for the given schema.
     * @exception InvalidOptionValueException If the data specifies
     * an illegal/invalid value for one of the options it contains.
     */
    public static ConfigResponse decode(ConfigSchema schema, byte[] data)
        throws EncodingException, 
               InvalidOptionException, 
               InvalidOptionValueException
    {
        ObjectInputStream objectStream;
        ByteArrayInputStream byteStream;
        ConfigResponse res;
        
        try {
            byteStream   = new ByteArrayInputStream(data);
            objectStream = new ObjectInputStream(byteStream);
            res          = new ConfigResponse(schema);
            
            // Read attributes
            while(true){
                String key, val;
                
                if((key = (String)objectStream.readObject()) == null)
                    break;
                
                val = (String)objectStream.readObject();
                res.setValue(key, val);
            }
            return res;
        } catch(IOException exc){
            throw new EncodingException(exc.toString());
        } catch(ClassNotFoundException exc){
            throw new EncodingException(exc.toString());
        }
    }

    /**
     * @return the encoded config, or null if the ConfigResponse passed in 
     * is null.  This is a handy method to avoid NPEs.
     */
    public static byte[] safeEncode(ConfigResponse cr) throws EncodingException{
        if (cr == null) return null;
        return cr.encode();
    }

    public byte[] encode()
        throws EncodingException
    {
        ObjectOutputStream objectStream;

        objectStream = null;
        byte[] retVal = null;
        try {
            ByteArrayOutputStream byteStream;
            Iterator i;
            Set keys;

            byteStream   = new ByteArrayOutputStream();
            objectStream = new ObjectOutputStream(byteStream);
            
            keys = this.attributes.keySet();
            i = keys.iterator();
            while(i.hasNext()){
                String key = (String) i.next();
                
                objectStream.writeObject(key);
                objectStream.writeObject((String)this.attributes.get(key));
            }
            objectStream.writeObject(null);
            objectStream.flush();
            retVal = byteStream.toByteArray();
        } catch(IOException exc){
            throw new EncodingException(exc.toString());
        } finally {
            // ObjectStreams MUST be closed.
            if (objectStream != null )
                try {objectStream.close();}
                catch(Exception ex){}
        }
        return retVal;
    }

    /**
     * Merge the values from another ConfigResponse into this object.
     *
     * @param other     Other ConfigResponse to merge data from
     * @param overWrite If true, values from the 'other' response will
     *                  overwrite values with the same name in the object.
     */
    public void merge(ConfigResponse other, boolean overWrite) {
        Set entrySet = other.attributes.entrySet();
        for (Iterator i = entrySet.iterator(); i.hasNext();) {
            Entry entry = (Entry) i.next();

            if (overWrite || this.attributes.get(entry.getKey()) == null) {
                this.attributes.put(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * @return The ConfigResponse as a Properties object
     */
    public Properties toProperties() {
        if (this.attributes instanceof Properties) {
            return (Properties)this.attributes;
        }
        Properties props = new Properties();
        Set entries = this.attributes.entrySet();
        Iterator it = entries.iterator();
        while(it.hasNext()) {
            Entry entry = (Entry)it.next();
            Object value = entry.getValue();
            if (value == null) {
                continue;
            }
            props.put(entry.getKey(), value);
        }
        return props; 
    }

    public String toString(){
        return this.attributes.toString();
    }

    public int size(){
        return this.attributes.size();
    }

    public boolean equals(Object o) {
        if (o == null || !(o instanceof ConfigResponse)) {
            return false;
        }

        return ((ConfigResponse)o).toProperties().equals(this.toProperties());
    }

    public int hashCode() {
        return this.toProperties().hashCode();
    }
    
    /**
     * Break the named preference  
     * @param delimiter the delimeter to break it up by
     * @param key the name of the preference
     * @return <code>List</code> of <code>String</code> tokens
     */
    public List getPreferenceAsList(String key, String delimiter)
        throws InvalidOptionException {
        return StringUtil.explode(getValue(key), delimiter);
    }
}
