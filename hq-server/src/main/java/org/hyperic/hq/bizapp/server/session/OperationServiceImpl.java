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
public class OperationServiceImpl implements OperationService {

    private final Log logger = LogFactory.getLog(this.getClass());

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
    public OperationServiceImpl(AgentManager agentManager, AuthManager authManager,
                                AuthzSubjectManager authzSubjectManager,
                                AutoinventoryManager autoinventoryManager,
                                ConfigManager configManager, ControlManager controlManager,
                                MeasurementManager measurementManager,
                                PlatformManager platformManager, ReportProcessor reportProcessor,
                                PermissionManager permissionManager, ZeventEnqueuer zeventManager,
                                MessagePublisher messagePublisher,
                                AgentCommandsClientFactory agentCommandsClientFactory,
                                HAService haService, ConcurrentStatsCollector concurrentStatsCollector) {
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

    public Object perform(Envelope envelope) throws OperationFailedException {
        return null;
    }
}
