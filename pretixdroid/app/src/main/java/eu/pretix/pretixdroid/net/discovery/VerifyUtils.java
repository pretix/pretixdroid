package eu.pretix.pretixdroid.net.discovery;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import eu.pretix.pretixdroid.net.crypto.CryptoUtils;
import eu.pretix.pretixdroid.net.crypto.X509NoopTrustManager;
import eu.pretix.pretixdroid.service.ServerService;

public class VerifyUtils {

    public static DiscoveredDevice verifyHost (DiscoveredDevice device, String secret) {
        X509NoopTrustManager trustManager = new X509NoopTrustManager();

        SSLContext sc = null;
        HttpsURLConnection urlConnection = null;
        try {
            URL url = new URL("https://" + device.getServiceInfo().getHost().getHostAddress() + ":"
                    + ServerService.PORT + "/verifykey");
            urlConnection = (HttpsURLConnection) url.openConnection();

            urlConnection.setHostnameVerifier(trustManager.new HostnameVerifier());
            sc = SSLContext.getInstance("TLS");
            sc.init(null, new TrustManager[]{trustManager}, new SecureRandom());
            urlConnection.setSSLSocketFactory(sc.getSocketFactory());

            BufferedReader br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
            br.close();

            String receivedMac = sb.toString().trim();
            String fingerprint = trustManager.fingerprints.get(0);
            if (receivedMac.equals(CryptoUtils.authenticatedFingerprint(fingerprint, secret))) {
                device.setState(DiscoveredDevice.State.VERIFIED);
                device.setFingerprint(fingerprint);
            } else {
                device.setState(DiscoveredDevice.State.KEYMISMATCH);
            }
        } catch (IOException | NoSuchAlgorithmException | KeyManagementException e) {
            e.printStackTrace();
            device.setState(DiscoveredDevice.State.ERROR);
        } finally {
            if (urlConnection != null)
                urlConnection.disconnect();
        }
        return device;
    }
}
