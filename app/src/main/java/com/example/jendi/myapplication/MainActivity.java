package com.example.jendi.myapplication;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
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
    public String textToWrite, textToRead, textToSend;
    public ListView listView;
    public ArrayList<String> values;
    private ArrayAdapter<String> textAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter != null) {
            //do nothing
        }
        else if (!bluetoothAdapter.isEnabled()) {
            // Poproś użytkownika o zgodę na włączenie Bluetooth
            Intent enableBtIntent = new
                    Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }

        final Spinner spinner = findViewById(R.id.spinner);
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

        Button chat = findViewById(R.id.buttonChat);
        chat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String targetDeviceDesc =
                        spinner.getItemAtPosition(0).toString();
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
        });

        Button button = findViewById(R.id.buttonSend);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                service.write();
            }
        });

        listView = findViewById(R.id.list);
        values = new ArrayList<>();
        textAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, android.R.id.text1, values);
        listView.setAdapter(textAdapter);

        acceptThread = new AcceptThread();
        acceptThread.start();
    }

    class AcceptThread extends Thread {
        private final BluetoothServerSocket bluetoothServerSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            try {
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(name, fromString(UUID));
            } catch (IOException e) {
                e.printStackTrace();
            }
            bluetoothServerSocket = tmp;
        }

        public void run() {
            BluetoothSocket bluetoothSocket = null;
            while (true) {
                try {
                    bluetoothSocket = bluetoothServerSocket.accept();
                    service = new ConnectedThread(bluetoothSocket);
                    service.start();
                } catch (IOException e) {
                    e.printStackTrace();
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

    public class ConnectedThread extends Thread {
        private static final String TAG = "";
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private byte[] mmBuffer; // mmBuffer store for the stream

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams; using temp objects because
            // member streams are final.
            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating input stream", e);
            }
            try {
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating output stream", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            mmBuffer = new byte[1024];
            // Keep listening to the InputStream until an exception occurs.
            while (true) {
                try {
                    // Read from the InputStream.
                    ObjectInputStream objectInputStream = new ObjectInputStream(mmInStream);
                    try {
                        textToRead = (String) objectInputStream.readObject();
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                    MainActivity.this.runOnUiThread(new Runnable() {
                        public void run() {
                            values.add("Them: "+textToRead);
                            textAdapter.notifyDataSetChanged();
                        }
                    });
                    // Send the obtained bytes to the UI activity.
                } catch (IOException e) {
                    Log.d(TAG, "Input stream was disconnected", e);
                    break;
                }
            }
        }

        // Call this from the main activity to send data to the remote device.
        public void write() {
            try {
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(mmOutStream);
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        EditText editText = findViewById(R.id.inputText);
                        textToWrite = "Me: " + editText.getText().toString();
                        textToSend = editText.getText().toString();
                        editText.setText("");
                        values.add(textToWrite);
                        textAdapter.notifyDataSetChanged();
                    }
                });
                objectOutputStream.writeObject(textToSend);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Call this method from the main activity to shut down the connection.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    }
}

