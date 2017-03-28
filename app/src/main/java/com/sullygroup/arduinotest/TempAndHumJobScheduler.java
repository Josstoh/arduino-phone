package com.sullygroup.arduinotest;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.os.PersistableBundle;
import android.util.Log;

/**
 * Classe dérivant de JobService permettant de gérer les requêtes vers la carte Arduino avec des
 * conditions particulières (e.g. une requête qui s'excute toutes les 2min).
 * Created by jocelyn.caraman on 15/03/2017.
 */

public class TempAndHumJobScheduler extends JobService {
    public final String TAG = "TempAndHumJobSheduler";
    protected boolean responseReceived = false;

    @Override
    public void onCreate() {
        Log.d(TAG,"onCreate");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("LocalService", "Received start id " + startId + ": " + intent);
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG,"onDestroy");
        super.onDestroy();
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        Log.d(TAG,"new job");
        PersistableBundle bundle = params.getExtras();
        if(bundle != null)
        {
            String command = bundle.getString("command");
            if(command != null && !command.isEmpty()) {
                // On envoie la commande au BluetoothService
                Tools.sendCommand(getApplicationContext(),command);
            }
        }
        else {
            Log.e(TAG,"Empty bundle...");
        }
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Log.e("MyJobServiceHandler", "onDestroy called, Looper is dead");
        return false;
    }
}
