package com.example.andrea.chattestuale;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private static final int ENABLE_BT = 1;
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String SUPSI_DEVICE_NAME = "SUPSI-BLUETOOTH-PHONE";
    private static final UUID MY_UUID = UUID.fromString("60b8abf0-9910-11e5-a837-0800200c9a66");

    private Button disconnectButton;
    private Button scanButton;
    private TextView status;
    private EditText textChat;
    private Button sendButton;
    private BluetoothAdapter btAdapter;
    private BluetoothSocket btSocket;
    private ProgressDialog dialog;
    private BroadcastReceiver scanBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(BluetoothDevice.ACTION_FOUND)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String name = device.getName();
                if (name.equals(SUPSI_DEVICE_NAME)) { //se trovo quello che mi serve interrompi la ricerca
                    dialog.setMessage(name);
                    btAdapter.cancelDiscovery();
                    status.setText(SUPSI_DEVICE_NAME + " trovato!");

                    ServerConnection connection = new ServerConnection();
                    connection.execute(device);

                }
                Log.d(TAG, "Dispositivo: " + device.getName() + " - " + device.getAddress());
            } else if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_STARTED)) {
                dialog.setMessage("Searching device...");
                dialog.show();
            } else if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
                dialog.dismiss();
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //UI initialize
        scanButton = (Button) findViewById(R.id.button_scan);
        status = (TextView) findViewById(R.id.textView_status);
        textChat = (EditText) findViewById(R.id.editText_chat);
        sendButton = (Button) findViewById(R.id.button_send);
        disconnectButton = (Button)  findViewById(R.id.button_disconnect);

        //Bluetooth adapter initialize
        btAdapter = BluetoothAdapter.getDefaultAdapter();


        //Dialog inizialize
        dialog = new ProgressDialog(this);



        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!btAdapter.isEnabled()) { //se il bluetooth non è abilitato, abilita il bluetooth
                    Intent enableIntent = new Intent (BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableIntent, ENABLE_BT);
                } else { //se abilitato parte con la discovery
                    btAdapter.startDiscovery();
                }
            }
        });

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String messageToSend = textChat.getText().toString();
                ConnectionThread myThread = new ConnectionThread(messageToSend);
                myThread.start();
            }
        });

        disconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    btSocket.close();
                    status.setText("Disconnesso");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        //registro il receiver dello scan
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(scanBroadcastReceiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // fermo lo scan receiver
        unregisterReceiver(scanBroadcastReceiver);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                // l'utente ha accettato l'attivazione bluetooth, quindi discovery
                btAdapter.startDiscovery();
            }
        }

    }

    private class ServerConnection extends AsyncTask<BluetoothDevice,Void,Boolean> {


        @Override
        protected Boolean doInBackground(BluetoothDevice... params) {
            try {
                //Creto bluetooth socket
                btSocket = params[0].createInsecureRfcommSocketToServiceRecord(MY_UUID);
                btSocket.connect();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            //super.onPostExecute(aBoolean);
            if (aBoolean) {
                status.setText("Connesso a " + SUPSI_DEVICE_NAME);
            } else {
                status.setText("impossibile collegarsi a " + SUPSI_DEVICE_NAME);
            }
        }
    }

    private class ConnectionThread extends Thread {
        private InputStream is;
        private OutputStream os;
        private String message;

        public ConnectionThread (String msg) {

            try {
                is = btSocket.getInputStream();
                os = btSocket.getOutputStream();
                message = msg;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                os.write(message.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
