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

package org.hyperic.hq.plugin.netservices;

import java.io.IOException;
import java.net.PortUnreachableException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import org.hyperic.hq.product.PluginException;

import org.xbill.DNS.DClass;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.ResolverConfig;
import org.xbill.DNS.Section;
import org.xbill.DNS.SimpleResolver;
import org.xbill.DNS.Type;

public class DNSCollector extends NetServicesCollector {

    private Message query;
    private String lookupName;
    private String nameserver;

    private SimpleResolver getResolver()
        throws UnknownHostException {

        String ip = getProperty(PROP_HOSTNAME);
        if (ip == null) {
            //this can change, e.g. changing /etc/resolv.conf
            //XXX although, need to refresh() to pick up changes
            ip = ResolverConfig.getCurrentConfig().server();
        }

        SimpleResolver resolver = new SimpleResolver(ip);
        resolver.setPort(getPort());
        resolver.setTimeout(getTimeout());

        this.nameserver = ip;
        
        setSource(this.nameserver + ":" + getPort() + "/" + this.lookupName);

        return resolver;
    }
    
    protected void init() throws PluginException {
        super.init();

        try {
            this.lookupName = getProperty("lookupname");

            Name name =
                Name.fromString(lookupName, Name.root);
        
            Record record =
                Record.newRecord(name, Type.A, DClass.IN);

            this.query =
                Message.newQuery(record);
        } catch (Exception e) {
            throw new PluginException(e.getMessage(), e);
        }
    }

    public void collect() {
        String errmsg;
        
        try {
            startTime();
            Message message =
                getResolver().send(this.query);
            endTime();

            Record[] answers =
                message.getSectionArray(Section.ANSWER);
            Record[] authority =
                message.getSectionArray(Section.AUTHORITY);
            Record[] additional =
                message.getSectionArray(Section.ADDITIONAL);

            setValue("Answers", answers.length);
            setValue("AuthorityRecords", authority.length);
            setValue("AdditionalRecords", additional.length);

            String msg;
            if (answers.length == 0) {
                msg =
                    this.nameserver +
                    " can't find " +
                    this.lookupName;
                this.setWarningMessage(msg);
            }
            else {
                msg =
                    "Non-authoritative answer: " +
                    answers[0].rdataToString();
                setInfoMessage(msg);
            }

            setAvailability(true);
            return;
        } catch (PortUnreachableException e) {
            errmsg = this.nameserver + " port unreachable";
        } catch (SocketTimeoutException e) {
            errmsg = this.nameserver + " socket timeout";
        } catch (IOException e) {
            errmsg = e.getMessage();
            if (errmsg == null) {
                errmsg = e.toString();
            }
        }

        setErrorMessage(errmsg);
        setAvailability(false);
    }
}
