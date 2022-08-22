package com.babbangona.connectdevicesjava;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    Button scan, send, listDevices;
    ListView listView;
    TextView bTScaleWeight, status, title, closeTitle;
   // EditText enteredMessage;
    BluetoothAdapter bluetoothAdapter;
    BluetoothDevice[] btArray;
    SendReceive sendReceive;


    // Constants for the  handler
    static final int STATE_LISTENING = 1;
    static final int STATE_CONNECTING = 2;
    static final int STATE_CONNECTED = 3;
    static final int STATE_CONNECTION_FAILED = 4;
    static final int STATE_MESSAGE_RECEIVED = 5;

    // int object to request enable bluetooth
    int REQUEST_ENABLE_BLUETOOTH = 7;

    private static final String APP_NAME = "BLUETOOTH_CHAT";
    private static final UUID THE_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewByIds();
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                startActivityForResult(enableIntent, REQUEST_ENABLE_BLUETOOTH);
            }

        }

        implementListeners();
    }

    private void implementListeners() {
        listDevices.setOnClickListener(view -> {
            // show all the list of already paired devices on a list view
            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Set<BluetoothDevice> bluetoothDeviceSet = bluetoothAdapter.getBondedDevices();
                String[] strings = new String[bluetoothDeviceSet.size()];
                btArray = new BluetoothDevice[bluetoothDeviceSet.size()];
                int index = 0;

                if (bluetoothDeviceSet.size() > 0) {
                    for (BluetoothDevice device : bluetoothDeviceSet) {
                        btArray[index] = device;
                        strings[index] = device.getName();
                        index++;
                    }
                    ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1, strings);
                    listView.setAdapter(arrayAdapter);
                }
            }

        });

        scan.setOnClickListener(view -> {
            ServerClass serverClass = new ServerClass();
            serverClass.start();
        });

        listView.setOnItemClickListener((adapterView, view, i, l) -> {
            ClientClass clientClass = new ClientClass(btArray[i]);
            clientClass.start();

            status.setText("Connecting");
        });

        send.setOnClickListener(view -> {
                    String stringTitle = String.valueOf(title.getText());;
                    String stringHub = "Hub : Lagos";
                    String stringName = "Name : Zainab Lawal";
                    String stringDate = "05/08/2022 13:04";
                    String stringCrop = "Crop : Maize";
                    String stringBTScale = String.valueOf(bTScaleWeight.getText());
                    String stringcloseTitle = String.valueOf(closeTitle.getText());

                    String stringFormat = "\n" + stringTitle + "\n" + stringHub + "\n" + stringName + "\n" +
                            stringDate + "\n" + stringCrop + "\n" + stringBTScale + "\n" + stringcloseTitle + "\n";


                    sendReceive.write(stringFormat.getBytes());
                /**    sendReceive.write(stringBTScale.getBytes());
                    sendReceive.write(stringcloseTitle.getBytes());*/


                }
        );
    }

    Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {

            switch (msg.what) {
                case STATE_LISTENING:
                    status.setText("Listening");
                    break;
                case STATE_CONNECTING:
                    status.setText("connecting");
                    break;
                case STATE_CONNECTED:
                    status.setText("connected");
                    break;
                case STATE_CONNECTION_FAILED:
                    status.setText("connection failed");
                    break;
                case STATE_MESSAGE_RECEIVED:
                    byte[] readBuffer = (byte[]) msg.obj;
                    String tempMsg = new String(readBuffer, 0, msg.arg1);
                    bTScaleWeight.setText(tempMsg);

                    break;
            }
            return true;
        }
    });

    private void findViewByIds() {
        scan = findViewById(R.id.button);
        send = findViewById(R.id.button3);
        listDevices = findViewById(R.id.button2);
        listView = findViewById(R.id._dynamic);
        bTScaleWeight = findViewById(R.id.textView2);
        title = findViewById(R.id.textView4);

        closeTitle = findViewById(R.id.textView17);
        status = findViewById(R.id.textView);
        //enteredMessage = findViewById(R.id.editTextTextPersonName);
    }

    private class ServerClass extends Thread {
        private BluetoothServerSocket serverSocket;

        @SuppressLint("MissingPermission")
        public ServerClass() {

            try {
                serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME, THE_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run() {
            BluetoothSocket socket = null;

            while (socket == null) {
                try {
                    Message message = Message.obtain();
                    message.what = STATE_CONNECTING;
                    handler.sendMessage(message);

                    socket = serverSocket.accept();
                } catch (IOException e) {
                    e.printStackTrace();
                    Message message = Message.obtain();
                    message.what = STATE_CONNECTION_FAILED;
                    handler.sendMessage(message);
                }

                if (socket != null) {
                    Message message = Message.obtain();
                    message.what = STATE_CONNECTED;
                    handler.sendMessage(message);

                    sendReceive = new SendReceive(socket);
                    sendReceive.start();

                    break;
                }
            }

        }


    }

    private class ClientClass extends Thread {
        private BluetoothDevice device;

        private BluetoothSocket socket;

        @SuppressLint("MissingPermission")
        public ClientClass(BluetoothDevice device1) {
            device = device1;
            try {
                socket = device.createRfcommSocketToServiceRecord(THE_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @SuppressLint("MissingPermission")
        public void run() {
            try {
                socket.connect();
                Message message = Message.obtain();
                message.what = STATE_CONNECTED;
                handler.sendMessage(message);

                sendReceive = new SendReceive(socket);
                sendReceive.start();


            } catch (IOException e) {
                e.printStackTrace();
                Message message = Message.obtain();
                message.what = STATE_CONNECTION_FAILED;
                handler.sendMessage(message);
            }
        }
    }

    private class SendReceive extends Thread {

        private final BluetoothSocket bluetoothSocket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        public SendReceive(BluetoothSocket socket) {
            bluetoothSocket = socket;
            InputStream tempIn = null;
            OutputStream tempOut = null;

            try {
                tempIn = bluetoothSocket.getInputStream();
                tempOut = bluetoothSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();

            }
            inputStream = tempIn;
            outputStream = tempOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            while (true) {
                try {
                    bytes = inputStream.read(buffer);
                    handler.obtainMessage(STATE_MESSAGE_RECEIVED, bytes, -1, buffer).sendToTarget();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void write(byte[] bytes) {
            try {
                outputStream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}