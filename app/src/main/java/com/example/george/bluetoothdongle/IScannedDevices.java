package com.example.george.bluetoothdongle;

/**
 * Created by George on 3/18/2015.
 */
public interface IScannedDevices {
    public void onDongleDetected(String address);
    public void onCabTagDetected(String address);
}
