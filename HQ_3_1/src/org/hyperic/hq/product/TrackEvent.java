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

package org.hyperic.hq.product;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.util.encoding.Base64;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.DataInputStream;
import java.io.DataOutputStream;

/**
 * Simple data class for sending config and log track events back to the
 * server.  Eventually these may need to be seperated out.
 */
public class TrackEvent implements java.io.Serializable {

    //maxlen as defined in schema (sql/events/EventLog.hbm.xml)
    //events are also encoded and stored on disk with a max size of 1024
    //see AgentDListProvider.RECSIZE
    public static final int MESSAGE_MAXLEN = 500;
    public static final int SOURCE_MAXLEN  = 100;

    private AppdefEntityID id;  // The appdef id.
    private long time;          // Timestamp of when the event was recorded.
    private int level;          // Log level. (see LogConstants.java)
    private String source;      // The source (file, class, etc)
    private String message;     // Message to report

    public TrackEvent(AppdefEntityID id, long time, int level,
                      String source, String message) {
        this.id = id;
        this.time = time;
        this.level = level;
        this.source = source;
        this.message = message;
    }

    public TrackEvent(String id, long time, int level,
                      String source, String message) {
        this(new AppdefEntityID(id), time, level, source, message);
    }

    public AppdefEntityID getAppdefId() {
        return this.id;
    }
    
    public String getSource() {
        return source;
    }

    public String getMessage() {
        return message;
    }

    public long getTime() {
        return this.time;
    }

    public int getLevel() {
        return this.level;
    }

    private String truncate(String str, int max) {
        if (str == null) {
            return "";
        }
        else if (str.length() > max) {
            return str.substring(0, max-1);
        }
        else {
            return str;
        }
    }

    public String encode()
        throws IOException
    {
        ByteArrayOutputStream bOs;
        DataOutputStream dOs;
        
        bOs = new ByteArrayOutputStream();
        dOs = new DataOutputStream(bOs);

        dOs.writeInt(this.id.getID());
        dOs.writeInt(this.id.getType());
        dOs.writeLong(time);
        dOs.writeInt(level);
        dOs.writeUTF(truncate(source, SOURCE_MAXLEN));
        dOs.writeUTF(truncate(message, MESSAGE_MAXLEN));

        return Base64.encode(bOs.toByteArray());
    }

    public static TrackEvent decode(String data) 
        throws IOException
    {
        ByteArrayInputStream bIs;
        DataInputStream dIs;
        String source, message;
        int id, type, level;
        long time;

        bIs = new ByteArrayInputStream(Base64.decode(data));
        dIs = new DataInputStream(bIs);

        id = dIs.readInt();
        type = dIs.readInt();
        time = dIs.readLong();
        level = dIs.readInt();
        source = dIs.readUTF();
        message = dIs.readUTF();

        return new TrackEvent(new AppdefEntityID(type, id),
                              time, level, source, message);
    }

    // XXX: for debuging purposes
    public String toString() {
        return id + ": msg=" + message + " file=" +
            source;
    }
}

