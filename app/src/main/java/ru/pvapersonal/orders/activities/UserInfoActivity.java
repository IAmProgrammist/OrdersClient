package ru.pvapersonal.orders.activities;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import ru.pvapersonal.orders.R;
import ru.pvapersonal.orders.service.SaveSharedPreferences;
import ru.pvapersonal.orders.service.ServerUpdateListener;
import ru.pvapersonal.orders.service.MainBroadcastReceiver;
import ru.pvapersonal.orders.service.listeners.ServerEventListener;

import static ru.pvapersonal.orders.other.App.URL;

public class UserInfoActivity extends AppCompatActivity {
    public static final String USER_ID = "user_id";
    int userIdExtra;
    Intent serviceIntent;
    ServerUpdateListener updateService;
    @Override
    protected void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.account_screen);
        findViewById(R.id.change_info).setVisibility(View.GONE);
        findViewById(R.id.active_users).setVisibility(View.GONE);
        findViewById(R.id.change_photo).setVisibility(View.GONE);
        findViewById(R.id.trans_hist).setVisibility(View.GONE);
        userIdExtra = getIntent().getIntExtra(USER_ID, 0);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        serviceIntent = new Intent(this, ServerUpdateListener.class);
        if (isMyServiceRunning()) {
            //Bind to the service
            bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        } else {
            updateService = new ServerUpdateListener();
            //Start the service
            startService(serviceIntent);
            //Bind to the service
            bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        }
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

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == android.R.id.home){
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            updateService = ((ServerUpdateListener.MyBinder) service).getService();
            findViewById(R.id.loader).setVisibility(View.GONE);
            tryToConnect();
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
        Intent intent = new Intent(this, MainBroadcastReceiver.class);
        sendBroadcast(intent);

        super.onDestroy();
    }

    public void tryToConnect() {
        findViewById(R.id.loader).setVisibility(View.VISIBLE);
        updateService.getUserInfo(SaveSharedPreferences.getUserAccessKey(this), userIdExtra, new ServerEventListener() {
            @Override
            public void eventExecuted(int code, String response) {
                if (code == 200) {
                    try {
                        findViewById(R.id.loader).setVisibility(View.GONE);
                        JSONObject res = new JSONObject(response);
                        String name = res.getString("name");
                        String surname = res.getString("surname");
                        String middlename = res.has("middlename") ? res.getString("middlename") : null;
                        String phone = res.getString("telNumber");
                        String image = res.has("fileName") ? res.getString("fileName") : null;
                        boolean isAdmin = res.getBoolean("isAdmin");
                        if(isAdmin){
                            ((AppCompatTextView)findViewById(R.id.role)).setText(R.string.role_admin);
                        }else {
                            ((AppCompatTextView) findViewById(R.id.role)).setText(R.string.role_member);
                        }
                        String resText = middlename == null ? String.format("%s %s", surname, name) :
                                String.format("%s %s %s", surname, name, middlename);
                        ((TextView) findViewById(R.id.name)).setText(resText);
                        try {
                            PhoneNumberUtil util = PhoneNumberUtil.getInstance();
                            Phonenumber.PhoneNumber number = util.parse(phone, "RU");
                            String pre = util.format(number, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL);
                            ((TextView) findViewById(R.id.phone)).setText(pre);
                        } catch (NumberParseException e) {
                            ((TextView) findViewById(R.id.phone)).setText(phone);
                        }
                        if (image != null) {
                            Picasso.get().load(URL + "images/" + image).into((ImageView) findViewById(R.id.avatar));
                        } else {
                            ((ImageView) findViewById(R.id.avatar)).setImageResource(R.drawable.ic_avatar_empty);
                        }
                    } catch (JSONException e) {
                        findViewById(R.id.loader).setVisibility(View.GONE);
                        ((TextView) findViewById(R.id.name)).setText(getResources().getString(R.string.user_not_found));
                        ((TextView) findViewById(R.id.phone)).setText("");
                        findViewById(R.id.change_info).setVisibility(View.GONE);
                        findViewById(R.id.change_photo).setVisibility(View.GONE);
                    }
                } else{
                    Toast.makeText(UserInfoActivity.this, R.string.connecion_failed, Toast.LENGTH_LONG).show();
                }
            }
        }, true);
    }
}
