/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.plugin.vrealize.automation;

import static com.vmware.hyperic.model.relations.ResourceTier.LOGICAL;
import static org.hyperic.plugin.vrealize.automation.VraConstants.CREATE_IF_NOT_EXIST;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.ServerResource;
import org.w3c.dom.Document;

import com.vmware.hyperic.model.relations.ObjectFactory;
import com.vmware.hyperic.model.relations.Resource;
import com.vmware.hyperic.model.relations.ResourceSubType;

/**
 *
 * @author glaullon
 */
public class VRAUtils {

    private static final Log log = LogFactory.getLog(VRAUtils.class);
    private static final HashMap<String,Properties> propertiesMap = new HashMap<String,Properties>();

	public static String executeXMLQuery(String path, String configFilePath) {
		File configFile = new File(configFilePath);
		return executeXMLQuery(path, configFile);
	}
    
    public static String executeXMLQuery(String path, File xmlFile) {
        String res = null;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = (Document) builder.parse(xmlFile);

            XPathFactory xFactory = XPathFactory.newInstance();
            XPath xpath = xFactory.newXPath();

            res = xpath.evaluate(path, doc);
        } catch (Exception ex) {
            log.debug("[executeXMLQuery] " + ex, ex);
        }
        return res;
    }

    protected static Properties configFile(String filePath) {
        if (propertiesMap.containsKey(filePath))
            return propertiesMap.get(filePath);

        Properties properties = new Properties();
        // TODO: German, to implement same for Windows OS
        File configFile = new File(filePath);
        if (configFile.exists()) {
            FileInputStream in = null;
            try {
                in = new FileInputStream(configFile);
                properties.load(in);
                propertiesMap.put(filePath, properties);
            } catch (FileNotFoundException ex) {
                log.debug(ex, ex);
            } catch (IOException ex) {
                log.debug(ex, ex);
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException ex) {
                        log.debug(ex, ex);
                    }
                }
            }
        }

        return properties;
    }

    /**
     * @param objectName
     * @param objectType
     * @return
     */
    protected static String getFullResourceName(String objectName,
                                                String objectType) {
        return String.format("%s %s", objectName, objectType);
    }

    /**
     * Returns parameterized string
     *
     * @param paramKey
     * @param objectType
     * @return
     */
    protected static String getParameterizedName(String paramKey,
                                                 String objectType) {
        String result = null;
        if (StringUtils.isEmpty(objectType)) {
            result = String.format("${%s}", paramKey);
        } else {
            result = String.format("${%s} %s", paramKey, objectType);
        }
        return result;
    }

    /**
     * @param paramKey
     * @return
     */
    protected static String getParameterizedName(String paramKey) {
        return getParameterizedName(paramKey, null);
    }

    protected static String marshallResource(Resource model) {
        ObjectFactory factory = new ObjectFactory();
        ByteArrayOutputStream fos = new ByteArrayOutputStream();
        factory.saveModel(model, fos);
        log.debug("[marshallResource] fos=" + fos.toString());
        return fos.toString();
    }

    public static void setModelProperty(ServerResource server,
                                        String model) {
        server.getProductConfig().setValue(VraConstants.PROP_EXTENDED_REL_MODEL,
                    new String(Base64.encodeBase64(model.getBytes())));
        
        // do not remove, why? please don't ask.
        server.setProductConfig(server.getProductConfig());
    }

    public static String getFqdn(String address) {
        if (StringUtils.isBlank(address))
            return StringUtils.EMPTY;
        address = address.replace("\\:", ":");
        String fqdnFromURI = getFQDNFromURI(address);
        if (StringUtils.isNotBlank(fqdnFromURI))
            return fqdnFromURI;
        return address.split(":")[0];
    }

    private static String getFQDNFromURI(String address) {
        try {
            URI uri = new URI(address);
            String fqdn = uri.getHost();
            return fqdn;

        } catch (Exception e) {
            e.printStackTrace();
            log.debug(String.format("Failed to parse address as URI: '%s'", address));
        }
        return null;
    }

    public static Resource createLogialResource(ObjectFactory objectFactory,
                                                String objectType,
                                                String objectName) {
        return createLogicalResource(objectFactory, objectType, VRAUtils.getFullResourceName(objectName, objectType));
    }

    public static Resource createLogicalResource(ObjectFactory objectFactory,
                                                 String objectType,
                                                 String objectName) {
        return objectFactory.createResource(CREATE_IF_NOT_EXIST, objectType, objectName,
                    LOGICAL, ResourceSubType.TAG);
    }

    public static Collection<String> getDnsNames(final String url){
        Collection<String> dnsNames = null;
        try {
            DnsNameExtractor dnsExtractor = new DnsNameExtractor();
            dnsNames = dnsExtractor.getDnsNames(url);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            dnsNames = new HashSet<String>();
        }
        return dnsNames;
    }
    
    public static String getWGet(String path) {
    	String retValue = null;
    	try {
    		TrustManager[] trustAllCerts = new TrustManager[] {new X509TrustManager() {
    			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
    				return null;
    			}
	            public void checkClientTrusted(X509Certificate[] certs, String authType) {
	            }
	            public void checkServerTrusted(X509Certificate[] certs, String authType) {
	            }
	        } };
		    // Install the all-trusting trust manager
		    SSLContext sc = SSLContext.getInstance("SSL");
		    sc.init(null, trustAllCerts, new java.security.SecureRandom());
		    HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());		    
		    // Create all-trusting host name verifier
		    HostnameVerifier allHostsValid = new HostnameVerifier() {
		        public boolean verify(String hostname, SSLSession session) {
		            return true;
		        }
		    };		    
		    // Install the all-trusting host verifier
		    HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);		    
		    URL url = new URL(path);
		    URLConnection con;
			try {
				con = url.openConnection();
			} catch (Exception e) {				
				log.debug("Couldnt connect to vRa API");
				return "";
			}
		    
		    Reader reader = new InputStreamReader(con.getInputStream());
		    while (true) {
		        int ch = reader.read();
		        if (ch==-1) {
		            break;
		        }
		        retValue += (char)ch;
		    }
    	} catch (Exception e) {
	    	log.error(e.getMessage(), e);
	    }
    	
    	return retValue;
    }
    
    public static String getApplicationServicePathFromJson(String applicationServicesPath) {
    	//TODO: Need to replaced by regular expressions.    	
    	log.debug("XML Content : " + applicationServicesPath);
    	int index = applicationServicesPath.indexOf("com.vmware.darwin.appd.csp");
    	String temp = applicationServicesPath.substring(index);
    	index = temp.indexOf("statusEndPointUrl");    	
    	temp = temp.substring(index);    	
    	index = temp.indexOf(":/");
    	temp = temp.substring(index+3);    	
    	index = temp.indexOf(":");
    	temp = temp.substring(0, index);
    	try {
    		log.debug("host name or ip = " + temp);
			InetAddress addr = InetAddress.getByName(temp);
			log.debug("Application services hostname = " + addr.getHostName());
			return addr.getHostName();
		} catch (UnknownHostException e) {			
			return "Unable to Resolve application services IP to hostname";
		}
    	
    }

}
