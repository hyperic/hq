/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.plugin.vrealize.automation;

import com.vmware.hyperic.model.relations.Identifier;
import com.vmware.hyperic.model.relations.ObjectFactory;
import static com.vmware.hyperic.model.relations.RelationType.PARENT;
import static com.vmware.hyperic.model.relations.RelationType.SIBLING;
import com.vmware.hyperic.model.relations.Resource;
import static com.vmware.hyperic.model.relations.ResourceTier.SERVER;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.NTCredentials;
import org.apache.http.auth.params.AuthPNames;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.AuthPolicy;
import org.hyperic.hq.agent.AgentKeystoreConfig;
import org.hyperic.hq.product.DaemonDetector;
import static org.hyperic.hq.product.GenericPlugin.getPlatformName;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.hq.product.ServiceResource;
import static org.hyperic.plugin.vrealize.automation.VraConstants.CREATE_IF_NOT_EXIST;
import static org.hyperic.plugin.vrealize.automation.VraConstants.KEY_APPLICATION_NAME;
import static org.hyperic.plugin.vrealize.automation.VraConstants.PROP_EXTENDED_REL_MODEL;
import static org.hyperic.plugin.vrealize.automation.VraConstants.PROP_INSTALL_PATH;
import static org.hyperic.plugin.vrealize.automation.VraConstants.TYPE_LOAD_BALANCER_TAG;
import static org.hyperic.plugin.vrealize.automation.VraConstants.TYPE_VCO_TAG;
import static org.hyperic.plugin.vrealize.automation.VraConstants.TYPE_VRA_APPLICATION;
import static org.hyperic.plugin.vrealize.automation.VraConstants.TYPE_VRA_IAAS_WEB;
import static org.hyperic.plugin.vrealize.automation.VraConstants.TYPE_VRA_IAAS_WEB_LOAD_BALANCER;
import static org.hyperic.plugin.vrealize.automation.VraConstants.TYPE_VRA_IAAS_WEB_LOAD_BALANCER_TAG;
import static org.hyperic.plugin.vrealize.automation.VraConstants.TYPE_VRA_IAAS_WEB_TAG;
import static org.hyperic.plugin.vrealize.automation.VraConstants.TYPE_VRA_SERVER;
import static org.hyperic.plugin.vrealize.automation.VraConstants.TYPE_VRA_SERVER_LOAD_BALANCER;
import static org.hyperic.plugin.vrealize.automation.VraConstants.TYPE_VRA_SERVER_TAG;
import static org.hyperic.plugin.vrealize.automation.VraConstants.TYPE_VRA_VCO;
import static org.hyperic.plugin.vrealize.automation.VraConstants.TYPE_VRA_VCO_LOAD_BALANCER;
import org.hyperic.sigar.SigarException;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.http.HQHttpClient;
import org.hyperic.util.http.HttpConfig;
import org.w3c.dom.Document;

/**
 *
 * @author glaullon
 */
public class DiscoveryVRAIaasWeb extends Discovery {

    private static final Log log = LogFactory.getLog(DiscoveryVRAIaasWeb.class);

    @Override
    protected ServerResource newServerResource(long pid,
            String exe) {
        ServerResource server = super.newServerResource(pid, exe);
        log.debug("[newServerResource] pid=" + pid);

        File configFile = null;
        try {
            String[] args = getSigar().getProcArgs(pid);
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if (arg.equalsIgnoreCase("-h")) {
                    configFile = new File(args[i + 1]);
                }
            }
        } catch (SigarException ex) {
            log.debug(ex, ex);
        }

        log.debug("[newServerResource] configFile=" + configFile);
        if (configFile != null) {
            String mmwebPath =
                        executeXMLQuery(
                            "//application[@applicationPool='RepositoryAppPool']/virtualDirectory[@path='/']/@physicalPath",
                            configFile);
            if (mmwebPath != null) {
                File installPath = new File(mmwebPath, "../..");
                log.debug("[newServerResource] installPath=" + getCanonicalPath(installPath));
                server.setInstallPath(getCanonicalPath(installPath));
                server.setIdentifier(server.getIdentifier().replace("%installpath%", server.getInstallPath()));

                server.getProductConfig().setValue(PROP_INSTALL_PATH, getCanonicalPath(installPath));

                // do not remove, why? please don't ask.
                setProductConfig(server, server.getProductConfig());
                server.setProductConfig(server.getProductConfig());
            }
        }
        return server;
    }

    @Override
    protected List discoverServices(ConfigResponse config) throws PluginException {
        log.debug("[discoverServices] config=" + config);
        List<ServiceResource> res = new ArrayList<ServiceResource>();

        String installPath = config.getValue(PROP_INSTALL_PATH, "");
        final String iaasWebServerFqdn = getPlatformName();
        final String vcoFqdn = getVCO(config);
        log.debug("[discoverServices] iaasWebServerFqdn=" + iaasWebServerFqdn);
        log.debug("[discoverServices] vcoFqdn=" + vcoFqdn);

        if (!StringUtils.isEmpty(vcoFqdn)) {
            ServiceResource service = createServiceResource("vCO discovery service");
            String type = getTypeInfo().getName();
            String name = getPlatformName() + " " + type + " ";
            service.setName(name + "vCO discovery service");

            File dataCfg = new File(installPath, "Server/Model Manager Data/Repoutil.exe.config");
            log.debug("[discoverServices] dataCfg=" + getCanonicalPath(dataCfg));
            String vRAIaaSWebLoadBalancer = executeXMLQuery("//add[@key='repositoryAddress']/@value", dataCfg);
            vRAIaaSWebLoadBalancer = vRAIaaSWebLoadBalancer.replaceAll("\\w*://([^:/]*).*", "$1");
            log.debug("[discoverServices] vRAIaaSWebLoadBalancer=" + vRAIaaSWebLoadBalancer);

            File webCfg = new File(installPath, "Server/Model Manager Web/Web.config");
            log.debug("[discoverServices] webCfg=" + getCanonicalPath(webCfg));
            String vRAServer = executeXMLQuery("//repository/@store", webCfg);
            vRAServer = vRAServer.replaceAll("\\w*://([^:/]*).*", "$1");
            log.debug("[discoverServices] vRAServer=" + vRAServer);

            String model = 
                    VRAUtils.marshallResource(getIaaSWebServerRelationsModel(vRAServer, iaasWebServerFqdn,
                                    vRAIaaSWebLoadBalancer, vcoFqdn));
            
            ConfigResponse c = new ConfigResponse();
            c.setValue(PROP_EXTENDED_REL_MODEL, new String(Base64.encodeBase64(model.getBytes())));

            setProductConfig(service, c);
            setMeasurementConfig(service, new ConfigResponse());
            res.add(service);
        }
        return res;
    }

    public static Resource getIaaSWebServerRelationsModel(String vRaApplicationFqdn,
            String iaasWebServerFqdn,
            String vRAIaaSWebLoadBalancerFqdn,
            String vcoFqdn) {

        log.debug("[getIaaSWebServerRelationsModel] vRaApplicationFqdn=" + vRaApplicationFqdn);
        log.debug("[getIaaSWebServerRelationsModel] iaasWebServerFqdn=" + iaasWebServerFqdn);
        log.debug("[getIaaSWebServerRelationsModel] vRAIaaSWebLoadBalancerFqdn=" + vRAIaaSWebLoadBalancerFqdn);
        log.debug("[getIaaSWebServerRelationsModel] vcoFqdn=" + vcoFqdn);

        ObjectFactory factory = new ObjectFactory();

        Resource iaasWebServer = factory.createResource(!CREATE_IF_NOT_EXIST, TYPE_VRA_IAAS_WEB,
                VRAUtils.getFullResourceName(iaasWebServerFqdn, TYPE_VRA_IAAS_WEB), SERVER);

        Resource iaasWebServerTag = VRAUtils.createLogialResource(factory, TYPE_VRA_IAAS_WEB_TAG, vRaApplicationFqdn);
        iaasWebServer.addRelations(factory.createRelation(iaasWebServerTag, PARENT));

        Resource vraAppTagResource = getVraAppTag(factory, vRaApplicationFqdn, TYPE_VRA_APPLICATION);
        iaasWebServerTag.addRelations(factory.createRelation(vraAppTagResource, PARENT));

        if (!StringUtils.isEmpty(vRAIaaSWebLoadBalancerFqdn) && !iaasWebServerFqdn.equals(vRAIaaSWebLoadBalancerFqdn)) {
            // Cluster - has load balancer

            // This is only if there is a load balancer. If not then nothing load balancer related should be created.
            Resource iaasWebLoadBalancer =
                        factory.createResource(Boolean.FALSE, TYPE_VRA_IAAS_WEB_LOAD_BALANCER,
                                    VRAUtils.getFullResourceName(vRAIaaSWebLoadBalancerFqdn,
                                                TYPE_VRA_IAAS_WEB_LOAD_BALANCER),
                                    SERVER);

            iaasWebServer.addRelations(factory.createRelation(iaasWebLoadBalancer, SIBLING));

            Resource iaasWebLoadBalancerTag =
                        VRAUtils.createLogialResource(factory, TYPE_VRA_IAAS_WEB_LOAD_BALANCER_TAG, vRaApplicationFqdn);
            iaasWebLoadBalancer.addRelations(factory.createRelation(iaasWebLoadBalancerTag, PARENT));
            // iaasWebLoadBalancerTag.addRelations(factory.createRelation(vraAppTagResource, PARENT));

            Resource loadBalancerSuperTag =
                        VRAUtils.createLogialResource(factory, TYPE_LOAD_BALANCER_TAG, TYPE_VRA_IAAS_WEB_LOAD_BALANCER_TAG);
            iaasWebLoadBalancerTag.addRelations(factory.createRelation(loadBalancerSuperTag, PARENT));

        }

        // Relation to VCO component might me of two types:
        // 1. Direct reference to VCO server
        // 2. Reference to a Load Balancer of VCO servers cluster
        // Therefore, the code below creates two kinds of relationship - only one of them suppose to be working
        Resource vco =
                    factory.createResource(!CREATE_IF_NOT_EXIST, TYPE_VRA_VCO,
                        VRAUtils.getFullResourceName(vcoFqdn, TYPE_VRA_VCO), SERVER);
        iaasWebServer.addRelations(factory.createRelation(vco, SIBLING));

        Resource vcoLoadBalancer =
                    factory.createResource(!CREATE_IF_NOT_EXIST, TYPE_VRA_VCO_LOAD_BALANCER,
                        VRAUtils.getFullResourceName(vcoFqdn, TYPE_VRA_VCO_LOAD_BALANCER), SERVER);
        iaasWebServer.addRelations(factory.createRelation(vcoLoadBalancer, SIBLING));

        Resource vcoTag = VRAUtils.createLogialResource(factory, TYPE_VCO_TAG, vRaApplicationFqdn);
        vcoTag.addRelations(factory.createRelation(vraAppTagResource, PARENT));
        vco.addRelations(factory.createRelation(vcoTag, PARENT, CREATE_IF_NOT_EXIST));

        /**
         * Adding both vRAVA directly and the vRAVA load balancer because we won't to which the FQDN we have points.
         */
        Resource vraServer =
                    factory.createResource(!CREATE_IF_NOT_EXIST, TYPE_VRA_SERVER,
                                VRAUtils.getFullResourceName(vRaApplicationFqdn, TYPE_VRA_SERVER),
                                SERVER);
        iaasWebServer.addRelations(factory.createRelation(vraServer, SIBLING));

        final String vraServerLoadBalancerName = vRaApplicationFqdn;

        Resource vraServerPossibleLoadBalancer =
                    factory.createResource(!CREATE_IF_NOT_EXIST, TYPE_VRA_SERVER_LOAD_BALANCER,
                                VRAUtils.getFullResourceName(vraServerLoadBalancerName, TYPE_VRA_SERVER_LOAD_BALANCER),
                                SERVER);
        iaasWebServer.addRelations(factory.createRelation(vraServerPossibleLoadBalancer, SIBLING, !CREATE_IF_NOT_EXIST));

        Resource vraServerTag = VRAUtils.createLogialResource(factory, TYPE_VRA_SERVER_TAG, vRaApplicationFqdn);
        vraServer.addRelations(factory.createRelation(vraServerTag, PARENT, CREATE_IF_NOT_EXIST));
        vraServerTag.addRelations(factory.createRelation(vraAppTagResource, PARENT));

        return iaasWebServer;
    }


    private static Resource getVraAppTag(ObjectFactory factory,
            String vRaApplicationName,
            String vraAppTagType) {
        final String vraAppTagName = VRAUtils.getFullResourceName(vRaApplicationName, vraAppTagType);
        Identifier vraAppTagIdentifier = factory.createIdentifier(KEY_APPLICATION_NAME, vRaApplicationName);
        Resource vraAppTagResource = VRAUtils.createLogicalResource(factory, vraAppTagType, vraAppTagName);
        vraAppTagResource.addIdentifiers(vraAppTagIdentifier);
        return vraAppTagResource;
    }

    /*
     * Use parameter instead of trying to discover VCO details
     * Assume that VCO has an Hyperic agent that performs discovery of VCO server and it's components/services
     */
    private static String getVCO(ConfigResponse config) {
        String vcoFNQ = null;
        String xml = null;

        String user = config.getValue("iaas.http.user", "");
        String pass = config.getValue("iaas.http.pass", "");
        String domain = config.getValue("iaas.http.domain", "");

        try {
            AgentKeystoreConfig ksCFG = new AgentKeystoreConfig();
            HQHttpClient client
                    = new HQHttpClient(ksCFG, new HttpConfig(5000, 5000, null, 0), ksCFG.isAcceptUnverifiedCert());

            List<String> authpref = new ArrayList<String>();
            authpref.add(AuthPolicy.NTLM);
            authpref.add(AuthPolicy.BASIC);
            client.getParams().setParameter(AuthPNames.TARGET_AUTH_PREF, authpref);

            client.getCredentialsProvider().setCredentials(
                    new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT, AuthPolicy.NTLM),
                    new NTCredentials(user, pass, "localhost", domain));

            HttpGet get =
                        new HttpGet("https://localhost/Repository/Data/ManagementModelEntities.svc/ManagementEndpoints");
            HttpResponse response = client.execute(get);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) {
                xml = readInputString(response.getEntity().getContent());
            } else {
                log.debug("[getVCOx] GET failed: " + response.getStatusLine().getReasonPhrase());
            }
        } catch (IOException ex) {
            log.debug("[getVCOx] " + ex, ex);
        }

        if (xml != null) {
            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document doc = (Document) builder.parse(new ByteArrayInputStream(xml.getBytes()));

                XPathFactory xFactory = XPathFactory.newInstance();
                XPath xpath = xFactory.newXPath();

                vcoFNQ = xpath.evaluate("//properties[InterfaceType[text()='vCO']]/ManagementEndpointName/text()", doc);
            } catch (Exception ex) {
                log.debug("[getVCOx] " + ex, ex);
            }
        }

        return vcoFNQ;
    }

    private String executeXMLQuery(String path,
            File xmlFile) {
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

    private String getCanonicalPath(File file) {
        try {
            return file.getCanonicalPath();
        } catch (IOException ex) {
            log.debug("[getCanonicalPath] " + ex, ex);
            return file.getAbsolutePath();
        }
    }

    public static String readInputString(InputStream in)
            throws IOException {
        StringBuilder out = new StringBuilder();
        byte[] b = new byte[4096];
        for (int n; (n = in.read(b)) != -1;) {
            out.append(new String(b, 0, n));
        }
        return out.toString();
    }

}
