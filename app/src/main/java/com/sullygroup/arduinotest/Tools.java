package com.sullygroup.arduinotest;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

/**
 * Created by jocelyn.caraman on 22/03/2017.
 */

public class Tools {

    public static void sendMessage(Context context, String typeEvent) {
        Intent intent = new Intent(typeEvent);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    public static void sendCommand(Context context, String command) {
        Intent intent = new Intent(TempAndHumService.EVENT_SEND_REQUEST);
        intent.putExtra("command",command);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    public static void sendMessage(Context context,String typeEvent, float value) {
        Intent intent = new Intent(typeEvent);
        intent.putExtra("message",value);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }
    public static void sendTempAndHum(Context context, float temp, float hum) {
        Intent intent = new Intent(TempAndHumService.EVENT_RESPONSE_RECEIVED);
        intent.putExtra("temp",temp);
        intent.putExtra("hum",hum);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    public static boolean checkValue(int value, int inf, int sup){
        return (inf <= value && value <= sup );
    }

    public static boolean checkValueColor(int value){
        return checkValue(value,0,255);
    }
}
