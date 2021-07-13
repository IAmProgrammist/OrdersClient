package ru.pvapersonal.orders.activities;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import ru.pvapersonal.orders.R;
import ru.pvapersonal.orders.service.SaveSharedPreferences;
import ru.pvapersonal.orders.service.ServerUpdateListener;
import ru.pvapersonal.orders.service.MainBroadcastReceiver;
import ru.pvapersonal.orders.service.listeners.ServerEventListener;

public class SplashScreen extends AppCompatActivity {
    Intent serviceIntent;
    ServerUpdateListener updateService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        serviceIntent = new Intent(this, ServerUpdateListener.class);
        if (isMyServiceRunning()) {
            bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        }else if (!isMyServiceRunning()) {
            updateService = new ServerUpdateListener();
            startService(serviceIntent);
        }
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void tryToConnect() {
        String key = SaveSharedPreferences.getUserAccessKey(this);
        updateService.checkForKeyValidness(key, new ServerEventListener() {
            @Override
            public void eventExecuted(int code, String response) {
                Intent launchIntent = null;
                if (code == 200) {
                    try {
                        JSONObject res = new JSONObject(response);
                        if (res.getBoolean("data")) {
                            launchIntent = new Intent(SplashScreen.this, RoomsActivity.class);
                        } else {
                            SaveSharedPreferences.setUserAccesskey(SplashScreen.this, null);
                            String userPhone = SaveSharedPreferences.getUserPhone(SplashScreen.this);
                            String userPassword = SaveSharedPreferences.getUserPassword(SplashScreen.this);
                            updateService.loginQuery(userPhone, userPassword, new ServerEventListener() {
                                @Override
                                public void eventExecuted(int code, String response) {
                                    Intent launchIntent;
                                    if (code == 200) {
                                        try {
                                            JSONObject res = new JSONObject(response);
                                            SaveSharedPreferences.setUserAccesskey(SplashScreen.this, res.getString("key"));
                                            updateService.setAccessKey(res.getString("key"));
                                            launchIntent = new Intent(SplashScreen.this, RoomsActivity.class);
                                        } catch (JSONException e) {
                                            launchIntent = new Intent(SplashScreen.this, LoginActivity.class);
                                        }
                                    } else if(code == 401 || code == 422){
                                        launchIntent = new Intent(SplashScreen.this, LoginActivity.class);
                                    }else{
                                        Toast.makeText(SplashScreen.this, SplashScreen.this.getResources().getString(R.string.connecion_failed), Toast.LENGTH_LONG).show();
                                        SplashScreen.this.tryToConnect();
                                        launchIntent = null;
                                    }
                                    if(launchIntent != null) {
                                        SplashScreen.this.startActivity(launchIntent);
                                        SplashScreen.this.finish();
                                    }
                                }
                            }, true);
                        }
                    } catch (JSONException e) {
                        SaveSharedPreferences.setUserAccesskey(SplashScreen.this, null);
                        launchIntent = new Intent(SplashScreen.this, LoginActivity.class);
                    }
                } else if(code == 401 || code == 422){
                    launchIntent = new Intent(SplashScreen.this, LoginActivity.class);
                }else{
                    Toast.makeText(SplashScreen.this, SplashScreen.this.getResources().getString(R.string.connecion_failed), Toast.LENGTH_LONG).show();
                    SplashScreen.this.tryToConnect();
                    launchIntent = null;
                }
                if(launchIntent != null) {
                    SplashScreen.this.startActivity(launchIntent);
                    SplashScreen.this.finish();
                }
            }
        }, true);
    }

    private boolean isMyServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (ServerUpdateListener.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            updateService = ((ServerUpdateListener.MyBinder) service).getService();
            updateService.removeGeneralListener();
            updateService.setAccessKey(SaveSharedPreferences.getUserAccessKey(SplashScreen.this));
            updateService.setPhone(SaveSharedPreferences.getUserPhone(SplashScreen.this));
            updateService.setPassword(SaveSharedPreferences.getUserPassword(SplashScreen.this));
            tryToConnect();
            //Set Initial Args
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            updateService = null;
        }
    };

    @Override
    protected void onDestroy() {
        unbindService(serviceConnection);
        stopService(serviceIntent);
        Intent intent = new Intent(SplashScreen.this, MainBroadcastReceiver.class);
        sendBroadcast(intent);

        super.onDestroy();
    }
}
