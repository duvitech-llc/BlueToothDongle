package com.example.george.bluetoothdongle;

import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.util.logging.LogRecord;


public class MainActivity extends ActionBarActivity implements IScannedDevices {

    private static final String TAG = "MainActivity";
    private BluetoothScannerService mService;
    private ScannerListenerReceiver rec;
    private boolean mBound;

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;


    /**
     * Name of the connected dongle
     */
    private String mConnectedDongleName = null;

    /**
     * String buffer for outgoing messages
     */
    private StringBuffer mOutStringBuffer;

    /**
     * Local Bluetooth adapter
     */
    private BluetoothAdapter mBluetoothAdapter = null;

    /**
     * Member object for the Dongle Communication service
     */
    private DongleCommService mDongleService = null;
    private CabTagCommService mCabTagService = null;


    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save the user's current application state

        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate()");
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter == null){
            // device does not have a bluetooth radio

            Toast.makeText(getApplicationContext(), "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
        }

        // start listener
        rec = new ScannerListenerReceiver();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart()");
        Intent intent = new Intent(this, BluetoothScannerService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        startService(intent);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause()");
        mService.cancelScanner();
        if(mConnection != null){
            unbindService(mConnection);
        }

        if(rec != null)
            unregisterReceiver(rec);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop()");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume()");
        if(rec != null) {
            registerReceiver(rec, new IntentFilter(BluetoothScannerService.DONGLE_DETECTED));
            registerReceiver(rec, new IntentFilter(BluetoothScannerService.CABTAG_DETECTED));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDongleDetected(String address) {
        // Dongle in range
        if(mDongleService == null){
            // connect to the dongle
            setupDongleChannel();
        }

    }

    @Override
    public void onCabTagDetected(String address) {
        // cabtag in range
        if(mCabTagService == null){
            // connect to the CabTag???
            setupCabTagChannel();
        }
    }

    private void setupDongleChannel(){
        // initialize the DongleCommService to perform a bluetooth connection
        mDongleService = new DongleCommService(this, mHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");
    }


    private void setupCabTagChannel(){

    }

    private void sendCommand(String dongleCommand){

    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            return rootView;
        }
    }

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

    private final Handler mHandler = new Handler(){

        @Override
        public void handleMessage(Message msg) {

        }
    };

}
