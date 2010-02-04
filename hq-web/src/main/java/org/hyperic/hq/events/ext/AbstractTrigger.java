/*
 * NOTE: This copyright doesnot cover user programs that use HQ program services
 * by normal system calls through the application program interfaces provided as
 * part of the Hyperic Plug-in Development Kit or the Hyperic Client Development
 * Kit - this is merely considered normal use of the program, and doesnot fall
 * under the heading of "derived work". Copyright (C) [2004-2008], Hyperic, Inc.
 * This file is part of HQ. HQ is free software; you can redistribute it and/or
 * modify it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed in the
 * hope that it will be useful, but WITHOUT ANY WARRANTY; without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See
 * the GNU General Public License for more details. You should have received a
 * copy of the GNU General Public License along with this program; if not, write
 * to the Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 * 02111-1307 USA.
 */

package org.hyperic.hq.events.ext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.events.AbstractEvent;
import org.hyperic.hq.events.TriggerFiredEvent;
import org.hyperic.hq.events.TriggerInterface;
import org.hyperic.hq.events.TriggerNotFiredEvent;
import org.hyperic.hq.events.server.session.AlertConditionEvaluator;

/**
 * Abstract class that defines a trigger, which can fire actions
 */
public abstract class AbstractTrigger implements TriggerInterface, RegisterableTriggerInterface {

    protected final Log log = LogFactory.getLog(AbstractTrigger.class);

    private Integer id = Integer.valueOf(-1);

    private AlertConditionEvaluator alertConditionEvaluator;

    private boolean enabled;

    public AbstractTrigger() {

    }

    protected final void fireActions(TriggerFiredEvent event) {
        alertConditionEvaluator.triggerFired(event);
    }

    public Integer getId() {
        return this.id;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    protected final void notFired(AbstractEvent nonFiringEvent) {
        if (log.isDebugEnabled()) {
            log.debug("Trigger [id=" + getId() + ", cls=" + getClass().getSimpleName() +
                      "] publishing TriggerNotFiredEvent");
        }
        TriggerNotFiredEvent notFired = new TriggerNotFiredEvent(getId());
        // Preserve the timestamp of the event that caused the condition not to
        // fire. We need to evaluate the events in order they occurred.
        notFired.setTimestamp(nonFiringEvent.getTimestamp());
        alertConditionEvaluator.triggerNotFired(notFired);
    }

    protected TriggerFiredEvent prepareTriggerFiredEvent(AbstractEvent source) {
        if (log.isDebugEnabled()) {
            log.debug("Trigger [id=" + getId() + ", cls=" + getClass().getSimpleName() +
                      "] creating TriggerFiredEvent");
        }
        return new TriggerFiredEvent(getId(), source);
    }

    public void setAlertConditionEvaluator(AlertConditionEvaluator alertConditionEvaluator) {
        this.alertConditionEvaluator = alertConditionEvaluator;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setId(Integer id) {
        this.id = id;
    }

}
