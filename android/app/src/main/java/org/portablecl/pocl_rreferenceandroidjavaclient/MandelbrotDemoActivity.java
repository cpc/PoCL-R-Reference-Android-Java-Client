package org.portablecl.pocl_rreferenceandroidjavaclient;

import static org.jocl.CL.CL_CONTEXT_PLATFORM;
import static org.jocl.CL.CL_DEVICE_TYPE_ALL;
import static org.jocl.CL.CL_DEVICE_VERSION;
import static org.jocl.CL.CL_MEM_READ_WRITE;
import static org.jocl.CL.CL_MEM_WRITE_ONLY;
import static org.jocl.CL.CL_TRUE;
import static org.jocl.CL.clBuildProgram;
import static org.jocl.CL.clCreateBuffer;
import static org.jocl.CL.clCreateCommandQueueWithProperties;
import static org.jocl.CL.clCreateContext;
import static org.jocl.CL.clCreateKernel;
import static org.jocl.CL.clCreateProgramWithSource;
import static org.jocl.CL.clEnqueueNDRangeKernel;
import static org.jocl.CL.clEnqueueReadBuffer;
import static org.jocl.CL.clEnqueueWriteBuffer;
import static org.jocl.CL.clGetDeviceIDs;
import static org.jocl.CL.clGetDeviceInfo;
import static org.jocl.CL.clGetPlatformIDs;
import static org.jocl.CL.clReleaseCommandQueue;
import static org.jocl.CL.clReleaseContext;
import static org.jocl.CL.clReleaseDevice;
import static org.jocl.CL.clReleaseMemObject;
import static org.jocl.CL.clReleaseProgram;
import static org.jocl.CL.clSetKernelArg;
import static org.portablecl.pocl_rreferenceandroidjavaclient.NativeUtils.remoteAddServer;
import static org.portablecl.pocl_rreferenceandroidjavaclient.NativeUtils.setNativeEnv;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import org.jocl.CL;
import org.jocl.CLException;
import org.jocl.Pointer;
import org.jocl.Sizeof;
import org.jocl.cl_command_queue;
import org.jocl.cl_context;
import org.jocl.cl_context_properties;
import org.jocl.cl_device_id;
import org.jocl.cl_kernel;
import org.jocl.cl_mem;
import org.jocl.cl_platform_id;
import org.jocl.cl_program;
import org.jocl.cl_queue_properties;
import org.portablecl.pocl_rreferenceandroidjavaclient.databinding.ActivityMendelbrotDemoBinding;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.IntBuffer;
import java.util.Random;

/**
 * and example program adapted from the JOCLSamples (https://github
 * .com/gpu/JOCLSamples/blob/master/src/main/java/org/jocl/samples/JOCLSimpleMandelbrot.java)
 * for Android
 */
public class MandelbrotDemoActivity extends AppCompatActivity {

    private static final int CL_DEVICE_REMOTE_SERVER_IP_POCL = 0x4503;
    private static final int CL_DEVICE_REMOTE_SERVER_PORT_POCL = 0x4504;
    /**
     * an index that can be used to switch between the
     * proxy device and remote
     * 0: proxy
     * 1: remote
     */
    int selectedDeviceIndex = 0;
    /**
     * listen to the switch and set the device accordingly
     */
    private final View.OnClickListener deviceSwithCallback = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            selectedDeviceIndex = ((Switch) v).isChecked() ? 1 : 0;
        }
    };
    /**
     * contains all the image components
     */
    ActivityMendelbrotDemoBinding binding;
    /**
     * a Surfaceview to draw the mandelbrot on
     */
    SurfaceView mandelbrotView;
    /**
     * Used to draw on the mandelbrotView
     */
    SurfaceHolder surfaceHolder;
    /**
     * The width of the image
     */
    private int sizeX = 0;
    /**
     * The height of the image
     */
    private int sizeY = 0;
    /**
     * The OpenCL context
     */
    private cl_context context;
    /**
     * The OpenCL command queue
     */
    private cl_command_queue[] commandQueues;
    /**
     * The OpenCL kernel which will actually compute the Mandelbrot
     * set and store the pixel data in a CL memory object
     */
    private cl_kernel kernel;
    /**
     * The OpenCL memory object which stores the pixel data
     */
    private cl_mem pixelMem;
    /**
     * An OpenCL memory object which stores a nifty color map,
     * encoded as integers combining the RGB components of
     * the colors.
     */
    private cl_mem colorMapMem;
    /**
     * The color map which will be copied to OpenCL for filling
     * the PBO.
     */
    private int[] colorMap;
    /**
     * The minimum x-value of the area in which the Mandelbrot
     * set should be computed
     */
    private float x0 = -2f;
    /**
     * The minimum y-value of the area in which the Mandelbrot
     * set should be computed
     */
    private float y0 = -1.3f;
    /**
     * The maximum x-value of the area in which the Mandelbrot
     * set should be computed
     */
    private float x1 = 0.6f;
    /**
     * The maximum y-value of the area in which the Mandelbrot
     * set should be computed
     */
    private float y1 = 1.3f;
    /**
     * an integer array to which to write the resulting image buffer
     */
    private int[] rawPixels;
    /**
     * Used to copy the rawPixels into and then drawn
     */
    private Bitmap bitmap;
    private RemoteDiscoveryManager remoteDiscoveryManager;
    private String remoteString;
    private String discoveredRemoteIP;

    private final Runnable MandelbrotRunnable = new Runnable() {
        @Override
        public void run() {
            boolean interrupted = false;
            while (!interrupted) {

                try {
                    int status = 0;
                    status = initCL();
                    if (status != 0) {
                        selectedDeviceIndex = 0;
                        Switch deviceSwitch = binding.deviceSwitch;
                        deviceSwitch.setChecked(true);
                    }

                    // add a bit of randomness
                    Random random = new Random();

                    while (!Thread.interrupted()) {

                        float dx = x1 - x0;
                        float dy = y1 - y0;
                        float delta = 0.80f + random.nextFloat() * 0.20f;
                        x0 += delta * dx;
                        x1 -= delta * dx;
                        y0 += delta * dy;
                        y1 -= delta * dy;
                        status = updateImage();
                        if (status != 0) {
                            selectedDeviceIndex = 0;
                            Switch deviceSwitch = binding.deviceSwitch;
                            deviceSwitch.setChecked(true);
                        }
                    }
                    interrupted = true;
                } catch (CLException e) {
                    selectedDeviceIndex = 0;
                    Switch deviceSwitch = binding.deviceSwitch;
                    deviceSwitch.setChecked(true);
                    Log.d("Mandelbrot", "error in mail loop.");
                }
            }
            releaseCL();
        }
    };
    /**
     * a thread to run things in background and not block the UI thread
     */
    private HandlerThread backgroundThread;
    /**
     * a Handler that is used to schedule work on the backgroundThread
     */
    private Handler backgroundThreadHandler;
    SurfaceHolder.Callback surfaceCreated = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(@NonNull SurfaceHolder holder) {
            surfaceHolder = holder;
            backgroundThreadHandler.post(MandelbrotRunnable);
        }

        @Override
        public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

        }

        @Override
        public void surfaceDestroyed(@NonNull SurfaceHolder holder) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMendelbrotDemoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // helper class to get options chosen by the user
        ConfigStore configStore = new ConfigStore(this);

        String cache_dir = getCacheDir().getAbsolutePath();
        System.setProperty("POCL_CACHE_DIR", cache_dir);
        setNativeEnv("POCL_CACHE_DIR", cache_dir);
        setNativeEnv("POCL_DEBUG", "proxy,warn,error");
        setNativeEnv("POCL_DISCOVERY", "1");

        String devicesString = configStore.getPoclDevices();
        remoteString = configStore.getRemoteIp();
        discoveredRemoteIP = configStore.getDiscoveredIp();

        // See the comment above handleInitialDeviceDiscovery()
        if (discoveredRemoteIP.equals(remoteString)) {
            setNativeEnv("POCL_DEVICES", "proxy ");
        } else {
            setNativeEnv("POCL_DEVICES", devicesString);
            setNativeEnv("POCL_REMOTE0_PARAMETERS", remoteString);
        }

        sizeX = 500;
        sizeY = 500;

        mandelbrotView = binding.MandelbrotView;
        Switch deviceSwitch = binding.deviceSwitch;
        deviceSwitch.setOnClickListener(deviceSwithCallback);

        // disable the switch if there is only the remote device
        if (!devicesString.contains("proxy")) {
            selectedDeviceIndex = 0;
            deviceSwitch.setClickable(false);
            deviceSwitch.setChecked(true);
        }

        Spinner discoverySpinner = binding.discoverySpinner2;
        remoteDiscoveryManager = new RemoteDiscoveryManager(this, discoverySpinner, deviceDiscoverySpinnerListener);

    }

    /**
     * An android activity has multiple states.
     * see https://developer.android.com/guide/components/activities/activity-lifecycle
     * what the purpose of this function is.
     */
    @Override
    protected void onResume() {
        super.onResume();

        startBackgroundThread();
        SurfaceHolder surfaceHolder = mandelbrotView.getHolder();
        surfaceHolder.setFixedSize(sizeX, sizeY);
        surfaceHolder.addCallback(surfaceCreated);

    }

    @Override
    protected void onPause() {

        stopBackgroundThreads();
        super.onPause();
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
        remoteDiscoveryManager.stopAllDiscoveries();

        super.onDestroy();
    }

    /**
     * If the IP address set during the startup activity matches one of the discovered servers,
     * do not add the server through environment variables during initialization. Instead, add it
     * dynamically during runtime using the device add function.
     * <p>
     * This is because a server discovered during startup is not registered in the native code
     * until this activity begins and OpenCL code is executed. If a device from that server is added
     * through environment variables, and the same server is discovered again and selected by the
     * user, the system will try to add it again.
     * <p>
     * While the discovery code can handle duplicates, it doesn’t recognize servers added via
     * environment variables. As a result, it treats the server as new and adds it again, leading
     * to potential errors.
     */
    private void handleInitialDeviceDiscovery() {
        if (discoveredRemoteIP.equals(remoteString)) {
            RemoteDiscoveryManager.ServerInfo temp = RemoteDiscoveryManager.discoveredServers.get(discoveredRemoteIP.substring(0, discoveredRemoteIP.length() - 2));
            assert temp != null;
            remoteAddServer(temp.serverID, "Android", temp.serverIP, "pocl", temp.deviceCount);
            discoveredRemoteIP = "";
        }
    }

    /**
     * Helper function to return string values for clGetDeviceInfo()
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
     * In order to match the device selected by the user through the spinner to the actual device
     * in the device list, we compare the IP and port of the selected device to devices from the
     * device list and return the matching index.
     */
    private int findDeviceIndexByServerIP(String serverIP) {
        final int platformIndex = 0;
        final long deviceType = CL_DEVICE_TYPE_ALL;

        // Enable exceptions and subsequently omit error checks in this sample
        CL.setExceptionsEnabled(true);

        // Obtain the number of platforms
        int[] numPlatformsArray = new int[1];
        clGetPlatformIDs(0, null, numPlatformsArray);
        int numPlatforms = numPlatformsArray[0];

        // Obtain a platform ID
        cl_platform_id[] platforms = new cl_platform_id[numPlatforms];
        clGetPlatformIDs(platforms.length, platforms, null);
        cl_platform_id platform = platforms[platformIndex];

        // Obtain the number of devices for the platform
        int[] numDevicesArray = new int[1];
        clGetDeviceIDs(platform, deviceType, 0, null, numDevicesArray);
        int numDevices = numDevicesArray[0];

        // Obtain a device ID
        cl_device_id[] devices = new cl_device_id[numDevices];
        clGetDeviceIDs(platform, deviceType, numDevices, devices, null);

        for (int i = 0; i < numDevices; i++) {

            /*
             * getting IP and port through clGetDeviceInfo() is only supported for the remote driver,
             * therefore we need to check if a device is a remote device.
             */
            String deviceVersion = getString(devices[i], CL_DEVICE_VERSION);
            if (deviceVersion.contains("remote")) {
                String deviceServerIP = getString(devices[i], CL_DEVICE_REMOTE_SERVER_IP_POCL);
                int deviceServerPort[] = new int[1];
                clGetDeviceInfo(devices[i], CL_DEVICE_REMOTE_SERVER_PORT_POCL, Sizeof.cl_int, Pointer.to(deviceServerPort), null);

                /*
                 * The first match may not be the right device as all devices from the same server
                 * have the same IP and port. We then consider the value after '/' and then increase
                 * the index accordingly. This works because the number after '/' are gotten through
                 * the TXT field during discovery which are set by the server itself.
                 */
                String[] p1 = serverIP.split("/");
                if (p1[0].equals(deviceServerIP + ":" + deviceServerPort[0])) {
                    return i + Integer.parseInt(p1[1]);
                }

            }
        }

        return -1;
    }

    private final AdapterView.OnItemSelectedListener deviceDiscoverySpinnerListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            String selectedServer = remoteDiscoveryManager.deviceList.get(position).getDeviceAddress();

            if (!selectedServer.equals(RemoteDiscoveryManager.DEFAULT_SPINNER_LABEL)) {

                runOnUiThread(() -> Toast.makeText(MandelbrotDemoActivity.this, "connecting to: " + selectedServer, Toast.LENGTH_SHORT).show());
                RemoteDiscoveryManager.ServerInfo temp = remoteDiscoveryManager.discoveredServers.get(selectedServer.substring(0, selectedServer.length() - 2));
                Switch deviceSwitch = binding.deviceSwitch;
                deviceSwitch.setChecked(true);
                stopBackgroundThreads();
                remoteAddServer(temp.serverID, "Android", temp.serverIP, "pocl", temp.deviceCount);
                selectedDeviceIndex = findDeviceIndexByServerIP(selectedServer);
                startBackgroundThread();
                backgroundThreadHandler.post(MandelbrotRunnable);
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    };

    /**
     * Initialize OpenCL: Create the context, the command queue
     * and the kernel.
     */
    private int initCL() {
        final int platformIndex = 0;
        final long deviceType = CL_DEVICE_TYPE_ALL;
        final int deviceIndex = 0;
        int[] errcode = new int[1];

        // Enable exceptions and subsequently omit error checks in this sample
        CL.setExceptionsEnabled(true);

        // Obtain the number of platforms
        int[] numPlatformsArray = new int[1];
        errcode[0] = clGetPlatformIDs(0, null, numPlatformsArray);
        if (errcode[0] != 0) return errcode[0];
        int numPlatforms = numPlatformsArray[0];

        // Obtain a platform ID
        cl_platform_id[] platforms = new cl_platform_id[numPlatforms];
        errcode[0] = clGetPlatformIDs(platforms.length, platforms, null);
        if (errcode[0] != 0) return errcode[0];
        cl_platform_id platform = platforms[platformIndex];

        // Initialize the context properties
        cl_context_properties contextProperties = new cl_context_properties();
        contextProperties.addProperty(CL_CONTEXT_PLATFORM, platform);

        // Obtain the number of devices for the platform
        int[] numDevicesArray = new int[1];
        errcode[0] = clGetDeviceIDs(platform, deviceType, 0, null, numDevicesArray);
        if (errcode[0] != 0) return errcode[0];
        int numDevices = numDevicesArray[0];

        // Device addition related steps needed on startup
        handleInitialDeviceDiscovery();

        // Obtain the number of devices for the platform again
        errcode[0] = clGetDeviceIDs(platform, deviceType, 0, null, numDevicesArray);
        if (errcode[0] != 0) return errcode[0];
        numDevices = numDevicesArray[0];

        // Obtain a device ID
        cl_device_id[] devices = new cl_device_id[numDevices];
        errcode[0] = clGetDeviceIDs(platform, deviceType, numDevices, devices, null);
        if (errcode[0] != 0) return errcode[0];

        // Create a context for the selected device
        context = clCreateContext(contextProperties, numDevices, devices, null, null, errcode);
        if (errcode[0] != 0) return errcode[0];

        commandQueues = new cl_command_queue[numDevices];

        // Create a command-queue for the selected device
        cl_queue_properties properties = new cl_queue_properties();
        for (int i = 0; i < numDevices; i++) {
            commandQueues[i] = clCreateCommandQueueWithProperties(context, devices[i], properties, errcode);
        }
        if (errcode[0] != 0) return errcode[0];

        String source = readFile("kernels/SimpleMandelbrot.cl");

        // Create the program
        cl_program clProgram = clCreateProgramWithSource(context, 1, new String[]{source}, new long[]{source.length()}, errcode);
        Log.e("manderbrot", "errcode : " + errcode[0]);
        if (errcode[0] != 0) return errcode[0];

        // Build the program
        errcode[0] = clBuildProgram(clProgram, numDevices, devices, null, null, null);
        if (errcode[0] != 0) return errcode[0];

        // Create the kernel
        kernel = clCreateKernel(clProgram, "computeMandelbrot", errcode);
        if (errcode[0] != 0) return errcode[0];

        // Create the memory object which will be filled with the
        // pixel data
        pixelMem = clCreateBuffer(context, CL_MEM_WRITE_ONLY, (long) sizeX * sizeY * Sizeof.cl_uint, null, errcode);
        if (errcode[0] != 0) return errcode[0];

        rawPixels = new int[sizeX * sizeY];

        bitmap = Bitmap.createBitmap(sizeX, sizeY, Bitmap.Config.ARGB_8888);

        // Create and fill the memory object containing the color map
        initColorMap(32, Color.RED, Color.GREEN, Color.BLUE);
        colorMapMem = clCreateBuffer(context, CL_MEM_READ_WRITE, (long) colorMap.length * Sizeof.cl_uint, null, errcode);
        if (errcode[0] != 0) return errcode[0];

        errcode[0] = clEnqueueWriteBuffer(commandQueues[deviceIndex], colorMapMem, true, 0, (long) colorMap.length * Sizeof.cl_uint, Pointer.to(colorMap), 0, null, null);
        if (errcode[0] != 0) return errcode[0];

        // release devices, we can continue with the commandqueues instead
        for (cl_device_id dev : devices) {
            errcode[0] = clReleaseDevice(dev);
            if (errcode[0] != 0) return errcode[0];
        }

        // release the program, we are done creating kernels
        errcode[0] = clReleaseProgram(clProgram);

        return errcode[0];
    }

    /**
     * release all the ocl objects
     */
    private void releaseCL() {

        clReleaseMemObject(colorMapMem);
        clReleaseMemObject(pixelMem);

        for (cl_command_queue commandQueue : commandQueues) {
            clReleaseCommandQueue(commandQueue);
        }

        clReleaseContext(context);
    }

    /**
     * Helper function which reads the file with the given name and returns
     * the contents of this file as a String. Will exit the application
     * if the file can not be read.
     *
     * @param fileName The name of the file to read.
     * @return The contents of the file
     */
    private String readFile(String fileName) {
        BufferedReader br = null;
        try {
            // the kernel has been included in the assets dir.
            // this means we need the assetmanager to decompress
            // and read it
            AssetManager assetManager = this.getAssets();
            InputStream fileInputStream = assetManager.open(fileName);
            br = new BufferedReader(new InputStreamReader(fileInputStream));
            StringBuffer sb = new StringBuffer();
            String line = null;
            while (true) {
                line = br.readLine();
                if (line == null) {
                    break;
                }
                sb.append(line).append("\n");
            }
            return sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
            return null;
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    /**
     * Creates the colorMap array which contains RGB colors as integers,
     * interpolated through the given colors with colors.length * stepSize
     * steps
     *
     * @param stepSize The number of interpolation steps between two colors
     * @param colors   The colors for the map
     */
    private void initColorMap(int stepSize, int... colors) {
        colorMap = new int[stepSize * colors.length];
        int index = 0;
        for (int i = 0; i < colors.length - 1; i++) {
            int c0 = colors[i];
            int r0 = Color.red(c0);
            int g0 = Color.green(c0);
            int b0 = Color.blue(c0);

            int c1 = colors[i + 1];
            int r1 = Color.red(c1);
            int g1 = Color.green(c1);
            int b1 = Color.blue(c1);

            int dr = r1 - r0;
            int dg = g1 - g0;
            int db = b1 - b0;

            for (int j = 0; j < stepSize; j++) {
                float alpha = (float) j / (stepSize - 1);
                int r = (int) (r0 + alpha * dr);
                int g = (int) (g0 + alpha * dg);
                int b = (int) (b0 + alpha * db);
                int rgb = (0xff << 24) | (r << 16) | (g << 8) | (b << 0);
                colorMap[index++] = rgb;
            }
        }
    }


    /**
     * Execute the kernel function and read the resulting pixel data
     * into the BufferedImage
     */
    private int updateImage() {
        // Set work size and execute the kernel
        long[] globalWorkSize = new long[2];
        globalWorkSize[0] = sizeX;
        globalWorkSize[1] = sizeY;

        int errcode = 0;

        int maxIterations = 250;
        errcode = clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(pixelMem));
        if (errcode != 0) return errcode;
        errcode = clSetKernelArg(kernel, 1, Sizeof.cl_uint, Pointer.to(new int[]{sizeX}));
        if (errcode != 0) return errcode;
        errcode = clSetKernelArg(kernel, 2, Sizeof.cl_uint, Pointer.to(new int[]{sizeY}));
        if (errcode != 0) return errcode;
        errcode = clSetKernelArg(kernel, 3, Sizeof.cl_float, Pointer.to(new float[]{x0}));
        if (errcode != 0) return errcode;
        errcode = clSetKernelArg(kernel, 4, Sizeof.cl_float, Pointer.to(new float[]{y0}));
        if (errcode != 0) return errcode;
        errcode = clSetKernelArg(kernel, 5, Sizeof.cl_float, Pointer.to(new float[]{x1}));
        if (errcode != 0) return errcode;
        errcode = clSetKernelArg(kernel, 6, Sizeof.cl_float, Pointer.to(new float[]{y1}));
        if (errcode != 0) return errcode;
        errcode = clSetKernelArg(kernel, 7, Sizeof.cl_int, Pointer.to(new int[]{maxIterations}));
        if (errcode != 0) return errcode;
        errcode = clSetKernelArg(kernel, 8, Sizeof.cl_mem, Pointer.to(colorMapMem));
        if (errcode != 0) return errcode;
        errcode = clSetKernelArg(kernel, 9, Sizeof.cl_int, Pointer.to(new int[]{colorMap.length}));
        if (errcode != 0) return errcode;

        errcode = clEnqueueNDRangeKernel(commandQueues[selectedDeviceIndex], kernel, 2, null, globalWorkSize, null, 0, null, null);
        if (errcode != 0) return errcode;

        errcode = clEnqueueReadBuffer(commandQueues[selectedDeviceIndex], pixelMem, CL_TRUE, 0, (long) Sizeof.cl_int * sizeY * sizeX, Pointer.to(rawPixels), 0, null, null);
        if (errcode != 0) return errcode;

        // finally, display it on the surface.
        Canvas canvas = surfaceHolder.lockCanvas();
        bitmap.copyPixelsFromBuffer(IntBuffer.wrap(rawPixels));
        canvas.drawBitmap(bitmap, 0, 0, null);
        surfaceHolder.unlockCanvasAndPost(canvas);

        return errcode;
    }


    /**
     * function to start the backgroundThread + handler
     */
    private void startBackgroundThread() {

        backgroundThread = new HandlerThread("MandelbrotBackground");
        backgroundThread.start();
        backgroundThreadHandler = new Handler(backgroundThread.getLooper());

    }

    /**
     * function to safely stop backgroundThread + handler
     */
    private void stopBackgroundThreads() {

        // interrupt the thread, causing the while loop exit
        backgroundThread.interrupt();
        backgroundThread.quitSafely();
        try {
            backgroundThread.join(1000);
            backgroundThread = null;
            backgroundThreadHandler = null;

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}