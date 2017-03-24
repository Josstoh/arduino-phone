package com.sullygroup.arduinotest;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by jocelyn.caraman on 15/03/2017.
 */

public class TempAndHumJobScheduler extends JobService {
    String TAG = "TempAndHumJobSheduler";
    private NotificationManager mNM;
    private final IBinder mBinder = new LocalBinder();
    private GetTempAndHumThread tempAndHumThread;
    protected boolean responseReceived = false;

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, intent.getAction());
            Float f;
            switch(intent.getAction())
            {
                case TempAndHumService.EVENT_RESPONSE_RECEIVED:
            }

        }
    };

    public class LocalBinder extends Binder {
        TempAndHumJobScheduler getServiceInstance() {
            return TempAndHumJobScheduler.this;
        }
    }

    @Override
    public void onCreate() {
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter(TempAndHumService.EVENT_RESPONSE_RECEIVED));

        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("LocalService", "Received start id " + startId + ": " + intent);
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG,"onDestroy");
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        tempAndHumThread = new GetTempAndHumThread(params);
        tempAndHumThread.start();
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        if(tempAndHumThread != null)
            tempAndHumThread.interrupt();
        return false;
    }

    private class GetTempAndHumThread extends Thread {
        JobParameters params;
        GetTempAndHumThread(JobParameters mParams){
            params = mParams;
        }

        @Override
        public void run() {
            super.run();
            try {
                Log.d(TAG,"thread tahjs");
                Tools.sendCommand(getApplicationContext(),DetailActivity.TEMP_AND_HUM_CMD);
                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }
                jobFinished( params, false );
            } catch(InterruptedException e) {
                Log.d(TAG,"Thread interrupted");
            }

        }
    }
}
