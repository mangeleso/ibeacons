package com.example.piris.weatherapp;

/**
 * Created by PIRIS on 5/19/2015.
 */
import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DeviceControlActivity extends Activity {
    private final static String TAG = DeviceControlActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    //for ibeacons
    public static final String EXTRAS_TX_DATA = "TX_DATA";
    public static final String EXTRAS_MAJOR = "MAJOR";
    public static final String EXTRAS_MINOR = "MINOR";
    public static final String EXTRAS_UUID = "UUID";
    public static final String EXTRAS_PREFIX = "PREFIX";

    private TextView mConnectionState;

    //for ibecons
    private TextView txDataField;
    private TextView majorDataField;
    private TextView minorDataField;
    private TextView prefixDataField;
    private TextView uuidDataField;




    private String mDeviceName;
    private String mDeviceAddress;
    private ExpandableListView mGattServicesList;
    private BluetoothLeService mBluetoothLeService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private boolean mConnected = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic;

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
                clearUI();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (BluetoothLeService.TEMPERATURE_MEASUREMENT.equals(action)) {
                Log.d(TAG, "Temp measurement...............");
                displayDataTemp(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            }
            else if (BluetoothLeService.HUMIDITY_MEASUREMENT.equals(action)) {
                Log.d(TAG, "Hum measurement...............");
                displayDataHum(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            }
            else{
                Log.d(TAG, action);
            }
        }
    };

    // If a given GATT characteristic is selected, check for supported features.  This sample
    // demonstrates 'Read' and 'Notify' features.  See
    // http://d.android.com/reference/android/bluetooth/BluetoothGatt.html for the complete
    // list of supported characteristic features.
    private final ExpandableListView.OnChildClickListener servicesListClickListner =
            new ExpandableListView.OnChildClickListener() {
                @Override
                public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
                                            int childPosition, long id) {


                    if (mGattCharacteristics != null) {
                        final BluetoothGattCharacteristic characteristic =
                                mGattCharacteristics.get(groupPosition).get(childPosition);
                        final int charaProp = characteristic.getProperties();
                        if ((charaProp & BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                            if ((charaProp & BluetoothGattCharacteristic.PROPERTY_WRITE) == 0) {
                                // If there is an active notification on a characteristic, clear
                                // it first so it doesn't update the data field on the user interface.
                                Log.d(TAG, "In property READ....");
                                if (mNotifyCharacteristic != null) {
                                    mBluetoothLeService.setCharacteristicNotification(
                                            mNotifyCharacteristic, false);
                                    mNotifyCharacteristic = null;
                                }
                                mBluetoothLeService.readCharacteristic(characteristic);
                            }
                        }
                        /*TODO: Implement for notifications*/
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                            if ((charaProp & BluetoothGattCharacteristic.PROPERTY_WRITE) == 0) {
                            }
                        }
                        /*TODO: Implement for fan control*/
                        if ((charaProp & BluetoothGattCharacteristic.PROPERTY_WRITE) > 0) {
                            mNotifyCharacteristic = characteristic;
                            Log.d(TAG, "In property WRITE...."+charaProp);
                            //byte [] dataSent =  new byte[] {(byte)0xc3,(byte) 0x50};
                        }
                        return true;
                    }
                    return false;
                }
            };

    private void clearUI() {
        mGattServicesList.setAdapter((SimpleExpandableListAdapter) null);
        //mDataField.setText(R.string.no_data);

        majorDataField.setText(R.string.no_data);
        txDataField.setText(R.string.no_data);
        minorDataField.setText(R.string.no_data);

        //rssiDataField.setText(R.string.no_data);
        prefixDataField.setText("No data");
        uuidDataField.setText("No data");

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gatt_services_characteristics);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        // for ibeacon
        String beacondTx = intent.getStringExtra(EXTRAS_TX_DATA);
        String beaconMinor = intent.getStringExtra(EXTRAS_MINOR);
        String beaconMajor = intent.getStringExtra(EXTRAS_MAJOR);
        String beaconPrefix = intent.getStringExtra(EXTRAS_PREFIX);
        String beaconUUID = intent.getStringExtra(EXTRAS_UUID);



        // Sets up UI references.
        ((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);
        mGattServicesList = (ExpandableListView) findViewById(R.id.gatt_services_list);
        mGattServicesList.setOnChildClickListener(servicesListClickListner);
        mConnectionState = (TextView) findViewById(R.id.connection_state);
        //mDataFieldTemp = (TextView) findViewById(R.id.data_value_temp);
        //mDataFieldHum = (TextView) findViewById(R.id.data_value_hum);

        //checkBox = (CheckBox) findViewById(R.id.checkBox);
        txDataField = (TextView) findViewById(R.id.data_value_tx);
        majorDataField = (TextView) findViewById(R.id.data_value_major);
        minorDataField = (TextView) findViewById(R.id.data_value_minor);
        uuidDataField = (TextView) findViewById(R.id.data_value_uuid);
        prefixDataField = (TextView) findViewById(R.id.data_value_prefix);

        //rssiDataField = (TextView) findViewById(R.id.data_value_rssi);


        getActionBar().setTitle(mDeviceName);
        getActionBar().setDisplayHomeAsUpEnabled(true);


        displayBeacon(beacondTx, beaconMinor, beaconMajor, beaconPrefix, beaconUUID);

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_connect:
                mBluetoothLeService.connect(mDeviceAddress);
                return true;
            case R.id.menu_disconnect:
                mBluetoothLeService.disconnect();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConnectionState.setText(resourceId);
            }
        });
    }

    private void displayDataTemp(String data) {
        Log.d(TAG,"Showing Temp: ");
        //Log.d(TAG, data + " °C");
        if (data != null) {
            //mDataFieldTemp.setText(data + " °C");
        }
    }

    private void displayDataHum(String data) {
        Log.d(TAG,"Showing Hum: ");
        if (data != null) {
            StringBuffer humidity = new StringBuffer(data.replaceAll("\\s",""));
            char temp1;
            char temp2;
            humidity.deleteCharAt(0);
            Log.d(TAG, "Humidity2 string: " + humidity.length());
            Log.d(TAG, "Humidity2 string: " + humidity.toString());
            humidity.deleteCharAt(0);
            Log.d(TAG, "Humidity3 string: " + humidity.length());
            Log.d(TAG, "Humidity3 string: " + humidity.toString());
            humidity.reverse();
            Log.d(TAG, "Humidity3 reverse: " + humidity.toString());
            temp1 = humidity.charAt(1);
            temp2 = humidity.charAt(2);
            humidity.deleteCharAt(1);
            humidity.deleteCharAt(1);
            humidity.insert(0,temp1);
            humidity.append(temp2);
            Log.d(TAG, "Humidity4 string: " + humidity.toString());
            float n = Long.parseLong(humidity.toString(), 16);
            n = n*0.01f;
            //mDataFieldHum.setText(String.valueOf(n) + "%");
            //mDataFieldHum.setText(humidity.toString() + "%");
            //mDataFieldHum.setText(data + "%");
        }
    }


    private void displayBeacon(String tx, String minor, String major,String prefix, String uuidB){
        txDataField.setText(tx);
        minorDataField.setText(minor);
        majorDataField.setText(major);
        prefixDataField.setText(prefix);
        uuidDataField.setText(uuidB);
    }

    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;
        //String weatherServiceString = getResources().getString(R.string.weather_service);
        //String fanServiceString = getResources().getString(R.string.fan_service);
        //String speedString = getResources().getString(R.string.speed_char);

        //String tempCharaString = getResources().getString(R.string.temp_characteristic);
        //String humidityCharaString = getResources().getString(R.string.humidity_characteristic);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
                = new ArrayList<ArrayList<HashMap<String, String>>>();
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        SimpleExpandableListAdapter gattServiceAdapter = new SimpleExpandableListAdapter(
                this,
                gattServiceData,
                android.R.layout.simple_expandable_list_item_2,
                new String[] {LIST_NAME, LIST_UUID},
                new int[] { android.R.id.text1, android.R.id.text2 },
                gattCharacteristicData,
                android.R.layout.simple_expandable_list_item_2,
                new String[] {LIST_NAME, LIST_UUID},
                new int[] { android.R.id.text1, android.R.id.text2 }
        );
        mGattServicesList.setAdapter(gattServiceAdapter);
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.TEMPERATURE_MEASUREMENT);
        intentFilter.addAction(BluetoothLeService.HUMIDITY_MEASUREMENT);
        return intentFilter;
    }


}

