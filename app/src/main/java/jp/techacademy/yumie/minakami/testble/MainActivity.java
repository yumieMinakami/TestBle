package jp.techacademy.yumie.minakami.testble;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.os.Build;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.PermissionChecker;
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

    private BluetoothManager    mBluetoothManager;
    private BluetoothAdapter    mBluetoothAdapter;
    private BluetoothAdapter.LeScanCallback mLeScanCallback;    // for API 19 or former
    private ScanCallback        mScanCallback;      // for API 21 or later
    private BluetoothLeScanner  mBluetoothLeScanner;
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothGatt       mBluetoothGatt;

    private Handler             mHandler;
    private boolean             mScanning;

    private static final int    BT_REQUEST_ENABLE = 1;
    private static final long   SCAN_PERIOD = 10000;
    private static final int    PERMISSIONS_REQUEST_LOCATION_STATE = 100;

    TextView mTextMajor;
    TextView mTextMinor;
    TextView mTextPower;
    TextView mTextuid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mHandler = new Handler();

        Log.d("life", "onCreate");

        // check Permissions for Android 6.0 or later
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            checkPermission();
        }

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

        // if Android 5.0 or later; use scancallback
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            // if API 21 or later
            if(mBluetoothAdapter != null){
                mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
                initScanCallback();
            }
        } else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
            // if API 18-19
            initLeScanCallback();
        } else {
            // if API 17 or former
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // textview initialization
//        TextView textMajor = (TextView) findViewById(R.id.major);
//        TextView textMinor = (TextView) findViewById(R.id.minor);
//        TextView textPower = (TextView) findViewById(R.id.txpower);
//        TextView textuid   = (TextView) findViewById(R.id.uuid);
        mTextMajor = (TextView) findViewById(R.id.major);
        mTextMinor = (TextView) findViewById(R.id.minor);
        mTextPower = (TextView) findViewById(R.id.txpower);
        mTextuid   = (TextView) findViewById(R.id.uuid);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        Log.d("life", "onActivityResult");

        // User chose not to enable Bluetooth.
        if (requestCode == BT_REQUEST_ENABLE && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.d("life", "onResume");

        // check Bluetooth supported
        if(mBluetoothAdapter == null){
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        // Ensures Bluetooth is enabled on the device.
        // If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
//            mTextView.setText(R.string.bt_off);
//            Log.d("life", "R.string.bt_off @ onResume");
            Toast.makeText(this, R.string.bt_on_request, Toast.LENGTH_SHORT).show();
            Intent setBtIntnt = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(setBtIntnt, BT_REQUEST_ENABLE);
//        } else {
//            mTextView.setText(R.string.bt_on);
//            Log.d("life", "R.string.bt_on @ onResume");
        }
        startBleScan();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopBleScan();
//        scanLeDevice(false);
//        mLeDeviceListAdapter.clear();
        Log.d("life", "onPause");
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void onDestroy(){
        mScanning = false;
        stopBleScan();

        super.onDestroy();
    }

//    private void scanLeDevice(final boolean enable){
//        if(enable){
//            if(Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
//                // API 19
//                // stop scanning after a pre-defined scan period
//                mHandler.postDelayed(new Runnable() {
//                    @Override
//                    public void run() {
//                        mScanning = false;
//                        mBluetoothAdapter.stopLeScan(mLeScanCallback);
//                    }
//                }, SCAN_PERIOD);
//            } else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
//            // API 21 or later
//                // stop scanning after a pre-defined scan period
//                mHandler.postDelayed(new Runnable() {
//                    @Override
//                    public void run() {
//                        mScanning = false;
//                        mBluetoothLeScanner.stopScan(mScanCallback);
//                    }
//                }, SCAN_PERIOD);
//            }  else {
//                // API 18 or lower
//                mScanning = false;
//                Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
//                finish();
//                return;
//            }
//        }
//    }

//    private void scan(boolean enable){
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
    protected class LeDeviceListAdapter extends BaseAdapter{

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

    // ScanCallback initialization ; API 21 or later
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    protected void initScanCallback(){
        Log.d("life", "initScanCallback @ lollipop");

        mScanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, final ScanResult result) {
                super.onScanResult(callbackType, result);

                Log.d("life", "onScanResult / initScanCallback @ lollipop");

//                if(result != null && result.getDevice() != null && mLeDeviceListAdapter != null){
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            mLeDeviceListAdapter.addDevice(result.getDevice());
//                            mLeDeviceListAdapter.notifyDataSetChanged();
//                        }
//                    });
//                }

                scanData(result.getDevice(), result.getRssi(), result.getScanRecord().getBytes());

            }

            @Override
            public void onBatchScanResults(List<ScanResult> results){
                super.onBatchScanResults(results);
                Log.d("life", "onBatchScanResult / initScanCallback @ lollipop");
                for(ScanResult result : results){
                    scanData(result.getDevice(), result.getRssi(), result.getScanRecord().getBytes());
                }
            }

            @Override
            public void onScanFailed(int errorCode){
                super.onScanFailed(errorCode);
                Log.d("life", "onScanFailed / initScanCallback @ lollipop");
            }

        };
    }

    // LeScanCallback initialization ; API 18-19
    @SuppressWarnings("deprecation")
    protected void initLeScanCallback(){
        Log.d("life", "initScanCallback @ kitkat");
//        setContentView(R.layout.activity_main);

        mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
            @Override
            public void onLeScan(BluetoothDevice bluetoothDevice, int i, byte[] scanRecord) {
                scanData(bluetoothDevice, i, scanRecord);
           }
        };
    }

    @SuppressWarnings("deprecation")
    protected void scanData(BluetoothDevice bluetoothDevice, int rssi, byte[] scanRecord){

        boolean flag = false;

        // Parse the payload of the advertising packet.
        List<ADStructure> str = ADPayloadParser.getInstance().parse(scanRecord);

        for(ADStructure structure : str){
            // if the ADStructure instance can be cast to IBeacon
            if(structure instanceof IBeacon){

                flag = true;

                IBeacon iBeacon = (IBeacon) structure;

                UUID uuid = iBeacon.getUUID();  // Proximity UUID
                int major = iBeacon.getMajor(); // Major number
                int minor = iBeacon.getMinor(); // Minor number
                int power = iBeacon.getPower(); // tx power

                mTextuid.setText(uuid.toString());
                mTextMajor.setText(String.valueOf(major));
                mTextMinor.setText(String.valueOf(minor));
                mTextPower.setText(String.valueOf(power));

                Log.d("life", "IBeacon");
                Log.d("life", "uuid : " + uuid + ", major : " + major + ", minor : " + minor + ", power : " + power);

            } else if(structure instanceof EddystoneUID){
                // do nothing
                flag = true;
                Log.d("life", "EddystoneUID");
            } else if(structure instanceof EddystoneURL){
                // do nothing
                flag = true;
                Log.d("life", "EddystoneURL");
            } else if(structure instanceof EddystoneTLM){
                //
                flag = true;
                Log.d("life", "EddystoneTLM");
            } else if(structure instanceof EddystoneEID){
                //
                flag = true;
                Log.d("life", "EddystoneEID");
            }
        }

        if(Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT){
            if (flag == true && mBluetoothAdapter != null) {
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            }
        } else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if(mBluetoothLeScanner != null)
                mBluetoothLeScanner.stopScan(mScanCallback);
        }
//        mBluetoothGatt = bluetoothDevice.connectGatt(getApplicationContext(), false, mBtGattCallback);
    }

    @TargetApi(Build.VERSION_CODES.M)       // Android 6.0 or later
    protected boolean checkPermission(){
        if(PermissionChecker.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_LOCATION_STATE);
            return false;
        }
        return true;
    }

//    @TargetApi(Build.VERSION_CODES.M)       // Android 6.0 or later
//    public void onRequestPermissionResult(int requestCode, String[] permission, int[] grantResults){
//        if(PERMISSIONS_REQUEST_LOCATION_STATE == requestCode){
//            if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
//                // permitted
//                Toast.makeText(this, R.string.permission_6, Toast.LENGTH_SHORT).show();
//            } else {
//                // not permitted
//                Toast.makeText(this, R.string.unpermission_6, Toast.LENGTH_SHORT).show();
//                finish();
//            }
//        }
//    }


    @SuppressWarnings("deprecation")
    protected void startBleScan(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            // API is 21 or later
            if(mBluetoothLeScanner != null)
                mBluetoothLeScanner.startScan(mScanCallback);

            Log.d("life", "startBleScan, lollipop <");

        } else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
            // API is 18-19
            // scan stops after SCAN_PERIOD msec
            if(mBluetoothAdapter != null){

                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mBluetoothAdapter.stopLeScan(mLeScanCallback);

                        // INSERT : BLE STATUS CHECK
                    }
                }, SCAN_PERIOD);

                mBluetoothAdapter.startLeScan(mLeScanCallback);
            }

            Log.d("life", "startBleScan, kitkat <");

        } else {
            // API is 17 or former
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();

            Log.d("life", "startBleScan,  < 17");

            finish();
            return;
        }
    }

    @SuppressWarnings("deprecation")
    protected void stopBleScan(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            // API is 21 or later
            if(mBluetoothLeScanner != null)
                mBluetoothLeScanner.stopScan(mScanCallback);

            Log.d("life", "stopBleScan, lollipop <");

        } else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
            // API is 18-19
            if(mBluetoothAdapter != null){
                mBluetoothAdapter.stopLeScan(mLeScanCallback);

            }

            Log.d("life", "stopBleScan, kitkat <");

        } else {
            // API is 17 or former
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();

            Log.d("life", "stopBleScan, < 17");

            finish();
            return;
        }
    }

//    private BluetoothGattCallback mBtGattCallback =
//            new BluetoothGattCallback() {
//                @Override
//                public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
//                    super.onConnectionStateChange(gatt, status, newState);
//
//                    Log.d("life", "onConnectionStateChange : " + status + " -> " + newState);
//
//                    if(newState == BluetoothProfile.STATE_CONNECTED){
//                        // Connection succeeded to GATT
//                        // Search Services
//                        gatt.discoverServices();
//
//                    } else if(newState == BluetoothProfile.STATE_DISCONNECTED){
//                        // Disconnected from GATT
//                        mBluetoothGatt = null;
//                    }
//                }
//            };

//    // Device scan callback
//    private BluetoothAdapter.LeScanCallback mLeScanCallback
//            = new BluetoothAdapter.LeScanCallback(){
//        @Override
//        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord){
//
//            scanData(device, rssi, scanRecord);
////            // Parse the payload of the advertising packet.
////            List<ADStructure> str = ADPayloadParser.getInstance().parse(scanRecord);
////
////            for(ADStructure structure : str){
////                // if the ADStructure instance can be cast to IBeacon
////                if(structure instanceof IBeacon){
////
////                    IBeacon iBeacon = (IBeacon) structure;
////
////                    UUID uuid = iBeacon.getUUID();  // Proximity UUID
////                    int major = iBeacon.getMajor(); // Major number
////                    int minor = iBeacon.getMinor(); // Minor number
////                    int power = iBeacon.getPower(); // tx power
////
////                    mTextuid.setText(uuid.toString());
////                    mTextMajor.setText(major);
////                    mTextMinor.setText(minor);
////                    mTextPower.setText(power);
////
////                    Log.d("life", "IBeacon");
////
////                } else if(structure instanceof EddystoneUID){
////                    // do nothing
////                    Log.d("life", "EddystoneUID");
////                } else if(structure instanceof EddystoneURL){
////                    // do nothing
////                    Log.d("life", "EddystoneURL");
////                } else if(structure instanceof EddystoneTLM){
////                    //
////                    Log.d("life", "EddystoneTLM");
////                } else if(structure instanceof EddystoneEID){
////                    //
////                    Log.d("life", "EddystoneEID");
////                }
////            }
////
////            runOnUiThread(new Runnable() {
////                @Override
////                public void run() {
////                    mLeDeviceListAdapter.addDevice(device);
////                    mLeDeviceListAdapter.notifyDataSetChanged();
////                }
////            });
//        }
//    };

    static class ViewHolder{
        TextView deviceName;
        TextView deviceAddress;
    }
}
