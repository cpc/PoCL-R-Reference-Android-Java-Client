package org.portablecl.pocl_rreferenceandroidjavaclient;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

public class ConfigStore {

    private final SharedPreferences preferences;

    private final SharedPreferences.Editor editor;

    private final String keyPrefix;

    private final static String ipKey = "ipaddresstextkey";

    private final static String devicesKey = "devicestextkey";

    private final static String discoveredIPKey = "discoveredipkey";

    /**
     * @param context can be an activity for example
     */
    public ConfigStore(@NonNull Context context) {
        keyPrefix = context.getResources().getString(R.string.app_name);
        this.preferences = context.getSharedPreferences(keyPrefix+".configstore",
                Context.MODE_PRIVATE);
        this.editor = preferences.edit();
    }

    public String getRemoteIp() {
        return preferences.getString(keyPrefix+ipKey, "0.0.0.0");
    }

    public void setRemoteIp(String ipString) {
        editor.putString(keyPrefix+ipKey, ipString);
    }

    public String getDiscoveredIp() {
        return preferences.getString(keyPrefix+discoveredIPKey, null);
    }
    public void setDiscoveredIp(String dipString) {
        editor.putString(keyPrefix+discoveredIPKey, dipString);
    }

    public String getPoclDevices() {
        return preferences.getString(keyPrefix+devicesKey, "proxy");
    }

    public void setPoclDevices(String devices) {
        editor.putString(keyPrefix+devicesKey, devices);
    }

    public void saveConfig() {
        editor.apply();
    }

}
