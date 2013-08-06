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

package org.hyperic.hq.measurement.server.session;

import java.util.List;

/**
 * The {@link DataInserter} takes data from the 
 * {@link ReportProcessorImpl} and sends it to the {@link DataManagerImpl} 
 * to put into the DB.
 */
public interface DataInserter<T> {
    /**
     * Insert data into the DB, possibly blocking.
     * 
     * @param data a list
     */
    public void insertData(List<T> data) throws InterruptedException, DataInserterException;

    /**
     * Insert priority data into the DB, possibly blocking.  This may or may not
     * be implemented by the inherited class.
     * 
     * @param metricData a list of type T
     * @param isPriority tells the inserter to prioritize the metricData List.
     * When implemented the DataInserter will give will insert the priority
     * data before the low priority data.
     */
    public void insertData(List<T> metricData, boolean isPriority)
        throws InterruptedException, DataInserterException;

    /**
     * Insert metric data calculated by the Server into the DB, possibly blocking.  This may or may not
     * be implemented by the inherited class.
     * 
     * @param metricData a list of type T
     */
    public void insertDataFromServer(List<T> metricData)
        throws InterruptedException, DataInserterException;

    public Object getLock();
}
