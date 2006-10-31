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

package org.hyperic.hibernate.dao;

import org.hibernate.Session;
import org.hyperic.hq.measurement.MeasurementArg;
import org.hyperic.hq.measurement.MeasurementTemplate;

/**
 * CRUD methods, finders, etc. for MeasurementArg
 */
public class MeasurementArgDAO extends HibernateDAO
{
    public MeasurementArgDAO(Session session) {
        super(MeasurementArg.class, session);
    }

    public MeasurementArg findById(Integer id) {
        return (MeasurementArg)super.findById(id);
    }

    public void remove(MeasurementArg entity) {
        super.remove(entity);
    }

    public MeasurementArg create(Integer placement,
                                 MeasurementTemplate mt) {
        return create(placement, mt, new Integer(0),
                      new Float(0), new Integer(0));
    }

    public MeasurementArg create(Integer placement,
                                 MeasurementTemplate mt,
                                 Integer ticks, Float weight,
                                 Integer previous) {
        MeasurementArg arg = new MeasurementArg();
        arg.setPlacement(placement);
        arg.setTemplate(mt);
        arg.setTicks(ticks);
        arg.setWeight(weight.floatValue());
        arg.setPrevious(previous);
        arg.setTemplateArg(mt);

        save(arg);
        return arg;
    }
}
