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

package org.hyperic.hq.bizapp.shared.lather;

import org.hyperic.lather.LatherKeyNotFoundException;
import org.hyperic.lather.LatherRemoteException;
import org.hyperic.lather.LatherValue;
import org.hyperic.hq.appdef.shared.AIServerExtValue;
import org.hyperic.hq.appdef.shared.AIServerValue;
import org.hyperic.hq.appdef.shared.AIServiceTypeValue;
import org.hyperic.hq.appdef.shared.AIServiceValue;

public class AiServerLatherValue extends AiLatherValue {
	private static final String PROP_CTIME                      = "CTime";
	private static final String PROP_MTIME                      = "MTime";
	private static final String PROP_AUTOINVENTORYIDENTIFIER    = "autoinventoryIdentifier";
	private static final String PROP_CONTROLCONFIG              = "controlConfig";
	private static final String PROP_DESCRIPTION                = "description";
	private static final String PROP_DIFF                       = "diff";
	private static final String PROP_ID                         = "id";
	private static final String PROP_IGNORED                    = "ignored";
	private static final String PROP_INSTALLPATH                = "installPath";
	private static final String PROP_MEASUREMENTCONFIG          = "measurementConfig";
	private static final String PROP_NAME                       = "name";
	private static final String PROP_PRODUCTCONFIG              = "productConfig";
	private static final String PROP_QUEUESTATUS                = "queueStatus";
	private static final String PROP_RESPONSETIMECONFIG         = "responseTimeConfig";
	private static final String PROP_SERVERTYPENAME             = "serverTypeName";
	private static final String PROP_SERVICESAUTOMANAGED        = "servicesAutomanaged";
	private static final String PROP_ISEXT                      = "isEXT";
	private static final String PROP_SERVICES                   = "services";
	private static final String PROP_SERVICE_TYPES              = "serviceTypes";
	private static final String PROP_PLACEHOLDER                = "placeHolder";
	private static final String PROP_AUTOENABLE                 = "autoEnable";
	private static final String PROP_MCONNECT_HASH              = "mConnectHash";
	private static final String PROP_CPROPS                     = "cprops";
    private static final String PROP_AUTO_APPROVE               = "autoApprove";

    public AiServerLatherValue() {
		super();
	}

	public AiServerLatherValue(AIServerValue v) {
		this();

		if (v.cTimeHasBeenSet() && v.getCTime() != null) {
			this.setDoubleValue(PROP_CTIME, (double) v.getCTime().longValue());
		}

		if (v.mTimeHasBeenSet()  && v.getMTime() != null) {
			this.setDoubleValue(PROP_MTIME, (double) v.getMTime().longValue());
		}

        String autoinventoryIdentifier = v.getAutoinventoryIdentifier();
        if (v.autoinventoryIdentifierHasBeenSet() && autoinventoryIdentifier != null) {
            this.setStringValue(PROP_AUTOINVENTORYIDENTIFIER, autoinventoryIdentifier);
		}

        byte[] controlConfig = v.getControlConfig();
        if (v.controlConfigHasBeenSet() && controlConfig != null) {
            this.setByteAValue(PROP_CONTROLCONFIG, controlConfig);
		}

        String description = v.getDescription();
        if (v.descriptionHasBeenSet() && description != null) {
            this.setStringValue(PROP_DESCRIPTION, description, 300);
		}

		if (v.diffHasBeenSet()) {
			this.setDoubleValue(PROP_DIFF, (double) v.getDiff());
		}

        Integer id = v.getId();
        if (v.idHasBeenSet() && id != null) {
            this.setIntValue(PROP_ID, id);
		}

		if (v.ignoredHasBeenSet()) {
			this.setIntValue(PROP_IGNORED, v.getIgnored() ? 1 : 0);
		}

        String installPath = v.getInstallPath();
        if (v.installPathHasBeenSet() && installPath != null) {
            this.setStringValue(PROP_INSTALLPATH, installPath);
		}

        byte[] measurementConfig = v.getMeasurementConfig();
        if (v.measurementConfigHasBeenSet() && measurementConfig != null) {
            this.setByteAValue(PROP_MEASUREMENTCONFIG, measurementConfig);
		}

        String name = v.getName();
        if (v.nameHasBeenSet() && name != null) {
            this.setStringValue(PROP_NAME, name);
		}

        byte[] customProperties = v.getCustomProperties();
        if (v.customPropertiesHasBeenSet() && customProperties != null) {
            this.setByteAValue(PROP_CPROPS, customProperties);
		}

        byte[] productConfig = v.getProductConfig();
        if (v.productConfigHasBeenSet() && productConfig != null) {
            this.setByteAValue(PROP_PRODUCTCONFIG, productConfig);
		}

		if (v.queueStatusHasBeenSet()) {
			this.setIntValue(PROP_QUEUESTATUS, v.getQueueStatus());
		}

        byte[] responseTimeConfig = v.getResponseTimeConfig();
        if (v.responseTimeConfigHasBeenSet() && responseTimeConfig != null) {
            this.setByteAValue(PROP_RESPONSETIMECONFIG, responseTimeConfig);
		}

        String serverTypeName = v.getServerTypeName();
        if (v.serverTypeNameHasBeenSet() && serverTypeName != null
                ) {
            this.setStringValue(PROP_SERVERTYPENAME, serverTypeName);
		}

		if (v.servicesAutomanagedHasBeenSet()) {
			this.setIntValue(PROP_SERVICESAUTOMANAGED, v.getServicesAutomanaged() ? 1 : 0);
		}

		// If this is a souped up AIServerValue with services hanging off it,
		// then add those as well
		if (v instanceof AIServerExtValue) {
			AIServerExtValue svExt = (AIServerExtValue) v;
			AIServiceValue[] services;

			this.setIntValue(PROP_ISEXT, 1);
			this.setIntValue(PROP_PLACEHOLDER, svExt.getPlaceholder() ? 1 : 0);
			this.setIntValue(PROP_AUTOENABLE, svExt.getAutoEnable() ? 1 : 0);
			this.setIntValue(PROP_MCONNECT_HASH, svExt
					.getMetricConnectHashCode());
			services = svExt.getAIServiceValues();
			if (services != null) {
                for (AIServiceValue service : services) {
                    this.addObjectToList(PROP_SERVICES, new AiServiceLatherValue(service));
                }
			}
			AIServiceTypeValue[] serviceTypes = svExt.getAiServiceTypes();
			if (serviceTypes != null) {
                for (AIServiceTypeValue serviceType : serviceTypes) {
                    this.addObjectToList(PROP_SERVICE_TYPES, new AiServiceTypeLatherValue(serviceType));
                }
			}
		} else {
			this.setIntValue(PROP_ISEXT, 0);
		}

        this.setIntValue(PROP_AUTO_APPROVE, v.isAutoApprove() ? 1 : 0);
    }

	public AIServerValue getAIServerValue() {
		AIServerValue r;
		boolean isExt;

		isExt = this.getIntValue(PROP_ISEXT) == 1;
		if (isExt) {
			r = new AIServerExtValue();
		} else {
			r = new AIServerValue();
		}

		try {
			r.setCTime((long) this.getDoubleValue(PROP_CTIME));
        } catch(LatherKeyNotFoundException exc){ /* ignore */}

        try {
			r.setMTime((long) this.getDoubleValue(PROP_MTIME));
        } catch(LatherKeyNotFoundException exc){ /* ignore */}

        try {
			r.setAutoinventoryIdentifier(this
					.getStringValue(PROP_AUTOINVENTORYIDENTIFIER));
        } catch(LatherKeyNotFoundException exc){ /* ignore */}

        try {
			r.setControlConfig(this.getByteAValue(PROP_CONTROLCONFIG));
        } catch(LatherKeyNotFoundException exc){ /* ignore */}

        try {
			r.setDescription(this.getStringValue(PROP_DESCRIPTION));
        } catch(LatherKeyNotFoundException exc){ /* ignore */}

        try {
			r.setDiff((long) this.getDoubleValue(PROP_DIFF));
        } catch(LatherKeyNotFoundException exc){ /* ignore */}

        try {
			r.setId(this.getIntValue(PROP_ID));
        } catch(LatherKeyNotFoundException exc){ /* ignore */}

        try {
			r.setIgnored(this.getIntValue(PROP_IGNORED) == 1);
        } catch(LatherKeyNotFoundException exc){ /* ignore */}

        try {
			r.setInstallPath(this.getStringValue(PROP_INSTALLPATH));
        } catch(LatherKeyNotFoundException exc){ /* ignore */}

        try {
			r.setMeasurementConfig(this.getByteAValue(PROP_MEASUREMENTCONFIG));
        } catch(LatherKeyNotFoundException exc){ /* ignore */}

        try {
			r.setName(this.getStringValue(PROP_NAME));
        } catch(LatherKeyNotFoundException exc){ /* ignore */}

        try {
			r.setProductConfig(this.getByteAValue(PROP_PRODUCTCONFIG));
        } catch(LatherKeyNotFoundException exc){ /* ignore */}

        try {
			r.setQueueStatus(this.getIntValue(PROP_QUEUESTATUS));
        } catch(LatherKeyNotFoundException exc){ /* ignore */}

        try {
			r.setResponseTimeConfig(this.getByteAValue(PROP_RESPONSETIMECONFIG));
        } catch(LatherKeyNotFoundException exc){ /* ignore */}

        try {
			r.setServerTypeName(this.getStringValue(PROP_SERVERTYPENAME));
        } catch(LatherKeyNotFoundException exc){ /* ignore */}

        try {
			r.setServicesAutomanaged(this.getIntValue(PROP_SERVICESAUTOMANAGED) == 1);
        } catch(LatherKeyNotFoundException exc){ /* ignore */}

        try {
			r.setCustomProperties(this.getByteAValue(PROP_CPROPS));
        } catch(LatherKeyNotFoundException exc){ /* ignore */}

        if (isExt) {
			AIServerExtValue svExt = (AIServerExtValue) r;

			svExt.setPlaceholder(this.getIntValue(PROP_PLACEHOLDER) == 1);
			svExt.setAutoEnable(this.getIntValue(PROP_AUTOENABLE) == 1);
			svExt.setMetricConnectHashCode(this.getIntValue(PROP_MCONNECT_HASH));

			try {
				LatherValue[] services =  (LatherValue[])this.getObjectList(PROP_SERVICES);
                for (LatherValue service : services) {
                    AiServiceLatherValue svc;
                    svc = (AiServiceLatherValue) service;
                    svExt.addAIServiceValue(svc.getAIServiceValue());
                }
			} catch (LatherKeyNotFoundException exc) {
				// No services were found which could be expected
				svExt.setAIServiceValues(new AIServiceValue[0]);
			}
			LatherValue[] serviceTypes = null;
			try {
				serviceTypes =  (LatherValue[])this.getObjectList(PROP_SERVICE_TYPES);
			} catch (LatherKeyNotFoundException exc) {
				//No service types were found which could be expected
				svExt.setAiServiceTypes(new AIServiceTypeValue[0]);
			}
			if (serviceTypes != null) {
                for (LatherValue serviceType : serviceTypes) {
                    AiServiceTypeLatherValue svcType = (AiServiceTypeLatherValue) serviceType;
                    try {
                        svExt.addAIServiceTypeValue(svcType.getAIServiceTypeValue());
                    } catch (LatherKeyNotFoundException exc) {
                        //Something that was expected by the AiServiceTypeLatherValue was missing.
                        // This is an error condition.
                    }
                }
			}
		}

        try {
            r.setAutoApprove(this.getIntValue(PROP_AUTO_APPROVE) == 1);
        } catch(LatherKeyNotFoundException exc){ /* ignore */}

        return r;
	}

	public void validate() throws LatherRemoteException {
	}
}
