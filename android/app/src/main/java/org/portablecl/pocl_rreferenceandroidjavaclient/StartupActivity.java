package org.portablecl.pocl_rreferenceandroidjavaclient;

import static java.lang.Character.isDigit;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.portablecl.pocl_rreferenceandroidjavaclient.databinding.ActivityStartupBinding;

public class StartupActivity extends AppCompatActivity {

    private ActivityStartupBinding binding;

    private Switch proxySwitch;

    private Switch remoteSwitch;

    private TextView remoteText;

    private Button demoButton1;

    private Button demoButton2;

    private ConfigStore configStore;

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


        proxySwitch = findViewById(R.id.proxySwitch);
        proxySwitch.setOnClickListener(deviceSwitchListener);
        remoteSwitch = findViewById(R.id.remoteSwitch);
        remoteSwitch.setOnClickListener(deviceSwitchListener);
        String poclDevicesString = configStore.getPoclDevices();
        if(poclDevicesString.contains("proxy")) {
            proxySwitch.setChecked(true);
        }
        if(poclDevicesString.contains("remote")) {
            remoteSwitch.setChecked(true);
        }

        remoteText = findViewById(R.id.remoteText);
        String remoteIp = configStore.getRemoteIp();
        remoteText.setText(remoteIp);

        demoButton1 = findViewById(R.id.demoButton1);
        demoButton1.setOnClickListener(demoButtonListener);
        demoButton2 = findViewById(R.id.demoButton2);
        demoButton2.setOnClickListener(demoButtonListener);
    }

    /**
     * Handle callbacks related to the user pressing on switches. In this case, we are turning
     * off the other switch when one switch is pressed.
     */
    private final View.OnClickListener deviceSwitchListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            if(v != proxySwitch) {
                proxySwitch.setChecked(false);
            }

            if(v != remoteSwitch) {
                remoteSwitch.setChecked(false);
            }

        }
    };

    /**
     * Handle demo button clicks. Check the submitted values and start up one of the demo
     * activities
     */
    private final View.OnClickListener demoButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            String poclDevices = "";
            if(proxySwitch.isChecked()) {
                poclDevices +="proxy ";
            }
            if(remoteSwitch.isChecked()) {
                poclDevices +="remote ";
            }
            if(poclDevices.isEmpty()) {
                Toast.makeText(StartupActivity.this, "Please select a device first",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            // todo add some ip string checking
            configStore.setRemoteIp(remoteText.getText().toString());
            configStore.setPoclDevices(poclDevices);
            configStore.saveConfig();

            Class demoClass;
            if(v == demoButton1) {
                demoClass = DeviceDemoActivity.class;
            }else {
                Toast.makeText(StartupActivity.this, "This demo has not been implemented yet",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            Intent i = new Intent(getApplicationContext(), demoClass);
            startActivity(i);
        }
    };
}