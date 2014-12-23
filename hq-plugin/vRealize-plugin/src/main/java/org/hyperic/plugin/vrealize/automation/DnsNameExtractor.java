/**
 *
 */
package org.hyperic.plugin.vrealize.automation;

import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class DnsNameExtractor{
    private static final Log log = LogFactory.getLog(DnsNameExtractor.class);
    private SSLSocketFactory originalDefaultSshSocketFactory = null;
    private final int DNS_NAME = 2;;

    public Collection<String> getDnsNames(final String url) throws Exception {
        Collection <String> names = new HashSet<String>();
        // Example: "https://ra-vco-a2-bg-01.refarch.eng.vmware.com:8281"
        final Certificate[] certificates = getCertificates(url);

        for (Certificate cert : certificates) {
            if (cert instanceof X509Certificate) {
                names.addAll(getDnsNames((X509Certificate) cert));
            }
        }

        return names;
    }

    private Certificate[] getCertificates(String url) throws Exception {
        try {
            disableCertificateValidation();

            URL destinationURL = new URL(url);
            HttpsURLConnection httpsURLConnection = (HttpsURLConnection) destinationURL.openConnection();
            httpsURLConnection.connect();
            return httpsURLConnection.getServerCertificates();
        }
        finally {
            resetCertificateValidation();
        }
    }

    /*
     * Example:
     *
     * Certificate subject: 'CN=ra-vco-a2-bg-00.refarch.eng.vmware.com, OU=Refarch, O=Eng, L=London, ST=London, C=GB'
     * Alternative DNS name: 'ra-vco-a2-bg-01'
     * Alternative DNS name: 'ra-vco-a2-bg-01.refarch.eng.vmware.com'
     * Alternative DNS name: 'ra-vco-a2-bg-02'
     * Alternative DNS name: 'ra-vco-a2-bg-02.refarch.eng.vmware.com'
     * Alternative DNS name: 'ra-vco-a2-bg-00'
     * Alternative DNS name: 'ra-vco-a2-bg-00.refarch.eng.vmware.com'
     */
    private Collection <String> getDnsNames(X509Certificate cert) throws CertificateParsingException {
        X509Certificate x509Certificate = cert;
        Collection <String> names = new HashSet<String>();
        final String subject = (x509Certificate.getSubjectDN().getName());
        log.debug(String.format("Certificate subject: '%s'", subject));
        String cn =  getCn(subject);
        if (StringUtils.isNotEmpty(cn)){
            names.add(cn);
        }
        for (@SuppressWarnings("rawtypes") List alternativeNames : x509Certificate.getSubjectAlternativeNames()) {
            Integer altNameType=(Integer)(alternativeNames.get(0));
            if(altNameType == DNS_NAME){
                String aName = "" + alternativeNames.get(1);
                if (StringUtils.isNotEmpty(aName)){
                    names.add(aName);
                    log.debug(String.format("Alternative DNS name: '%s'", alternativeNames.get(1)));
                }
            }
        }

        return names;
    }

    /**
     * @param subject
     * @return
     */
    private String getCn(String subject) {
        String [] subjectTokens = subject.split(",");
        for (int i=0; i < subjectTokens.length ; i++){
            if (!StringUtils.isEmpty(subjectTokens[i]) && subjectTokens[i].startsWith("CN=")){
                return subjectTokens[i].substring(3);
            }
        }
        return null;
    }

    private void resetCertificateValidation() {
        if (originalDefaultSshSocketFactory != null) {
            HttpsURLConnection.setDefaultSSLSocketFactory(originalDefaultSshSocketFactory);
        }
    }

    private void disableCertificateValidation() throws NoSuchAlgorithmException, KeyManagementException {
        TrustManager[] naiveTrustManager = getNaiveTrustManager();
        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, naiveTrustManager, new java.security.SecureRandom());
        originalDefaultSshSocketFactory = HttpsURLConnection.getDefaultSSLSocketFactory();

        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
    }

    private TrustManager[] getNaiveTrustManager() {
        return new TrustManager[] {
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                    public void checkClientTrusted(
                            X509Certificate[] certs, String authType) {
                    }
                    public void checkServerTrusted(
                            X509Certificate[] certs, String authType) {
                    }
                }
        };
    }
}