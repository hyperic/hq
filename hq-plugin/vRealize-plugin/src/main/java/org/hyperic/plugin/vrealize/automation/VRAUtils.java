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
import java.net.URI;
import java.util.Properties;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.util.config.ConfigResponse;

import com.vmware.hyperic.model.relations.ObjectFactory;
import com.vmware.hyperic.model.relations.Resource;
import com.vmware.hyperic.model.relations.ResourceSubType;

/**
 *
 * @author glaullon
 */
public class VRAUtils {

    private static final Log log = LogFactory.getLog(VRAUtils.class);
    private static final Properties props = new Properties();

    protected static Properties configFile(String filePath) {
        if (props.isEmpty()) {
            // TODO: German, to implement same for Windows OS
            File configFile = new File(filePath);
            if (configFile.exists()) {
                FileInputStream in = null;
                try {
                    in = new FileInputStream(configFile);
                    props.load(in);
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
        }
        return props;
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

    public static String getFQDN(String address) {
        if (StringUtils.isBlank(address))
            return StringUtils.EMPTY;
        address = address.replace("\\:", ":");
        String fqdnFromURI = getFQDNFromURI(address);
        if (StringUtils.isNotBlank(fqdnFromURI))
            return fqdnFromURI;
        return address.split(":")[0];
    }

    public static String getFQDNFromURI(String address) {
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
}
