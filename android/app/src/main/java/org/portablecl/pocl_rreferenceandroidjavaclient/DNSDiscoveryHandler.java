package org.portablecl.pocl_rreferenceandroidjavaclient;

import android.app.Activity;
import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;

/**
 * Class that implements necessary methods for utilizing Android's NSD (Network Service Discovery)
 * service for network discovery using mDNS and DNS-SD protocols.
 */
public class DNSDiscoveryHandler {
    private NsdManager nsdManager; // Manages network service discovery
    private NsdManager.DiscoveryListener discoveryListener; // Listens for discovery events
    public static final String MDNS_SERVICE_TYPE = "_pocl._tcp"; // The service type to discover
    public static final String TAG = "DISC";

    /*
     * Instance of RemoteDiscoveryManager to manage discovered devices.
     * This is specific to the current use case and may need modification
     * if reused for other applications.
     */
    RemoteDiscoveryManager remoteDiscoveryManager;

    /**
     * Initializes the DNS discovery process. Sets up the NSD Manager, stops any existing
     * discoveries, and starts discovering services.
     */
    public void init(RemoteDiscoveryManager ds, Activity activity) {
        // Obtain NSD service from the application context
        nsdManager =
                (NsdManager) activity.getApplicationContext().getSystemService(Context.NSD_SERVICE);
        // Assign the RemoteDiscoveryManager instance
        remoteDiscoveryManager = ds;

        stop(); // Stop any ongoing discovery process, if any
        setupDiscoveryListener(); // Initialize discovery listener
        nsdManager.discoverServices(MDNS_SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener);
    }

    /**
     * Stops the current service discovery process.
     */
    public void stop() {
        if (discoveryListener != null) {
            try {
                nsdManager.stopServiceDiscovery(discoveryListener);
                discoveryListener = null;
                nsdManager = null;
                remoteDiscoveryManager = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Sets up the DiscoveryListener to handle service discovery events such as
     * service found, lost, or any discovery failures.
     */
    private void setupDiscoveryListener() {
        discoveryListener = new NsdManager.DiscoveryListener() {
            @Override
            public void onStartDiscoveryFailed(String s, int i) {
                Log.e(TAG, "Discovery failed: Error code: " + i);
            }

            @Override
            public void onStopDiscoveryFailed(String s, int i) {
                Log.e(TAG, "Discovery failed: Error code: " + i);
            }

            @Override
            public void onDiscoveryStarted(String s) {
                Log.d(TAG, "Discovery started:  " + s);
            }

            @Override
            public void onDiscoveryStopped(String s) {
                Log.d(TAG, "Discovery stopped:  " + s);
            }

            @Override
            public void onServiceFound(NsdServiceInfo nsdServiceInfo) {
                Log.d(TAG, "Service found: " + nsdServiceInfo);
                // Attempt to resolve the service
                nsdManager.resolveService(nsdServiceInfo, setupResolveListener());
            }

            @Override
            public void onServiceLost(NsdServiceInfo nsdServiceInfo) {
                Log.d(TAG, "Service lost: " + nsdServiceInfo);
                // Remove the lost service from the spinner
                remoteDiscoveryManager.removeDeviceFromSpinner(nsdServiceInfo.getServiceName());
            }
        };
    }

    /**
     * Sets up the ResolveListener to handle events for resolving a discovered service.
     */
    private NsdManager.ResolveListener setupResolveListener() {
        return new NsdManager.ResolveListener() {
            @Override
            public void onResolveFailed(NsdServiceInfo nsdServiceInfo, int i) {
                Log.e(TAG, "Resolve failed: Error code" + i);
            }

            @Override
            public void onServiceResolved(NsdServiceInfo nsdServiceInfo) {
                Log.d(TAG,
                        "Service resolved: " + nsdServiceInfo.getHost().toString() + ":" + nsdServiceInfo.getPort());
                // Pass resolved service to the RemoteDiscoveryManager
                remoteDiscoveryManager.addDiscoveredServer(nsdServiceInfo);
            }
        };
    }

}
