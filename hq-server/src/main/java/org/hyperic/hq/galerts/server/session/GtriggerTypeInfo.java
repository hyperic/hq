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

package org.hyperic.hq.galerts.server.session;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hyperic.hq.common.SystemException;

@Entity
@Table(name = "EAM_GTRIGGER_TYPES")
public class GtriggerTypeInfo implements Serializable {
    @Id
    @GeneratedValue(generator = "combo")
    @GenericGenerator(name = "combo", parameters = { @Parameter(name = "sequence", value = "EAM_GTRIGGER_TYPES_ID_SEQ") }, 
        strategy = "org.hyperic.hibernate.id.ComboGenerator")
    @Column(name = "ID")
    private Integer id;

    @Column(name = "TYPE_CLASS", nullable = false)
    private Class<?> type;

    @Column(name = "VERSION_COL", nullable = false)
    @Version
    private Long version;

    protected GtriggerTypeInfo() {
    }

    public GtriggerTypeInfo(Class<?> typeClass) {
        this.type = typeClass;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof GtriggerTypeInfo)) {
            return false;
        }
        Integer objId = ((GtriggerTypeInfo) obj).getId();

        return getId() == objId || (getId() != null && objId != null && getId().equals(objId));
    }

    public Integer getId() {
        return id;
    }

    public GtriggerType getType() {
        try {
            return (GtriggerType) type.newInstance();
        } catch (InstantiationException e) {
            throw new SystemException(e);
        } catch (IllegalAccessException e) {
            throw new SystemException(e);
        }
    }

    /**
     * Get the trigger type class which extends {@link GtriggerType}
     */
    protected Class<?> getTypeClass() {
        return type;
    }

    public Long getVersion() {
        return version;
    }

    public int hashCode() {
        int result = 17;
        result = 37 * result + (getId() != null ? getId().hashCode() : 0);
        return result;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    protected void setTypeClass(Class<?> typeClass) {
        this.type = typeClass;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
