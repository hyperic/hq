/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2009-2010], VMware, Inc.
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

package org.hyperic.hq.operation.rabbit.mapping;

/**
 * Temporary
 * @author Helena Edelson
 */
public class Operations {

    public static final String AGENT_PING_REQUEST = "agent:ping-request";

    public static final String AGENT_PING_RESPONSE = "agent:ping-response";

    public static final String PING = "ping";

    public static final String START = "start";

    public static final String RESTART = "restart";

    public static final String DIE = "die";

    public static final String UPGRADE = "upgrade";

    public static final String GET_AGENT_BUNDLE = "getCurrentAgentBundle";

    public static final String SEND_FILE = "agentSendFileData";

    public static final String REMOVE_FILE = "agentRemoveFile";

 
}
