package com.example.healthapp;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    public static final String EXTRA_MESSAGE = "com.example.btapp2.deviceAction";
    BluetoothAdapter btAdapter;

    Button btnONOFF;
    ListView listView;

    private final BroadcastReceiver mBroadcastReceiver1 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            assert action != null;
            if(action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(TAG, "onReceive: STATE OFF");
                        btnText();
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.d(TAG, "mBroadcastReceiver1: STATE TURNING OFF");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.d(TAG, "mBroadcastReceiver1: STATE ON");
                        btnText();
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(TAG, "mBroadcastReceiver1: STATE TURNING ON");
                        break;
                }
            }
        }
    };

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy: called.");
        super.onDestroy();
        unregisterReceiver(mBroadcastReceiver1);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnONOFF = findViewById(R.id.btnONOFF);
        listView = findViewById(R.id.pairedDevicesList);

        btAdapter = BluetoothAdapter.getDefaultAdapter();

        btnText();
        btnONOFF.setOnClickListener(v -> enableDisableBT());

    }

    public void enableDisableBT() {
        if(btAdapter == null) {
            Log.d(TAG, "enableDisableBT: Does not Bluetooth capabilities");
        }
        if(!btAdapter.isEnabled()) {
            Intent enableBTintent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBTintent);

            IntentFilter BTintent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(mBroadcastReceiver1, BTintent);
        }
        else if(btAdapter.isEnabled()) {
            btAdapter.disable();

            IntentFilter BTintent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(mBroadcastReceiver1, BTintent);
        }

    }

    public void btnText() {
        Button bntONOFF = findViewById(R.id.btnONOFF);
        TextView btStatusText = findViewById(R.id.btStatus);

        if(!btAdapter.isEnabled()) { // IF Bluetooth is OFF
            bntONOFF.setText(R.string.onText);
            btStatusText.setText(R.string.bluetoothOFF);
            btStatusText.setTextColor(Color.parseColor("#ff0055"));
            displayPairedDevices(false);
        } else if (btAdapter.isEnabled()) { // IF Bluetooth is ON
            bntONOFF.setText(R.string.offText);
            btStatusText.setText(R.string.bluetoothON);
            btStatusText.setTextColor(Color.parseColor("#009900"));
            displayPairedDevices(true);
        }
    }

    private void displayPairedDevices(boolean show) { //when bluetooth is turned on, show the list of paired devices
        if(!show) {
            listView.setAdapter(null);
            return;
        }
        Set<BluetoothDevice> btDevices = btAdapter.getBondedDevices();
        BluetoothDevice[] deviceList = new BluetoothDevice[btDevices.size()];
        String[] str = new String[btDevices.size()];
        int index = 0;

        if(btDevices.size()>0) {
            for(BluetoothDevice device: btDevices) {
                str[index] = device.getName();
                deviceList[index] = device;
                index++;
            }

            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1, str);
            listView.setAdapter(arrayAdapter);

            listView.setOnItemClickListener((parent, view, position, id) -> {
                String name = deviceList[position].getName();
                String MACaddress = deviceList[position].getAddress();
                Log.d(TAG, name);
                Log.d(TAG, MACaddress);

                // make an Intent to send the 'device' object to another activity
                Intent intent = new Intent(this, sensorMonitor.class);
                intent.putExtra(EXTRA_MESSAGE, deviceList[position]);
                startActivity(intent);
            });
        }

    }

}