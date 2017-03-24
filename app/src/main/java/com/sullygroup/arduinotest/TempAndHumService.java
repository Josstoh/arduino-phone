package com.sullygroup.arduinotest;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.Icon;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.nio.charset.StandardCharsets;

/**
 * Created by jocelyn.caraman on 16/03/2017.
 */

public class TempAndHumService extends Service implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        DataApi.DataListener, MessageApi.MessageListener{
    public final static String TAG = "TempAndHumService";

    public static final String EVENT_RESPONSE_RECEIVED = "event_response_received";
    public static final String EVENT_DEVICE_CONNECTED = "event_device_connected";
    public static final String EVENT_BTSERVICE_FAILED = "event_btservice_failed";
    public static final String EVENT_SEND_REQUEST = "event_send_request";

    public static  final int TYPE_TEMP = 0;
    public static  final int TYPE_HUM = 1;
    public static  final int TYPE_ROT = 2;

    public static final String PATH_TEMP = "/stats/temp";
    public static final String PATH_HUM = "/stats/hum";
    public static final String PATH_ROT = "/rotate";

    public static final String CONNECT_TO_ARDUINO_MESSAGE_PATH = "/connect_to_arduino";

    public static  final int TEMP_AND_HUM_JOB_ID = 0;
    public static  final int ROTATE_JOB_ID = 0;
    public static  final int LED_COLOR_JOB_ID = 0;

    private NotificationManager mNM;
    private int idNotification = 2;

    private JobScheduler mJobScheduler;
    GoogleApiClient mGoogleApiClient;

    private boolean isAutoUpdating = false;
    private final IBinder mBinder = new LocalBinder();
    private TempAndHumServiceListener activity;
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, intent.getAction());
            float t,h;
            switch(intent.getAction())
            {
                case EVENT_DEVICE_CONNECTED:
                    isConnectedToDevice = true;
                    if(activity != null)
                        activity.onConnected();
                    break;
                case EVENT_RESPONSE_RECEIVED:
                    t = intent.getFloatExtra("temp",-100f);
                    if(t != -100){
                        temp = (int) t;
                        if(activity != null)
                            activity.onTempResponse(t);
                        updateStat(TYPE_TEMP, temp);
                        showNotification();
                    }
                    h = intent.getFloatExtra("hum",-100f);
                    if(h != -100){
                        hum = (int) h;
                        if(activity != null)
                            activity.onHumResponse(h);
                        updateStat(TYPE_HUM, hum);
                        showNotification();
                    }
                    break;
                case EVENT_BTSERVICE_FAILED:
                    Log.d(TAG,"BTService failed... Stopping service");
                    Toast.makeText(getApplicationContext(),intent.getStringExtra("message"),Toast.LENGTH_LONG).show();
                    stop();
                    break;
            }

        }
    };
    private Intent btServiceIntent;
    private boolean isConnectedToDevice = false;
    private int temp = -1;
    private int hum = -1;
    private int rotate = -1;

    private static boolean isRunning = false;

    @Override
    public void onCreate() {
        super.onCreate();

        isRunning = true;
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();
        mGoogleApiClient.connect();
        mJobScheduler = (JobScheduler) getSystemService( Context.JOB_SCHEDULER_SERVICE );
        btServiceIntent = new Intent(TempAndHumService.this, BluetoothService.class);
        startService(btServiceIntent);
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter(EVENT_RESPONSE_RECEIVED));
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter(EVENT_DEVICE_CONNECTED));
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter(EVENT_BTSERVICE_FAILED));

        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        Log.d(TAG, "SERVICE CREATED");
        showNotification();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "startcommand");
        String action = intent.getAction();
        if(action != null)
        {
            if(action.equals("STOP_SERVICE")){
                Log.d(TAG,"stop");
                stop();
                return START_NOT_STICKY;
            }
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG, "onConnected: " + bundle);
        startForeground(idNotification,getNotification());
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Connected to Google Api Service");
        }
        Wearable.DataApi.addListener(mGoogleApiClient, this);
        Wearable.MessageApi.addListener(mGoogleApiClient,this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "onConnectionSuspended: " + i);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed: " + connectionResult);
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {
        for (DataEvent event : dataEventBuffer) {
            if (event.getType() == DataEvent.TYPE_DELETED) {
                Log.d(TAG, "DataItem deleted: " + event.getDataItem().getUri());
            } else if (event.getType() == DataEvent.TYPE_CHANGED) {
                DataItem item = event.getDataItem();
                Log.d(TAG, "DataItem changed: " + event.getDataItem().getUri());
                if(item.getUri().getPath().compareTo("/rotate") == 0) {
                    DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                    activity.updateRotate(dataMap.getInt("value"));
                }
            }
        }
        dataEventBuffer.release();
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.d(TAG,"messageapi reçu");
        if (messageEvent.getPath().equals(CONNECT_TO_ARDUINO_MESSAGE_PATH)) {
            Log.d(TAG,"messageapi type " + CONNECT_TO_ARDUINO_MESSAGE_PATH);
            try {
                String command = new String(messageEvent.getData(), StandardCharsets.UTF_8);
                int angle = Integer.valueOf(command.substring(1));
                requestRotate(angle);
            }
            catch(Exception e) {
                Log.e(TAG,"A String Object is requested.");
                e.printStackTrace();
            }
        }
    }

    class LocalBinder extends Binder {
        TempAndHumService getServiceInstance(){
            return TempAndHumService.this;
        }
    }

    public void registerClient(Activity activity){
        Log.d(TAG,"registerClient");
        this.activity = (TempAndHumServiceListener)activity;
    }

    public void unRegisterClient(){
        Log.d(TAG,"unRegisterClient");
        this.activity = null;
    }

    public void setAutoUpdate(boolean b) {
        if(b && !isAutoUpdating) {
            JobInfo.Builder builder = new JobInfo.Builder( TEMP_AND_HUM_JOB_ID,
                    new ComponentName( getPackageName(),
                            TempAndHumJobScheduler.class.getName() ) );
            PersistableBundle bundle = new PersistableBundle(1);
            bundle.putString("command",DetailActivity.TEMP_AND_HUM_CMD);
            builder.setExtras(bundle);
            builder.setPeriodic(5000);
            if( mJobScheduler.schedule( builder.build() ) <= 0 ) {
                //If something goes wrong
                Log.d(TAG,"JobScheduler : Something went wrong...");
            }
            isAutoUpdating = true;
        }
        else if(!b && isAutoUpdating) {
            mJobScheduler.cancelAll();
            isAutoUpdating = false;
        }
    }

    private void showNotification() {
        mNM.notify(idNotification, getNotification());
    }

    private Notification getNotification() {
        String txTemp;
        txTemp = "Temp.:";
        txTemp += (temp==-1) ? "N/A ": temp+"C°\n";
        txTemp += "Hum.:";
        txTemp += (hum==-1) ? "N/A ": hum+"%\n";
        txTemp += "Rot.:";
        txTemp += (rotate==-1) ? "N/A ": rotate+"°\n";

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, DetailActivity.class), 0);
        Intent stop = new Intent(this, TempAndHumService.class);
        stop.setAction("STOP_SERVICE");
        PendingIntent stopIntent = PendingIntent.getService(this, 0, stop , 0);

        return new Notification.Builder(this)
                .setSmallIcon(R.drawable.ic_bluetooth_connected_black_36dp)  // the status icon
                //.setLargeIcon(Icon.createWithResource(this,R.drawable.ic_wb_sunny_black_36dp))
                .setTicker(txTemp)  // the status text
                .setWhen(System.currentTimeMillis())  // the time stamp
                .setContentTitle("Temperature and Humidity")  // the label of the entry
                .setContentText(txTemp)  // the contents of the entry
                .setContentIntent(contentIntent)  // The intent to send when the entry is clicked
                .addAction(new Notification.Action.Builder(R.drawable.ic_close_black_36dp,"Stop",stopIntent).build())
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_HIGH)
                .setColor(Color.BLACK)
                .build();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG,"onDestroy");
        super.onDestroy();
        if (null != mGoogleApiClient && mGoogleApiClient.isConnected()) {
            Wearable.DataApi.removeListener(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
        mJobScheduler.cancelAll();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        isRunning = false;
        stopService(btServiceIntent);
    }

    public boolean isConnectedToDevice()
    {
        return isConnectedToDevice;
    }

    private void updateStat(int typeValue, int value) {
        String path="";
        switch (typeValue){
            case TYPE_TEMP:
                path = PATH_TEMP;
                break;
            case TYPE_HUM:
                path = PATH_HUM;
                break;
            case TYPE_ROT:
                path = PATH_ROT;
                break;
            default:
                Log.e(TAG,"Wrong type...");
        }
        PutDataMapRequest putDataMapReq = PutDataMapRequest.create(path);
        putDataMapReq.getDataMap().putInt("value", value);
        PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
        PendingResult<DataApi.DataItemResult> pendingResult =
                Wearable.DataApi.putDataItem(mGoogleApiClient, putDataReq);
        pendingResult.setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
            @Override
            public void onResult(final DataApi.DataItemResult result) {
                if(result.getStatus().isSuccess()) {
                    Log.d(TAG, "Data item set: " + result.getDataItem().getUri());
                }
            }
        });
    }

    private void stop() {
        if(activity != null)
            activity.onServiceClosing();
        mJobScheduler.cancelAll();
        //stopForeground(true);
        stopSelf();
    }

    public static boolean isRunning() {
        return isRunning;
    }

    public boolean requestRotate(int angle) {
        try{
            if(Tools.checkValue(angle,0,145)) {
                //Tools.sendCommand(this,DetailActivity.ROTATE_CMD + angle);
                JobInfo.Builder builder = new JobInfo.Builder( ROTATE_JOB_ID,
                        new ComponentName( getPackageName(),
                                TempAndHumJobScheduler.class.getName() ) );
                PersistableBundle bundle = new PersistableBundle(1);
                bundle.putString("command",DetailActivity.ROTATE_CMD + angle);
                builder.setExtras(bundle);
                builder.setOverrideDeadline(2000);
                if( mJobScheduler.schedule( builder.build() ) <= 0 ) {
                    Log.d(TAG,"JobScheduler : Something went wrong...");
                }
                updateStat(TYPE_ROT,angle);
                return true;
            }
            else
            {
                Toast.makeText(getApplicationContext(),"L'angle doit être compris entre 0 et 145° inclus",Toast.LENGTH_LONG).show();
                return false;
            }

        }catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void requestLEDColor(int red, int green, int blue) {
        try{
            if(Tools.checkValueColor(red) && Tools.checkValueColor(green) && Tools.checkValueColor(blue)) {
                JobInfo.Builder builder = new JobInfo.Builder( LED_COLOR_JOB_ID,
                        new ComponentName( getPackageName(),
                                TempAndHumJobScheduler.class.getName() ) );
                PersistableBundle bundle = new PersistableBundle(1);
                bundle.putString("command",DetailActivity.COLOR_CMD + red + ";" + green + ";" + blue);
                builder.setExtras(bundle);
                builder.setOverrideDeadline(50);
                if( mJobScheduler.schedule( builder.build() ) <= 0 ) {
                    Log.d(TAG,"JobScheduler : Something went wrong...");
                }
            }
            else
                Toast.makeText(getBaseContext(),"Each field must be a number between 0 and 255.",Toast.LENGTH_LONG).show();
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    interface TempAndHumServiceListener{
        void onConnected();
        void onTempResponse(float msg);
        void onHumResponse(float msg);
        void updateRotate(int angle);
        void onServiceClosing();
    }
}
