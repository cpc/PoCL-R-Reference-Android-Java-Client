package org.portablecl.pocl_rreferenceandroidjavaclient;

/**
 * Helper utilities that are executing in native code for convenience
 */
public class NativeUtils {
    // load the native library
    static {
        System.loadLibrary("pocl_rreferenceandroidjavaclient");
    }

    /**
     * Set environment variables which are used to configure pocl.
     * see http://portablecl.org/docs/html/using.html#tuning-pocl-behavior-with-env-variables
     * for a list of options
     *
     * @param key   / name of the variable
     * @param value the value to set the variable to
     */
    public static native void setNativeEnv(String key, String value);

    /**
     * Dynamically add discovered server and its devices to the PoCL runtime through the remote
     * driver.
     *
     * @param id Unique ID with which server advertises itself.
     * @param domain Domain name in which the server was found.
     * @param IpPort Combination of "IP:port"
     * @param type Type of the discovered service. Eg type: "_pocl._tcp"
     * @param deviceCount Number of devices in the remote server.
     */
    public static native void remoteAddServer(String id, String domain, String IpPort, String type, int deviceCount);
}
