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

package org.hyperic.hq.hqu.server.session;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

@Entity
@Table(name = "EAM_UI_PLUGIN")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class UIPlugin implements Serializable {
    @Id
    @GeneratedValue(generator = "combo")
    @GenericGenerator(name = "combo", parameters = { @Parameter(name = "sequence", value = "EAM_UI_PLUGIN_ID_SEQ") }, 
        strategy = "org.hyperic.hibernate.id.ComboGenerator")
    @Column(name = "ID")
    private Integer id;

    @Column(name = "NAME", nullable = false, length = 100, unique = true)
    private String name;

    @Column(name = "PLUGIN_VERSION", nullable = false, length = 30)
    private String pluginVersion;

    @Column(name = "VERSION_COL", nullable = false)
    @Version
    private Long version;

    @OneToMany(mappedBy = "plugin", cascade = CascadeType.ALL, orphanRemoval = true)
    private Collection<View<?>> views = new ArrayList<View<?>>();

    protected UIPlugin() {
    }

    public UIPlugin(String name, String pluginVersion) {
        this.name = name;
        this.pluginVersion = pluginVersion;
    }

    void addView(View<?> v) {
        getViewsBag().add(v);
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof UIPlugin)) {
            return false;
        }

        UIPlugin o = (UIPlugin) obj;
        return o.getName().equals(getName());
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPluginVersion() {
        return pluginVersion;
    }

    public Long getVersion() {
        return version;
    }

    public Collection<View<?>> getViews() {
        return Collections.unmodifiableCollection(views);
    }

    protected Collection<View<?>> getViewsBag() {
        return views;
    }

    public int hashCode() {
        int result = 17;

        result = 37 * result + getName().hashCode();

        return result;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    protected void setName(String name) {
        this.name = name;
    }

    protected void setPluginVersion(String pluginVer) {
        pluginVersion = pluginVer;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    protected void setViewsBag(Collection<View<?>> v) {
        views = v;
    }

    public String toString() {
        return getName() + " version " + getPluginVersion();
    }
}
