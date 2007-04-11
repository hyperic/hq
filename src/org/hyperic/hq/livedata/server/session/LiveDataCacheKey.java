package org.hyperic.hq.livedata.server.session;

import org.hyperic.hq.livedata.shared.LiveDataCommand;

class LiveDataCacheKey {

    private LiveDataCommand[] _commands;

    public LiveDataCacheKey(LiveDataCommand[] cmds) {
        _commands = cmds;
    }

    public LiveDataCommand[] getCommands() {
        return _commands;
    }

    public boolean equals(Object o) {
        if (!(o instanceof LiveDataCacheKey)) {
            return false;
        }

        LiveDataCommand[] cmds = ((LiveDataCacheKey)o).getCommands();
        for (int i = 0; i < cmds.length; i++) {
            if (!(cmds[i].equals(_commands[i]))) {
                return false;
            }
        }
        return true;
    }

    public int hashCode() {
        int result = 17;
        for (int i = 0; i < _commands.length; i++) {
            result = result*37 + _commands[i].hashCode();
        }

        return result;
    }
}
