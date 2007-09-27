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

/**
 * Simple helper class to help keep plugin config schemas
 * consistent and concise.
 */
public class SchemaBuilder {

    private ConfigSchema schema;
    private ConfigResponse config;

    public SchemaBuilder(ConfigSchema schema, ConfigResponse config) {
        this.schema = schema;
        this.config = config;
    }

    /**
     * @param config Base config used for default values.
     */
    public SchemaBuilder(ConfigResponse config) {
        this(new ConfigSchema(), config);
    }

    public SchemaBuilder() {
        this(new ConfigResponse());
    }

    public ConfigSchema getSchema() {
        return this.schema;
    }

    public ConfigOption add(String prop,
                            String description,
                            String defaultValue,
                            boolean optional) {
        StringConfigOption opt = new StringConfigOption(prop,
                                                        description,
                                                        defaultValue);
        opt.setOptional(optional);
        return addOption(opt);
    }

    public ConfigOption add(String prop,
                            String description,
                            String defaultValue) {
        return add(prop, description, defaultValue, false);
    }

    public ConfigOption add(String prop,
                            String description,
                            int defaultValue) {
        return add(prop, description, new Integer(defaultValue));
    }

    public ConfigOption add(String prop,
                            String description,
                            Integer defaultValue) {
        return addOption(new IntegerConfigOption(prop,
                                                 description,
                                                 defaultValue));
    }

    public ConfigOption add(String prop,
                            String description,
                            double defaultValue) {
        return add(prop, description, new Double(defaultValue));
    }

    public ConfigOption add(String prop,
                            String description,
                            Double defaultValue) {
        return addOption(new DoubleConfigOption(prop,
                                                description,
                                                defaultValue));
    }

    public ConfigOption add(String prop,
                            String description,
                            boolean defaultValue) {
        return addOption(new BooleanConfigOption(prop,
                                                 description,
                                                 defaultValue));
    }

    public ConfigOption addSecret(String prop,
                                  String description) {
        StringConfigOption opt = 
            new StringConfigOption(prop,
                                   description,
                                   null);

        opt.setSecret(true);

        return addOption(opt);
    }

    public ConfigOption addPort(String prop,
                                String description,
                                int defaultValue) {
        return addPort(prop,
                       description,
                       new Integer(defaultValue));
    }

    public ConfigOption addPort(String prop,
                                String description,
                                Integer defaultValue) {
        return addOption(new PortConfigOption(prop,
                                              description,
                                              defaultValue));
    }

    public ConfigOption addEnum(String prop,
                                String description,
                                String[] opts) { 
        return addEnum(prop,
                       description,
                       opts,
                       opts[0]); 
    } 

    public ConfigOption addEnum(String prop,
                                String description,
                                String[] opts, 
                                String defaultValue) { 
        return addOption(new EnumerationConfigOption(prop,
                                                     description, 
                                                     defaultValue,
                                                     opts)); 
    } 

    public ConfigOption addRegex(String prop,
                                 String description,
                                 String defaultValue) { 
        return addOption(new RegexArrayConfigOption(prop,
                                                    description, 
                                                    defaultValue));
    }

    public ConfigOption addStringArray(String prop,
                                 String description,
                                 String defaultValue) {
        return addOption(new StringArrayConfigOption(prop,
                                                    description, 
                                                    defaultValue)); 
    }

    public ConfigOption add(String[] opt) {
        return add(opt[0], opt[1], opt[2]);
    }

    private ConfigOption addOption(ConfigOption opt) {
        setDefault(opt);
        this.schema.addOption(opt);
        return opt;
    }

    private void setDefault(ConfigOption opt) {
        String defaultValue = this.config.getValue(opt.getName());

        if (defaultValue != null) {
            opt.setDefault(defaultValue);
        }
    }
}
