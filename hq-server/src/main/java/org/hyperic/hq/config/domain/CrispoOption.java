/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2007], Hyperic, Inc.
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

package org.hyperic.hq.config.domain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OrderColumn;
import javax.persistence.Table;
import javax.persistence.Version;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Index;

@Entity
@Table(name = "EAM_CRISPO_OPT")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class CrispoOption implements Serializable {
    private static final String ARRAY_DELIMITER = "|";

    private static final String[] ARRAY_DESCRIMINATORS = { ".resources",
                                                          ".portlets.",
                                                          ".indicator.views",
                                                          ".charts",
                                                          ".groups" };
    @ElementCollection
    @CollectionTable(name = "EAM_CRISPO_ARRAY", joinColumns = @JoinColumn(name = "OPT_ID"))
    @Column(name = "VAL", nullable = false, length = 4000)
    @OrderColumn(name = "IDX", nullable = false)
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private List<String> array = new ArrayList<String>(0);

    @ManyToOne(optional = false)
    @JoinColumn(name = "CRISPO_ID")
    @Index(name = "CRISPO_IDX")
    private Crispo crispo;

    @Id
    @GenericGenerator(name = "mygen1", strategy = "increment")
    @GeneratedValue(generator = "mygen1")
    @Column(name = "ID")
    private Integer id;

    @Column(name = "PROPKEY", nullable = false)
    private String key;

    private transient final Log log = LogFactory.getLog(CrispoOption.class.getName());

    @Column(name = "VAL", length = 4000)
    private String val;

    @Column(name = "VERSION_COL", nullable = false)
    @Version
    private Long version;

    protected CrispoOption() {
    }

    public CrispoOption(Crispo crispo, String key, String val) {
        this.crispo = crispo;
        this.key = key;
        setValue(val);
    }

    public boolean equals(Object obj) {
        if (this == obj)
            return true;

        if (obj == null || obj instanceof CrispoOption == false)
            return false;

        CrispoOption opt = (CrispoOption) obj;
        return opt.getKey().equals(key) && opt.getCrispo().equals(crispo) &&
               opt.getArray().equals(array);
    }

    protected List<String> getArray() {
        return array;
    }

    public Crispo getCrispo() {
        return crispo;
    }

    public Integer getId() {
        return id;
    }

    public String getKey() {
        return key == null ? "" : key;
    }

    public String getOptionValue() {
        return val == null ? "" : val;
    }

    public String getValue() {
        if (val != null && val.trim().length() > 0) {
            return val;
        } else if (val == null && array.size() == 0) {
            return "";
        } else {
            StringBuffer val = new StringBuffer();
            for (String item : array) {
                if (item != null && item.length() > 0) {
                    val.append(ARRAY_DELIMITER).append(item);
                }
            }
            return val.toString();
        }
    }

    public Long getVersion() {
        return version;
    }

    public int hashCode() {
        int result = 17;

        result = 37 * result + crispo.hashCode();
        result = 37 * result + key.hashCode();
        result = 37 * result + array.hashCode();
        return result;
    }

    protected void setArray(List<String> array) {
        this.array = array;
    }

    protected void setCrispo(Crispo crispo) {
        this.crispo = crispo;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    protected void setKey(String key) {
        this.key = key;
    }

    public void setOptionValue(String val) {
        this.val = val;
    }

    public void setValue(String val) {
        for (int i = 0; i < ARRAY_DESCRIMINATORS.length; i++) {
            if (key.indexOf(ARRAY_DESCRIMINATORS[i]) > -1) {
                if (val != null && val.trim().length() > 0) {
                    array = new ArrayList<String>();
                    String[] elem = val.split("\\" + ARRAY_DELIMITER);
                    for (int j = 0; j < elem.length; j++) {
                        if (elem[j] != null && elem[j].trim().length() > 0)
                            array.add(elem[j]);

                        if (log.isDebugEnabled())
                            log.debug("Adding: {" + elem[j] + "}");
                    }
                }
                val = null;
                return;
            }
        }

        this.val = val;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

}
