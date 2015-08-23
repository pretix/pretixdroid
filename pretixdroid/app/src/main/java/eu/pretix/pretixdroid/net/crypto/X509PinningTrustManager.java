package eu.pretix.pretixdroid.net.crypto;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;

import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;

/**
 * This class provides an X509 Trust Manager trusting only one certificate.
 */
public class X509PinningTrustManager implements X509TrustManager {

    String pinned = null;

    /**
     * Creates the Trust Manager.
     *
     * @param pinnedFingerprint The certificate to be pinned. Expecting a SHA1 fingerprint in
     *                          lowercase without colons.
     */
    public X509PinningTrustManager(String pinnedFingerprint) {
        pinned = pinnedFingerprint;
    }

    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[]{};
    }

    public void checkClientTrusted(X509Certificate[] certs, String authType)
            throws CertificateException {
        checkServerTrusted(certs, authType);
    }

    public void checkServerTrusted(X509Certificate[] certs, String authType)
            throws CertificateException {
        for (X509Certificate cert : certs) {
            try {
                String fingerprint = new String(Hex.encodeHex(DigestUtils
                        .sha1(cert.getEncoded())));
                if (pinned.equals(fingerprint))
                    return;
            } catch (CertificateEncodingException e) {
                e.printStackTrace();
            }
        }
        throw new CertificateException("Certificate did not match, pinned to "
                + pinned);
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
