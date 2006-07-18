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

package org.hyperic.hq.measurement.server.express;

import instantj.expression.Expression;

/**
* An wrapper for cached expressions. At some point we may want to add things
* like timestamp and/or accessFrequency to this class and implement different
* caching algorithms.
* */
public class ExpCacheEntry {
    public ExpCacheEntry ( Integer i, Expression e) {
        setMid(i);
        setExpression(e);
    }

    public Integer getMid(){ return mid; }

    public void setMid(Integer mid){ this.mid = mid; }

    public Expression getExpression(){ return expression; }

    public void setExpression(Expression expression){ this.expression = expression; }

    private Integer mid = null;
    private Expression expression = null;
}
