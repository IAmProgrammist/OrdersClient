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
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
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

public class ChangeUserInfoActivity extends AppCompatActivity {

    Intent serviceIntent;
    ServerUpdateListener updateService;
    public static final int CHANGE_INFO_SUCCESS = 0;
    public static final int CHANGE_INFO_ERROR = 1;
    public static final int CHANGE_INFO_LAUNCH_LOGIN = 2;
    public static final int CHANGE_USER_INFO_CODE = 2153;
    public int returnCode = CHANGE_INFO_ERROR;

    @Override
    protected void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.change_info);
        findViewById(R.id.loader).setVisibility(View.VISIBLE);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == android.R.id.home){
            setResult(returnCode);
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
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

        String nameText = getIntent().getExtras().getString("name", "");
        String phoneText = getIntent().getExtras().getString("phone", "");
        String surNameText = getIntent().getExtras().getString("surname", "");
        String middleNameText = getIntent().getExtras().getString("middlename", "");
        EditText phone = findViewById(R.id.editTextPhone);
        final MaskedTextChangedListener listener = MaskedTextChangedListener.Companion.installOn(
                phone,
                "+7 ([000]) [000]-[00]-[00]",
                (maskFilled, extractedValue, formattedText) -> {

                }
        );
        phone.setText(phoneText);
        EditText name = findViewById(R.id.editTextName);
        EditText surname = findViewById(R.id.editTextSurname);
        EditText middlename = findViewById(R.id.editTextMiddlename);
        name.setText(nameText);
        surname.setText(surNameText);
        middlename.setText(middleNameText);

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
            EditText nameEd = findViewById(R.id.editTextName);
            EditText surname = findViewById(R.id.editTextSurname);
            EditText middlename = findViewById(R.id.editTextMiddlename);
            EditText phone = findViewById(R.id.editTextPhone);
            TextInputLayout phoneTil = findViewById(R.id.editTextPhoneTil);
            TextInputLayout nameTil = findViewById(R.id.editTextNameTil);
            TextInputLayout surnameTil = findViewById(R.id.editTextSurnameTil);
            findViewById(R.id.loader).setVisibility(View.GONE);
            findViewById(R.id.next).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    phoneTil.setError(null);
                    nameTil.setError(null);
                    surnameTil.setError(null);
                    try {
                        PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
                        Phonenumber.PhoneNumber num = phoneNumberUtil.parse(phone.getText().toString(), "RU");
                        if (!phoneNumberUtil.isValidNumberForRegion(num, "RU")) {
                            throw new NumberParseException(NumberParseException.ErrorType.NOT_A_NUMBER, "");
                        }
                        String telNumber = phoneNumberUtil.format(num, PhoneNumberUtil.PhoneNumberFormat.E164);
                        String nameText = nameEd.getText().toString().trim();
                        String surnameText = surname.getText().toString().trim();
                        String middlenameText = middlename.getText().toString().trim();
                        boolean isValid = true;
                        if (nameText.equalsIgnoreCase("")) {
                            nameTil.setError(getResources().getString(R.string.pole_incorrect));
                            isValid = false;
                        }
                        if (surnameText.equalsIgnoreCase("")) {
                            surnameTil.setError(getResources().getString(R.string.pole_incorrect));
                            isValid = false;
                        }
                        if (isValid) {
                            findViewById(R.id.loader).setVisibility(View.VISIBLE);
                            tryToConnect(telNumber, nameText, surnameText, middlenameText);
                        }
                    } catch (NumberParseException e) {
                        phoneTil.setError(ChangeUserInfoActivity.this.getResources().getString(R.string.phone_incorrect));
                    }
                }
            });
            //Set Initial Args
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            updateService = null;
        }

        public void tryToConnect(String telNumber, String nameText, String surnameText, String middlenameText) {
            TextInputLayout phoneTil = findViewById(R.id.editTextPhoneTil);
            updateService.updateUserInfo(telNumber,
                    nameText, surnameText, middlenameText, SaveSharedPreferences
                            .getUserAccessKey(ChangeUserInfoActivity.this),
                    new ServerEventListener() {
                        @Override
                        public void eventExecuted(int code, String response) {
                            ChangeUserInfoActivity.this.findViewById(R.id.loader).setVisibility(View.GONE);
                            if (code == 403) {
                                phoneTil.setError(getResources().getString(R.string.user_exists));
                            } else if (code == 200) {
                                returnCode = CHANGE_INFO_SUCCESS;
                                if (SaveSharedPreferences.getUserPhone(ChangeUserInfoActivity.this) != null) {
                                    SaveSharedPreferences.setUserPhone(ChangeUserInfoActivity.this, telNumber);
                                    updateService.setPhone(telNumber);
                                }
                                findViewById(R.id.loader).setVisibility(View.GONE);
                                Intent data = new Intent();
                                data.putExtra("name", nameText);
                                data.putExtra("surname", surnameText);
                                data.putExtra("middlename", middlenameText);
                                data.putExtra("phone", telNumber);
                                setResult(returnCode, data);
                                ChangeUserInfoActivity.this.finish();
                            } else if (code == 401) {
                                if (SaveSharedPreferences
                                        .getUserPhone(ChangeUserInfoActivity.this) == null ||
                                        SaveSharedPreferences
                                                .getUserPassword(ChangeUserInfoActivity.this) == null) {

                                } else {
                                    updateService.loginQuery(SaveSharedPreferences
                                                    .getUserPhone(ChangeUserInfoActivity.this),
                                            SaveSharedPreferences
                                                    .getUserPassword(ChangeUserInfoActivity.this),
                                            new ServerEventListener() {
                                                @Override
                                                public void eventExecuted(int code, String response) {
                                                    Intent launchIntent;
                                                    if (code == 200) {
                                                        try {
                                                            JSONObject res = new JSONObject(response);
                                                            SaveSharedPreferences.setUserAccesskey(ChangeUserInfoActivity.this, res.getString("key"));
                                                            updateService.setAccessKey(res.getString("key"));
                                                            tryToConnect(telNumber, nameText, surnameText, middlenameText);
                                                        } catch (JSONException e) {
                                                            Toast.makeText(ChangeUserInfoActivity.this, ChangeUserInfoActivity.this.getResources().getString(R.string.app_error), Toast.LENGTH_LONG).show();
                                                            tryToConnect(telNumber, nameText, surnameText, middlenameText);
                                                        }
                                                    } else if (code == 401 || code == 422) {
                                                        returnCode = CHANGE_INFO_LAUNCH_LOGIN;
                                                        setResult(returnCode);
                                                        ChangeUserInfoActivity.this.finish();
                                                    } else {
                                                        Toast.makeText(ChangeUserInfoActivity.this, ChangeUserInfoActivity.this.getResources().getString(R.string.connecion_failed), Toast.LENGTH_LONG).show();
                                                        tryToConnect(telNumber, nameText, surnameText, middlenameText);
                                                    }
                                                }
                                            }, true);
                                }
                            } else {
                                Toast.makeText(ChangeUserInfoActivity.this,
                                        getResources().getString(R.string.server_error),
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                    }, true);
        }
    };

    @Override
    protected void onStop() {

        unbindService(serviceConnection);
        stopService(serviceIntent);
        Intent intent = new Intent(this, MainBroadcastReceiver.class);
        sendBroadcast(intent);

        super.onStop();
    }

    @Override
    public void onBackPressed() {
        setResult(returnCode);
        super.onBackPressed();
    }
}
