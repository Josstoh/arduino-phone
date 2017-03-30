package com.sullygroup.arduinotest;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

/**
 * Classe regroupant différentes méthodes utilisées par plusieurs classes.
 * Created by jocelyn.caraman on 22/03/2017.
 */

class Tools {

    /**
     * Envoie un message par l'intermédiaire du LocalBroadcastManager à toutes les instances qui
     * s'y sont inscrites.
     * @param context Context de l'application
     * @param typeEvent Type d'évènement à envoyer {@see TempAndHumService}
     */
    static void sendMessage(Context context, String typeEvent) {
        Intent intent = new Intent(typeEvent);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    /**
     * Envoie un message de type EVENT_SEND_REQUEST par l'intermédiaire du LocalBroadcastManager
     * à toutes les instances qui s'y sont inscrites.
     * @param context Context de l'application
     * @param command La commande à envoyer pour être exécuter sur la carte Arduino
     */
    static void sendCommand(Context context, String command) {
        Intent intent = new Intent(TempAndHumService.EVENT_SEND_REQUEST);
        intent.putExtra("command",command);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    /**
     * Envoie un message par l'intermédiaire du LocalBroadcastManager à toutes les instances qui s'y sont inscrites.
     * @param context Context de l'application
     * @param typeEvent Type d'évènement à envoyer {@see TempAndHumService}
     * @param typeValue Type de valeur {@link TempAndHumService}
     * @param value valeur a envoyé
     */
    public static void sendMessage(Context context,String typeEvent, int typeValue, float value) {
        Intent intent = new Intent(typeEvent);
        intent.putExtra("message",value);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    /**
     * Envoie un message de type EVENT_RESPONSE_RECEIVED contenant la température et l'humidité par l'intermédiaire du LocalBroadcastManager à toutes les instances qui s'y sont inscrites.
     * @param context Context de l'application.
     * @param temp La température en float.
     * @param hum L'humidité en float.
     */
    static void sendTempAndHum(Context context, float temp, float hum) {
        Intent intent = new Intent(TempAndHumService.EVENT_RESPONSE_RECEIVED);
        intent.putExtra("temp",temp);
        intent.putExtra("hum",hum);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    /**
     * Vérifie qu'une valeur est comprise entre des bornes données.
     * @param value Valeur à vérifier.
     * @param inf Borne inférieur.
     * @param sup Borne suppérieur.
     * @return true si value est compris entre inf et sup, false sinon;
     */
    static boolean checkValue(int value, int inf, int sup){
        return (inf <= value && value <= sup );
    }

    /**
     * Vérifie que value est valide pour un champ couleur RGB
     * @param value Valeur à vérifier.
     * @return true si value est compris entre 0 et 255, false sinon;
     */
    static boolean checkValueColor(int value){
        return checkValue(value,0,255);
    }

    static boolean willWaitForResponse(String command) {
        return command.startsWith(TempAndHumService.HUMIDITY_CMD) && command.startsWith(TempAndHumService.TEMPERATURE_CMD) && command.startsWith(TempAndHumService.TEMP_AND_HUM_CMD);
    }
}
