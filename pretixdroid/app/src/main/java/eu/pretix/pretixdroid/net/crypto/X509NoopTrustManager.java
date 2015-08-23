package eu.pretix.pretixdroid.net.crypto;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;

import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;

/**
 * This class provides an X509 Trust Manager trusting every certificate.
 */
public class X509NoopTrustManager implements X509TrustManager {
    public List<String> fingerprints;

    public X509NoopTrustManager() {
        fingerprints = new ArrayList<String>();
    }

    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[]{};
    }

    public void checkClientTrusted(X509Certificate[] certs, String authType)
            throws CertificateException {
    }

    public void checkServerTrusted(X509Certificate[] certs, String authType)
            throws CertificateException {
        for (X509Certificate cert : certs) {
            try {
                String fingerprint = new String(Hex.encodeHex(DigestUtils
                        .sha1(cert.getEncoded())));
                fingerprints.add(fingerprint);
            } catch (CertificateEncodingException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * This hostname verifier does not verify hostnames. This is not necessary,
     * though, as we only accept one single certificate.
     */
    public class HostnameVerifier implements javax.net.ssl.HostnameVerifier {
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    }
}
