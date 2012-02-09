/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */
package org.hyperic.util.exec;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.tanukisoftware.wrapper.WrapperManager;

/**
 * JVM Shutdown strategies responsible for disposing of the JVMs.<br>
 * Integer Reverse mapping is available so as to support exit code based
 * factory.
 * 
 * @author guy
 * 
 */
public enum ShutdownType {

    Restart(12) {
        
        @Override
        protected final void executeNormalShutdown() {
            m_logger.error("Restart was requested but Wrapper watchdog was not detected - "
                    + "please restart manually!!");
            super.executeNormalShutdown();
        }//EOM 

        @Override
        protected final void executeWrapperManagedShutdown() {
            WrapperManager.restartAndReturn();
        }//EOM 
    }, // EO Restart enum
    NormalStop(0) {

    }, // EO NormalStop enum
    AbnormalStop(-1) {

    };// EO AbnormalStop enum

    private static final Log m_logger = LogFactory.getLog(ShutdownType.class);
    private static Map<Integer, ShutdownType> m_mapReverseMapping = new HashMap<Integer, ShutdownType>();

    static {
        for (ShutdownType enumShutdownType : values()) {
            m_mapReverseMapping.put(enumShutdownType.m_iExitCode,
                    enumShutdownType);
        }// EO while there are more enum members
    }// EO static block

    private int m_iExitCode;

    /**
     * @param iExitCode
     *            System.exit return code.
     */
    ShutdownType(int iExitCode) {
        this.m_iExitCode = iExitCode;
    }// EOM

    /**
     * @param iExitCode
     *            System.exit return code
     * @return Strategy mapped to the exit code or the {@link #NormalStop} as
     *         the null object.
     */
    public static final ShutdownType reverseValueOf(final int iExitCode) {
        final ShutdownType enumShutdownType = m_mapReverseMapping.get(iExitCode);
        return (enumShutdownType == null ? NormalStop : enumShutdownType);
    }// EOC

    /**
     * Default implementation invoking the {@link System#exit(int)}.
     */
    public void shutdown() {
        //Attempt to detect a wrapper presence and if found, delegate, 
        // else invoke the normal system.exit
        if (WrapperManager.isControlledByNativeWrapper())
            this.executeWrapperManagedShutdown() ; 
        else {
            this.executeNormalShutdown() ; 
        }//EO else if no wrapper presence was detected 
    }// EOM
    
    protected void executeNormalShutdown() { 
        System.exit(this.m_iExitCode);
    }//EOM 
    
    protected void executeWrapperManagedShutdown() { 
        WrapperManager.stopAndReturn(this.m_iExitCode) ;  
    }//EOM 

    /**
     * @return {@link System#exit(int)} exit code
     */
    public final int exitCode() {
        return this.m_iExitCode;
    }// EOM

}// EOE ShutdownType
