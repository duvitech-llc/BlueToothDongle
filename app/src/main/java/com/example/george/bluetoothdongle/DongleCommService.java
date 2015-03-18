package com.example.george.bluetoothdongle;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class DongleCommService extends Service {
    public DongleCommService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
