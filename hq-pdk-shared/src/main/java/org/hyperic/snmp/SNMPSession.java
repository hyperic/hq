/*
 * 'SNMPSession.java' NOTE: This copyright does *not* cover user programs that
 * use HQ program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development Kit or
 * the Hyperic Client Development Kit - this is merely considered normal use of
 * the program, and does *not* fall under the heading of "derived work".
 * Copyright (C) [2004, 2005, 2006, 2007, 2008, 2009], Hyperic, Inc. This file
 * is part of HQ. HQ is free software; you can redistribute it and/or modify it
 * under the terms version 2 of the GNU General Public License as published by
 * the Free Software Foundation. This program is distributed in the hope that it
 * will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details. You should have received a copy of the GNU
 * General Public License along with this program; if not, write to the Free
 * Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

package org.hyperic.snmp;

import java.util.List;
import java.util.Map;

/*
 * Generic interface for any version of the SNMP protocol. Use the
 * SNMPClient.getSession to get an instance of a class that implements this
 * interface.
 */
public interface SNMPSession {
    /*
     * Retrieve a single data variable from an SNMP agent.
     * @param mibName The name of the variable to retrieve.
     * @return a SNMPValue object representing the value of the variable.
     * @exception SNMPException if an error occurs communicating with the SNMP
     * agent.
     */
    public SNMPValue getSingleValue(String mibName) throws SNMPException;

    /*
     * Retrieve the value that is equal to or logically next after the specified
     * name.
     * @param mibName The name of the MIB variable to start looking.
     * @return An SNMPValue object representing the value of the specified MIB
     * name, or if not found, the next logical MIB name.
     * @exception SNMPException if an error occurs communicating with the SNMP
     * agent.
     */
    public SNMPValue getNextValue(String mibName) throws SNMPException;

    /*
     * Retrieves all values from a column of an SNMP table.
     * @param mibName The name of the column of the SNMP table.
     * @return a List of SNMPValue objects representing the values found in the
     * column.
     * @exception SNMPException if an error occurs communicating with the SNMP
     * agent.
     */
    public List getColumn(String mibName) throws SNMPException;

    public Map getTable(String mibName, int index) throws SNMPException;

    public SNMPValue getTableValue(String name, int index, String leaf) throws SNMPException;

    public List getBulk(String mibName) throws SNMPException;
}
