package org.hyperic.hq;

import org.hyperic.hq.bizapp.shared.CommandHandlerUtil;
import org.hyperic.hq.bizapp.shared.CommandHandlerLocal;

import javax.ejb.CreateException;
import javax.naming.NamingException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;

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
/**
 * for command execution Context
 */
public class CommandContext extends VisitorContext {
    private int executionTime;
    private boolean requiresNew;
    private ArrayList commands = new ArrayList(0);

    public static CommandContext createContext() {
        return new CommandContext();
    }

    public static CommandContext createContext(Command command) {
        CommandContext context = new CommandContext();
        context.addCommand(command);
        return context;
    }

    public static CommandContext createContext(List commands) {
        CommandContext context = new CommandContext();
        context.getCommands().addAll(commands);
        return context;
    }

    private static CommandHandlerLocal commandHandler;
    static {
        try {
            commandHandler = CommandHandlerUtil.getLocalHome().create();
        } catch (CreateException e) {
            commandHandler = null;
            throw new RuntimeException(
                "Can't instantiate CommandHander Session Bean");
        } catch (NamingException e) {
            commandHandler = null;
            throw new RuntimeException(
                "Can't lookup CommandHander Session Bean");
        }
    }

    public boolean isRequiresNew()
    {
        return requiresNew;
    }

    public void setRequiresNew(boolean requiresNew)
    {
        this.requiresNew = requiresNew;
    }

    protected CommandContext() {
    }

    public void execute(Command command) {
        clear();
        addCommand(command);
        execute();
    }

    public void execute(List commands) {
        clear();
        addCommand(commands);
        execute();
    }

    public void execute() {
        if (commandHandler == null) {
            throw new IllegalStateException("CommandHandler not initialized");
        }
        long start = System.currentTimeMillis();
        try {
            if (requiresNew) {
                commandHandler.executeRequiresNew(this);
            } else {
                commandHandler.executeHandler(this);
            }
        } finally {
            executionTime = (int)(System.currentTimeMillis() - start);
        }
    }

    public int getExecutionTime() {
        return executionTime;
    }

    public List getCommands() {
        return commands;
    }

    protected void setCommands(ArrayList commands) {
        this.commands = commands;
    }

    public void addCommand(Command command) {
        commands.add(command);
    }

    public void addCommand(List commandList) {
        commands.addAll(commandList);
    }

    public void clear() {
        getCommands().clear();
    }
}

