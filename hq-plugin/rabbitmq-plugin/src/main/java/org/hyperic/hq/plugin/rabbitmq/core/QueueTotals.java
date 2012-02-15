/*
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2012], VMware, Inc.
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
 *
 */
package org.hyperic.hq.plugin.rabbitmq.core;

import java.util.HashMap;
import java.util.Map;

public class QueueTotals {

    private double messages;
    private double messagesReady;
    private double messagesUnacknowledged;

    private QueueTotalsMessage messagesDetails;
    private QueueTotalsMessage messagesReadyDetails;
    private QueueTotalsMessage messagesUnacknowledgedDetails;

    public double getMessages() {
        return messages;
    }

    public void setMessages(double messages) {
        this.messages = messages;
    }

    public double getMessagesReady() {
        return messagesReady;
    }

    public void setMessagesReady(double messagesReady) {
        this.messagesReady = messagesReady;
    }

    public double getMessagesUnacknowledged() {
        return messagesUnacknowledged;
    }

    public void setMessagesUnacknowledged(double messagesUnacknowledged) {
        this.messagesUnacknowledged = messagesUnacknowledged;
    }

    public QueueTotalsMessage getMessagesDetails() {
        return messagesDetails;
    }

    public void setMessagesDetails(QueueTotalsMessage messagesDetails) {
        this.messagesDetails = messagesDetails;
    }

    public QueueTotalsMessage getMessagesReadyDetails() {
        return messagesReadyDetails;
    }

    public void setMessagesReadyDetails(QueueTotalsMessage messagesReadyDetails) {
        this.messagesReadyDetails = messagesReadyDetails;
    }

    public QueueTotalsMessage getMessagesUnacknowledgedDetails() {
        return messagesUnacknowledgedDetails;
    }

    public void setMessagesUnacknowledgedDetails(QueueTotalsMessage messagesUnacknowledgedDetails) {
        this.messagesUnacknowledgedDetails = messagesUnacknowledgedDetails;
    }

    @Override
    public String toString() {
        return "QueueTotals{messages=" + getMessages() +
                ", messagesDetails=" + getMessagesDetails() +
                ", messagesReady=" + getMessagesReady() +
                ", messagesReadyDetails=" + getMessagesReadyDetails() +
                ", messagesUnacknowledged=" + getMessagesUnacknowledged() +
                ", messagesUnacknowledgedDetails=" + getMessagesUnacknowledgedDetails() +
                '}';
    }
}
