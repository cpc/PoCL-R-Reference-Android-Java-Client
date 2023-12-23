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
     * @param key / name of the variable
     * @param value the value to set the variable to
     */
    public static native void setNativeEnv(String key, String value);
}
