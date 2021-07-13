package ru.pvapersonal.orders.activities;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputLayout;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.redmadrobot.inputmask.MaskedTextChangedListener;

import org.json.JSONException;
import org.json.JSONObject;

import ru.pvapersonal.orders.R;
import ru.pvapersonal.orders.service.SaveSharedPreferences;
import ru.pvapersonal.orders.service.ServerUpdateListener;
import ru.pvapersonal.orders.service.MainBroadcastReceiver;
import ru.pvapersonal.orders.service.listeners.ServerEventListener;

//Activity

//Bind to Service Example

public class LoginActivity extends AppCompatActivity {
    Intent serviceIntent;
    ServerUpdateListener updateService;
    private static final int REGISTRY_CODE = 23;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_screen);
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
        setupPrefixSample();
        findViewById(R.id.loginbut).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText phone = findViewById(R.id.editTextPhone);
                TextInputLayout phoneTil = findViewById(R.id.editTextPhoneTil);
                EditText password = findViewById(R.id.editTextPassword);
                TextInputLayout passwordTil = findViewById(R.id.editTextPasswordTil);
                CheckBox check = findViewById(R.id.checkBox);
                try {
                    String phoneStr = phone.getText().toString();
                    String passwStr = password.getText().toString();
                    boolean shouldSave = check.isChecked();
                    PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
                    Phonenumber.PhoneNumber number = phoneUtil.parse(phoneStr, "RU");
                    if(!phoneUtil.isValidNumberForRegion(number, "RU")){
                        phoneTil.setError("Введите верный номер");
                        return;
                    }
                    findViewById(R.id.loader).setVisibility(View.VISIBLE);
                    updateService.loginQuery(phoneStr, passwStr, new ServerEventListener() {
                        @Override
                        public void eventExecuted(int code, String response) {
                            findViewById(R.id.loader).setVisibility(View.GONE);
                            try {
                                if (code == 401) {
                                    phoneTil.setError("Введите верный номер телефона или пароль");
                                } else if (code == 500 || code == 422) {
                                    phoneTil.setError("Сервер пока недоступен, попробуйте позже");
                                } else if(code == 200) {
                                    JSONObject resp = new JSONObject(response);
                                    if (shouldSave) {
                                        SaveSharedPreferences.setUserPhone(LoginActivity.this,
                                                phoneStr);
                                        SaveSharedPreferences.setUserPassword(LoginActivity.this,
                                                passwStr);
                                        SaveSharedPreferences.setUserAccesskey(LoginActivity.this,
                                                resp.getString("key"));
                                        updateService.setAccessKey(resp.getString("key"));
                                        updateService.setPassword(passwStr);
                                        updateService.setPhone(phoneStr);
                                    } else {
                                        SaveSharedPreferences.setUserPhone(LoginActivity.this,
                                                null);
                                        SaveSharedPreferences.setUserPassword(LoginActivity.this,
                                                null);
                                        SaveSharedPreferences.setUserAccesskey(LoginActivity.this,
                                                resp.getString("key"));
                                        updateService.setAccessKey(resp.getString("key"));
                                        updateService.setPassword("");
                                        updateService.setPhone("");
                                    }
                                    Intent launchIntent = new Intent(LoginActivity.this,
                                            RoomsActivity.class);
                                    LoginActivity.this.startActivity(launchIntent);
                                    LoginActivity.this.finish();
                                }else{
                                    Toast.makeText(LoginActivity.this,
                                            LoginActivity.this.getResources().getString(R.string.connecion_failed),
                                            Toast.LENGTH_LONG).show();
                                }
                            }catch (JSONException e){
                                phoneTil.setError("Сервер пока недоступен, попробуйте позже");
                            }
                        }
                    }, true);
                } catch (NumberParseException e) {
                    phoneTil.setError("Введите верный номер");
                }
            }
        });
        findViewById(R.id.noaccbut).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, RegistrationActivity.class);
                startActivityForResult(intent, REGISTRY_CODE);
            }
        });
    }

    private void setupPrefixSample() {
        EditText editText = findViewById(R.id.editTextPhone);
        final MaskedTextChangedListener listener = MaskedTextChangedListener.Companion.installOn(
                editText,
                "+7 ([000]) [000]-[00]-[00]",
                (maskFilled, extractedValue, formattedText) -> {

                }
        );
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

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            updateService = ((ServerUpdateListener.MyBinder) service).getService();
            updateService.removeGeneralListener();
            //Set Initial Args
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            updateService = null;
        }
    };

    @Override
    protected void onDestroy() {
        //UnBind from service
        unbindService(serviceConnection);
        //Stop Service
        stopService(serviceIntent);
        //Prepare intent to broadcast reciver
        Intent intent = new Intent(LoginActivity.this, MainBroadcastReceiver.class);
        /*intent.setAction(ServiceRunnerBCR.ACTION_SET_UpdateService);
        intent.putExtra(ServiceRunnerBCR.keyVal_arg0, "");
        intent.putExtra(ServiceRunnerBCR.keyVal_arg1, 0);*/
        //Send broadcast to start UpdateService after the activity ended
        sendBroadcast(intent);

        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REGISTRY_CODE) {
            if (resultCode == 0) {
                tryToConnect();
            } else if (resultCode == 1) {

            }
        }
    }

    private void tryToConnect(){
        findViewById(R.id.loader).setVisibility(View.VISIBLE);
        String key = SaveSharedPreferences.getUserAccessKey(this);
        updateService.checkForKeyValidness(key, new ServerEventListener() {
            @Override
            public void eventExecuted(int code, String response) {
                Intent launchIntent = null;
                if (code == 200) {
                    try {
                        JSONObject res = new JSONObject(response);
                        if (res.getBoolean("data")) {
                            findViewById(R.id.loader).setVisibility(View.GONE);
                            launchIntent = new Intent(LoginActivity.this, RoomsActivity.class);
                        } else {
                            SaveSharedPreferences.setUserAccesskey(LoginActivity.this, null);
                            updateService.setAccessKey("");
                            String userPhone = SaveSharedPreferences.getUserPhone(LoginActivity.this);
                            String userPassword = SaveSharedPreferences.getUserPassword(LoginActivity.this);
                            updateService.loginQuery(userPhone, userPassword, new ServerEventListener() {
                                @Override
                                public void eventExecuted(int code, String response) {
                                    Intent launchIntent;
                                    findViewById(R.id.loader).setVisibility(View.GONE);
                                    if (code == 200) {
                                        try {
                                            JSONObject res = new JSONObject(response);
                                            SaveSharedPreferences.setUserAccesskey(LoginActivity.this, res.getString("key"));
                                            updateService.setAccessKey("");
                                            launchIntent = new Intent(LoginActivity.this, RoomsActivity.class);
                                        } catch (JSONException e) {
                                            launchIntent = null;
                                            Toast.makeText(LoginActivity.this, getResources().getString(R.string.server_error), Toast.LENGTH_LONG).show();
                                        }
                                    } else if(code == 401 || code == 422){
                                        Toast.makeText(LoginActivity.this, getResources().getString(R.string.server_error), Toast.LENGTH_LONG).show();
                                        launchIntent = null;
                                    }else{
                                        Toast.makeText(LoginActivity.this, getResources().getString(R.string.server_error), Toast.LENGTH_LONG).show();
                                        launchIntent = null;
                                    }
                                    if(launchIntent != null) {
                                        LoginActivity.this.startActivity(launchIntent);
                                        LoginActivity.this.finish();
                                    }
                                }
                            }, true);
                        }
                    } catch (JSONException e) {
                        SaveSharedPreferences.setUserAccesskey(LoginActivity.this, null);
                        updateService.setAccessKey("");
                        Toast.makeText(LoginActivity.this, getResources().getString(R.string.server_error), Toast.LENGTH_LONG).show();
                    }
                } else if(code == 401 || code == 422){
                    Toast.makeText(LoginActivity.this, getResources().getString(R.string.server_error), Toast.LENGTH_LONG).show();
                    launchIntent = null;
                }else{
                    Toast.makeText(LoginActivity.this, LoginActivity.this.getResources().getString(R.string.connecion_failed), Toast.LENGTH_LONG).show();
                    launchIntent = null;
                }
                if(launchIntent != null) {
                    LoginActivity.this.startActivity(launchIntent);
                    LoginActivity.this.finish();
                }
            }
        }, true);
    }
}