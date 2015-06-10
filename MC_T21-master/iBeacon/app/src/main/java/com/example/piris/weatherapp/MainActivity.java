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
import android.os.SystemClock;
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

import java.text.DecimalFormat;
import java.util.ArrayList;
import android.bluetooth.le.ScanFilter;

import java.util.HashMap;
import java.util.UUID;


public class MainActivity extends ListActivity {
    private final static String TAG = DeviceControlActivity.class.getSimpleName();

    // for ibeacons getting last scan values for tx, minor,  major and uuid

    private ArrayList<Integer> rssiListA = new ArrayList<Integer>();
    private ArrayList<Integer> rssiListB = new ArrayList<Integer>();
    private ArrayList<Integer> rssiListC = new ArrayList<Integer>();
    private ArrayList<Double> errorA = new ArrayList<Double>();
    private ArrayList<Double> errorB = new ArrayList<Double>();
    private ArrayList<Double> errorC = new ArrayList<Double>();

    //public int [] errorA = new int [5];
    //public int [] errorB = new int [5];
    //public int [] errorC = new int [5];
    /*******************************************************************/
    private int meters = 1;
    private HashMap TxLastScan = new HashMap<String, Integer>();
    private HashMap MajorLastScan = new HashMap<String, Integer>();
    private HashMap MinorLastScan = new HashMap<String, Integer>();
    private HashMap UUIDLastScan = new HashMap<String, String>();
    private HashMap PrefixLastScan = new HashMap<String, Integer>();
    private HashMap RssiLastScan = new HashMap<String, Integer>();
    private HashMap distanceLastScan = new HashMap<String, Double>();
    private HashMap calibratedRSSI = new HashMap<String, Integer>();
    private HashMap calibratedDistance = new HashMap<String, Double>();
    private boolean firstScanDone = false;
    private boolean secondScanDone = false;

    /************/
    private HashMap firstCal = new HashMap<String, Double>();
    private HashMap secondCal = new HashMap<String, Double>();
    private HashMap afterCal = new HashMap<String, Double>();





    private String iBeaconUUID = "e2c56db5-dffb-48d2-b060-d0f5a71096e0";
    private HashMap iBeacons = new HashMap<String, Boolean>();
    private HashMap calibrateVal = new HashMap<String, Integer>();
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private volatile boolean mScanning;
    private Handler mHandler;
    private static final int REQUEST_ENABLE_BT = 1;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 1500;
    private static final long STOP_INTERVAL = 500;
    private static final String LOG_TAG = "debugger";
    private static boolean isSecondScan = false;



    private Runnable startScan = new Runnable() {
        @Override
        public void run() {

            printRssi();
            if(rssiListA.size() == 10 && rssiListB.size() == 10 && rssiListC.size() == 10)// WHen is taken 10 samples
            //if(rssiListA.size() == 3 && rssiListB.size() == 3 && rssiListC.size() == 3)// WHen is taken 10 samples
            {
                    if(errorA.size() <= 5 && errorA.size() <= 5  && errorC.size() <= 5)
                    {

                        mBluetoothAdapter.stopLeScan(mLeScanCallback);
                        Compare();
                    }


            }
            else{
                mBluetoothAdapter.startLeScan(mLeScanCallback);
                mHandler.postDelayed(stopScan, SCAN_PERIOD);
            }
        }
    };

    private Runnable stopScan = new Runnable() {
        @Override
        public void run() {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            mHandler.postDelayed(startScan, STOP_INTERVAL);
        }
    };


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
            menu.findItem(R.id.menu_makeScans).setVisible(false);
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

                //while(rssiListA.size() < 10) {
                    scanLeDevice(true);
                   // SystemClock.sleep(17000);
                //}
                //printRssi();
                break;
            case R.id.menu_stop:
                scanLeDevice(false);
                break;
            case R.id.menu_makeScans:
                Toast.makeText(this, "Please go ahead and place yourself at " + meters + " m", Toast.LENGTH_SHORT).show();
                //mHandlerstartScan.run();
                mHandler.postDelayed(startScan, 1000);
                break;
            case R.id.menu_move:
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
        //scanLeDevice(true);
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
            intent.putExtra(DeviceControlActivity.EXTRAS_RSSI, RssiLastScan.get(device.getAddress()).toString());
            intent.putExtra(DeviceControlActivity.EXTRAS_DISTANCE, distanceLastScan.get(device.getAddress()).toString());
            if (device.getAddress().equals("00:1A:7D:DA:71:07"))
                intent.putExtra(DeviceControlActivity.EXTRAS_ERROR, errorA);
            else if (device.getAddress().equals("00:1A:7D:DA:71:13"))
                intent.putExtra(DeviceControlActivity.EXTRAS_ERROR,  errorB);
            if (device.getAddress().equals("5C:F3:70:61:43:C8"))
                intent.putExtra(DeviceControlActivity.EXTRAS_ERROR,  errorC);

        }
        else {
            intent.putExtra(DeviceControlActivity.EXTRAS_TX_DATA, "No data");
            intent.putExtra(DeviceControlActivity.EXTRAS_MINOR, "No data");
            intent.putExtra(DeviceControlActivity.EXTRAS_MAJOR, "No data");
            intent.putExtra(DeviceControlActivity.EXTRAS_PREFIX, "No data");
            intent.putExtra(DeviceControlActivity.EXTRAS_UUID, "No data");
            intent.putExtra(DeviceControlActivity.EXTRAS_RSSI, "No data");
            intent.putExtra(DeviceControlActivity.EXTRAS_DISTANCE, "No data");
        }
        if (mScanning) {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            mScanning = false;
        }
        startActivity(intent);
    }



    private void makePlotBars(){



        Log.d(TAG, "Plootiingg........******************************************************************");

        for (int i = 0; i<errorA.size(); i++) {
            Log.d(TAG, Double.toString(errorA.get(i)));
            Log.d(TAG, Double.toString(errorB.get(i)));
            Log.d(TAG, Double.toString(errorC.get(i)));

        }
        Toast.makeText(this, "sTARTINT TO PLOT ", Toast.LENGTH_SHORT).show();

    }


    private void makeSecondScan(){
        emptyLists();
        secondScanDone = true;
        startScan.run();

    }

    //Get average of samples
    private  void Compare(){


        Integer sumA = 0;
        Integer sumB = 0;
        Integer sumC = 0;
        for(int rssi : rssiListA){
            sumA += rssi;
        }
        for(int rssi : rssiListB){
            sumB += rssi;
        }

        for(int rssi : rssiListC){
            sumC += rssi;
        }



        double expectedValueA = sumA.doubleValue() / rssiListA.size();
        double expectedValueB = sumB.doubleValue() / rssiListB.size();
        double expectedValueC = sumC.doubleValue() / rssiListC.size();
        calibratedRSSI.put("00:1A:7D:DA:71:07", expectedValueA);
        calibratedRSSI.put("00:1A:7D:DA:71:13", expectedValueB);
        calibratedRSSI.put("5C:F3:70:61:43:C8", expectedValueC);

        calibratedDistance.put("00:1A:7D:DA:71:07", calculateDistance((Integer) TxLastScan.get("00:1A:7D:DA:71:07"), expectedValueA));
        calibratedDistance.put("00:1A:7D:DA:71:13", calculateDistance((Integer) TxLastScan.get("00:1A:7D:DA:71:13"), expectedValueB));
        calibratedDistance.put("5C:F3:70:61:43:C8", calculateDistance((Integer) TxLastScan.get("5C:F3:70:61:43:C8"), expectedValueC));




        errorA.add(Math.abs((double) calibratedDistance.get("00:1A:7D:DA:71:07") - (double) meters));
        errorB.add(Math.abs((double) calibratedDistance.get("00:1A:7D:DA:71:13") - (double) meters));
        errorC.add(Math.abs((double) calibratedDistance.get("5C:F3:70:61:43:C8") - (double) meters));



        emptyLists();


        if (meters <= 5)
        {

            Toast.makeText(this, "Please go and move " + Integer.toString(meters) + " meters away more", Toast.LENGTH_SHORT).show();

            mHandler.postDelayed(startScan, STOP_INTERVAL);

        }

        else {

            makePlotBars();

        }


        meters++;


    }

    private void emptyLists(){
        rssiListA.clear();
        rssiListB.clear();
        rssiListC.clear();
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
            },SCAN_PERIOD);

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
                viewHolder.distance = (TextView) view.findViewById(R.id.device_distance);
                viewHolder.RSSI = (TextView) view.findViewById(R.id.device_rssi);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();

            }

            BluetoothDevice device = mLeDevices.get(i);
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else {
                if (iBeacons.containsKey(device.getAddress())) {
                    viewHolder.deviceName.setText("iBeacon");

                    //if( calibratedDistance.isEmpty()) {
                    viewHolder.distance.setText("Distance in m: " + new DecimalFormat("##.##").format(distanceLastScan.get(device.getAddress())));
                    viewHolder.RSSI.setText("RSSI: " + RssiLastScan.get(device.getAddress()).toString());

                }
                else
                    viewHolder.deviceName.setText("Unknown");
                //String dist = distanceLastScan.get(device.getAddress()).toString();
            }
            viewHolder.deviceAddress.setText(device.getAddress());
            //String dist = distanceLastScan.get(device.getAddress()).toString();
            //viewHolder.distance.setText(distanceLastScan.get(device.getAddress()).toString());
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
                                Parcer par = new Parcer(scanRecord);
                                double distance_Beacon =  calculateAccuracy(par.getTX(), (double) rssi);
                                double distanceI = calculateDistance(par.getTX(), (double) rssi);

                                TxLastScan.put(device.getAddress(), par.getTX());
                                MinorLastScan.put(device.getAddress(), par.getMinor());
                                MajorLastScan.put(device.getAddress(), par.getMajor());
                                UUIDLastScan.put(device.getAddress(), iBeaconUUID);
                                PrefixLastScan.put(device.getAddress(), par.getPrefix());
                                RssiLastScan.put(device.getAddress(), rssi);

                                if (device.getAddress().equals("00:1A:7D:DA:71:07"))
                                    rssiListA.add(rssi);
                                else if (device.getAddress().equals("00:1A:7D:DA:71:13"))
                                    rssiListB.add(rssi);
                                else if (device.getAddress().equals("5C:F3:70:61:43:C8"))
                                    rssiListC.add(rssi);
                                distanceLastScan.put(device.getAddress(), distanceI);



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


    public static double calculateAccuracy(int txPower, double rssi) {
        if (rssi == 0) {
            return -1.0; // if we cannot determine accuracy, return -1.
        }

        double ratio = rssi*1.0/txPower;
        if (ratio < 1.0) {
            return Math.pow(ratio,10);
        }
        else {
            double accuracy =  (0.89976)*Math.pow(ratio,7.7095) + 0.111;
            return accuracy;
        }
    }

    public static double calculateDistance(int txPower, double rssi) {
        if (rssi == 0) {
            return -1.0; // if we cannot determine accuracy, return -1.
        }

        double d = Math.exp(Math.log(10)*(rssi - txPower)/(-20));
        return d;
    }

    public static double calculateRssi(int distance, int Tx){
        double d = (double) distance;
        double calculatedRssi = (-20)*Math.log10(d) + Tx;
        return calculatedRssi;
    }


    public void printRssi(){
        Log.d(TAG, "IN PRINTITG THE LIST");
        int size = rssiListA.size();
        Log.d(TAG, Integer.toString(size));
        //for (int i = 0; i < size; i++)
          //  Log.d(TAG, Integer.toString(rssiListA.get(i)));
    }

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
        TextView distance;
        TextView RSSI;
    }
}

