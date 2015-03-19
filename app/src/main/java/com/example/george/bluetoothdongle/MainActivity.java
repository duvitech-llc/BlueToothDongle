package com.example.george.bluetoothdongle;

import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends ActionBarActivity implements IDataServiceListener {

    private static final String TAG = "MainActivity";
    private BluetoothScannerService mService;
    private ScannerListenerReceiver rec;
    private boolean mBound;

    private final IDataServiceListener activity = this;

    private DataUpdateService mDataService;
    private boolean mDataServiceBound;

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;

    /**
     * Local Bluetooth adapter
     */
    private BluetoothAdapter mBluetoothAdapter = null;



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
        rec = new ScannerListenerReceiver(this);

        Intent intent = new Intent(getBaseContext(), DataUpdateService.class);
        getBaseContext().startService(intent);

    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart()");
        Intent intent = new Intent(this, BluetoothScannerService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        intent = new Intent(getBaseContext(), DataUpdateService.class);
        bindService(intent, mDataConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause()");
        mService.cancelScanner();
        if(mConnection != null){
            boolean isBound = false;
            isBound = bindService( new Intent(getApplicationContext(), BluetoothScannerService.class), mConnection, Context.BIND_AUTO_CREATE );
            if(isBound)
                unbindService(mConnection);
        }

        if(mDataConnection != null){
            boolean isBound = false;
            isBound = bindService( new Intent(getApplicationContext(), DataUpdateService.class), mDataConnection, Context.BIND_AUTO_CREATE );
            if(isBound)
                unbindService(mDataConnection);
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

        if(mService == null){
            Log.d(TAG,"onResume has a null mService Reference");
            mBound = false;
        }
        else
            mService.startScanner();
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
        Log.d(TAG, "onDongleDetected");
        Toast.makeText(getBaseContext(), "Dongle Detected.", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onCabTagDetected(String address) {
        Log.d(TAG,"onCabTagDetected");
        Toast.makeText(getBaseContext(), "CabTag Detected.", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onLocationUpdated(Location location) {


    }

    @Override
    public void onDongleResponse(String message) {
        Log.d(TAG,"Dongle Response: " + message);
    }

    @Override
    public void onDongleStateChange(int dongleState) {
        PlaceholderFragment frag = (PlaceholderFragment)
                getSupportFragmentManager().findFragmentById(R.id.container);

        switch (dongleState){
            case DongleCommService.STATE_NONE:
            case DongleCommService.STATE_CONNECTING:
            case DongleCommService.STATE_LISTEN:
                frag.updateStatus("Not Connected");
                break;
            case DongleCommService.STATE_CONNECTED:
                frag.updateStatus("Connected");
                break;
            default:
                frag.updateStatus("Unknown");
                break;
        }
    }

    public void sendDongleCommand(String command){
        if(mDataService != null)
            if(mDataService.isDongleConnectd())
                mDataService.sendDongleCommand(command);
            else
                Log.i(TAG, "No Dongle connected");
        else
            Log.i(TAG, "Dataservice not available");
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment{
        private static String FTAG = "PlaceholderFragment";
        /**
         * Name of the connected dongle
         */
        private String mConnectedDongleName = null;

        /**
         * String buffer for outgoing messages
         */
        private StringBuffer mOutStringBuffer;

        /**
         * Member object for the Dongle Communication service
         */
        private DongleCommService mDongleService = null;
        private CabTagCommService mCabTagService = null;

        private TextView tvStatus;

        public PlaceholderFragment() {
        }

        public void setupCabTagChannel(BluetoothDevice device){
            Log.d(FTAG, "setupCabTagChannel");

        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            tvStatus = (TextView)rootView.findViewById(R.id.tv_status);

            Button btnSendCommand = (Button)rootView.findViewById(R.id.btnSend);
            btnSendCommand.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    MainActivity temp = (MainActivity)getActivity();
                    temp.sendDongleCommand("0105");
                }
            });

            return rootView;
        }

        @Override
        public void onResume() {
            super.onResume();
            Log.d(FTAG,"onResume()");
        }

        @Override
        public void onPause() {
            super.onPause();
        }

        @Override
        public void onStop() {
            super.onStop();
//            if (mDongleService != null) {
//                mDongleService.stop();
//            }
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            Log.d(FTAG, "onDestroy()");
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);

        }


        public void updateStatus(String status){
            tvStatus.setText(status);
        }

        /**
         * Updates the status on the action bar.
         *
         * @param subTitle status
         */
        private void setStatus(CharSequence subTitle) {
            FragmentActivity activity = getActivity();
            if(tvStatus != null)
                tvStatus.setText(subTitle);
            if (null == activity) {
                return;
            }
            final ActionBar actionBar = activity.getActionBar();
            if (null == actionBar) {
                return;
            }
            actionBar.setSubtitle(subTitle);
        }

        /**
         * Updates the status on the action bar.
         *
         * @param resId a string resource ID
         */
        private void setStatus(int resId) {
            FragmentActivity activity = getActivity();
            if(tvStatus != null)
                tvStatus.setText(getResources().getText(resId));
            if (null == activity) {
                return;
            }
            final ActionBar actionBar = activity.getActionBar();
            if (null == actionBar) {
                return;
            }
            actionBar.setSubtitle(resId);
        }
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            Log.d("ServiceConnection", "BluetoothListenerService Bound");
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            BluetoothScannerService.LocalBinder binder = (BluetoothScannerService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
            //mService.startScanner();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {

            Log.d("ServiceConnection", "BluetoothListenerService UnBound");
            mBound = false;
        }
    };

    private ServiceConnection mDataConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.d("ServiceConnection", "DataUpdateService Bound");
            DataUpdateService.LocalBinder binder = (DataUpdateService.LocalBinder) iBinder;
            mDataService = binder.getService();
            mDataServiceBound = true;

            mDataService.RegisterListener(activity);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d("ServiceConnection", "DataUpdateService UnBound");

            mDataService.UnregisterListener(activity);
            mDataServiceBound = false;

        }
    };


}
