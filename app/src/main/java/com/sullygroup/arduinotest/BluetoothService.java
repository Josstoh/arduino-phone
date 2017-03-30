package com.sullygroup.arduinotest;

import android.app.Service;
import android.app.job.JobParameters;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Service gérant la connexion Bluetooth entre le téléphone et la carte Arduino. Son cycle de vie
 * est géré par {@link TempAndHumService}
 * Created by jocelyn.caraman on 15/03/2017.
 */

public class BluetoothService extends Service {
    private static final String DEVICE_ADDRESS = "98:D3:31:FC:40:F8";
    private static final UUID PORT_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

    public static final String TAG = "BluetoothService";

    private ConnectingThread mConnectingThread;
    private ConnectedThread mConnectedThread;
    private SendMessageHandler sendMessageHandler;
    private Looper looper;

    private String mCurrentFetchingOperation;
    boolean stopThread;
    private BluetoothAdapter btAdapter = null;
    private StringBuilder mStringBuilder;

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, intent.getAction());
            String command;
            switch(intent.getAction())
            {
                case TempAndHumService.EVENT_SEND_REQUEST:
                    command = intent.getStringExtra("command");
                    if(sendMessageHandler != null) {
                        Message msg = sendMessageHandler.obtainMessage(1,command);
                        sendMessageHandler.sendMessage(msg);
                    } else {
                        stopSelf();
                    }


                    break;
            }

        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter(TempAndHumService.EVENT_SEND_REQUEST));
        Log.d(TAG, "SERVICE CREATED");
        stopThread = false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        BTinit();
        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * Initialise la connexion BT : vérifie que celui-ci est bien activé tente de se connecter
     * à l'appareil.
     */
    private void BTinit() {
        btAdapter = BluetoothAdapter.getDefaultAdapter();

        if (btAdapter == null) {
            Toast.makeText(getApplicationContext(), "This device doesn't support Bluetooth", Toast.LENGTH_SHORT).show();
        }

        try {
            if(!btAdapter.isEnabled()) {
                Intent enableAdapter = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                enableAdapter.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getApplication().startActivity(enableAdapter);
                throw new Exception("BT not activated");
            }else {
                BluetoothDevice device = btAdapter.getRemoteDevice(DEVICE_ADDRESS);
                mConnectingThread = new ConnectingThread(device);
                mConnectingThread.start();
            }
        } catch (IllegalArgumentException e) {
                onCatchException(e,"ILLEGAL MAC ADDRESS, STOPPING SERVICE");
        } catch (Exception e) {
            onCatchException(e,"BT not activated");
        }


    }

    /**
     * Gère l'ouverture de la socket.
     */
    private class ConnectingThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        ConnectingThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket temp = null;
            try {
                temp = mmDevice.createRfcommSocketToServiceRecord(PORT_UUID);
            } catch (IOException e) {
                onCatchException(e,"SOCKET CREATION FAILED, STOPPING SERVICE");
            }
            mmSocket = temp;
        }

        @Override
        public void run() {
            super.run();
            try {
                mmSocket.connect();
                mConnectedThread = new ConnectedThread(mmSocket);
                mConnectedThread.start();
            } catch (Exception e) {
                try {
                    mmSocket.close();
                    onCatchException(e, "SOCKET CONNECTION FAILED, STOPPING SERVICE");
                } catch (IOException e2) {
                    Log.d(TAG, "SOCKET CLOSING FAILED, STOPPING SERVICE");
                    stopSelf();
                }
            }
        }

        void closeSocket() {
            try {
                mmSocket.close();
            } catch (IOException e2) {
                onCatchException(e2,"SOCKET CLOSING FAILED, STOPPING SERVICE");
            }
        }
    }

    /**
     * Gère l'ouverture des streams (input et output) et si c'est un succès, gère l'envoie et la
     * réception des données par BT avec la carte.
     */
    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        //creation of the connect thread
        ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                //Create I/O streams for connection
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                onCatchException(e,"UNABLE TO READ/WRITE, STOPPING SERVICE");
            }
            mmOutStream = tmpOut;
            mmInStream = tmpIn;
            HandlerThread myThread = new HandlerThread("BTHandlerThread");
            myThread.start();
            looper = myThread.getLooper();
            sendMessageHandler = new SendMessageHandler(looper);
            Log.d(TAG,"Connected to device");
            Tools.sendMessage(getApplicationContext(),TempAndHumService.EVENT_DEVICE_CONNECTED);
        }

        public void run() {
            float t,h;
            stopThread = false;
            mStringBuilder = new StringBuilder();

            //Boucle d'écoute sur l'input stream
            while(!Thread.currentThread().isInterrupted() && !stopThread) {
                try {
                    int byteCount = mmInStream.available();

                    if(byteCount > 1) {
                        byte[] rawBytes = new byte[byteCount];
                        mmInStream.read(rawBytes);
                        final String string = new String(rawBytes, "UTF-8");
                        mStringBuilder.append(string);
                        Log.d(TAG,"message reçu");
                        if(string.contains("\n")) {
                            try{
                                switch(mCurrentFetchingOperation) {
                                    case TempAndHumService.TEMPERATURE_CMD :
                                        t = Float.parseFloat(mStringBuilder.toString());
                                        Tools.sendMessage(getApplicationContext(),TempAndHumService.EVENT_RESPONSE_RECEIVED,TempAndHumService.TYPE_TEMP,t);
                                        break;
                                    case TempAndHumService.HUMIDITY_CMD:
                                        h = Float.parseFloat(mStringBuilder.toString());
                                        Tools.sendMessage(getApplicationContext(),TempAndHumService.EVENT_RESPONSE_RECEIVED,TempAndHumService.TYPE_HUM,h);
                                        break;
                                    case TempAndHumService.TEMP_AND_HUM_CMD:
                                        String[] results = mStringBuilder.toString().split(";");
                                        t = Float.parseFloat(results[0]);
                                        h = Float.parseFloat(results[1]);
                                        Tools.sendTempAndHum(getApplicationContext(),t,h);
                                        break;
                                    default:
                                        break;
                                }
                            }
                            catch (Exception e) {
                                e.printStackTrace();
                            }
                            mStringBuilder = new StringBuilder();
                            mCurrentFetchingOperation = null;
                        }
                    }

                } catch (IOException ex) {
                    stopThread = true;
                    onCatchException(ex,"erreur ?");
                }
            }
        }

        /**
         * Ecrit la commande sur la socket de sortie.
         * @param command commande à envoyer à la carte Arduino.
         */
        void write(String command) {
            try {
                byte[] d = new byte[30];
                d = command.getBytes();
                mmOutStream.write(d);
                mCurrentFetchingOperation = command;
                Log.d(TAG,"write");
            } catch (IOException e) {
                e.printStackTrace();
                //Toast.makeText(this, "Error while sending request", Toast.LENGTH_SHORT).show();
            }
        }

        public void closeStreams() {
            try {
                //Don't leave Bluetooth sockets open when leaving activity
                mmInStream.close();
                mmOutStream.close();
                looper.quit();
            } catch (IOException e) {
                onCatchException(e,"STREAM CLOSING FAILED, STOPPING SERVICE");
                stopSelf();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        Tools.sendMessage(this,TempAndHumService.EVENT_BTSERVICE_FAILED);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        stopThread = true;
        if (mConnectedThread != null) {
            mConnectedThread.closeStreams();
        }
        if (mConnectingThread != null) {
            mConnectingThread.closeSocket();
        }
    }

    void onCatchException(Exception e, String error) {
        Log.d(TAG, error);
        sendMessageError(error);
        e.printStackTrace();
    }

    void sendMessageError(String message) {
        Intent intent = new Intent(TempAndHumService.EVENT_BTSERVICE_FAILED);
        intent.putExtra("message",message);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        stopSelf();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Handler qui sert de file d'attente pour les requêtes à envoyer à l'Arduino.
     */
    private class SendMessageHandler extends Handler {

        SendMessageHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG,"handler début");
            if(msg.what == 1){
                if(mConnectedThread != null && mConnectedThread.isAlive()){
                    try {
                        String command = (String)msg.obj;
                        mConnectedThread.write(command);
                        Thread.sleep(250);
                    } catch(Exception e) {
                        onCatchException(e,"msg.obj is not a String command");
                    }
                }
                else
                    Log.d(TAG,"error");
            }
            Log.d(TAG,"handler fin");
            super.handleMessage(msg);
        }
    }

}
