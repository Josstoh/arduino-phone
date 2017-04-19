package com.sullygroup.arduinotest;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements TempAndHumService.TempAndHumServiceListener{
    private static final String TAG = "MainActivity";

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            Toast.makeText(MainActivity.this, "onServiceConnected called "+ className.getShortClassName(), Toast.LENGTH_SHORT).show();
            switch (className.getShortClassName())
            {
                case "."+TempAndHumService.TAG:
                    TempAndHumService.LocalBinder binder2 = (TempAndHumService.LocalBinder) service;
                    tahService = binder2.getServiceInstance();
                    tahService.registerClient(MainActivity.this);
                    if(tahService.isConnectedToDevice()) {
                        onConnected();
                    }
                    SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
                    boolean isAutoUpdating = sharedPref.getBoolean(getString(R.string.preference_auto_update),false);
                    tahService.setAutoUpdate(isAutoUpdating);
                    autoUpdateButton.setChecked(isAutoUpdating);
                    break;
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            Toast.makeText(MainActivity.this, "onServiceDisconnected called", Toast.LENGTH_SHORT).show();

            switch (className.getShortClassName())
            {
                case "."+TempAndHumService.TAG:
                    tahService.unRegisterClient();
                    tahService = null;
                    break;
            }
        }
    };

    TempAndHumService tahService;
    private Intent tahServiceIntent;

    private SparseArray<Integer> mTemperatureArray;
    private SparseArray<Integer> mHumidityArray;
    private SparseArray<Long> correspondanceHeure;
    private int timeID = -1;
    private LineChart mChart;
    private Button mConnectButton;
    private Button mDisconnectButton;
    private ToggleButton autoUpdateButton;
    private Button requestTempHumButton;
    private Button mClearButton;
    private Button mRotateButton;
    private EditText mAngleRotate;
    private SeekBarHint mSeekbar;
    public TextView hintSeekBar;
    private EditText etRed;
    private EditText etGreen;
    private EditText etBlue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG,"onCreate " + this.toString());
        setContentView(R.layout.activity_main);
        tahServiceIntent = new Intent(MainActivity.this, TempAndHumService.class);

        mConnectButton = (Button) findViewById(R.id.activity_detail_connect_button);
        mDisconnectButton = (Button) findViewById(R.id.activity_detail_disconnect_button);
        autoUpdateButton = (ToggleButton) findViewById(R.id.activity_main_auto_update_button);
        requestTempHumButton = (Button) findViewById(R.id.activity_main_request_temp_hum_button);
        mClearButton = (Button) findViewById(R.id.activity_main_clear_button);
        mRotateButton = (Button) findViewById(R.id.activity_main_rotate_button);
        mAngleRotate = (EditText) findViewById(R.id.et_rotate);
        etRed = (EditText) findViewById(R.id.et_red);
        etGreen = (EditText) findViewById(R.id.et_green);
        etBlue = (EditText) findViewById(R.id.et_blue);
        mChart = (LineChart) findViewById(R.id.activity_detail_chart);
        mSeekbar = (SeekBarHint) findViewById(R.id.seekBar);

        mConnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startService(tahServiceIntent); //Starting the service
                bindService(tahServiceIntent, mConnection,Context.BIND_AUTO_CREATE);
            }
        });

        mDisconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                unbindService(mConnection);
                stopService(tahServiceIntent);
                setUiEnabled(false);
            }
        });
        autoUpdateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(autoUpdateButton.isChecked()) {
                    tahService.setAutoUpdate(true);
                }
                else {
                    tahService.setAutoUpdate(false);
                }

            }
        });
        requestTempHumButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tahService.requestTempAndHum();
            }
        });
        mClearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearChart();
            }
        });
        mRotateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String strAngle = mAngleRotate.getText().toString();

                if(!strAngle.isEmpty()) {
                    try{
                        int angle = Integer.valueOf(strAngle);
                        if(tahService.requestRotate(angle)){
                            mSeekbar.setProgress(angle);
                        }
                    } catch (Exception e) {
                        Toast.makeText(getBaseContext(),"This is not a number",Toast.LENGTH_LONG).show();
                    }
                }
            }
        });

        mSeekbar.addOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            private int progress;
            @Override
            public void onProgressChanged(SeekBar seekBar, int mProgress, boolean b) {
                progress = mProgress;
                mAngleRotate.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                tahService.requestRotate(progress);
            }
        });

        setUiEnabled(false);
        mTemperatureArray = new SparseArray<>();
        mHumidityArray = new SparseArray<>();
        correspondanceHeure = new SparseArray<>();

        initChart();
        clearChart();

        hintSeekBar = new TextView(this);
        hintSeekBar.setVisibility(View.GONE);
        hintSeekBar.setBackgroundColor(Color.GRAY);
        hintSeekBar.setTextSize(TypedValue.COMPLEX_UNIT_SP,18);
        RelativeLayout relativeLayout = (RelativeLayout) findViewById(R.id.root);
        relativeLayout.addView(hintSeekBar);

        Button btSetColor = (Button) findViewById(R.id.bt_set_color);
        btSetColor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String strRed = etRed.getText().toString();
                String strGreen = etGreen.getText().toString();
                String strBlue = etBlue.getText().toString();

                if(!strRed.isEmpty() && !strGreen.isEmpty() && !strBlue.isEmpty())
                {
                    try{
                        int red = Integer.valueOf(strRed);
                        int green = Integer.valueOf(strGreen);
                        int blue = Integer.valueOf(strBlue);
                        tahService.requestLEDColor(red,green,blue);
                    }catch(Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        if(TempAndHumService.isRunning()){
            bindService(tahServiceIntent, mConnection,Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(getString(R.string.preference_auto_update), autoUpdateButton.isChecked());
        editor.apply();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(tahService != null)
            tahService.unRegisterClient();
        unbindService(mConnection);
    }

    /**
     * Active/désactive l'IU.
     * @param bool
     */
    private void setUiEnabled(final boolean bool) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConnectButton.setEnabled(!bool);
                mDisconnectButton.setEnabled(bool);
                autoUpdateButton.setEnabled(bool);
                requestTempHumButton.setEnabled(bool);
                mClearButton.setEnabled(bool);
                mRotateButton.setEnabled(bool);
                mAngleRotate.setEnabled(bool);
                mSeekbar.setEnabled(bool);
            }
        });
    }

    private void initChart() {
        mChart.setDrawGridBackground(true);
        // no description text
        mChart.getDescription().setEnabled(false);
        // enable touch gestures
        mChart.setTouchEnabled(true);
        // enable scaling and dragging
        mChart.setDragEnabled(true);
        //mChart.setScaleEnabled(true);
        mChart.setScaleXEnabled(true);
        mChart.setScaleYEnabled(false);
        mChart.getXAxis().setGranularity(1f);
        mChart.getXAxis().setValueFormatter(new HourAxisFormatter(mChart,this));
    }

    private void refreshChart() {
        ArrayList<Entry> temperatureDataset = new ArrayList<>();
        if(mTemperatureArray.size() > 0) {
            for (int i = 0; i < mTemperatureArray.size(); i++) {
                int key = mTemperatureArray.keyAt(i);
                temperatureDataset.add(new Entry(key, mTemperatureArray.get(key)));
            }
        }

        ArrayList<Entry> humidityDataset = new ArrayList<>();
        if(mHumidityArray.size() > 0) {
            for (int i = 0; i < mHumidityArray.size(); i++) {
                int key = mHumidityArray.keyAt(i);
                humidityDataset.add(new Entry(key, mHumidityArray.get(key)));
            }
        }

        int maxSize = (mTemperatureArray.size() > mHumidityArray.size() ? mTemperatureArray.size() : mHumidityArray.size());
        String[] xAxis = new String[maxSize];
        for(int i = 0 ; i < maxSize ; i++) {
            xAxis[i] = "" + i;
        }

        final ArrayList<ILineDataSet> lines = new ArrayList<ILineDataSet> ();

        LineDataSet temperatureLineDataSet = null;
        if(!temperatureDataset.isEmpty()) {
            temperatureLineDataSet = new LineDataSet(temperatureDataset, "Temperature");
            temperatureLineDataSet.setColor(getResources().getColor(R.color.Red));
            temperatureLineDataSet.setCircleColor(getResources().getColor(R.color.Red));
        }

        LineDataSet humidityLineDataSet = null;
        if(!humidityDataset.isEmpty()) {
            humidityLineDataSet = new LineDataSet(humidityDataset, "Humidity");
            humidityLineDataSet.setColor(getResources().getColor(R.color.colorAccent));
            humidityLineDataSet.setCircleColor(getResources().getColor(R.color.colorAccent));
        }

        if(temperatureLineDataSet != null)
            lines.add(temperatureLineDataSet);

        if(humidityLineDataSet != null)
            lines.add(humidityLineDataSet);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(!lines.isEmpty()) {
                    mChart.setData(new LineData(lines));
                    mChart.invalidate();
                }
            }
        });

    }

    private void clearChart() {
        mTemperatureArray.clear();
        mHumidityArray.clear();

        mChart.setData(new LineData());
        mChart.invalidate();
    }

    @Override
    public void onConnected() {
        Log.d(TAG,"onConnected");
        setUiEnabled(true);
    }

    @Override
    public void onTempResponse(float msg) {
        int temp = (int) msg;
        mTemperatureArray.put(timeID,temp);
        refreshChart();
    }

    @Override
    public void onHumResponse(float msg) {
        int hum = (int) msg;
        mHumidityArray.put(timeID,hum);
        refreshChart();
    }

    @Override
    public void updateRotate(int angle) {
        mSeekbar.setProgress(angle);
    }

    @Override
    public void onServiceClosing() {
        tahService.unRegisterClient();
        tahService = null;
        unbindService(mConnection);
        setUiEnabled(false);
    }

    @Override
    public void updateTime() {
        correspondanceHeure.put(++timeID,System.currentTimeMillis());
    }

    /**
     * Récupère l'heure en ms associé à la clé key.
     * @param key clé dans le tableau associé à l'heure
     * @return l'heure associé à la clé ou -1 s'il n'y en a pas
     */
    long getTime(int key){
        Long value = correspondanceHeure.get(key);
        return value == null ? -1 : value;
    }

}
