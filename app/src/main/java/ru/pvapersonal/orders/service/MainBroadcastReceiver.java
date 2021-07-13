package ru.pvapersonal.orders.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import ru.pvapersonal.orders.R;
import ru.pvapersonal.orders.other.App;
import ru.pvapersonal.orders.other.Notifier;
import ru.pvapersonal.orders.service.listeners.MainRoomListener;

import static ru.pvapersonal.orders.other.App.URL;

import static android.content.Context.ALARM_SERVICE;

public class MainBroadcastReceiver extends BroadcastReceiver {

    public static final String ACTION_SET_UpdateService = "ru.pvapersonal.orders.action.action_alarm";
    public static final String RESTART_SERVICE = "ru.pvapersonal.orders.action.restart_service";
    public static final String ALARM_MANAGER_SEARCH_FOR_UPDATES = "ru.pvapersonal.orders.action.alarm_manager_search_for_updates";
    public static final String BOOT_RECEIVER = "android.intent.action.BOOT_COMPLETED";
    public static final String QUICK_BOOT_RECEIVER = "android.intent.action.QUICKBOOT_POWERON";
    public static final String NET_CHANGE = "android.net.conn.CONNECTIVITY_CHANGE";
    private static final String KEY_ARG = "KEY_ARG";
    private static final String PHONE_ARG = "PHONE_ARG";
    private static final String PASSWORD_ARG = "PASSWORD_ARG";


    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("Orders", "Received on bcr");
        if (intent != null && intent.getAction() != null) {
            if (intent.getAction().equals(ACTION_SET_UpdateService)) {
                updateIntent(context,
                        intent.getExtras().getString(KEY_ARG, SaveSharedPreferences.getUserAccessKey(context)),
                        intent.getExtras().getString(PHONE_ARG, SaveSharedPreferences.getUserPhone(context)),
                        intent.getExtras().getString(PASSWORD_ARG, SaveSharedPreferences.getUserPassword(context)));
            } else if (intent.getAction().equals(RESTART_SERVICE)) {
                Log.d("Orders", "Gotta restart service!");
                updateIntent(context,
                        SaveSharedPreferences.getUserAccessKey(context),
                        SaveSharedPreferences.getUserPhone(context),
                        SaveSharedPreferences.getUserPassword(context));
            } else if (intent.getAction().equals(ALARM_MANAGER_SEARCH_FOR_UPDATES)) {
                Log.i("Orders", "Look for singleshot updates");
                lookForUpdates(context);
            } else if (intent.getAction().equals(NET_CHANGE) && isOnline(context)) {
                Log.i("Orders", "Look for singleshot updates cause internet appeared");
                lookForUpdates(context);
            } else if (intent.getAction().equals(BOOT_RECEIVER) || intent.getAction().equals(QUICK_BOOT_RECEIVER)) {
                Log.i("Orders", "Started to register some y'know yes");
                AlarmManager am = (AlarmManager) context.getSystemService(ALARM_SERVICE);
                Intent intentt = new Intent(context, MainBroadcastReceiver.class);
                intentt.setAction(MainBroadcastReceiver.ALARM_MANAGER_SEARCH_FOR_UPDATES);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intentt, 0);
                am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), App.INTERVAL, pendingIntent);
            }
        }
    }

    private void lookForUpdates(Context ctx) {
        Thread whyGoddamnAndroidIsSoComplicated = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d("Orders", "Started looking for updates singleshot");
                OkHttpClient okHttpClient;
                String accessKey = SaveSharedPreferences.getUserAccessKey(ctx);
                String phone = SaveSharedPreferences.getUserPhone(ctx);
                String password = SaveSharedPreferences.getUserPassword(ctx);
                try {
                    if (accessKey != null) {
                        okHttpClient = new OkHttpClient();
                        Request request = new Request.Builder()
                                .url(String.format(URL + "updates" + "?key=%s&includeGeneral=false", accessKey))
                                .build();
                        try {
                            try (Response resp = okHttpClient.newCall(request).execute()) {
                                int code = resp.code();
                                String responseString = resp.body().string();
                                if (code / 100 == 2) {
                                    Log.i("Orders", "Some updates for ya, get dem");
                                    JSONObject updates = new JSONObject(responseString);
                                    MainRoomListener.executeUpdates(ctx, updates);
                                } else if (code == 403 && (!"".equals(phone) || !"".equals(password))) {
                                    Log.d("Orders", "Trying to login");
                                    okHttpClient = new OkHttpClient();
                                    request = new Request.Builder()
                                            .url(String.format(URL + "login" + "?telNumber=%s&password=%s",
                                                    phone, password))
                                            .build();
                                    try (Response respp = okHttpClient.newCall(request).execute()) {
                                        code = respp.code();
                                        responseString = respp.body().string();
                                        if (code / 100 == 2) {
                                            JSONObject obj = new JSONObject(responseString);
                                            accessKey = obj.getString("key");
                                            SaveSharedPreferences.setUserAccesskey(ctx, accessKey);
                                            okHttpClient = new OkHttpClient();
                                            request = new Request.Builder()
                                                    .url(String.format(URL + "updates" + "?key=%s&includeGeneral=false", accessKey))
                                                    .build();
                                            try (Response resppp = okHttpClient.newCall(request).execute()) {
                                                code = resppp.code();
                                                responseString = resppp.body().string();
                                                if (code / 100 == 2) {
                                                    Log.i("Orders", "Some updates for ya, get dem, even after relogin");
                                                    JSONObject updates = new JSONObject(responseString);
                                                    MainRoomListener.executeUpdates(ctx, updates);
                                                }
                                            }
                                        }
                                    }
                                }
                            } catch (IOException e) {
                                Log.d("Orders", "Cancelled working");
                            }
                        } catch (Exception e) {
                            Log.d("Orders", "Null ha-ha");
                        }
                    }
                } catch (Exception e) {
                    Log.e("Orders", "Couldn't get updates - very bad");
                }
            }
        });
        whyGoddamnAndroidIsSoComplicated.start();
    }

    private boolean isOnline(Context context) {

        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return (netInfo != null && netInfo.isConnected());
    }

    private void updateIntent(Context context, String accessKey, String login, String password) {
        Intent intent = new Intent(context, ServerUpdateListener.class);
        intent.setAction(ACTION_SET_UpdateService);
        intent.putExtra(KEY_ARG, accessKey);
        intent.putExtra(PHONE_ARG, login);
        intent.putExtra(PASSWORD_ARG, password);
        context.startService(intent);
    }
}