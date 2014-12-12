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
import com.vmware.hyperic.model.relations.ResourceSubType;
import static com.vmware.hyperic.model.relations.ResourceTier.LOGICAL;
import static com.vmware.hyperic.model.relations.ResourceTier.SERVER;
import java.io.ByteArrayInputStream;
import static org.hyperic.plugin.vrealize.automation.VraConstants.*;
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
import org.hyperic.hq.product.ServerResource;
import org.hyperic.sigar.SigarException;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.http.HQHttpClient;
import org.hyperic.util.http.HttpConfig;
import org.w3c.dom.Document;

/**
 *
 * @author glaullon
 */
public class DiscoveryVRAIaasWeb extends DaemonDetector {

    private static final Log log = LogFactory.getLog(DiscoveryVRAServer.class);

    @Override
    protected ServerResource newServerResource(long pid, String exe) {
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
            String mmwebPath = executeXMLQuery("//application[@applicationPool='RepositoryAppPool']/virtualDirectory[@path='/']/@physicalPath", configFile);
            if (mmwebPath != null) {
                File installPath = new File(mmwebPath, "../..");
                log.debug("[newServerResource] installPath=" + getCanonicalPath(installPath));
                server.setInstallPath(getCanonicalPath(installPath));
                server.setIdentifier(server.getIdentifier().replace("%installpath%", server.getInstallPath()));

                File dataCfg = new File(installPath, "Server/Model Manager Data/Repoutil.exe.config");
                log.debug("[newServerResource] dataCfg=" + getCanonicalPath(dataCfg));
                String vRAIaaSWebLoadBalancer = executeXMLQuery("//add[@key='repositoryAddress']/@value", dataCfg);
                vRAIaaSWebLoadBalancer = vRAIaaSWebLoadBalancer.replaceAll("\\w*://([^:/]*).*", "$1");
                log.debug("[newServerResource] vRAIaaSWebLoadBalancer=" + vRAIaaSWebLoadBalancer);

                File webCfg = new File(installPath, "Server/Model Manager Web/Web.config");
                log.debug("[newServerResource] webCfg=" + getCanonicalPath(webCfg));
                String vRAServer = executeXMLQuery("//repository/@store", webCfg);
                vRAServer = vRAServer.replaceAll("\\w*://([^:/]*).*", "$1");
                log.debug("[newServerResource] vRAServer=" + vRAServer);

                final String vcoFqdn = getVCO();
                log.debug("[newServerResource] vcoFqdn=" + vcoFqdn);

                // TODO: implement discovery of IaaS Web Server component
                final String iaasWebServerFqdn = getPlatformName();

                String model = VRAUtils.marshallResource(getIaaSWebServerRelationsModel(vRAServer, iaasWebServerFqdn, vRAIaaSWebLoadBalancer, vcoFqdn));
                ConfigResponse c = new ConfigResponse();
                c.setValue(PROP_EXTENDED_REL_MODEL, new String(Base64.encodeBase64(model.getBytes())));

                // do not remove, why? please don't ask.
                server.setProductConfig(server.getProductConfig());
                server.setCustomProperties(c);

            }
        }
        return server;
    }

    private static String getVCO() {
        String vcoFNQ = null;
        String xml = null;
        try {
            AgentKeystoreConfig ksCFG = new AgentKeystoreConfig();
            HQHttpClient client = new HQHttpClient(ksCFG, new HttpConfig(5000, 5000, null, 0), ksCFG.isAcceptUnverifiedCert());

            List<String> authpref = new ArrayList<String>();
            authpref.add(AuthPolicy.NTLM);
            authpref.add(AuthPolicy.BASIC);
            client.getParams().setParameter(AuthPNames.TARGET_AUTH_PREF, authpref);

            client.getCredentialsProvider().setCredentials(
                    new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT, AuthPolicy.NTLM),
                    new NTCredentials("svc_vcac", "VMware1!", "localhost", "refarch"));

            HttpGet get = new HttpGet("https://localhost/Repository/Data/ManagementModelEntities.svc/ManagementEndpoints");
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

    public static Resource getIaaSWebServerRelationsModel(String vRaApplicationFqdn, String iaasWebServerFqdn, String vRAIaaSWebLoadBalancerFqdn, String vcoFqdn) {

        log.debug("[newServerResource] vRaApplicationFqdn=" + vRaApplicationFqdn);
        log.debug("[newServerResource] iaasWebServerFqdn=" + iaasWebServerFqdn);
        log.debug("[newServerResource] vRAIaaSWebLoadBalancerFqdn=" + vRAIaaSWebLoadBalancerFqdn);
        log.debug("[newServerResource] vcoFqdn=" + vcoFqdn);

        ObjectFactory factory = new ObjectFactory();

        Resource iaasWebServer = factory.createResource(!CREATE_IF_NOT_EXIST, TYPE_VRA_IAAS_WEB,
                iaasWebServerFqdn, SERVER);

        Resource iaasWebServerTag = createLogialResource(factory, TYPE_VRA_IAAS_WEB_GROUP, vRaApplicationFqdn);
        iaasWebServer.addRelations(factory.createRelation(iaasWebServerTag, PARENT));

        Resource vraAppTagResource = getVraAppTag(factory, TYPE_VRA_APPLICATION, vRaApplicationFqdn);
        iaasWebServerTag.addRelations(factory.createRelation(vraAppTagResource, PARENT));

        //This is only if there is a load balancer. If not then nothing load balancer related should be created.
        Resource loadBalancer = factory.createResource(Boolean.FALSE, TYPE_VRA_IAAS_WEB_LOAD_BALANCER, vRAIaaSWebLoadBalancerFqdn, SERVER);
        iaasWebServer.addRelations(factory.createRelation(loadBalancer, SIBLING));

        Resource loadBalancerTag = createLogialResource(factory, TYPE_VRA_LOAD_BALANCER, vRaApplicationFqdn);
        loadBalancer.addRelations(factory.createRelation(loadBalancerTag, PARENT));
        loadBalancerTag.addRelations(factory.createRelation(vraAppTagResource, PARENT));

        Resource loadBalancerSuperTag = createLogialResource(factory, TYPE_LOAD_BALANCER, TYPE_VRA_LOAD_BALANCER);
        loadBalancerTag.addRelations(factory.createRelation(loadBalancerSuperTag, PARENT));

        Resource vco = factory.createResource(!CREATE_IF_NOT_EXIST, TYPE_VRA_VCO, vcoFqdn, SERVER);
        iaasWebServer.addRelations(factory.createRelation(vco, SIBLING));

        Resource vcoTag = createLogialResource(factory, TYPE_VCO, vRaApplicationFqdn);
        vcoTag.addRelations(factory.createRelation(vraAppTagResource, PARENT));
        vco.addRelations(factory.createRelation(vcoTag, PARENT, CREATE_IF_NOT_EXIST));

        /**
         * Adding both vRAVA directly and the vRAVA load balancer because we
         * won't to which the FQDN we have points.
         */
        Resource vrava = factory.createResource(!CREATE_IF_NOT_EXIST, TYPE_VRA_SERVER, vRaApplicationFqdn, SERVER);
        iaasWebServer.addRelations(factory.createRelation(vrava, SIBLING));

        final String vravaLoadBalancerName = vRaApplicationFqdn;
        //final String vravaLoadBalancerType = "Manager Server Load Balancer Service";
        Resource vravaPossibleLoadBalancer = factory.createResource(!CREATE_IF_NOT_EXIST, TYPE_VRA_VA_LOAD_BALANCER,
                vravaLoadBalancerName, SERVER);
        iaasWebServer.addRelations(factory.createRelation(vravaPossibleLoadBalancer, SIBLING, !CREATE_IF_NOT_EXIST));

        //final String vravaTagType = "vRealize Automation Server";
        Resource vravaTag = createLogialResource(factory, TYPE_VRA_SERVER, vRaApplicationFqdn);
        vrava.addRelations(factory.createRelation(vravaTag, PARENT, CREATE_IF_NOT_EXIST));
        vravaTag.addRelations(factory.createRelation(vraAppTagResource, PARENT));

        return iaasWebServer;
    }

    private static Resource createLogialResource(ObjectFactory objectFactory, String appName, String type) {
        return createLogicalResource(objectFactory, type, VRAUtils.getFullResourceName(type, appName));
    }

    private static Resource createLogicalResource(ObjectFactory objectFactory, String iassWebServerTagType,
            String iassWebServerTagName) {
        return objectFactory.createResource(CREATE_IF_NOT_EXIST, iassWebServerTagType, iassWebServerTagName,
                LOGICAL, ResourceSubType.TAG);
    }

    private static Resource getVraAppTag(ObjectFactory factory, String vRaApplicationName, String vraAppTagType) {
        final String vraAppTagName = VRAUtils.getFullResourceName(vraAppTagType, vRaApplicationName);
        Identifier vraAppTagIdentifier = factory.createIdentifier("application.name", vRaApplicationName);
        Resource vraAppTagResource = createLogicalResource(factory, vraAppTagType, vraAppTagName);
        vraAppTagResource.addIdentifiers(vraAppTagIdentifier);
        return vraAppTagResource;
    }

    private String executeXMLQuery(String path, File xmlFile) {
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

    public static String readInputString(InputStream in) throws IOException {
        StringBuilder out = new StringBuilder();
        byte[] b = new byte[4096];
        for (int n; (n = in.read(b)) != -1;) {
            out.append(new String(b, 0, n));
        }
        return out.toString();
    }
}
