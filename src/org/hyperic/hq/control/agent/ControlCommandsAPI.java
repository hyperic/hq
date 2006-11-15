package org.hyperic.hq.control.agent;

import org.hyperic.hq.agent.AgentAPIInfo;

public final class ControlCommandsAPI extends AgentAPIInfo {
    private static final byte MAJOR_VER  = 0x00;
    private static final byte MINOR_VER  = 0x00;
    private static final byte BUGFIX_VER = 0x01;

    // Properties that the control commands server defines
    private static final String PROP_PREFIX = 
        "covalent.control.";

    // Commands the control commands server knows about
    private static final String CMD_PREFIX = "control:";
    public static final String command_controlPluginAdd =
        CMD_PREFIX + "controlPluginAdd";
    public static final String command_controlPluginCommand =
        CMD_PREFIX + "controlPluginCommand";
    public static final String command_controlPluginRemove =
        CMD_PREFIX + "controlPluginRemove";
    public static final String command_controlAgtRcvFileData =
        CMD_PREFIX + "controlAgtRcvFileData";
    public static final String command_controlAgtUndoFileData =
        CMD_PREFIX + "controlAgtUndoFileData";

    public static final String[] commandSet = {
        command_controlPluginAdd,
        command_controlPluginCommand,
        command_controlPluginRemove,
        command_controlAgtRcvFileData,
        command_controlAgtUndoFileData
    };

    public ControlCommandsAPI(){
        super(MAJOR_VER, MINOR_VER, BUGFIX_VER);
    }
}
