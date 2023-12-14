package org.portablecl.pocl_rreferenceandroidjavaclient;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;

import org.portablecl.pocl_rreferenceandroidjavaclient.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    // Used to load the 'pocl_rreferenceandroidjavaclient' library on application startup.
    static {
        System.loadLibrary("pocl_rreferenceandroidjavaclient");
    }

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Example of a call to a native method
        TextView tv = binding.sampleText;
        tv.setText(stringFromJNI());
    }

    /**
     * A native method that is implemented by the 'pocl_rreferenceandroidjavaclient' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
}