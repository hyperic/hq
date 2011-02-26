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
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Index;
import org.hyperic.hq.hqu.ViewDescriptor;

@Entity
@Table(name="EAM_UI_VIEW")
@Inheritance(strategy=InheritanceType.JOINED)
@Cache(usage=CacheConcurrencyStrategy.READ_WRITE)
public abstract class View<T extends Attachment> implements Serializable
{ 
    @Id
    @GenericGenerator(name = "mygen1", strategy = "increment")  
    @GeneratedValue(generator = "mygen1")  
    @Column(name = "ID")
    private Integer id;

    @Column(name="VERSION_COL",nullable=false)
    @Version
    private Long version;
    
    @ManyToOne(fetch=FetchType.LAZY,cascade={CascadeType.PERSIST,CascadeType.MERGE})
    @JoinColumn(name="UI_PLUGIN_ID",nullable=false)
    @Index(name="UI_PLUGIN_ID_IDX")
    private UIPlugin   plugin;
    
    @Column(name="PATH",nullable=false,length=255,unique=true)
    private String     path;
    
    @Column(name="DESCRIPTION",nullable=false,length=255)
    private String     descr;
    
    @SuppressWarnings("unused")
    @Column(name="ATTACH_TYPE",nullable=false)
    private int attachTypeEnum;
    
    private transient AttachType attachType;
    
    @OneToMany(mappedBy="view",fetch=FetchType.LAZY,cascade=CascadeType.ALL,orphanRemoval=true,targetEntity=Attachment.class)
    private Collection<T> attachments = new ArrayList<T>();
    
    protected View() {}
    
    protected View(UIPlugin plugin, ViewDescriptor view, AttachType attach) {
        this.plugin     = plugin;
        path       = view.getPath();
        descr      = view.getDescription();
        attachType = attach;
    }
    
    
    
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public UIPlugin getPlugin() {
        return plugin;
    }
    
    protected void setPlugin(UIPlugin plugin) {
        this.plugin = plugin;
    }
    
    public String getPath() {
        return path;
    }
    
    protected void setPath(String path) {
        this.path = path;
    }

    public String getDescription() {
        return descr;
    }
    
    protected void setDescription(String descr) {
        this.descr = descr;
    }
    
    protected int getAttachTypeEnum() {
        return attachType.getCode();
    }
    
    protected void setAttachTypeEnum(int code) {
        attachType = AttachType.findByCode(code);
    }
    
    public AttachType getAttachType() {
        return attachType;
    }
    
    void addAttachment(T a) {
        getAttachmentsBag().add(a);
    }
    
    void removeAttachment(T a) {
        getAttachmentsBag().remove(a);
    }
    
    protected Collection<T> getAttachmentsBag() {
        return attachments;
    }
    
    protected void setAttachmentsBag(Collection<T> a) {
        attachments = a;
    }
    
    public Collection<T> getAttachments() {
        return Collections.unmodifiableCollection(attachments);
    }
    
    public String toString() {
        return getPath() + " [" + getDescription() + "] attachable to [" +
            getAttachType().getDescription() + "]";
    }
    
    
    public boolean equals(Object obj) {
        if (!(obj instanceof View)) {
            return false;
        }
        
        View o = (View)obj;
        return o.getPlugin().equals(getPlugin()) &&
               o.getPath().equals(getPath());
    }

    public int hashCode() {
        int result = 17;

        result = 37 * result + getPlugin().hashCode();
        result = 37 * result + getPath().hashCode();

        return result;
    }
}
