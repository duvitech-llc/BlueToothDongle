package com.example.george.bluetoothdongle;

import android.location.Location;

/**
 * Created by George on 3/19/2015.
 */
public interface IDataServiceListener extends IScannedDeviceListener {

    public void onLocationUpdated(Location location);
    public void onDongleResponse(String Message);
}
