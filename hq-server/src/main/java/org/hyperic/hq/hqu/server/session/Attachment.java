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

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Index;

@Entity
@Table(name = "EAM_UI_ATTACHMENT")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Inheritance(strategy = InheritanceType.JOINED)
public class Attachment implements Serializable {
    @Column(name = "ATTACH_TIME", nullable = false)
    private long attachTime;

    @Id
    @GenericGenerator(name = "mygen1", strategy = "increment")
    @GeneratedValue(generator = "mygen1")
    @Column(name = "ID")
    private Integer id;

    @Column(name = "VERSION_COL", nullable = false)
    @Version
    private Long version;

    @ManyToOne(cascade = { CascadeType.MERGE, CascadeType.PERSIST })
    @JoinColumn(name = "VIEW_ID", nullable = false)
    @Index(name = "UI_ATTACHMENT_VIEW_ID_IDX")
    private View<?> view;

    protected Attachment() {
    }

    public Attachment(View<?> view) {
        this.view = view;
        attachTime = System.currentTimeMillis();
    }

    /**
     * TODO: We probably need to subclass the attachments into their specific
     * types (such as admin, etc.), via a Hibernate subclass. This way each
     * object can do a proper .equals()
     */
    public boolean equals(Object obj) {
        if (!(obj instanceof Attachment)) {
            return false;
        }

        Attachment o = (Attachment) obj;
        return o.getView().equals(getView()) && o.getAttachTime() == getAttachTime();
    }

    public long getAttachTime() {
        return attachTime;
    }

    public Integer getId() {
        return id;
    }

    public Long getVersion() {
        return version;
    }

    public View getView() {
        return view;
    }

    public int hashCode() {
        int result = 17;

        result = 37 * result + getView().hashCode();
        result = 37 * result + (int) getAttachTime();

        return result;
    }

    protected void setAttachTime(long attachTime) {
        this.attachTime = attachTime;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    protected void setView(View<?> view) {
        this.view = view;
    }

    public String toString() {
        return view.getPath() + " [" + view.getDescription() + "] attached";
    }
}
