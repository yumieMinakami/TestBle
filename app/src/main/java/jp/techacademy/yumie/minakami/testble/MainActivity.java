package jp.techacademy.yumie.minakami.testble;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


import com.neovisionaries.bluetooth.ble.advertising.ADPayloadParser;
import com.neovisionaries.bluetooth.ble.advertising.ADStructure;
import com.neovisionaries.bluetooth.ble.advertising.Eddystone;
import com.neovisionaries.bluetooth.ble.advertising.EddystoneEID;
import com.neovisionaries.bluetooth.ble.advertising.EddystoneTLM;
import com.neovisionaries.bluetooth.ble.advertising.EddystoneUID;
import com.neovisionaries.bluetooth.ble.advertising.EddystoneURL;
import com.neovisionaries.bluetooth.ble.advertising.IBeacon;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter    mBluetoothAdapter;
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private Handler             mHandler;
    private boolean             mScanning;
    private ScanCallback        mScanCallback;
    private BluetoothLeScanner  mBluetoothLeScanner;

    private static final int    BT_REQUEST_ENABLE = 1;
    private static final long   SCAN_PERIOD = 10000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mHandler = new Handler();

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        // initialize Bluetooth Adapter
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // check Bluetooth supported
        if(mBluetoothAdapter == null){
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        TextView textMajor = (TextView) findViewById(R.id.major);
        TextView textMinor = (TextView) findViewById(R.id.minor);
        TextView textPower = (TextView) findViewById(R.id.txpower);
        TextView textuid   = (TextView) findViewById(R.id.uuid);

    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.d("life", "onResume");

        // Ensures Bluetooth is enabled on the device.
        // If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
//            mTextView.setText(R.string.bt_off);
//            Log.d("life", "R.string.bt_off @ onResume");
            Intent setBtIntnt = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(setBtIntnt, BT_REQUEST_ENABLE);
//        } else {
//            mTextView.setText(R.string.bt_on);
//            Log.d("life", "R.string.bt_on @ onResume");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

//        Log.d("life", "onActivityResult");

        // User chose not to enable Bluetooth.
        if (requestCode == BT_REQUEST_ENABLE && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
        mLeDeviceListAdapter.clear();
//        Log.d("life", "onPause");
    }

    // Initialization of ScanCallback
    private ScanCallback initCallback(){
        return new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);

//                if(result != null && result.getDevice() != null){
//                    if(isAdded(result.getDevice())){
//                        // No Addition
//                    } else {
//                        saveDevice(result.getDevice());
//                    }
//                }
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results){
                super.onBatchScanResults(results);
            }

            @Override
            public void onScanFailed(int errorCode){
                super.onScanFailed(errorCode);
            }
        };
    }

    private void scanLeDevice(final boolean enable){
        if(enable){
            // stop scanning after a pre-defined scan period
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                }
            }, SCAN_PERIOD);
        }
    }
//    private void scan(List<ScanFilter> filters, ScanSettings settings, boolean enable){
//
//        mScanCallback = initCallback();
//
//        if(enable){
//            // Stop scanning after a pre-defined scan period
//            mHandler.postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    mScanning = false;
//                    mBluetoothLeScanner.stopScan(mScanCallback);
//                }
//            }, SCAN_PERIOD);
//
//            mScanning = true;
//            mBluetoothLeScanner.startScan(mScanCallback);
//
//        } else {
//            mScanning = false;
//            mBluetoothLeScanner.stopScan(mScanCallback);
//        }
//    }
//
//    // Stop scanning
//    public void stopScan(){
//        if(mBluetoothLeScanner != null){
//            mBluetoothLeScanner.stopScan(mScanCallback);
//        }
//    }


//    // Check whether scanned devices are on the list or not
//    public boolean isAdded(BluetoothDevice device){
//        if(deviceList != null && deviceList.size() > 0){
//            return deviceList.contains(device);
//        } else {
//            return false;
//        }
//    }

    // Adapter for holding devices found through scan
    private class LeDeviceListAdapter extends BaseAdapter{

        private ArrayList<BluetoothDevice>  mLeDevices;
        private LayoutInflater                  mInflator;

        public LeDeviceListAdapter(){
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
            mInflator = MainActivity.this.getLayoutInflater();
        }

        public void addDevice(BluetoothDevice device){
            if(!mLeDevices.contains(device)){
                mLeDevices.add(device);
            }
        }

        public BluetoothDevice getDevice(int position){
            return mLeDevices.get(position);
        }

        public void clear(){
            mLeDevices.clear();
        }

        @Override
        public int getCount(){
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i){
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i){
            return i;
        }

        @Override
        public View getView(int i, View v, ViewGroup viewGroup){
            ViewHolder viewHolder;

            // General ListView optimization code
            if(v == null){
                v = mInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) v.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) v.findViewById(R.id.device_name);
                v.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) v.getTag();
            }

            BluetoothDevice device = mLeDevices.get(i);
            final String deviceName = device.getName();
            if(deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText("Unknown device");
            viewHolder.deviceAddress.setText(device.getAddress());

            return v;
        }
    }

    // Device scan callback
    private BluetoothAdapter.LeScanCallback mLeScanCallback
            = new BluetoothAdapter.LeScanCallback(){
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord){

            // Parse the payload of the advertising packet.
            List<ADStructure> str = ADPayloadParser.getInstance().parse(scanRecord);

            for(ADStructure structure : str){
                // if the ADStructure instance can be cast to IBeacon
                if(structure instanceof IBeacon){

                    IBeacon iBeacon = (IBeacon) structure;

                    UUID uuid = iBeacon.getUUID();  // Proximity UUID

                    int major = iBeacon.getMajor(); // Major number

                    int minor = iBeacon.getMinor(); // Minor number

                    int power = iBeacon.getPower(); // tx power

                } else if(structure instanceof EddystoneUID){
                    // do nothing
                    Log.d("life", "EddystoneUID");
                } else if(structure instanceof EddystoneURL){
                    // do nothing
                    Log.d("life", "EddystoneURL");
                } else if(structure instanceof EddystoneTLM){
                    //
                    Log.d("life", "EddystoneTLM");
                } else if(structure instanceof EddystoneEID){
                    //
                    Log.d("life", "EddystoneEID");
                }
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mLeDeviceListAdapter.addDevice(device);
                    mLeDeviceListAdapter.notifyDataSetChanged();
                }
            });
        }
    };

    static class ViewHolder{
        TextView deviceName;
        TextView deviceAddress;
    }
}
