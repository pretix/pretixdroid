package eu.pretix.pretixdroid.net.server;

import android.content.Context;
import android.content.SharedPreferences;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ssl.SslSelectChannelConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import eu.pretix.pretixdroid.PretixDroid;
import eu.pretix.pretixdroid.net.api.PretixApi;
import eu.pretix.pretixdroid.net.crypto.CryptoUtils;
import eu.pretix.pretixdroid.net.crypto.SSLUtils;

public class WebServer extends Server {
    public WebServer(Context ctx, String KEYSTORE_PASSWORD, int PORT) throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException {
        SslContextFactory sslContextFactory = new SslContextFactory();
        InputStream in = ctx.openFileInput(SSLUtils.FILE_NAME);
        KeyStore keyStore = KeyStore.getInstance("BKS");
        try {
            keyStore.load(in, KEYSTORE_PASSWORD.toCharArray());
        } finally {
            in.close();
        }
        sslContextFactory.setKeyStore(keyStore);
        sslContextFactory.setKeyStorePassword(KEYSTORE_PASSWORD);
        sslContextFactory.setKeyManagerPassword(KEYSTORE_PASSWORD);
        sslContextFactory.setCertAlias(SSLUtils.KEY_ALIAS);
        sslContextFactory.setKeyStoreType("bks");
        sslContextFactory.setIncludeProtocols("TLSv1", "TLSv1.1", "TLSv1.2");
        sslContextFactory.setExcludeProtocols("SSLv3");
        sslContextFactory.setIncludeCipherSuites("TLS_DHE_RSA_WITH_AES_128_CBC_SHA");
        SslSelectChannelConnector sslConnector = new SslSelectChannelConnector(
                sslContextFactory);
        sslConnector.setPort(PORT);
        addConnector(sslConnector);

        ServletContextHandler handler = new ServletContextHandler(this, "/");
        ServletHolder keyVerifyServletHolder = new ServletHolder(VerifyKeyServlet.class);
        String fingerprint = SSLUtils.getSHA1Hash(ctx, PretixDroid.KEYSTORE_PASSWORD);
        SharedPreferences settings = ctx.getSharedPreferences(PretixApi.PREFS_NAME, 0);
        keyVerifyServletHolder.setInitParameter("fingerprint", fingerprint);
        keyVerifyServletHolder.setInitParameter("signature",
                CryptoUtils.authenticatedFingerprint(fingerprint, settings.getString("key", null)));
        handler.addServlet(keyVerifyServletHolder, "/verifykey");

    }
}
