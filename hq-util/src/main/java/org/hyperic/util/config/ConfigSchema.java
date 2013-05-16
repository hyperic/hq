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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ConfigSchema implements Serializable {

    private static final long serialVersionUID = 8171794117881852319L;
    private static final Object configLock = new Object();
    private static HashMap secrets = new HashMap();

    private ArrayList<ConfigOption> configOptions;

    public ConfigSchema() {
        this.configOptions = new ArrayList<ConfigOption>();
    }

    /**
     * Construct a ConfigSchema based on the array of ConfigOptions provided.
     *
     * @param options An array of ConfigOptions to populate this schema with.
     */
    public ConfigSchema(ConfigOption[] options) {
        this();
        this.configOptions.addAll(Arrays.asList(options));
    }

    /**
     * Returns an unmodifiable list of the configuration schema options.
     *
     * @return configOption
     */
    public List<ConfigOption> getOptions() {
        return Collections.unmodifiableList(new ArrayList<ConfigOption>(this.configOptions));
    }

    /**
     * Appends the given option to the end of the options list. If the option is already part of the
     * list then it is pushed to the end of it.
     *
     * @param option the option to add.
     */
    public void addOption(ConfigOption option) {
        synchronized (configLock) {
            this.configOptions.remove(option);
            this.configOptions.add(option);
        }
    }

    /**
     * Sets the given option as the first element of the list. If the option is already part of the
     * list then it is pulled to the first position.
     *
     * @param option the option to add.
     */
    public void addOptionAsFirst(ConfigOption option) {
        synchronized (configLock) {
            this.configOptions.remove(option);
            this.configOptions.add(0, option);
        }
    }

    /**
     * Add all options to the configuration options list. Any option that already exists in the
     * configuration options list is pushed to the end. The order is as in the provided list.
     *
     * @param options the list of options to add.
     */
    public void addOptions(List<ConfigOption> options) {
        synchronized (configLock) {
            this.configOptions.removeAll(options);
            this.configOptions.addAll(options);
        }
    }

    /**
     * @return Map of getOptions() using ConfigOption.getName() for the keys
     */
    public Map getOptionsMap() {
        //XXX would use this.optionsMap but dont
        //want to break serial uid
        synchronized (configLock) {
            Map opts = new HashMap();
            for (Object configOption : this.configOptions) {
                ConfigOption option = (ConfigOption) configOption;
                opts.put(option.getName(), option);
            }
            return opts;
        }
    }

    /**
     * @param name ConfigOption.getName() value
     * @return ConfigOption that matches the name param, 
     * or null if there is no such option
     */
    public ConfigOption getOption(String name) {
        return (ConfigOption) this.getOptionsMap().get(name);
    }

    public String[] getOptionNames() {
        synchronized (configLock) {
            int size = this.configOptions.size();
            String[] names = new String[size];
            for (int i = 0; i < size; i++) {
                ConfigOption option = (ConfigOption) this.configOptions.get(i);
                names[i] = option.getName();
            }
            return names;
        }
    }

    public Map getDefaultProperties() {
        synchronized (configLock) {
            HashMap props = new HashMap();
            for (Object configOption : this.configOptions) {
                ConfigOption opt = (ConfigOption) configOption;
                String defVal = opt.getDefault();
                if (defVal != null) {
                    props.put(opt.getName(), defVal);
                }
            }
            return props;
        }
    }

    public byte[] encode() throws EncodingException {
        ObjectOutputStream objectStream = null;
        byte[] retVal = null;
        try {
            final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            objectStream = new ObjectOutputStream(byteStream);

            synchronized (configLock) {
                for (Iterator iterator = configOptions.iterator(); iterator.hasNext(); ) {
                    final ConfigOption configOption = (ConfigOption) iterator.next();
                    objectStream.writeObject(configOption);
                }
            }

            objectStream.writeObject(null);
            objectStream.flush();
            retVal = byteStream.toByteArray();
        } catch (IOException exc) {
            throw new EncodingException(exc.toString());
        } finally {
            // ObjectStreams MUST be closed.
            if (objectStream != null)
                try {
                    objectStream.close();
                } catch (Exception ex) {
                }
        }
        return retVal;
    }

    public static ConfigSchema decode(byte[] data) throws EncodingException,
            InvalidOptionException, InvalidOptionValueException {
        try {
            ByteArrayInputStream byteStream = new ByteArrayInputStream(data);
            ObjectInputStream objectStream = new ObjectInputStream(byteStream);
            ConfigSchema schema = new ConfigSchema();

            // Read attributes
            while (true) {
                ConfigOption configOption;
                if ((configOption = (ConfigOption) objectStream.readObject()) == null) {
                    break;
                }
                schema.addOption(configOption);
            }
            return schema;
        } catch (IOException exc) {
            throw new EncodingException(exc.toString());
        } catch (ClassNotFoundException exc) {
            throw new EncodingException(exc.toString());
        }
    }

    /**
     * Change the default value for a given property within the schema.
     */
    public void setDefault(String prop, String value) {
        synchronized (configLock) {
            for (int i = 0; i < this.configOptions.size(); i++) {
                ConfigOption opt = (ConfigOption) this.configOptions.get(i);
                if (opt.getName().equals(prop)) {
                    opt.setDefault(value);
                    break;
                }
            }
        }
    }

    public static boolean isSecret(String key) {
        synchronized (secrets) {
            Object obj = secrets.get(key);
            return (obj != null && obj.equals(Boolean.TRUE)) ? true : false;
        }
    }

    public static void addSecret(String key) {
        synchronized (secrets) {
            secrets.put(key, Boolean.TRUE);
        }
    }
}
