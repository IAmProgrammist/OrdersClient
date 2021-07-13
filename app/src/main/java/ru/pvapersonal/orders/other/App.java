package ru.pvapersonal.orders.other;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Intent;

import ru.pvapersonal.orders.service.LookForUpdatesSingle;
import ru.pvapersonal.orders.service.MainBroadcastReceiver;

public class App extends Application {

    public static long INTERVAL = 5*60*1000;
    public static String URL = "/*Enter your IP-Address here*/";

    @Override
    public void onCreate() {
        super.onCreate();
        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent intent = new Intent(this, MainBroadcastReceiver.class);
        intent.setAction(MainBroadcastReceiver.ALARM_MANAGER_SEARCH_FOR_UPDATES);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
        am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), INTERVAL,pendingIntent);
        Notifier.registerChannels(this);
    }
}
