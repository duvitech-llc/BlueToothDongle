package com.example.george.bluetoothdongle;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.Toast;

import java.security.Timestamp;

public class DataUpdateService extends Service implements LocationListener,IScannedDeviceListener {
    private static final String TAG = "DataUpdateService";
    private static final int UPDATE_INTERVAL = 20000;
    private static final int UPDATE_DISTANCE = 1;

    private boolean mBound = false;
    private BluetoothScannerService mService;
    private ScannerListenerReceiver receiver;
    private CabTagCommService mCabTagService = null;
    private static String mConnectedDongleName = null;
    private DongleCommService mDongleService = null;
    private StringBuffer mOutStringBuffer;

    boolean mDonglePresent = false;
    String mDongleAddress = null;

    boolean mCabTagPresent = false;
    String mCabTagAddress = null;

    boolean mLocationListener = false;
    Location mLastLocation = null;
    Timestamp mLastLocationUpdate = null;

    OBDIIData mOBDIIData = null;
    Timestamp mLastODBIIDataUpdate = null;
    private BluetoothAdapter mBluetoothAdapter;


    public DataUpdateService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        receiver = new ScannerListenerReceiver(this);
        registerReceiver(receiver, new IntentFilter(BluetoothScannerService.DONGLE_DETECTED));
        registerReceiver(receiver, new IntentFilter(BluetoothScannerService.CABTAG_DETECTED));

        // Register the listener with the Location Manager to receive location updates
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, UPDATE_INTERVAL, UPDATE_DISTANCE, this);
        mLocationListener = true;

        // get scanner service reference
        Intent btIntent = new Intent(this, BluetoothScannerService.class);
        bindService(btIntent, mConnection, Context.BIND_AUTO_CREATE);

        startService(btIntent);


        return START_NOT_STICKY;
    }


    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mDongleService != null) {
            mDongleService.stop();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "onLocationChanged");
        Log.d(TAG, "Position: " + location.getLatitude() + ", " + location.getLongitude());
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.d(TAG, "onStatusChanged");

    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.d(TAG, "onProviderEnabled");

    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.d(TAG, "onProviderDisabled");

    }

    @Override
    public void onDongleDetected(String address) {
        Log.d(TAG, "onDongleDetected");
        mService.disableDongleDiscovery();
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if(device != null)
           setupDongleChannel(device);
        else
            Log.w(TAG,"Dongle Name NOT FOUND");
    }

    @Override
    public void onCabTagDetected(String address) {
        Log.d(TAG, "onCabTagDetected");

    }

    public void setupDongleChannel(BluetoothDevice device){
        Log.d(TAG, "setupDongleChannel");
        if(mDongleService != null){
            // we are already connected
            Log.d(TAG, "Already connected to a dongle");

            if(mDongleService.getState() == DongleCommService.STATE_NONE){
                mDongleService.connect(device ,false);
            }


        }
        else {
            // initialize the DongleCommService to perform a bluetooth connection
            mDongleService = new DongleCommService(getBaseContext(), mHandler);
            // Initialize the buffer for outgoing messages
            mOutStringBuffer = new StringBuffer("");
            mDongleService.connect(device, false);
        }
    }

    private void sendCommand(String dongleCommand){
        // Check that we're actually connected before trying anything
        if (mDongleService.getState() != DongleCommService.STATE_CONNECTED) {
            Log.d(TAG, "Send Failed Not Connected");
            return;
        }

        // Check that there's actually something to send
        if (dongleCommand.length() > 0) {
            if(!dongleCommand.endsWith("\r"))
                dongleCommand = dongleCommand + "\r";

            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = dongleCommand.getBytes();
            mDongleService.write(send);

            // Reset out string buffer to zero
            mOutStringBuffer.setLength(0);
            // and clear the edit text field
            //mOutEditText.setText(mOutStringBuffer);
        }
    }

    private void showToast(String message){
        Toast.makeText(getApplicationContext(), message,
                Toast.LENGTH_SHORT).show();
    }

    private final Handler mHandler = new Handler(){

        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG,"Message Received");

            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    Log.d(TAG,"Message: MESSAGE_STATE_CHANGE");
                    switch (msg.arg1) {
                        case DongleCommService.STATE_CONNECTED:
                            //setStatus(getString(R.string.title_connected_to, mConnectedDongleName));
                            break;
                        case DongleCommService.STATE_CONNECTING:
                            //setStatus(R.string.title_connecting);
                            break;
                        case DongleCommService.STATE_LISTEN:
                        case DongleCommService.STATE_NONE:
                            //setStatus(R.string.title_not_connected);
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    Log.d(TAG,"Message: MESSAGE_WRITE");
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    if(writeMessage.endsWith("\r"))
                        writeMessage = writeMessage.substring(0, writeMessage.length()-1);

                    Log.d(TAG,"Message: " + writeMessage);
                    break;
                case Constants.MESSAGE_READ:
                    Log.d(TAG,"Message: MESSAGE_READ");
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);

                    Log.d(TAG,"Message: " + readMessage);
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    Log.d(TAG,"Message: MESSAGE_DEVICE_NAME");
                    // save the connected device's name
                    mConnectedDongleName = msg.getData().getString(Constants.DEVICE_NAME);

                    Log.d(TAG,"Connected TO: " + mConnectedDongleName);
                    break;
                case Constants.MESSAGE_TOAST:
                    Log.d(TAG,"Message: MESSAGE_TOAST");
                    break;
            }
        }
    };

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            BluetoothScannerService.LocalBinder binder = (BluetoothScannerService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
            mService.startScanner();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

}
