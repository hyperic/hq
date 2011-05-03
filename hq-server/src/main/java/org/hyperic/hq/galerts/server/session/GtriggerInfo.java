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
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.Parameter;
import org.hyperic.hq.config.domain.Crispo;
import org.hyperic.hq.galerts.processor.Gtrigger;
import org.hyperic.util.config.ConfigResponse;

@Entity
@Table(name = "EAM_GTRIGGERS")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class GtriggerInfo implements Serializable {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CONFIG_ID", nullable = false)
    @Index(name = "GTRIGGERS_CONFIG_ID_IDX")
    private Crispo config;

    @Id
    @GeneratedValue(generator = "combo")
    @GenericGenerator(name = "combo", parameters = { @Parameter(name = "sequence", value = "EAM_GTRIGGERS_ID_SEQ") }, 
        strategy = "org.hyperic.hibernate.id.ComboGenerator")
    @Column(name = "ID")
    private Integer id;

    @Column(name = "LIDX", nullable = false)
    private int listIndex;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "STRAT_ID", nullable = false)
    @Index(name = "GTRIGGERS_STRAT_ID_IDX")
    private ExecutionStrategyInfo strategy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TYPE_ID", nullable = false)
    @Index(name = "GTRIGGERS_TYPE_ID_IDX")
    private GtriggerTypeInfo typeInfo;

    @Column(name = "VERSION_COL", nullable = false)
    @Version
    private Long version;

    protected GtriggerInfo() {
    }

    GtriggerInfo(GtriggerTypeInfo typeInfo, ExecutionStrategyInfo strategy, Crispo config,
                 int listIndex) {
        this.typeInfo = typeInfo;
        this.strategy = strategy;
        this.config = config;
        this.listIndex = listIndex;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof GtriggerInfo)) {
            return false;
        }
        Integer objId = ((GtriggerInfo) obj).getId();

        return getId() == objId || (getId() != null && objId != null && getId().equals(objId));
    }

    public ConfigResponse getConfig() {
        return config.toResponse();
    }

    protected Crispo getConfigCrispo() {
        return config;
    }

    /**
     * Return GtriggerInfo like a "value" object, parallel to existing API. This
     * guarantees that the pojo values have been loaded.
     * @return this with the values loaded
     */
    GtriggerInfo getGtriggerInfoValue() {
        getConfig();
        getStrategy();
        getTypeInfo();
        return this;
    }

    public Integer getId() {
        return id;
    }

    protected int getListIndex() {
        return listIndex;
    }

    public ExecutionStrategyInfo getStrategy() {
        return strategy;
    }

    public Gtrigger getTrigger() {
        return typeInfo.getType().createTrigger(getConfig());
    }

    protected GtriggerTypeInfo getTypeInfo() {
        return typeInfo;
    }

    public Long getVersion() {
        return version;
    }

    public int hashCode() {
        int result = 17;
        result = 37 * result + (getId() != null ? getId().hashCode() : 0);
        return result;
    }

    protected void setConfigCrispo(Crispo config) {
        this.config = config;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    protected void setListIndex(int listIndex) {
        this.listIndex = listIndex;
    }

    protected void setStrategy(ExecutionStrategyInfo strategy) {
        this.strategy = strategy;
    }

    protected void setTypeInfo(GtriggerTypeInfo typeInfo) {
        this.typeInfo = typeInfo;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
