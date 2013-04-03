/*
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2013], VMware, Inc.
 * This file is part of Hyperic.
 *
 * Hyperic is free software; you can redistribute it and/or modify
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
package org.hyperic.hq.api.model.measurements;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.hyperic.hq.api.model.RestApiConstants;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name="HttpEndpointDefinition", namespace=RestApiConstants.SCHEMA_NAMESPACE)
@XmlType(name="HttpEndpointDefinition", namespace=RestApiConstants.SCHEMA_NAMESPACE)
public class HttpEndpointDefinition {
    
    @XmlAttribute
    private String url;
    @XmlAttribute
    private String username;
    @XmlAttribute
    private String password;
    @XmlAttribute(name="content-type")
    private String contentType;
    @XmlAttribute
    private String encoding;
    @XmlAttribute
    private String bodyPrepend;

    public HttpEndpointDefinition() {
        super();
    }
    public HttpEndpointDefinition(String url, String username, String password, String contentType, String encoding) {
        super();
        this.url = url;
        this.username = username;
        this.password = password;
        this.contentType = contentType;
        this.encoding = encoding;
    }
    public String getUrl() {
        return url;
    }
    public String getUsername() {
        return username;
    }
    public String getPassword() {
        return password;
    }
    public String getContentType() {
        return contentType;
    }
    public String getEncoding() {
        return encoding;
    }
    public void setUrl(String url) {
        this.url = url;
    }
    public void setUsername(String username) {
        this.username = username;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }
    public String getBodyPrepend() {
        return bodyPrepend;
    }
    public void setBodyPrepend(String bodyPrepend) {
        this.bodyPrepend = bodyPrepend;
    }

}
