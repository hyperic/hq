/**
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
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
 *  In addition, as a special exception, the copyright holders give permission to link the code of portions 
 *  of this program with AspectJ under certain conditions as described in each individual source file, 
 *  and distribute linked combinations including the two.
 *  
 *  You must obey the GNU General Public License in all respects for all of the code used other than AspectJ.  
 *  If you modify file(s) with this exception, you may extend this exception to your version of the file(s), 
 *  but you are not obligated to do so.  
 *  If you do not wish to do so, delete this exception statement from your version. 
 *
 */
package org.hyperic.hq.monitor.aop;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * SystemMonitorArchitecture
 * 
 * @author Helena Edelson
 */
@Aspect
@Component
public class MonitorArchitecture {
	@Pointcut("inServiceLayer() && execution(* *.hyperic.hq..*.*(..))")
	public void serviceLayerOperationDuration() {
	}

	/* Layer Definitions */
	@Pointcut("serviceOperation() && transactionalOperation()")
	public void inServiceLayer() {
	}

	/* Concern Definitions */
	@Pointcut("execution(* (@org.springframework.transaction.annotation.Transactional *).*(..))")
	public void transactionalOperation() {
	}

	@Pointcut("execution(* (@org.springframework.stereotype.Service *).*(..))")
	public void serviceOperation() {
	}
}
