package org.hyperic.hq.events.server.session;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.events.ActionExecuteException;
import org.hyperic.hq.events.ActionExecutionInfo;
import org.hyperic.hq.events.ActionInterface;
import org.hyperic.hq.events.AlertInterface;
import org.hyperic.hq.events.InvalidActionDataException;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.InvalidOptionException;
import org.hyperic.util.config.InvalidOptionValueException;
/**
 * Mock implementation of {@link ActionInterface}.  We can't use EasyMock for this because Actions.execute() will
 * later try to instantiate via reflection
 * @author jhickey
 *
 */
public class MockAction implements ActionInterface {

    /**
     *
     */
    public MockAction() {

    }
    public String execute(AlertInterface alert, ActionExecutionInfo info) throws ActionExecuteException {
        return "return!";
    }

    public void setParentActionConfig(AppdefEntityID aeid, ConfigResponse config) throws InvalidActionDataException
    {

    }

    public ConfigResponse getConfigResponse() throws InvalidOptionException, InvalidOptionValueException {
       return new ConfigResponse();
    }

    public ConfigSchema getConfigSchema() {
        return null;
    }

    public String getImplementor() {
        return getClass().getName();
    }

    public void init(ConfigResponse config) throws InvalidActionDataException {

    }

    public void setImplementor(String implementor) {

    }

}