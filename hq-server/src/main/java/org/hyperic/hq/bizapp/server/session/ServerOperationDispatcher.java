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
package org.hyperic.hq.bizapp.server.session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.client.AgentCommandsClientFactory;
import org.hyperic.hq.appdef.shared.AgentManager;
import org.hyperic.hq.appdef.shared.ConfigManager;
import org.hyperic.hq.appdef.shared.PlatformManager;
import org.hyperic.hq.auth.shared.AuthManager;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.authz.shared.PermissionManager;
import org.hyperic.hq.autoinventory.shared.AutoinventoryManager;
import org.hyperic.hq.bizapp.shared.ServerOperationService;
import org.hyperic.hq.bizapp.shared.lather.CommandInfo;
import org.hyperic.hq.common.util.MessagePublisher;
import org.hyperic.hq.control.shared.ControlManager;
import org.hyperic.hq.ha.HAService;
import org.hyperic.hq.measurement.shared.MeasurementManager;
import org.hyperic.hq.measurement.shared.ReportProcessor;
import org.hyperic.hq.operation.Envelope;
import org.hyperic.hq.operation.OperationFailedException;
import org.hyperic.hq.operation.OperationService;
import org.hyperic.hq.stats.ConcurrentStatsCollector;
import org.hyperic.hq.zevents.ZeventEnqueuer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.HashSet;

/**
 * @author Helena Edelson
 */
@Service
@Transactional
public class ServerOperationDispatcher implements ServerOperationService {

    private final Log logger = LogFactory.getLog(this.getClass());

    private final OperationService operationService;
    //private final OperationMappingRegistry operationMappingRegistry;

    private HashSet<String> secureCommands = new HashSet<String>();

    private AgentManager agentManager;
    private AuthManager authManager;
    private AuthzSubjectManager authzSubjectManager;
    private AutoinventoryManager autoinventoryManager;
    private ConfigManager configManager;
    private ControlManager controlManager;
    private MeasurementManager measurementManager;
    private PlatformManager platformManager;
    private ReportProcessor reportProcessor;
    private PermissionManager permissionManager;
    private ZeventEnqueuer zeventManager;
    private MessagePublisher messagePublisher;
    private AgentCommandsClientFactory agentCommandsClientFactory;
    private ConcurrentStatsCollector concurrentStatsCollector;
    private HAService haService;

    @Autowired
    public ServerOperationDispatcher(OperationService operationService, AgentManager agentManager, AuthManager authManager,
                                AuthzSubjectManager authzSubjectManager,
                                AutoinventoryManager autoinventoryManager,
                                ConfigManager configManager, ControlManager controlManager,
                                MeasurementManager measurementManager,
                                PlatformManager platformManager, ReportProcessor reportProcessor,
                                PermissionManager permissionManager, ZeventEnqueuer zeventManager,
                                MessagePublisher messagePublisher,
                                AgentCommandsClientFactory agentCommandsClientFactory,
                                HAService haService, ConcurrentStatsCollector concurrentStatsCollector) {
        this.operationService = operationService;
        this.haService = haService;
        this.agentManager = agentManager;
        this.authManager = authManager;
        this.authzSubjectManager = authzSubjectManager;
        this.autoinventoryManager = autoinventoryManager;
        this.configManager = configManager;
        this.controlManager = controlManager;
        this.measurementManager = measurementManager;
        this.platformManager = platformManager;
        this.reportProcessor = reportProcessor;
        this.permissionManager = permissionManager;
        this.zeventManager = zeventManager;
        this.agentCommandsClientFactory = agentCommandsClientFactory;
        this.messagePublisher = messagePublisher;
        this.concurrentStatsCollector = concurrentStatsCollector;

    }

    @PostConstruct
    public void initialize() {
    	this.secureCommands.addAll(Arrays.asList(CommandInfo.SECURE_COMMANDS)); 
    }

    public Object perform() throws OperationFailedException {
        return this.operationService.perform(new Envelope(null, null, null, null));
    }
}
