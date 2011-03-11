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

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name = "EAM_CONFIG_PROPS", uniqueConstraints = { @UniqueConstraint(name = "configPropertyId", columnNames = { "PREFIX",
                                                                                                                    "PROPKEY" }) })
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class ConfigProperty implements Serializable {
    @Column(name = "DEFAULT_PROPVALUE", length = 300)
    private String defaultValue;

    @Id
    @GenericGenerator(name = "mygen1", strategy = "increment")
    @GeneratedValue(generator = "mygen1")
    @Column(name = "ID")
    private Integer id;

    @Column(name = "PROPKEY", length = 80)
    private String key;

    @Column(name = "PREFIX", length = 80)
    private String prefix;

    @Column(name = "FREAD_ONLY")
    private boolean readOnly = false;

    @Column(name = "PROPVALUE", length = 300)
    private String value;

    @Column(name = "VERSION_COL", nullable = false)
    @Version
    private Long version;

    public ConfigProperty() {
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof ConfigProperty) || !super.equals(obj)) {
            return false;
        }
        ConfigProperty o = (ConfigProperty) obj;
        return ((prefix == o.getPrefix()) || (prefix != null && o.getPrefix() != null && prefix
            .equals(o.getPrefix()))) &&
               ((key == o.getKey()) || (key != null && o.getKey() != null && key.equals(o.getKey())));
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public Integer getId() {
        return id;
    }

    public String getKey() {
        return key;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getValue() {
        return value;
    }

    public Long getVersion() {
        return version;
    }

    public int hashCode() {
        int result = super.hashCode();

        result = 37 * result + (prefix != null ? prefix.hashCode() : 0);
        result = 37 * result + (key != null ? key.hashCode() : 0);

        return result;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setDefaultValue(String defaultPropValue) {
        defaultValue = defaultPropValue;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setKey(String propKey) {
        key = propKey;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public void setReadOnly(boolean flag) {
        readOnly = flag;
    }

    public void setValue(String propValue) {
        value = propValue;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
