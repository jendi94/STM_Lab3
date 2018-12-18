package com.example.jendi.myapplication;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;

import static java.util.UUID.fromString;

public class MainActivity extends AppCompatActivity {

    private BluetoothDevice targetDevice;
    private BluetoothAdapter bluetoothAdapter;
    private static final String UUID = "00001101-0000-1000-8000-00805F9B34FB";
    private String name = "name";
    private ConnectedThread service;
    private AcceptThread acceptThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter != null) {
              System.out.println("Nigdy tu nie wejde xD");
        }
        else if (!bluetoothAdapter.isEnabled()) {
            // Poproś użytkownika o zgodę na włączenie Bluetooth
            Intent enableBtIntent = new
                    Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }

        Spinner spinner = findViewById(R.id.spinner);
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        ArrayList<String> pairedDevicesList = new ArrayList<String>();
        for (BluetoothDevice device : pairedDevices) {
            pairedDevicesList.add(device.getName() + "\n[" +
                    device.getAddress() + "]");
        }

        ArrayAdapter<CharSequence> adapter = new ArrayAdapter(this,
                android.R.layout.simple_list_item_1, pairedDevicesList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        final Set<BluetoothDevice> finalDevices = pairedDevices;

        spinner.setOnItemSelectedListener(new
        AdapterView.OnItemSelectedListener() {
           @Override
           public void onItemSelected(AdapterView<?> arg0, View arg1,
                                      int arg2, long arg3) {
               String targetDeviceDesc =
                       arg0.getItemAtPosition(arg2).toString();
               String mac =
                       targetDeviceDesc.substring(targetDeviceDesc.indexOf('[')
                               +1, targetDeviceDesc.indexOf(']'));
               for (BluetoothDevice device : finalDevices) {
                    if(device.getAddress().equals(mac)) {
                        targetDevice = device;
                        acceptThread.cancel();
                        ConnectThread connectThread = new ConnectThread(targetDevice);
                        connectThread.start();
                    }
               }
           }
           @Override
           public void onNothingSelected(AdapterView<?> arg0) {}
        });

        acceptThread = new AcceptThread();
        acceptThread.start();
    }

    class AcceptThread extends Thread {
        private final BluetoothServerSocket bluetoothServerSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            try {
                tmp = bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(name, fromString(UUID));
            } catch (IOException e) {
                e.printStackTrace();
            }
            bluetoothServerSocket = tmp;
        }

        public void run() {
            BluetoothSocket bluetoothSocket;
            while (true) {
                try {
                    bluetoothSocket = bluetoothServerSocket.accept();
                    service = new ConnectedThread(bluetoothSocket);
                    service.start();
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }

                if (bluetoothSocket != null) {
                    try {
                        bluetoothServerSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }

        public void cancel() {
            try {
                bluetoothServerSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    class ConnectThread extends Thread {
        private final BluetoothSocket bluetoothSocket;
        private  final BluetoothDevice bluetoothDevice;

        public ConnectThread(BluetoothDevice device) {
            BluetoothSocket tmp = null;
            bluetoothDevice = device;

            try {
                tmp = bluetoothDevice.createRfcommSocketToServiceRecord(fromString(UUID));
            } catch (IOException e) {
                e.printStackTrace();
            }
            bluetoothSocket = tmp;
        }

        public void run() {
            bluetoothAdapter.cancelDiscovery();
            try {
                bluetoothSocket.connect();
                service = new ConnectedThread(bluetoothSocket);
                service.start();
            } catch (IOException e) {
                try {
                    bluetoothSocket.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }

        public void cancel() throws IOException {
            bluetoothSocket.close();
        }
    }
}

