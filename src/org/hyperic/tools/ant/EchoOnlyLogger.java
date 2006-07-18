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

package org.hyperic.tools.ant;

import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.Project;

import java.lang.StringBuffer;
import java.io.BufferedReader;
import java.io.StringReader;
import java.io.IOException;

/**
 * With ant (at least 1.5.x), there's no way to force it shut up.
 * If you really don't want the user to see what's going on except
 * for what is explicitly output from the echo task, then use this
 * as the ant logger
 *
 * Usage (in the ant invocation wrapper):
 * org.apache.tools.ant.Main -logger org.hyperic.tools.ant.EchoOnlyLogger
 */
public class EchoOnlyLogger extends DefaultLogger {

    // subclass this if there's actually some other task name
    // you want to be the magic one to log
    protected static String TASK_TO_LOG = "echo";

    // the 12 spaces used by DefaultLogger has always seemed icky
    protected static int THE_LEFT_COLUMN_SIZE = 4;

    public void targetStarted(BuildEvent event) {
        // hush! little! baby! don't say a word!
    }

    public void messageLogged(BuildEvent event) {
        if (event.getTask() != null && !emacsMode) {
            StringBuffer message = new StringBuffer();
            if (TASK_TO_LOG.equals(event.getTask().getTaskName())) {
                int size = THE_LEFT_COLUMN_SIZE;
                StringBuffer tmp = new StringBuffer();
                for (int i = 0; i < size; i++) {
                    tmp.append(" ");
                }
                try {
                    BufferedReader r = 
                        new BufferedReader(
                            new StringReader(event.getMessage()));
                    String line = r.readLine();
                    boolean first = true;
                    while (line != null) {
                        if (!first) {
                            // lSep is the platform line separator, defined
                            // in our parent class
                            message.append(lSep);
                        }
                        first = false;
                        message.append(tmp.toString()).append(line);
                        line = r.readLine();
                    }
                } catch (IOException e) {
                    // shouldn't be possible
                    message.append(tmp.toString()).append(event.getMessage());
                }
                String msg = message.toString();
                if (event.getPriority() != Project.MSG_ERR) {
                    printMessage(msg, out, event.getPriority());
                } else {
                    printMessage(msg, err, event.getPriority());
                }
                log(msg);
            } 
        }
    }
}
