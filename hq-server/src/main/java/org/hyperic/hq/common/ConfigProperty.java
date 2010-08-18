/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2004-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */
package org.hyperic.hq.common;

import org.hyperic.hibernate.PersistedObject;

public class ConfigProperty 
    extends PersistedObject
{
    private String  _prefix;
    private String  _key;
    private String  _value;
    private String  _defaultValue;
    private boolean _readOnly = false;

    public ConfigProperty() {
    }

    public String getPrefix() {
        return _prefix;
    }

    public void setPrefix(String prefix) {
        _prefix = prefix;
    }

    public String getKey() {
        return _key;
    }

    public void setKey(String propKey) {
        _key = propKey;
    }

    public String getValue() {
        return _value;
    }

    public void setValue(String propValue) {
        _value = propValue;
    }

    public String getDefaultValue() {
        return _defaultValue;
    }

    public void setDefaultValue(String defaultPropValue) {
        _defaultValue = defaultPropValue;
    }

    public boolean isReadOnly() {
        return _readOnly;
    }

    /**
     * @deprecated use isReadOnly()
     */
    public boolean getReadOnly() {
        return isReadOnly();
    }

    public void setReadOnly(boolean flag) {
        _readOnly = flag;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof ConfigProperty) || !super.equals(obj)) {
            return false;
        }
        ConfigProperty o = (ConfigProperty) obj;
        return
               ((_prefix == o.getPrefix()) ||
                (_prefix != null && o.getPrefix() != null &&
                 _prefix.equals(o.getPrefix())))
               &&
               ((_key == o.getKey()) ||
                (_key != null && o.getKey() != null &&
                 _key.equals(o.getKey())));
    }

    public int hashCode() {
        int result = super.hashCode();

        result = 37*result + (_prefix != null ? _prefix.hashCode() : 0);
        result = 37*result + (_key != null ? _key.hashCode() : 0);

        return result;
    }
}
