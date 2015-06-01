package com.example.piris.weatherapp;

import android.app.Activity;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import java.nio.ByteBuffer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import android.bluetooth.le.ScanFilter;

import java.util.HashMap;
import java.util.UUID;


public class MainActivity extends ListActivity {
    private final static String TAG = DeviceControlActivity.class.getSimpleName();

    // for ibeacons getting last scan values for tx, minor,  major and uuid
    private HashMap TxLastScan = new HashMap<String, Integer>();
    private HashMap MajorLastScan = new HashMap<String, Integer>();
    private HashMap MinorLastScan = new HashMap<String, Integer>();
    private HashMap UUIDLastScan = new HashMap<String, String>();
    private HashMap PrefixLastScan = new HashMap<String, Integer>();

    //private HashMap RssiLastScan = new HashMap<String, Integer>();

    private String iBeaconUUID = "e2c56db5-dffb-48d2-b060-d0f5a71096e0";
    private HashMap iBeacons = new HashMap<String, Boolean>();



    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private boolean mScanning;
    private Handler mHandler;
    private static final int REQUEST_ENABLE_BT = 1;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;

    private static final String LOG_TAG = "debugger";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setTitle(R.string.title_devices);
        mHandler = new Handler();

        iBeacons.put("00:1A:7D:DA:71:07", true);
        iBeacons.put("00:1A:7D:DA:71:13", true);
        iBeacons.put("5C:F3:70:61:43:C8", true);


        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }


        // istantiate BL Scaner of beacons jiji
       // mBluetoothLeScanner = mBluetoothAdapter.getBluetoothScanner();


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        if (!mScanning) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
           menu.findItem(R.id.menu_refresh).setActionView(
                   R.layout.actionbar_indeterminate_progress);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                mLeDeviceListAdapter.clear();
                scanLeDevice(true);
                break;
            case R.id.menu_stop:
                scanLeDevice(false);
                break;
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }

        // Initializes list view adapter.
        mLeDeviceListAdapter = new LeDeviceListAdapter();
        setListAdapter(mLeDeviceListAdapter);
        scanLeDevice(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
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
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
        if (device == null) return;
        final Intent intent = new Intent(this, DeviceControlActivity.class);
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, device.getName());
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
        if (iBeacons.containsKey(device.getAddress())) {
            intent.putExtra(DeviceControlActivity.EXTRAS_TX_DATA, TxLastScan.get(device.getAddress()).toString());
            intent.putExtra(DeviceControlActivity.EXTRAS_MINOR, MinorLastScan.get(device.getAddress()).toString());
            intent.putExtra(DeviceControlActivity.EXTRAS_MAJOR, MajorLastScan.get(device.getAddress()).toString());
            intent.putExtra(DeviceControlActivity.EXTRAS_UUID, UUIDLastScan.get(device.getAddress()).toString());
            intent.putExtra(DeviceControlActivity.EXTRAS_PREFIX, PrefixLastScan.get(device.getAddress()).toString());

        }
        else {
            intent.putExtra(DeviceControlActivity.EXTRAS_TX_DATA, "No data");
            intent.putExtra(DeviceControlActivity.EXTRAS_MINOR, "No data");
            intent.putExtra(DeviceControlActivity.EXTRAS_MAJOR, "No data");
            intent.putExtra(DeviceControlActivity.EXTRAS_PREFIX, "No data");
            intent.putExtra(DeviceControlActivity.EXTRAS_UUID, "No data");
        }
        if (mScanning) {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            mScanning = false;
        }
        startActivity(intent);
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        invalidateOptionsMenu();
    }

    // Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
            mInflator = MainActivity.this.getLayoutInflater();
        }

        public void addDevice(BluetoothDevice device) {
            if(!mLeDevices.contains(device)) {
                mLeDevices.add(device);
            }
        }

        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }

        public void clear() {
            mLeDevices.clear();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            BluetoothDevice device = mLeDevices.get(i);
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else {
                if (iBeacons.containsKey(device.getAddress()))
                    viewHolder.deviceName.setText("iBeacon");
                else
                    viewHolder.deviceName.setText("Not found");
            }
            viewHolder.deviceAddress.setText(device.getAddress());
            return view;
        }
    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
                    runOnUiThread(new Runnable() {


                        @Override
                        public void run() {
                            if(iBeacons.containsKey(device.getAddress())){
                                Log.d(TAG, "IN SCAN CALL BACK ");
                                Log.d(TAG, Integer.toString(scanRecord.length));
                                Log.d(TAG, "RSSI>");
                                Log.d(TAG, Integer.toString(rssi));
                                Parcer par = new Parcer(scanRecord);

                                TxLastScan.put(device.getAddress(), par.getTX());
                                MinorLastScan.put(device.getAddress(), par.getMinor());
                                MajorLastScan.put(device.getAddress(), par.getMajor());
                                UUIDLastScan.put(device.getAddress(), iBeaconUUID);
                                PrefixLastScan.put(device.getAddress(), par.getPrefix());

                                /*
                                for (int i = 0; i < scanRecord.length; i++){
                                    if(i == 29)
                                          TxLastScan.put(device.getAddress(), scanRecord[i]);
                                    Log.d(TAG, "i: " + Integer.toString(i) + " value:" + Byte.toString(scanRecord[i]));
                                    Log.d(TAG, Byte.toString(scanRecord[i]));

                              }*/

                            }

                            mLeDeviceListAdapter.addDevice(device);
                            mLeDeviceListAdapter.notifyDataSetChanged();
                        }
                    });
                }
            };

    public static class Parcer{
        public byte[] scanRecord;
        Parcer(byte [] scan )
        {
            scanRecord = scan;
        }

        public int getTX(){
            int size = scanRecord.length;

            //first data structure
            int firstLeng = scanRecord[0];
            int secondLeng = scanRecord[firstLeng + 1];
            int txIndex = 1 + firstLeng + secondLeng;


        return scanRecord[txIndex];
        }


        public int getMinor(){
            int size = scanRecord.length;
            int firstLeng = scanRecord[0];
            int secondLeng = scanRecord[firstLeng + 1];
            byte firstByte = scanRecord[1 + firstLeng + secondLeng - 2];
            byte secondByte = scanRecord[1 + firstLeng + secondLeng - 1];
            return ((firstByte & 0xFF) << 8) | (secondByte & 0xFF);
        }

        public int getMajor(){
            int size = scanRecord.length;
            int firstLeng = scanRecord[0];
            int secondLeng = scanRecord[firstLeng + 1];
            byte firstByte = scanRecord[1 + firstLeng + secondLeng - 4];
            byte secondByte = scanRecord[1 + firstLeng + secondLeng - 3];
            return ((firstByte & 0xFF) << 8) | (secondByte & 0xFF);
        }

        public int getPrefix() {
            int size = scanRecord.length;
            int firstLeng = scanRecord[0];
            int secondLeng = scanRecord[firstLeng + 1];

            byte firstByte = scanRecord[1 + firstLeng + secondLeng - 22];
            byte secondByte = scanRecord[1 + firstLeng + secondLeng - 21];
            return ((firstByte & 0xFF) << 8) | (secondByte & 0xFF);

        }


    }

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
        
    }
}

