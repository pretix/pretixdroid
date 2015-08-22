package eu.pretix.pretixdroid.net.server;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.ssl.SslSelectChannelConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import eu.pretix.pretixdroid.R;
import eu.pretix.pretixdroid.net.crypto.SSLUtils;
import eu.pretix.pretixdroid.ui.MainActivity;

public class ServerService extends Service {
    public static final int NOTIFICATION_ID = 0x00001;
    public static final int PORT = 8765;

    public static boolean RUNNING = false;
    public static String SECRET = "";

    private String KEYSTORE_PASSWORD = "Y5>?x^gv|;1[";

    private Server server;

    @Override
    public void onCreate() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        SharedPreferences sp = getSharedPreferences("server",
                Context.MODE_PRIVATE);
        SECRET = sp.getString("secret", null);

        Handler handler = new AbstractHandler() {
            public void handle(String target, Request request,
                               HttpServletRequest servletRequest,
                               HttpServletResponse servletResponse) throws IOException,
                    ServletException {
                //((Request) request).setHandled(true);
            }
        };

        try {
            SslContextFactory sslContextFactory = new SslContextFactory();
            // InputStream in =
            // getResources().openRawResource(R.raw.ssl_keystore);
            InputStream in = openFileInput(SSLUtils.FILE_NAME);
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
            sslContextFactory.setIncludeProtocols("TLS");
            sslContextFactory
                    .setIncludeCipherSuites("TLS_DHE_RSA_WITH_AES_128_CBC_SHA");

            server = new Server();
            SslSelectChannelConnector sslConnector = new SslSelectChannelConnector(
                    sslContextFactory);
            sslConnector.setPort(PORT);
            server.addConnector(sslConnector);
            server.setHandler(handler);
            server.start();
        } catch (Exception e) {
            stopSelf();
            e.printStackTrace();
            return START_REDELIVER_INTENT;
        }
        Notification notification = new NotificationCompat.Builder(this)
                .setContentTitle(getString(R.string.app_name))
                .setContentIntent(
                        PendingIntent.getActivity(this, 0, new Intent(this,
                                MainActivity.class), 0))
                .setContentText(getString(R.string.server_running))
                .setSmallIcon(R.drawable.ic_logo).build();
        startForeground(NOTIFICATION_ID, notification);

        RUNNING = true;
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        RUNNING = false;
        if (server != null) {
            try {
                server.stop();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        stopForeground(true);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return null;
    }
}
