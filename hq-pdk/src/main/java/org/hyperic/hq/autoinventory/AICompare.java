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

package org.hyperic.hq.autoinventory;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.hyperic.hq.appdef.shared.AIIpValue;
import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.hq.appdef.shared.AIServerExtValue;
import org.hyperic.hq.appdef.shared.AIServerValue;
import org.hyperic.hq.appdef.shared.AIServiceTypeValue;
import org.hyperic.hq.appdef.shared.AIServiceValue;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.product.MeasurementInfo;
import org.hyperic.hq.product.MeasurementInfos;
import org.hyperic.util.config.ConfigOption;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.EncodingException;

/**
 * A utility class to facilitate comparisons of AI value objects.
 * This is used in determining if we should send a default scan or
 * a runtime scan to the server.  If nothing has changed, we don't
 * send the report.
 *
 * @see org.hyperic.hq.autoinventory.ScanState#isSameState
 * @see org.hyperic.hq.product.RuntimeResourceReport#isSameReport
 */
public class AICompare {

    private AICompare () {}

    public static boolean compareAIPlatforms(AIPlatformValue p1,
                                             AIPlatformValue p2) {
        // NOTE: we only compare attributes that are autodiscovered.
        // So, for example, we don't care that the ctime/mtime is different -
        // because it will always be different.  Also, the certDN is set
        // later by the agent itself before sending the report, so there's
        // no sense diffing on that.
        if ( !compare(p1.getPlatformTypeName(), p2.getPlatformTypeName()) ||
             !compare(p1.getFqdn(),      p2.getFqdn()) ||
             !compare(p1.getName(),      p2.getName()) ||
             !compare(p1.getLocation(),  p2.getLocation()) ||
             !compare(p1.getDescription(), p2.getDescription()) ||
             !configsEqual(p1.getProductConfig(),     p2.getProductConfig()) ||
             !configsEqual(p1.getControlConfig(),     p2.getControlConfig()) ||
             !configsEqual(p1.getMeasurementConfig(), p2.getMeasurementConfig()) ||
             !configsEqual(p1.getCustomProperties(),  p2.getCustomProperties())) {
             //!MathUtil.compare(p1.getCpuCount(), p2.getCpuCount()) ||
            // System.err.println("AIC: platform attrs differ");
            return false;
        }

        // Compare IP sets
        AIIpValue[] ips1, ips2;
        ips1 = p1.getAIIpValues();
        ips2 = p2.getAIIpValues();
        if (ips1.length != ips2.length) {
            // System.err.println("AIC: IP sets different length");
            return false;
        }
        boolean foundMatchingIP;
        for (int i=0; i<ips1.length; i++) {
            foundMatchingIP = false;
            for (int j=0; j<ips2.length; j++) {
                if (compare(ips1[i].getAddress(),    ips2[j].getAddress()) &&
                    compare(ips1[i].getMACAddress(), ips2[j].getMACAddress()) &&
                    compare(ips1[i].getNetmask(),    ips2[j].getNetmask())) {
                    foundMatchingIP = true;
                    break;
                }
            }
            if (!foundMatchingIP) {
                // System.err.println("AIC: no matching IP: " + ips1[i]);
                return false;
            }
        }

        AIServerValue[] sarray1, sarray2;
        sarray1 = p1.getAIServerValues();
        sarray2 = p2.getAIServerValues();
        return compareAIServers(Arrays.asList(sarray1),
                                Arrays.asList(sarray2));
    }

    public static boolean compareAIServers ( Collection servers1, 
                                             Collection servers2 ) {
        if (servers1.size() != servers2.size()) {
            // System.err.println("AIC: server lengths differ");
            return false;
        }
        boolean foundMatchingServer;
        AIServerValue s1, s2;
        for (Iterator i=servers1.iterator(); i.hasNext(); ) {
            s1 = (AIServerValue) i.next();
            foundMatchingServer = false;
            for (Iterator j=servers2.iterator(); j.hasNext(); ) {
                s2 = (AIServerValue) j.next();
                // As above with the aiplatforms, we only care about 
                // autodiscovered fields, no ctime/mtime, etc.
                if (configsEqual(s1.getProductConfig(),      s2.getProductConfig()) &&
                    configsEqual(s1.getControlConfig(),      s2.getControlConfig()) &&
                    configsEqual(s1.getMeasurementConfig(),  s2.getMeasurementConfig()) &&
                    configsEqual(s1.getResponseTimeConfig(), s2.getResponseTimeConfig()) &&
                    configsEqual(s1.getCustomProperties(),   s2.getCustomProperties()) &&
                    compare(s1.getName(), s2.getName()) &&
                    compare(s1.getServerTypeName(),s2.getServerTypeName()) &&
                    compare(s1.getDescription(), s2.getDescription()) &&
                    compare(s1.getAutoinventoryIdentifier(),s2.getAutoinventoryIdentifier()) &&
                    compare(s1.getInstallPath(),   s2.getInstallPath())) {
                    if (s1 instanceof AIServerExtValue) {
                        if (s2 instanceof AIServerExtValue) {
                            AIServerExtValue se1 = (AIServerExtValue) s1;
                            AIServerExtValue se2 = (AIServerExtValue) s2;
                            if (se1.getPlaceholder() != se2.getPlaceholder()) {
                                // System.err.println("AIC: placeholder status differs");
                                return false;
                            }
                            AIServiceValue[] svc1, svc2;
                            svc1 = se1.getAIServiceValues();
                            svc2 = se2.getAIServiceValues();
                            if (!compareAIServices(svc1, svc2)) {
                                //System.err.println("AIC: services differ (svc1="+StringUtil.arrayToString(svc1)
                                //                   + ",svc2="+StringUtil.arrayToString(svc1)+")");
                                return false;
                            }
                        	if(! aIServiceTypesEqual(se1.getAiServiceTypes(),se2.getAiServiceTypes())) {
                        		return false;
                        	}
                        } else {
                            // System.err.println("AIC: both servers are not exts");
                            return false;
                        }
                    } else if (s2 instanceof AIServerExtValue) {
                        // System.err.println("AIC: both servers are not exts (2)");
                        return false;
                    }
                    foundMatchingServer = true;
                    break;
                }
            }
            if (!foundMatchingServer) {
                // System.err.println("AIC: no matching server:"+s1);
                return false;
            }
        }
        return true;
    }

    public static boolean compareAIServices (AIServiceValue[] services1,
                                             AIServiceValue[] services2) {
        if (services1 == null) return (services2 == null);
        if (services2 == null) return false;

        if (services1.length != services2.length) {
            // System.err.println("SVC_AIC: lengths differ (l1="+services1.length+",l2="+services2.length+")\n(s1="+StringUtil.arrayToString(services1)+",s2="+StringUtil.arrayToString(services2)+")");
            return false;
        }
        boolean foundMatchingService;
        AIServiceValue s1, s2;
        for (int i=0; i<services1.length; i++) {
            foundMatchingService = false;
            s1 = services1[i];
            for (int j=0; j<services2.length; j++) {
                s2 = services2[j];
                if (compare(s1.getServiceTypeName(),         s2.getServiceTypeName()) &&
                    configsEqual(s1.getProductConfig(),      s2.getProductConfig()) &&
                    configsEqual(s1.getControlConfig(),      s2.getControlConfig()) &&
                    configsEqual(s1.getMeasurementConfig(),  s2.getMeasurementConfig()) &&
                    configsEqual(s1.getResponseTimeConfig(), s2.getResponseTimeConfig()) &&
                    configsEqual(s1.getCustomProperties(),   s2.getCustomProperties()) &&
                    compare(s1.getName(),                    s2.getName()) &&
                    compare(s1.getDescription(),             s2.getDescription())) {
                    // System.err.println("SVC_AIC: found match for: " + s1.getName());
                    foundMatchingService = true;
                    break;
                }
            }
            if (!foundMatchingService) {
                // System.err.println("SVC_AIC: NO match for: " + s1.getName());
                return false;
            }
        }
        return true;
    }
    
    public static boolean aIServiceTypesEqual(AIServiceTypeValue[] serviceTypes1, AIServiceTypeValue[] serviceTypes2) {
    	if (serviceTypes1 == null)
    		return (serviceTypes2 == null);
    		if (serviceTypes2 == null)
    			return false;
    		  
    		if (serviceTypes1.length != serviceTypes2.length) {
    			return false;
    		}
    		boolean foundMatchingServiceType;
    		AIServiceTypeValue s1, s2;
    		for (int i = 0; i < serviceTypes1.length; i++) {
    			foundMatchingServiceType = false;
    			s1 = serviceTypes1[i];
    			for (int j = 0; j < serviceTypes2.length; j++) {
    				s2 = serviceTypes2[j];
    				if (aiServiceTypesEqual(s1, s2)){
    					foundMatchingServiceType = true;
    					break;
    				}
    			}
    			if (!foundMatchingServiceType) {
    				return false;
    			}
    		}
    		return true;
    }
     	
    public static boolean aiServiceTypesEqual(AIServiceTypeValue s1, AIServiceTypeValue s2) {
    		return compare(s1.getDescription(), s2.getDescription()) &&
    			compare(s1.getName(),s2.getName()) &&
    			compare(s1.getServiceName(), s2.getServiceName()) &&
    			configsEqual(s1.getPluginClasses(), s2
    					.getPluginClasses()) &&
    			configsEqual(s1.getProperties(), s2 
    					.getProperties()) &&
    			configSchemasEqual(s1.getCustomProperties(), s2
    					.getCustomProperties()) &&
    			compare(s1.getControlActions(),s2.getControlActions()) &&
    				measurementInfosEqual(s1.getMeasurements(), s2.getMeasurements());
    }
    		 	
    public static boolean measurementInfosEqual(byte[] m1,byte[]m2) {
    	if (m1 == m2) {
    		return true;
    	}
    	if ((m1 == null) || (m2 == null)) {
    		return false;
    	}
    	//skip length comparison here - measurement infos may or may not have an attributes map, but could be equal in all ways that count
    	if ((m1.length == 0) && (m2.length == 0)) {
    		return true; // both empty
    	}
    	try {
    		MeasurementInfos mi1 = MeasurementInfos.decode(m1);
    		MeasurementInfos mi2 = MeasurementInfos.decode(m2);
    		if(! mi1.equals(mi2)) {
    			return false;
    		}
    		//we know that the measurement names are equal at this point;
    		Set measurementNames = mi1.getMeasurementNames();
    		for(Iterator iterator = measurementNames.iterator();iterator.hasNext();) {
    			final String measurementName = (String)iterator.next();
    			boolean measurementsEqual = compare(mi1.getMeasurement(measurementName),mi2.getMeasurement(measurementName));
    			if(! measurementsEqual) {
    				return false;
    			}
    		}
    		return true;
    	} catch (EncodingException e) {
    		throw new SystemException(e.getMessage(), e);
    	}
    }
    			
    public static boolean configSchemasEqual(byte[] c1, byte[] c2) {
    	if (c1 == c2) {
    		return true;
    	}
    	if ((c1 == null) || (c2 == null)) {
    		return false;
    	}
    	if (c1.length != c2.length) {
    		return false;
    	}
    	if ((c1.length == 0) && (c2.length == 0)) {
    		return true; // both empty
    	}
    	// can't use Arrays.equals(c1, c2), order may have changed.
    	try {
    		ConfigSchema cs1 = ConfigSchema.decode(c1);
    		ConfigSchema cs2 = ConfigSchema.decode(c2);
    		boolean configNamesEqual = compare(cs1.getOptionNames(),cs2.getOptionNames());
    		if(! configNamesEqual) {
    			return false;
    		}
    		final String[] optionNames = cs1.getOptionNames();
    		for(int i=0;i< optionNames.length;i++) {
    			ConfigOption option1 = cs1.getOption(optionNames[i]);
    			ConfigOption option2 = cs2.getOption(optionNames[i]);
    			//compare only name and description, as that is all we set for custom properties in ServiceTypeFactory
    			if(!(compare(option1.getName(),option2.getName()) || compare(option1.getDescription(),option2.getDescription()))) {
    				return false;
    		}
    		}
    		return true;
    	} catch (EncodingException e) {
    		throw new SystemException(e.getMessage());
    	}
    }

    public static boolean configsEqual(byte[] c1, byte[] c2) {
        if ((c1 == null) != (c2 == null)) {
            return false;
        }
        if (c1 == c2 || Arrays.equals(c1, c2)) {
            return true;
        }
        if (c1.length != c2.length) {
            return false;
        }
        if ((c1.length == 0) && (c2.length == 0)) {
            return true; //both empty
        }
        
        //can't use Arrays.equals(c1, c2), order may have changed.
        try {
            ConfigResponse cr1 = ConfigResponse.decode(c1); 
            ConfigResponse cr2 = ConfigResponse.decode(c2);
            return cr1.equals(cr2);
        } catch (EncodingException e) {
            throw new SystemException(e.getMessage(), e);
        }
    }

    public static class ConfigDiff {
        protected ConfigResponse newConf = new ConfigResponse();
        protected ConfigResponse changedConf = new ConfigResponse();
        protected ConfigResponse deletedConf = new ConfigResponse();
        public ConfigResponse getNewConf() {
            return newConf;
        }
        public void setNewConf(ConfigResponse newConf) {
            this.newConf = newConf;
        }
        public ConfigResponse getChangedConf() {
            return changedConf;
        }
        public void setChangedConf(ConfigResponse changedConf) {
            this.changedConf = changedConf;
        }
        public ConfigResponse getDeletedConf() {
            return deletedConf;
        }
        public void setDeletedConf(ConfigResponse deletedConf) {
            this.deletedConf = deletedConf;
        }
    }
    public static ConfigDiff configsDiff(byte[] newConf, byte[] oldConf) throws EncodingException {
        ConfigDiff res = new ConfigDiff();
        if ((oldConf == null) != (newConf == null)) { // only one is empty (null)
            res.setNewConf(ConfigResponse.decode(oldConf!=null?oldConf:newConf));
            return res;
        }
        if (oldConf == newConf || Arrays.equals(oldConf, newConf)) {
            return res;
        }
        if ((oldConf.length == 0) && (newConf.length == 0)) {
            return res; //both empty
        }
        
        //can't use Arrays.equals(oldConf, newConf), order may have changed.
        try {
            ConfigResponse cr1 = ConfigResponse.decode(oldConf); 
            ConfigResponse cr2 = ConfigResponse.decode(newConf);
            Map<String,String> m1 = cr1.getConfig();
            Map<String,String> m2 = cr2.getConfig();
            
            for(String k1:m1.keySet()) {
                String v1 = m1.get(k1);
                String v2 = m2.get(k1);
                if (v2==null) {
                    res.getDeletedConf().setValue(k1, v1);
                } else if ((v1==null && v2!=null) || (v1!=null && v2==null) || !v1.equals(v2)) {
                    res.getChangedConf().setValue(k1, v2);
                }
            }
            for(String k2:m2.keySet()) {
                if (!m1.containsKey(k2)) {
                    res.getNewConf().setValue(k2, m2.get(k2));
                }
            }
            return res;
        } catch (EncodingException e) {
            throw new SystemException(e.getMessage(), e);
        }
    }

    /**
     * Compare 2 strings for equality.
     * @return true if both strings are null, or if they are both non-null
     * and ".equals" returns true.  return false otherwise
     */
    private static boolean compare(String s1, String s2) {
        if (s1 == s2) {
            return true;
        }
        if (s1 == null || s2 == null) {
            return false;
        }
        return s1.equals(s2);
    }
	private static boolean compare(MeasurementInfo m1, MeasurementInfo m2) {
		if (m1 == m2) {
			return true;
		}
		if (m1 == null || m2 == null) {
			return false;
		}
		return compare(m1.getAlias(),m2.getAlias()) && compare(m1.getCategory(),m2.getCategory()) && 
			compare(m1.getGroup(),m2.getGroup()) && compare(m1.getName(),m2.getName()) && compare(m1.getRate(),m2.getRate()) && 
			compare(m1.getTemplate(), m2.getTemplate()) && compare(m1.getUnits(),m2.getUnits()) && (m1.getCollectionType() == m2.getCollectionType()) && 
				(m1.getInterval() == m2.getInterval()) && (m1.isDefaultOn() == m2.isDefaultOn()) && (m1.isIndicator() == m2.isIndicator());
		}
		 	
	/**
	 * Compare String arrays without regard to order of elements
	 * @param strArray1
	 * @param strArray2
	 * @return
	 */
	private static boolean compare(String[] strArray1, String[] strArray2) {
		if (strArray1 == strArray2) {
			return true;
		}
		if ((strArray1 == null) || (strArray2 == null)) {
			return false;
		}
		Set set1 = new HashSet(Arrays.asList(strArray1));
		Set set2 = new HashSet(Arrays.asList(strArray2));
		return set1.equals(set2);
	}
		  

}
