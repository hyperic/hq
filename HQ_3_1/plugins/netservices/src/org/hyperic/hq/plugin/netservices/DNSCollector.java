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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.hyperic.hq.product.Metric;
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
    private Pattern pattern;
    private boolean isMatchAny = false;
    private int type = Type.A;

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

            String pattern = getProperty("pattern");
            if (pattern != null) {
                if (pattern.equals("*")) {
                    pattern = ".*";
                    this.isMatchAny = true;
                }
                try {
                    this.pattern = Pattern.compile(pattern);
                } catch (PatternSyntaxException e) {
                    throw new PluginException(pattern + ": " + e);
                }
            }

            String recordType = getProperty("type");
            if (recordType != null) {
                this.type = Type.value(recordType);
                if (this.type == -1) {
                    throw new PluginException("Invalid record type: " +
                                              recordType);
                }

            }

            Name name =
                Name.fromString(lookupName, Name.root);
        
            Record record =
                Record.newRecord(name, this.type, DClass.IN);

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
            double avail = Metric.AVAIL_UP; //DNS server itself is available

            Record[] answers =
                message.getSectionArray(Section.ANSWER);
            Record[] authority =
                message.getSectionArray(Section.AUTHORITY);
            Record[] additional =
                message.getSectionArray(Section.ADDITIONAL);

            setValue("Answers", answers.length);
            setValue("AuthorityRecords", authority.length);
            setValue("AdditionalRecords", additional.length);

            boolean matchRequired = this.pattern != null;
            boolean matched = false;

            String msg;
            if (answers.length == 0) {
                msg =
                    this.nameserver +
                    " can't find " +
                    this.lookupName;
                if (matchRequired) {
                    this.setErrorMessage(msg);
                    //DNS server is available but lookup failed
                    avail = Metric.AVAIL_WARN;
                }
                else {
                    this.setWarningMessage(msg);
                }
            }
            else {
                List rdata = null;
                msg =
                    "Non-authoritative answer: '" +
                    answers[0].rdataToString() + "'";
                if ((this.pattern == null) || this.isMatchAny) {
                    matched = true;
                }
                else {
                    rdata = new ArrayList();
                    for (int i=0; i<answers.length; i++) {
                        String data = answers[i].rdataToString();
                        rdata.add(data);
                        matched = this.pattern.matcher(data).find();
                        if (matched) {
                            break;
                        }
                    }
                }
                if (matchRequired && !matched) {
                    avail = Metric.AVAIL_WARN;
                    if (answers.length > 1) {
                        msg = "answers: " + rdata.toString();
                    }
                    setErrorMessage(msg + " invalid, expecting: '" +
                                    this.pattern.pattern() + "'");
                }
                else {
                    setInfoMessage(msg);
                }
            }

            setAvailability(avail);
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
