/**
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2010], VMware, Inc.
 *  This file is part of Hyperic.
 *
 *  Hyperic is free software; you can redistribute it and/or modify
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

package org.hyperic.hq.agent.server;

import com.gemstone.gemfire.cache.CacheRuntimeException;

/**
 * A runtime exception thrown by a RegionQueuewhen an unexpected error is encountered.
 * Because RegionQueue implements the BlockingQueue interface,
 * it cannot add 'throws CacheException' to its method declarations.
 * So, the BlockQueue's implementation has to catch any CacheException thrown
 * by region operations and wrap it in a RegionQueueException. 
 */
public class RegionQueueException extends CacheRuntimeException {

  /**
   * Creates a new <code>RegionQueueException</code>
   */
  public RegionQueueException(String message) {
    super(message);
  }

  /**
   * Creates a new <code>RegionQueueException</code> with the given
   * cause.
   */
  public RegionQueueException(String message, Throwable cause) {
    super(message, cause);
  }

}
