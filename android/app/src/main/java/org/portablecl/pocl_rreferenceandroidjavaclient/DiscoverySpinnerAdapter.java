package org.portablecl.pocl_rreferenceandroidjavaclient;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.List;

/**
 * Custom adapter for a Spinner to display device details from a list of
 * DeviceSelectionEntry objects in a specific format.
 */
public class DiscoverySpinnerAdapter extends ArrayAdapter<RemoteDiscoveryManager.DeviceSelectionEntry> {

    private List<RemoteDiscoveryManager.DeviceSelectionEntry> objects; // List of device entries to be displayed in the spinner

    public DiscoverySpinnerAdapter(@NonNull Context context, int resource,
                                   @NonNull List<RemoteDiscoveryManager.DeviceSelectionEntry> objects) {
        super(context, resource, objects);
        this.objects = objects;
    }

    /**
     * Provides the view for the selected item displayed in the spinner.
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView label = (TextView) super.getView(position, convertView, parent);
        if (position == 0) {
            label.setText(objects.get(position).getDeviceAddress());
            return label;
        }
        label.setText(objects.get(position).getDescription());

        return label;
    }

    /**
     * Provides the view for each item in the dropdown list of the spinner.
     */
    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        TextView label = (TextView) super.getDropDownView(position, convertView, parent);
        for (RemoteDiscoveryManager.DeviceSelectionEntry s : objects) {
            if(s.deviceAddress.equals(RemoteDiscoveryManager.DEFAULT_SPINNER_LABEL)) {
                continue;
            }
        }
        if (position == 0) {
            label.setText(objects.get(position).getDeviceAddress());
            return label;
        }
        label.setText(objects.get(position).getDescription());

        return label;
    }

}
