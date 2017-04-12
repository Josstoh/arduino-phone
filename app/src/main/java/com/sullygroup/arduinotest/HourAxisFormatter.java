package com.sullygroup.arduinotest;

import android.util.Log;
import android.util.SparseArray;

import com.github.mikephil.charting.charts.BarLineChartBase;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Classe qui permet la personnalisation de l'axe X sur le graphique en affichant l'heure des relevés
 * Created by jocelyn.caraman on 10/04/2017.
 */

public class HourAxisFormatter implements IAxisValueFormatter
{

    protected String[] mMonths = new String[]{
            "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    };

    private SparseArray<String> labels;
    private BarLineChartBase<?> chart;
    private DetailActivity activity;

    public HourAxisFormatter(BarLineChartBase<?> chart,DetailActivity mActivity) {
        this.chart = chart;
        activity = mActivity;
        labels = new SparseArray<>();
    }

    @Override
    public String getFormattedValue(float value, AxisBase axis) {
        String hour = getHourFromKey((int)value);
        if(hour.equals("")) {
            return hour;
        } else {
            int pred = (int)value - 1;
            if(pred < 0) return  hour;
            else {
                String hourPred = getHourFromKey(pred);
                if(hour.equals(hourPred)) {
                    return  "";
                } else {
                    return hour;
                }
            }
        }
    }

    String getHourFromKey(int key) {
        long time = activity.getTime(key);
        if(time < 0 ){
            Log.d("hour","error");
            return "";
        } else {
            Date date = new Date(time);
            DateFormat formatter;
            formatter = new SimpleDateFormat("HH:mm", Locale.FRANCE);
            String heure = formatter.format(date);
            return heure;
        }
    }


}
