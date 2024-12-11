package org.portablecl.pocl_rreferenceandroidjavaclient;

import static org.jocl.CL.CL_DEVICE_NAME;
import static org.jocl.CL.CL_DEVICE_TYPE_ALL;
import static org.jocl.CL.CL_DEVICE_VERSION;
import static org.jocl.CL.CL_DRIVER_VERSION;
import static org.jocl.CL.CL_PLATFORM_NAME;
import static org.jocl.CL.clGetDeviceIDs;
import static org.jocl.CL.clGetDeviceInfo;
import static org.jocl.CL.clGetPlatformIDs;
import static org.jocl.CL.clGetPlatformInfo;
import static org.portablecl.pocl_rreferenceandroidjavaclient.NativeUtils.setNativeEnv;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.jocl.Pointer;
import org.jocl.cl_device_id;
import org.jocl.cl_platform_id;
import org.portablecl.pocl_rreferenceandroidjavaclient.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * inspired by the JOCLSamples devices query
 * (
 * <a href="https://github.com/gpu/JOCLSamples/blob/master/src/main/java/org/jocl/samples/JOCLDeviceQuery.java">...</a>
 * )
 */
public class DeviceDemoActivity extends AppCompatActivity {

    /**
     * used to print text on the app screen
     */
    TextView tv;
    private ActivityMainBinding binding;

    /**
     * Returns the value of the device info parameter with the given name
     *
     * @param device The device
     * @param paramName The parameter name
     * @return The value
     */
    private static String getString(cl_device_id device, int paramName) {
        // Obtain the length of the string that will be queried
        long[] size = new long[1];
        clGetDeviceInfo(device, paramName, 0, null, size);

        // Create a buffer of the appropriate size and fill it with the info
        byte[] buffer = new byte[(int) size[0]];
        clGetDeviceInfo(device, paramName, buffer.length, Pointer.to(buffer), null);

        // Create a string from the buffer (excluding the trailing \0 byte)
        return new String(buffer, 0, buffer.length - 1);
    }

    /**
     * Returns the value of the platform info parameter with the given name
     *
     * @param platform The platform
     * @param paramName The parameter name
     * @return The value
     */
    private static String getString(cl_platform_id platform, int paramName) {
        // Obtain the length of the string that will be queried
        long[] size = new long[1];
        clGetPlatformInfo(platform, paramName, 0, null, size);

        // Create a buffer of the appropriate size and fill it with the info
        byte[] buffer = new byte[(int) size[0]];
        clGetPlatformInfo(platform, paramName, buffer.length, Pointer.to(buffer), null);

        // Create a string from the buffer (excluding the trailing \0 byte)
        return new String(buffer, 0, buffer.length - 1);
    }

    /**
     * function to print string both to logcat and the tv textview
     * @param input string to be printed, no need for a newline
     */
    private void logString(String input) {
        Log.w("jocl", input);
        tv.append(input + "\n");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // helper class to get options chosen by the user
        ConfigStore configStore = new ConfigStore(this);

        String cache_dir = getCacheDir().getAbsolutePath();
        System.setProperty("POCL_CACHE_DIR", cache_dir);
        setNativeEnv("POCL_CACHE_DIR", cache_dir);
        String devicesString = configStore.getPoclDevices();
        setNativeEnv("POCL_DEVICES", devicesString);
        String remoteString = configStore.getRemoteIp();
        setNativeEnv("POCL_REMOTE0_PARAMETERS", remoteString);
        setNativeEnv("POCL_DEBUG", "all");

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        tv = binding.sampleText;
        tv.setText("");

        try {
            // Obtain the number of platforms
            int[] numPlatforms = new int[1];
            clGetPlatformIDs(0, null, numPlatforms);

            String displayString = "";
            displayString = "Number of platforms: " + numPlatforms[0];
            logString(displayString);

            // Obtain the platform IDs
            cl_platform_id[] platforms = new cl_platform_id[numPlatforms[0]];
            clGetPlatformIDs(platforms.length, platforms, null);

            // Collect all devices of all platforms
            List<cl_device_id> devices = new ArrayList<cl_device_id>();
            for (int i = 0; i < platforms.length; i++) {
                String platformName = getString(platforms[i], CL_PLATFORM_NAME);

                // Obtain the number of devices for the current platform
                int[] numDevices = new int[1];
                clGetDeviceIDs(platforms[i], CL_DEVICE_TYPE_ALL, 0, null, numDevices);

                displayString = "Number of devices in platform " + platformName + ": " + numDevices[0];
                logString(displayString);

                cl_device_id[] devicesArray = new cl_device_id[numDevices[0]];
                clGetDeviceIDs(platforms[i], CL_DEVICE_TYPE_ALL, numDevices[0], devicesArray, null);

                devices.addAll(Arrays.asList(devicesArray));
            }

            // Print some info on each device
            for (int i = 0; i < devices.size(); i++) {
                cl_device_id device = devices.get(i);
                String deviceName = getString(device, CL_DEVICE_NAME);
                displayString = String.format("device %d name: %s", i, deviceName);
                logString(displayString);

                String deviceVersion = getString(device, CL_DEVICE_VERSION);
                displayString = String.format("device %d version: %s", i, deviceVersion);
                logString(displayString);

                String deviceDriverVersion = getString(device, CL_DRIVER_VERSION);
                displayString = String.format("device %d driver version: %s", i, deviceDriverVersion);
                logString(displayString);
            }
        }catch (OutOfMemoryError | Exception e) {
            Toast.makeText( this, "error occurred, possibly incorrect server address?",
                    Toast.LENGTH_LONG).show();
        }
    }

    /**
     * override the destroy function to restart the app. This allows pocl to be destroyed and
     * reinitialized with different parameters.
     */
    @Override
    protected void onDestroy() {

        // exit and restart the app after going back to startup activity
        Context appctx = getApplicationContext();
        Intent i = appctx.getPackageManager().getLaunchIntentForPackage(appctx.getPackageName());
        Intent restartIntent = Intent.makeRestartActivityTask(i.getComponent());
        appctx.startActivity(restartIntent);
        Runtime.getRuntime().exit(0);

        super.onDestroy();
    }
}