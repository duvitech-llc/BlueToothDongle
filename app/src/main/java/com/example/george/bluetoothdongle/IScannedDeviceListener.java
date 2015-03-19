package com.example.george.bluetoothdongle;

import android.location.Location;

/**
 * Created by George on 3/18/2015.
 */
public interface IScannedDeviceListener {
    public void onDongleDetected(String address);
    public void onCabTagDetected(String address);
}
