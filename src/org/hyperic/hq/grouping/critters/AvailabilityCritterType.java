/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2008], Hyperic, Inc.
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

package org.hyperic.hq.grouping.critters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Query;
import org.hyperic.hq.grouping.Critter;
import org.hyperic.hq.grouping.CritterDump;
import org.hyperic.hq.grouping.CritterTranslationContext;
import org.hyperic.hq.grouping.CritterType;
import org.hyperic.hq.grouping.GroupException;
import org.hyperic.hq.grouping.prop.EnumCritterProp;
import org.hyperic.hq.measurement.server.session.AvailabilityDataRLE;
import org.hyperic.hq.measurement.shared.AvailabilityType;

public class AvailabilityCritterType extends BaseCritterType {
    
    private final Log _log = LogFactory.getLog(AvailabilityCritterType.class);
    
    private static final String PROP_ID = "value";
    
    public AvailabilityCritterType() {
        super();
        initialize("org.hyperic.hq.grouping.Resources", "availabilityType"); 
        try {
            addEnumPropDescription(PROP_ID, AvailabilityType.AVAIL_DOWN.getClass(), true);
        } catch (Exception e) {
            _log.warn(e.getMessage(), e);
        }
    }

    public Critter compose(CritterDump dump) throws GroupException {
        throw new GroupException("compose not supported");
    }

    public void decompose(Critter critter, CritterDump dump)
        throws GroupException {
        throw new GroupException("decompose not supported");
    }

    public boolean isSystem() {
        return true;
    }

    public Critter newInstance(AvailabilityType availType) {
        return new AvailabilityCritter(availType, this);
    }

    public Critter newInstance(Map critterProps) throws GroupException {
        validate(critterProps);
        EnumCritterProp prop = (EnumCritterProp)critterProps.get(PROP_ID);
        AvailabilityType availType = (AvailabilityType)prop.getEnum();
        return new AvailabilityCritter(availType, this);
    }
    
    class AvailabilityCritter implements Critter {
        
        private final AvailabilityType _availType;
        private final List _props;
        private final AvailabilityCritterType _type;
        
        public AvailabilityCritter(AvailabilityType availType,
                            AvailabilityCritterType type) {
            _availType = availType;
            List props = new ArrayList();
            props.add(availType);
            _props = Collections.unmodifiableList(props);
            _type = type;
        }

        public void bindSqlParams(CritterTranslationContext ctx, Query q) {
            q.setParameter(ctx.escape("availval"),
                new Double(_availType.getAvailabilityValue()));
            q.setParameter(ctx.escape("last"),
                new Long(AvailabilityDataRLE.getLastTimestamp()));
        }

        public String getConfig() {
            Object[] args = {_availType};
            return _type.getInstanceConfig().format(args);
        }

        public CritterType getCritterType() {
            return _type;
        }

        public List getProps() {
            return _props;
        }

        public String getSql(CritterTranslationContext ctx,
                             String resourceAlias) {
            return "@rle@.availval = :@availval@ AND @rle@.endtime = :@last@";
        }

        public String getSqlJoins(CritterTranslationContext ctx,
                                  String resourceAlias) {
            return new StringBuilder()
                .append("JOIN EAM_MEASUREMENT @meas@ on ")
                .append(resourceAlias).append(".id = @meas@.resource_id")
                .append(" JOIN HQ_AVAIL_DATA_RLE @rle@ on")
                .append(" @rle@.measurement_id = @meas@.id")
                .toString();
        }

    }
}
