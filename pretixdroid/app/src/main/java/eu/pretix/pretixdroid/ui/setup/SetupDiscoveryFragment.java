package eu.pretix.pretixdroid.ui.setup;


import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.lang.reflect.Array;
import java.net.InetAddress;

import eu.pretix.pretixdroid.PretixDroid;
import eu.pretix.pretixdroid.R;
import eu.pretix.pretixdroid.api.PretixApi;
import eu.pretix.pretixdroid.net.crypto.CryptoUtils;
import eu.pretix.pretixdroid.net.crypto.SSLUtils;
import eu.pretix.pretixdroid.net.discovery.DiscoveredDevice;
import eu.pretix.pretixdroid.net.server.ServerService;

/**
 * A simple {@link Fragment} subclass.
 */
public class SetupDiscoveryFragment extends Fragment {
    private Callbacks callbacks;
    private View view;
    private NsdManager nsdManager;
    private NsdManager.DiscoveryListener nsdDiscoveryListener;
    private NsdManager.RegistrationListener nsdRegistrationListener;
    private NsdManager.ResolveListener nsdResolveListener;
    private NsdServiceInfo nsdServiceInfo;
    private String serviceName;
    private DeviceAdapter listAdapter;
    private ListView listView;
    private BroadcastReceiver connChangeReceiver;

    public SetupDiscoveryFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_setup_discovery, container, false);
        listView = (ListView) view.findViewById(R.id.lvDevices);
        listAdapter = new DeviceAdapter(getActivity());
        listView.setAdapter(listAdapter);
        return view;
    }

    public void updateText() {
        ConnectivityManager cm =
                (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
        boolean isWiFi = activeNetwork != null && activeNetwork.getType() == ConnectivityManager.TYPE_WIFI;
        TextView tvInstructions = (TextView) view.findViewById(R.id.tvInstructions);
        if (isConnected && isWiFi) {
            tvInstructions.setText(R.string.setup_discovery_instructions);
        } else {
            tvInstructions.setText(R.string.setup_discovery_wifi);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Activities containing this fragment must implement its callbacks.
        if (!(activity instanceof Callbacks)) {
            throw new IllegalStateException(
                    "Activity must implement fragment's callbacks.");
        }

        callbacks = (Callbacks) activity;

        connChangeReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                updateText();
            }
        };
        getActivity().registerReceiver(connChangeReceiver, new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));

        if (!ServerService.RUNNING) {
            getActivity().startService(new Intent(getActivity(), ServerService.class));
        }

        initnsd();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        callbacks = null;
        getActivity().unregisterReceiver(connChangeReceiver);
    }

    public interface Callbacks {
    }

    @Override
    public void onPause() {
        super.onPause();
        if (nsdManager != null) {
            nsdManager.unregisterService(nsdRegistrationListener);
            nsdManager.stopServiceDiscovery(nsdDiscoveryListener);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        nsdManager.registerService(nsdServiceInfo, NsdManager.PROTOCOL_DNS_SD, nsdRegistrationListener);
        nsdManager.discoverServices("_http._tcp.", NsdManager.PROTOCOL_DNS_SD, nsdDiscoveryListener);
    }

    public class DeviceAdapter extends ArrayAdapter<DiscoveredDevice> {

        public DeviceAdapter(Context context) {
            super(context, R.layout.listitem_discovered_device);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                LayoutInflater vi;
                vi = LayoutInflater.from(getContext());
                v = vi.inflate(R.layout.listitem_discovered_device, null);
            }

            DiscoveredDevice d = getItem(position);

            if (d != null) {
                TextView tvDeviceIp = (TextView) v.findViewById(R.id.tvDeviceIp);

                if (tvDeviceIp != null) {
                    tvDeviceIp.setText(d.getServiceInfo().getHost().toString());
                }
            }

            return v;
        }

        public DiscoveredDevice find(DiscoveredDevice lookup) {
            for (int i = 0; i < getCount(); i++) {
                if (getItem(i).equals(lookup)) {
                    return getItem(i);
                }
            }
            return null;
        }
    }

    public void initnsd() {
        nsdServiceInfo = new NsdServiceInfo();
        nsdServiceInfo.setServiceName(PretixDroid.SERVICE_NAME);
        nsdServiceInfo.setServiceType("_http._tcp.");
        nsdServiceInfo.setPort(ServerService.PORT);
        /*
        String fingerprint = SSLUtils.getSHA1Hash(getActivity(), PretixDroid.KEYSTORE_PASSWORD);
        nsdServiceInfo.setAttribute("sslkey", fingerprint);
        SharedPreferences settings = getActivity().getSharedPreferences(PretixApi.PREFS_NAME, 0);
        nsdServiceInfo.setAttribute("sslauth", CryptoUtils.authenticatedFingerprint(
                fingerprint, settings.getString("key", null)
        ))
        */

        nsdManager = (NsdManager) getActivity().getSystemService(Context.NSD_SERVICE);

        nsdRegistrationListener = new NsdManager.RegistrationListener() {
            @Override
            public void onServiceRegistered(NsdServiceInfo NsdServiceInfo) {
                // Save the service name.  Android may have changed it in order to
                // resolve a conflict, so update the name you initially requested
                // with the name Android actually used.
                serviceName = NsdServiceInfo.getServiceName();
                Log.e("discovery", "Registration succeeded: " + serviceName);
            }

            @Override
            public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Log.e("discovery", "Registration failed: " + errorCode);
            }

            @Override
            public void onServiceUnregistered(NsdServiceInfo arg0) {
            }

            @Override
            public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Log.e("discovery", "Unregistration failed: " + errorCode);
            }
        };

        nsdResolveListener = new NsdManager.ResolveListener() {

            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Log.e("discovery", "Resolve failed: " + errorCode);
            }

            @Override
            public void onServiceResolved(final NsdServiceInfo serviceInfo) {
                Log.d("discovery", "Resolve Succeeded: " + serviceInfo);

                if (serviceInfo.getServiceName().equals(serviceName)) {
                    Log.d("discovery", "Resolve was on same IP.");
                    return;
                }
                InetAddress host = serviceInfo.getHost();
                Log.d("discovery", "Resolve found host: " + host);
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        listAdapter.add(new DiscoveredDevice(serviceInfo));
                    }
                });
            }
        };

        // Instantiate a new DiscoveryListener
        nsdDiscoveryListener = new NsdManager.DiscoveryListener() {

            //  Called as soon as service discovery begins.
            @Override
            public void onDiscoveryStarted(String regType) {
                Log.d("discovery", "Service discovery started");
            }

            @Override
            public void onServiceFound(NsdServiceInfo service) {
                if (!service.getServiceType().equals("_http._tcp.")) {
                    Log.d("discovery", "Found unknown service: " + service.getServiceType());
                } else if (service.getServiceName().equals(serviceName)) {
                    Log.d("discovery", "Found service on the same machine: " + serviceName);
                } else if (service.getServiceName().contains(PretixDroid.SERVICE_NAME)) {
                    Log.d("discovery", "Found remote service: " + service);
                    nsdManager.resolveService(service, nsdResolveListener);
                }
            }

            @Override
            public void onServiceLost(final NsdServiceInfo serviceInfo) {
                Log.e("discovery", "Service lost" + serviceInfo);
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        DiscoveredDevice inList = listAdapter.find(new DiscoveredDevice(serviceInfo));
                        if (inList != null) {
                            listAdapter.remove(inList);
                        }
                    }
                });
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.i("discovery", "Discovery stopped: " + serviceType);
            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.e("discovery", "Discovery failed: Error code:" + errorCode);
                nsdManager.stopServiceDiscovery(this);
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.e("discovery", "Stopping discovery failed: Error code:" + errorCode);
                nsdManager.stopServiceDiscovery(this);
            }
        };
    }
}
