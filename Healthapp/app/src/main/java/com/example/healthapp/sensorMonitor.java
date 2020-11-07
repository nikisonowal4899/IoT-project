package com.example.healthapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.UUID;

import static com.example.healthapp.MainActivity.*;

public class sensorMonitor extends AppCompatActivity {
    private static final String TAG = "MyConnection__";
    public static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    TextView showStatus, tempView, bpmView, pressView;
    TextView tempAlert, bpmAlert, pressAlert;

    BluetoothAdapter btAdapter;
    BluetoothDevice btDevice;
    BluetoothSocket btSocket;

    ConnectThread connect_thread; // Thread class for making a connection
    ConnectedThread connectedThread; // Thread class for transferring data

    static final int STATE_CONNECTING = 1;
     static final int STATE_CONNECTED = 2;
      static final int STATE_CONNECTION_FAILED = 3;
       static final int SOCKET_CREATED = 4;
      static final int SOCKET_FAILED = 5;
     static final int STATE_DISCONNECTED = 6;
    static final int STATE_MESSAGE_RECEIVED = 7;

    public volatile boolean threadRunning = false;
    public volatile boolean socketConnected = false;

    FirebaseDatabase database;
    DatabaseReference databaseReference;

    public String incomingData; // This string will store the incoming data

    Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {

            switch (msg.arg1) {
                case STATE_CONNECTING:
                    showStatus.setText("Connecting...");
                    showStatus.setTextColor(Color.parseColor("#cc8800"));
                    break;
                case STATE_CONNECTED:
                    showStatus.setText("Connected !");
                    showStatus.setTextColor(Color.parseColor("#00b300"));
                    socketConnected = true;
                    break;
                case STATE_CONNECTION_FAILED:
                    showStatus.setText("Connection Failed");
                    showStatus.setTextColor(Color.parseColor("#ff6666"));
                    break;
                case SOCKET_CREATED:
                    showStatus.setText("Socket Created");
                    break;
                case SOCKET_FAILED:
                    showStatus.setText("Socket Failed");
                    break;
                case STATE_DISCONNECTED:
                    showStatus.setText("Disconnected");
                    showStatus.setTextColor(Color.parseColor("#ff0000"));
                    socketConnected = false;
                    break;
                case STATE_MESSAGE_RECEIVED:
                    byte[] readBuffer = (byte[]) msg.obj;
                    String tempMsg = new String(readBuffer, 0, msg.arg2);
                    Log.d(TAG, "Temperature: "+ tempMsg);
                    stringProcessor(tempMsg);
                    break;
            }

            return false;
        }
    });

    @Override
    protected void onDestroy() {
        if(connectedThread != null) {
            connectedThread.cancel();
        }
        else if (connect_thread != null) {
            connect_thread.cancel();
        }
        Log.d(TAG, "onDestroy: Connection activity destroy hoi gol");
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor_monitor);

        Intent intent  = getIntent();
        btDevice = intent.getParcelableExtra(EXTRA_MESSAGE);
        btAdapter = BluetoothAdapter.getDefaultAdapter();

        incomingData = ""; // making it empty at the beginning

        showStatus = findViewById(R.id.connectionStatus);
        tempView = findViewById(R.id.tempView);
        bpmView = findViewById(R.id.bpmView);
        pressView = findViewById(R.id.pressureView);

        tempAlert = findViewById(R.id.tempAlert);
        bpmAlert = findViewById(R.id.bpmAlert);
        pressAlert = findViewById(R.id.pressAlert);

        database = FirebaseDatabase.getInstance();
        databaseReference = database.getReference("Patient data");

        MakeConnection();

    } //onCreate function ends here

    void MakeConnection() {
        connect_thread = new ConnectThread(btDevice.getAddress()); // Declaring the Bluetooth Connect class
        new Thread(connect_thread).start();
    }

    public class ConnectThread implements Runnable { //Thread for making connection to HC-05 module
        private final BluetoothSocket thisSocket;

        public ConnectThread(String address) { // This is the Constructor

            BluetoothDevice thisDevice = btAdapter.getRemoteDevice(address);

            BluetoothSocket temp = null;
            try {
                temp = thisDevice.createRfcommSocketToServiceRecord(myUUID);
                UpdateStatus(SOCKET_CREATED);
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "ConnectThread: Socket create() method failed", e);
                UpdateStatus(SOCKET_FAILED);
            }
            thisSocket = temp;
        }

        @Override
        public void run() {
            threadRunning = true;
            btAdapter.cancelDiscovery();

            try {
                Log.d(TAG, "run: Connecting...");
                UpdateStatus(STATE_CONNECTING);
                thisSocket.connect(); // Connect to the Bluetooth Module
                Log.d(TAG, "run: Socket Connected !");

            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "run: Connect koribo nuarile", e);
                UpdateStatus(STATE_CONNECTION_FAILED);

                this.cancel(); // unable to connect socket, close socket and return

                threadRunning = false;
                return;
            }

            // If the code has reached here, the connection has been established !
            btSocket = thisSocket;
            this.MakeStream(btSocket);

            threadRunning = false;
        } // end of run()

        public void MakeStream(BluetoothSocket btSocket) {
            connectedThread = new ConnectedThread(btSocket);
            new Thread(connectedThread).start();
        }

        public void cancel() {
            try {
                thisSocket.close();
                Log.d(TAG, "cancel: Socket closed");
                threadRunning = false;

            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "cancel: Could not close client socket", e);
            }
        }
    } // END of connect thread class



    public class ConnectedThread implements Runnable {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private final BluetoothSocket mmSocket;

        public ConnectedThread(BluetoothSocket socket) {
            InputStream tempIn = null;
            OutputStream tempOut = null;
            this.mmSocket = socket;

            try {
                tempIn = socket.getInputStream();
                Log.d(TAG, "ConnectedThread: Input Stream created successfully !");

            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "ConnectedThread: Error occurred when creating Input Stream", e);
            }

            try {
                tempOut = socket.getOutputStream();
                Log.d(TAG, "ConnectedThread: Output Stream created successfully !");

            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "ConnectedThread: Error occurred when creating Output Stream", e);
            }

            mmInStream = tempIn;
            mmOutStream = tempOut;
            UpdateStatus(STATE_CONNECTED); // ready to transfer data
        }

        @Override
        public void run() {
            // Will contain the message.
            byte[] mmBuffer = new byte[1024]; // will contain the message
            int numBytes; // number of bytes in the message

            while(true) {
                try {
                    numBytes = mmInStream.read(mmBuffer); // Reading from bluetooth

                    Message readMsg = Message.obtain();
                    readMsg.arg1 = STATE_MESSAGE_RECEIVED;
                    readMsg.arg2 = numBytes;
                    readMsg.obj = mmBuffer;
                    handler.sendMessage(readMsg);

                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "run: Input stream was Disconnected!", e);
                    UpdateStatus(STATE_DISCONNECTED); // Stream disconnected. Close socket and reconnect
                    this.cancel();
                    break;
                }
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
                mmInStream.close();
                mmOutStream.close();
                Log.d(TAG, "cancel: Closed everything");

            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "cancel: Error closing the socket", e);
            }
        }
    }

    public void UpdateStatus(int STATE) {
        Message msg = Message.obtain();
        msg.arg1 = STATE;
        handler.sendMessage(msg);
    }

    public void stringProcessor(String temp) { // process the incoming string and make noise free
        // Idea: A complete data packet contains a 'A' at beginning and 'Z' at the end
        // global data storing string is 'incomingData'
        // If data packet is complete and correct display it on screen and send it to Firebase database

        // this algorithm is made by observing the incoming data from the HC-05 module.

        if(temp.length() == 7) { // if there are 7 characters in the packet, most probably correct character
            afterStringProcessor(temp);
            incomingData = "";
        }
        else if(temp.length() < 7) { // if the whole packet didn't came
            incomingData = incomingData + temp;
            if(incomingData.length() == 7) {
                afterStringProcessor(incomingData);
                incomingData = "";
            }
        }
        else if(temp.length() > 7) { // things went wrong and garbage is present in the incomingData string. So clear it
            incomingData = "";
        }
    }

    public void afterStringProcessor(String temp) {
        if(temp.charAt(0) == 'A' && temp.charAt(6) == 'Z') { // extract the middle float number
            temp = temp.substring(1, 6);
            float f;
            try {
                f = Float.parseFloat(temp);
            } catch(ArithmeticException e) {
                Log.e(TAG, "afterStringProcessor: Error occured while extracting the temperature", e);
                return;
            }

            if(f < 99.0) { // all processing done. Final temperature value
                temp = String.valueOf(f);
                temp = temp + " °C";
                tempView.setText(temp);

                Random rand = new Random();
                String bpm = String.valueOf(rand.nextInt(120 - 50) + 50);
                String pressure = String.valueOf(rand.nextInt(150 - 120)+ 120);

                bpmView.setText(bpm + "bpm");
                pressView.setText(pressure + "mmHg");

                if(f > 30.0) {
                    // if body temperature is greater than 30 °C, set temp alert
                    tempAlert.setText(R.string.alert);
                    tempAlert.setTextColor(Color.parseColor("#ff1a1a"));
                } else {
                    tempAlert.setText(R.string.ok);
                    tempAlert.setTextColor(Color.parseColor("#2db300"));
                }

                if(Integer.parseInt(bpm) > 100 || Integer.parseInt(bpm) < 55) { // Heart rate alert
                    bpmAlert.setText(R.string.alert);
                    bpmAlert.setTextColor(Color.parseColor("#ff1a1a"));
                } else {
                    bpmAlert.setText(R.string.ok);
                    bpmAlert.setTextColor(Color.parseColor("#2db300"));
                }

                if(Integer.parseInt(pressure) > 145) {
                    pressAlert.setText(R.string.alert);
                    pressAlert.setTextColor(Color.parseColor("#ff1a1a"));
                } else {
                    pressAlert.setText(R.string.ok);
                    pressAlert.setTextColor(Color.parseColor("#2db300"));
                }

                // at this point we have the data ready to send to firebase.
                Firebase_upload firebase_upload = new Firebase_upload(temp, bpm, pressure);
                new Thread(firebase_upload).start();
            }
        }
    }

    public class Firebase_upload implements Runnable {
        private final String temperature, pressure, bpm;

        public Firebase_upload(String temp, String bpm, String press) { // constructor
            this.temperature = temp;
            this.bpm = bpm;
            this.pressure = press;
        }

        @Override
        public void run() {
            DataClass dataReady = new DataClass(temperature, bpm, pressure);

            // get the system date and time as the id
            SimpleDateFormat formatter = new SimpleDateFormat("dd_MM_yyyy_HH:mm:ss");
            Date date = new Date();
            String id = formatter.format(date);

            databaseReference.child(id).setValue(dataReady); // storing to the Firebase database
        }
    }
}