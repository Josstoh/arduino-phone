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
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PersistableBundle;
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

    public static final int WHAT = 11021974;
    MyHandler myHandler;
    Looper myLooper;

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, intent.getAction());
            switch(intent.getAction())
            {
                case TempAndHumService.EVENT_RESPONSE_RECEIVED:
                    Log.d(TAG,"response received");
                    responseReceived = true;
                    break;
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
        Log.d(TAG,"onCreate");
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter(TempAndHumService.EVENT_RESPONSE_RECEIVED));


    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("LocalService", "Received start id " + startId + ": " + intent);
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG,"onDestroy");
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        myLooper.quit();
        super.onDestroy();
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        Log.d(TAG,"new job");
        HandlerThread myThread = new HandlerThread("HandlerThread");
        myThread.start();
        myLooper=myThread.getLooper();
        myHandler=new MyHandler(myLooper);
        Message msg = myHandler.obtainMessage(WHAT,params);
        myHandler.sendMessage(msg);
        responseReceived = false;
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Log.e("MyJobServiceHandler", "onDestroy called, Looper is dead");
        if(myLooper != null)
            myLooper.quit();
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
                PersistableBundle bundle = params.getExtras();
                if(bundle != null)
                {
                    String command = bundle.getString("command");
                    if(command != null && !command.isEmpty())
                    {
                        Tools.sendCommand(getApplicationContext(),command);
                        Thread.sleep(750);
                        /*if (Thread.interrupted()) {
                            throw new InterruptedException();
                        }*/
                        if(!Tools.willWaitForResponse(command))
                            jobFinished( params, false );
                        else {
                            if(!responseReceived)
                                Log.e(TAG,"Response not received in time, rescheduling...");
                            jobFinished( params, !responseReceived );
                        }
                    }
                }
                else {
                    Log.e(TAG,"Empty bundle...");
                    jobFinished( params, false );
                }

            } catch(InterruptedException e) {
                Log.d(TAG,"Thread interrupted");
            }

        }
    }

    public class MyHandler extends Handler {

        public MyHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
           Log.i("MyHandler","The work is done in a separate thread called "+Thread.currentThread().getName());
            if(msg.what==WHAT){
                try {
                    Log.d(TAG,"thread tahjs");
                    JobParameters params = (JobParameters)msg.obj;
                    PersistableBundle bundle = params.getExtras();
                    if(bundle != null)
                    {
                        String command = bundle.getString("command");
                        if(command != null && !command.isEmpty())
                        {
                            Tools.sendCommand(getApplicationContext(),command);
                            Thread.sleep(750);
                        /*if (Thread.interrupted()) {
                            throw new InterruptedException();
                        }*/
                            if(!Tools.willWaitForResponse(command))
                                jobFinished( params, false );
                            else {
                                if(!responseReceived)
                                    Log.e(TAG,"Response not received in time, rescheduling...");
                                jobFinished( params, !responseReceived );
                            }
                        }
                    }
                    else {
                        Log.e(TAG,"Empty bundle...");
                        jobFinished( params, false );
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                jobFinished((JobParameters) msg.obj,false);
            }
            super.handleMessage(msg);
        }
    }
}
