/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2010], Hyperic, Inc.
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
package org.hyperic.hq.plugin.vsphere.events.util;

/**
 * EventChannel
 *
 * @author hedelson
 */
public class EventChannel {

    /** define this */
    public static final String[] DEFAULT_SUBSCRIPTION = new String[] {VM.VmClonedEvent, VM.VmDisconnectedEvent};

    public class VM {

        public static final String VmCreatedEvent = "VmCreatedEvent";

        public static final String VmClonedEvent = "VmClonedEvent";

        public static final String VmDisconnectedEvent = "VmDisconnectedEvent";

        public static final String VmConnectedEvent = "VmConnectedEvent";

        public static final String VmUuidChangedEvent = "VmUuidChangedEvent";

        public static final String VmUuidConflictEvent = "VmUuidConflictEvent";

        public static final String VmPoweredOffEvent = "VmPoweredOffEvent";

        public static final String VmPoweredOnEvent = "VmPoweredOnEvent";

        public static final String VmSuspendedEvent = "VmSuspendedEvent";

        public static final String VmRenamedEvent = "VmRenamedEvent";

        public static final String VmDiscoveredEvent = "VmDiscoveredEvent";

    }

    public class Host {
        public static final String HostAddedEvent = "HostAddedEvent";
    }

    public class Cluster {
        public static final String ClusterCreatedEvent = "ClusterCreatedEvent";

        public static final String ClusterDestroyedEvent = "ClusterDestroyedEvent";
    }

    public class VDC {
        public static final String DatacenterEvent = "DatacenterEvent";
    }

    public class Task {
        public static final String TaskEvent = "TaskEvent";
    }

}
