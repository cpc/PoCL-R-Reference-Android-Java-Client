package org.portablecl.pocl_rreferenceandroidjavaclient;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.portablecl.pocl_rreferenceandroidjavaclient.databinding.ActivityStartupBinding;

public class StartupActivity extends AppCompatActivity {

    private ActivityStartupBinding binding;

    private Switch proxySwitch;

    private Switch remoteSwitch;

    private TextView remoteText;

    private String discoveredRemoteIP;

    private Button demoButton1;

    private Button demoButton2;

    private ConfigStore configStore;

    RemoteDiscoveryManager remoteDiscoveryManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityStartupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // default toolbar stuff
        Toolbar toolbar = binding.toolbar;
        setSupportActionBar(toolbar);
        CollapsingToolbarLayout toolBarLayout = binding.toolbarLayout;
        toolBarLayout.setTitle(getTitle());

        FloatingActionButton fab = binding.fab;
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // open the browser when someone clicks on the email icon
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.tuni" +
                        ".fi/cpc/index.html"));
                startActivity(browserIntent);
            }
        });

        configStore = new ConfigStore(this);
        String poclDevicesString = configStore.getPoclDevices();
        proxySwitch = findViewById(R.id.proxySwitch);
        if (poclDevicesString.contains("proxy")) {
            proxySwitch.setChecked(true);
        }
        remoteSwitch = findViewById(R.id.remoteSwitch);
        // always include remote device
        remoteSwitch.setChecked(true);
        remoteSwitch.setClickable(false);

        remoteText = findViewById(R.id.remoteText);
        remoteText.setText("");
        discoveredRemoteIP = configStore.getDiscoveredIp();

        demoButton1 = findViewById(R.id.demoButton1);
        demoButton1.setOnClickListener(demoButtonListener);
        demoButton2 = findViewById(R.id.demoButton2);
        demoButton2.setOnClickListener(demoButtonListener);

        Spinner discoverySpinner = findViewById(R.id.discoverySpinner);
        // Set up a listener for the discovery spinner to handle user selection of a discovered device.
        remoteDiscoveryManager = new RemoteDiscoveryManager(this, discoverySpinner,
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position,
                                               long id) {

                        String selectedServer = remoteDiscoveryManager.deviceList.get(position).getDeviceAddress();
                        if (!selectedServer.equals(RemoteDiscoveryManager.DEFAULT_SPINNER_LABEL)) {
                            Log.d("DISC", "Spinner position selected: " + position + " : server " +
                                    "selected " +
                                    ": " + selectedServer);
                            remoteText.setText(selectedServer);
                            discoveredRemoteIP = selectedServer;
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                });

    }

    /**
     * Handle demo button clicks. Check the submitted values and start up one of the demo
     * activities
     */
    private final View.OnClickListener demoButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            String poclDevices = "";
            if (proxySwitch.isChecked()) {
                poclDevices += "proxy ";
            }
            if (remoteSwitch.isChecked()) {
                poclDevices += "remote ";
            }
            if (poclDevices.isEmpty()) {
                Toast.makeText(StartupActivity.this, "Please select a device first",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            // todo add some ip string checking
            configStore.setRemoteIp(remoteText.getText().toString());
            configStore.setPoclDevices(poclDevices);
            configStore.setDiscoveredIp(discoveredRemoteIP);
            configStore.saveConfig();

            Class demoClass;
            if (v == demoButton1) {
                demoClass = DeviceDemoActivity.class;
            } else {
                demoClass = MandelbrotDemoActivity.class;
            }
            Intent i = new Intent(getApplicationContext(), demoClass);
            startActivity(i);
        }
    };

    @Override
    protected void onPause() {
        super.onPause();
        remoteDiscoveryManager.stopAllDiscoveries();
    }

    @Override
    protected void onDestroy() {
        remoteDiscoveryManager.stopAllDiscoveries();
        super.onDestroy();
    }
}