package org.portablecl.pocl_rreferenceandroidjavaclient;

import android.app.Activity;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;
import android.widget.AdapterView;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * This class handles the discovery of remote devices and manages the spinner for choosing available
 * devices. It implements necessary sub-classes for server information and device selection entries,
 * and includes methods for adding/removing discovered devices.
 */
public class RemoteDiscoveryManager {

    // Information about a discovered server
    public class ServerInfo {
        String serverID;
        String serverIP;
        String serverInfo;
        int deviceCount;

        public ServerInfo(String serverID, String serverIP, String serverInfo, int deviceCount) {
            this.serverID = serverID;
            this.serverIP = serverIP;
            this.serverInfo = serverInfo;
            this.deviceCount = deviceCount;
        }
    }

    // Represents a device in the spinner, with an address and type.
    public class DeviceSelectionEntry {
        public String deviceAddress;
        public String deviceType;

        // Default constructor with a placeholder value.
        public DeviceSelectionEntry() {
            this.deviceAddress = DEFAULT_SPINNER_LABEL;
        }

        // Constructor with address and device type.
        public DeviceSelectionEntry(String address, String device_type) {
            this.deviceAddress = address;
            this.deviceType = device_type;
        }

        // Returns string description of the device.
        public String getDescription() {
            return (deviceType + " | " + deviceAddress);
        }

        // Returns device address.
        public String getDeviceAddress() {
            return (deviceAddress);
        }
    }

    private final Activity currentActivity;
    public static final String TAG = "DISC";
    public static final String DEFAULT_SPINNER_LABEL = "Select a remote device";
    private final DiscoverySpinnerAdapter deviceSpinnerAdapter;
    public ArrayList<DeviceSelectionEntry> deviceList;

    public static HashMap<String, ServerInfo> discoveredServers;
    private static DNSDiscoveryHandler dnsDiscoveryHandler;

    // Constructor to initialize the RemoteDiscoveryManager with the given activity, spinner, and
    // listener.
    public RemoteDiscoveryManager(Activity activity, Spinner discoverySpinner, AdapterView.OnItemSelectedListener listener) {
        this.currentActivity = activity;
        if (discoveredServers == null) {
            initializeServerMap();
        }

        // Initialize device list and spinner adapter
        deviceList = new ArrayList<>();
        deviceList.add(new DeviceSelectionEntry());
        deviceSpinnerAdapter = new DiscoverySpinnerAdapter(activity, android.R.layout.simple_spinner_item, deviceList);
        discoverySpinner.setAdapter(deviceSpinnerAdapter);
        discoverySpinner.setOnItemSelectedListener(listener);

        // Initialize DNS discovery handler
        dnsDiscoveryHandler = new DNSDiscoveryHandler();
        dnsDiscoveryHandler.init(this, activity);
    }

    // Stops all discovery processes and clears server map.
    public void stopAllDiscoveries() {
        dnsDiscoveryHandler.stop();
        clearServerMap();
    }

    // Initializes the hashmap for discovered servers.
    private void initializeServerMap() {
        clearServerMap();
        discoveredServers = new HashMap<>();
    }

    // Clears the server map to release resources.
    private void clearServerMap() {
        if (discoveredServers != null) {
            discoveredServers.clear();
            discoveredServers = null;
        }
    }

    /**
     * Adds a newly discovered server to the list, updating the server map. This function is called
     * by DNSDiscoveryhandler with the argument of type NsdServiceInfo.
     */
    public void addDiscoveredServer(NsdServiceInfo serviceInfo) {
        String key = serviceInfo.getHost().toString().substring(1) + ":" + serviceInfo.getPort();
        String sName = serviceInfo.getServiceName();
        // NSD discovery adds '{' before the txt sent from the server and adds '=null}' after the
        // txt. These have to be accounted for when using the txt field.
        String txt = serviceInfo.getAttributes().toString();
        int devices = txt.length() - 7; // Adjust the txt string to remove unwanted characters
        txt = txt.substring(1, devices + 1);
        updateServerMap(key, sName, txt, devices);
    }

    /**
     * Updates the server map with the provided information, adding or updating servers.
     */
    private void updateServerMap(String key, String sID, String txt, int deviceNum) {
        if (discoveredServers == null) {
            Log.e(TAG, "(RESOLVER) discoveredServers is null");
            return;
        }
        // logic to decide if a discovered server is new or old, if server already exists, update it
        if (discoveredServers.containsKey(key)) {
            if (discoveredServers.get(key).serverID.equals(sID)) {
                Log.d(TAG, "(RESOLVER) Service " + sID + " is known with same session.");
                addDevicesToSpinner(key, txt, deviceNum);
            } else {
                Log.d(TAG, "(RESOLVER) Service " + sID + " is known but old session has " + "expired" + ".");
                discoveredServers.get(key).serverID = sID;
                discoveredServers.get(key).serverIP = key;
                discoveredServers.get(key).serverInfo = txt;
                discoveredServers.get(key).deviceCount = deviceNum;
                addDevicesToSpinner(key, txt, deviceNum);
            }
        } else {
            String _key = null;
            for (Map.Entry<String, ServerInfo> entry : discoveredServers.entrySet()) {
                if (entry.getValue().serverID.equals(sID)) {
                    _key = entry.getKey();
                }
            }
            if (_key != null) {
                Log.d(TAG, "(RESOLVER) Service " + sID + " is registered with a different " + "address.");
            } else {
                // If server is new, add it to the map
                ServerInfo found = new ServerInfo(sID, key, txt, deviceNum);
                discoveredServers.put(key, found);
                Log.d(TAG, "(RESOLVER) Service " + sID + "is being added.\n");
                addDevicesToSpinner(key, txt, deviceNum);
            }
        }
    }

    /**
     * Adds the devices corresponding to the discovered server into the spinner.
     */
    public void addDevicesToSpinner(String key, String txt, int deviceNum) {
        for (int i = 0; i < deviceNum; i++) {
            // 0:CL_DEVICE_TYPE_CPU , 1:CL_DEVICE_TYPE_GPU , 2:CL_DEVICE_TYPE_ACCELERATOR ,
            // 4:CL_DEVICE_TYPE_CUSTOM
            String deviceType;
            switch (txt.charAt(i)) {
                case '0':
                    deviceType = "CPU";
                    break;
                case '1':
                    deviceType = "GPU";
                    break;
                case '2':
                    deviceType = "Accelerator";
                    break;
                case '4':
                    deviceType = "Custom";
                    break;
                default:
                    deviceType = "NA";
            }

            DeviceSelectionEntry so = new DeviceSelectionEntry(key + "/" + String.valueOf(i), deviceType);

            currentActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    boolean contains = false;
                    // Check if the device is already in the list
                    for (DeviceSelectionEntry value : deviceList) {
                        if (value.deviceAddress.contains(so.deviceAddress)) {
                            contains = true;
                            break;
                        }
                    }
                    // If the device is not already in the list, add it
                    if (!contains) {
                        deviceList.add(so);
                        deviceSpinnerAdapter.notifyDataSetChanged();
                    }
                }
            });
        }
    }

    /**
     * Removes the devices from the spinner for a given server.
     */
    public void removeDeviceFromSpinner(String serverID) {
        String key = null;
        int deviceNum = 0;

        // Find the corresponding server info using the discovered serverID
        for (Map.Entry<String, ServerInfo> entry : discoveredServers.entrySet()) {
            if (Objects.equals(serverID, entry.getValue().serverID)) {
                try {
                    key = entry.getKey();
                    deviceNum = entry.getValue().deviceCount;
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            }
        }

        // If no matching server found then return
        if (key == null) {
            Log.w(TAG, "Key is null. Spinner entry " + serverID + " previously removed.");
            return;
        }

        // If no devices then return
        if (deviceNum == 0) {
            Log.w(TAG, "Number of devices is 0. Nothing to remove from the spinner");
            return;
        }

        // Remove devices from spinner
        for (int i = 0; i < deviceNum; i++) {
            String finalKey = key + "/" + String.valueOf(i);
            currentActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    for (DeviceSelectionEntry value : deviceList) {
                        if (value.deviceAddress.contains(finalKey)) {
                            deviceList.remove(value);
                            deviceSpinnerAdapter.notifyDataSetChanged();
                            break;
                        }
                    }
                }
            });
        }
    }

}
