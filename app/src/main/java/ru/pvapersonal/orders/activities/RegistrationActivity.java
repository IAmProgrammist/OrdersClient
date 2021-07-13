package ru.pvapersonal.orders.activities;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.transition.Scene;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.transition.TransitionManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputLayout;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.redmadrobot.inputmask.MaskedTextChangedListener;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import ru.pvapersonal.orders.R;
import ru.pvapersonal.orders.service.SaveSharedPreferences;
import ru.pvapersonal.orders.service.ServerUpdateListener;
import ru.pvapersonal.orders.service.MainBroadcastReceiver;
import ru.pvapersonal.orders.service.listeners.ServerEventListener;

public class RegistrationActivity extends AppCompatActivity {
    Intent serviceIntent;
    ServerUpdateListener updateService;
    Uri resultUri = null;
    View viewPhoto;
    View viewRegistry;
    boolean isLoading = false;
    private static final int OK = 0;
    private static final int NOT_REGISTERED = 1;
    private int returnCode = NOT_REGISTERED;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.registry_root);
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
        ViewGroup root = (ViewGroup) findViewById(R.id.registry_root);
        viewRegistry = View.inflate(this, R.layout.registry_scene1, null);
        viewPhoto = View.inflate(this, R.layout.registry_scene2, null);
        Scene sceneOne = new Scene(root, viewRegistry);
        Scene sceneTwo = new Scene(root, viewPhoto);
        sceneOne.enter();
        Transition fadeTransition = TransitionInflater.from(this).inflateTransition(R.transition.fade_transition);

        //Setting up first scene
        EditText editText = viewRegistry.findViewById(R.id.editTextPhone);
        final MaskedTextChangedListener listener = MaskedTextChangedListener.Companion.installOn(
                editText,
                "+7 ([000]) [000]-[00]-[00]",
                (maskFilled, extractedValue, formattedText) -> {

                }
        );
        TextInputLayout phoneTil = viewRegistry.findViewById(R.id.editTextPhoneTil);
        TextInputLayout nameTil = viewRegistry.findViewById(R.id.editTextNameTil);
        TextInputLayout surnameTil = viewRegistry.findViewById(R.id.editTextSurnameTil);
        TextInputLayout passwordTil = viewRegistry.findViewById(R.id.editTextPasswordTil);
        EditText phone = viewRegistry.findViewById(R.id.editTextPhone);
        EditText name = viewRegistry.findViewById(R.id.editTextName);
        EditText surname = viewRegistry.findViewById(R.id.editTextSurname);
        EditText middlename = viewRegistry.findViewById(R.id.editTextMiddlename);
        EditText password = viewRegistry.findViewById(R.id.editTextPassword);
        EditText passwordConfirm = viewRegistry.findViewById(R.id.editTextTextPasswordConfirm);
        viewRegistry.findViewById(R.id.next).setOnClickListener(v -> {
            CheckBox check = viewRegistry.findViewById(R.id.checkBox);
            phoneTil.setError(null);
            nameTil.setError(null);
            surnameTil.setError(null);
            passwordTil.setError(null);
            try {
                PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
                Phonenumber.PhoneNumber num = phoneNumberUtil.parse(phone.getText().toString(), "RU");
                if (!phoneNumberUtil.isValidNumberForRegion(num, "RU")) {
                    throw new NumberParseException(NumberParseException.ErrorType.NOT_A_NUMBER, "");
                }
                String telNumber = phoneNumberUtil.format(num, PhoneNumberUtil.PhoneNumberFormat.E164);
                String nameText = name.getText().toString().trim();
                String surnameText = surname.getText().toString().trim();
                String middlenameText = middlename.getText().toString().trim();
                String passwordText = password.getText().toString();
                String passwordConfirmText = passwordConfirm.getText().toString();
                boolean isValid = true;
                if (nameText.equalsIgnoreCase("")) {
                    nameTil.setError(getResources().getString(R.string.pole_incorrect));
                    isValid = false;
                }
                if (surnameText.equalsIgnoreCase("")) {
                    surnameTil.setError(getResources().getString(R.string.pole_incorrect));
                    isValid = false;
                }
                if (passwordText.length() < 6) {
                    passwordTil.setError(getResources().getString(R.string.password_incorrect));
                    isValid = false;
                } else if (!passwordText.equals(passwordConfirmText)) {
                    passwordTil.setError(getResources().getString(R.string.passwords_are_not_equal));
                    isValid = false;
                }
                if (isValid) {
                    RegistrationActivity.this.findViewById(R.id.loader).setVisibility(View.VISIBLE);
                    updateService.registerQuery(nameText, surnameText, middlenameText, telNumber,
                            passwordText, new ServerEventListener() {
                                @Override
                                public void eventExecuted(int code, String response) {
                                    try {
                                        RegistrationActivity.this.findViewById(R.id.loader).setVisibility(View.GONE);
                                        if (code == 401) {
                                            phoneTil.setError(getResources().getString(R.string.user_exists));
                                        }else if (code == 200) {
                                            returnCode = OK;
                                            JSONObject resp = new JSONObject(response);
                                            if (check.isChecked()) {
                                                SaveSharedPreferences.setUserPhone(
                                                        RegistrationActivity.this, telNumber);
                                                SaveSharedPreferences.setUserPassword(
                                                        RegistrationActivity.this, passwordText);
                                                SaveSharedPreferences.setUserAccesskey(
                                                        RegistrationActivity.this,
                                                        resp.getString("key"));
                                                updateService.setAccessKey(resp.getString("key"));
                                                updateService.setPassword(passwordText);
                                                updateService.setPhone(telNumber);
                                            } else {
                                                SaveSharedPreferences.setUserPhone(
                                                        RegistrationActivity.this, null);
                                                SaveSharedPreferences.setUserPassword(
                                                        RegistrationActivity.this, null);
                                                SaveSharedPreferences.setUserAccesskey(
                                                        RegistrationActivity.this,
                                                        resp.getString("key"));
                                                updateService.setAccessKey(resp.getString("key"));
                                                updateService.setPassword("");
                                                updateService.setPhone("");
                                            }
                                            TransitionManager.go(sceneTwo, fadeTransition);
                                        } else {
                                            Toast.makeText(RegistrationActivity.this,
                                                    getResources().getString(R.string.server_error),
                                                    Toast.LENGTH_LONG).show();
                                        }
                                    } catch (JSONException e) {
                                        Toast.makeText(RegistrationActivity.this,
                                                getResources().getString(R.string.app_error),
                                                Toast.LENGTH_LONG).show();
                                    }
                                }
                            }, true);
                }
            } catch (UnsupportedEncodingException | JSONException e) {
                e.printStackTrace();
            } catch (NumberParseException e) {
                phoneTil.setError(RegistrationActivity.this.getResources().getString(R.string.phone_incorrect));
            }
        });
        viewPhoto.findViewById(R.id.pick_image).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CropImage.activity()
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .setAspectRatio(1, 1)
                        .setRequestedSize(500, 500)
                        .start(RegistrationActivity.this);
            }
        });
        viewPhoto.findViewById(R.id.next).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(resultUri != null) {
                    try {
                        findViewById(R.id.loader).setVisibility(View.VISIBLE);
                        isLoading = true;
                        updateService.setUserImage(
                                SaveSharedPreferences.getUserAccessKey(RegistrationActivity.this),
                                resultUri, new ServerEventListener() {
                                    @Override
                                    public void eventExecuted(int code, String response) {
                                        findViewById(R.id.loader).setVisibility(View.GONE);
                                        isLoading = false;
                                        if (code == 200) {
                                            setResult(returnCode);
                                            finish();
                                            return;
                                        } else {
                                            Toast.makeText(RegistrationActivity.this,
                                                    getResources().getString(R.string.server_error),
                                                    Toast.LENGTH_LONG).show();
                                            setResult(returnCode);
                                            finish();
                                            return;
                                        }
                                    }
                                }, true);
                    } catch (FileNotFoundException e) {
                        Toast.makeText(RegistrationActivity.this,
                                getResources().getString(R.string.file_not_found),
                                Toast.LENGTH_LONG).show();
                        resultUri = null;
                    }
                }else {
                    returnCode = OK;
                    setResult(returnCode);
                    finish();
                }
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                resultUri = result.getUri();
                if (viewPhoto != null) {
                    Drawable chosen;
                    try {
                        InputStream inputStream = getContentResolver().openInputStream(resultUri);
                        chosen = Drawable.createFromStream(inputStream, resultUri.toString());
                    } catch (FileNotFoundException e) {
                        chosen = getResources().getDrawable(R.drawable.ic_avatar_empty);
                    }
                    ((ImageView) viewPhoto.findViewById(R.id.avatar)).setImageDrawable(chosen);
                }
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Toast.makeText(this, getResources().getString(R.string.crop_image_error),
                        Toast.LENGTH_LONG).show();
            }
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
        unbindService(serviceConnection);
        stopService(serviceIntent);
        Intent intent = new Intent(RegistrationActivity.this, MainBroadcastReceiver.class);
        sendBroadcast(intent);

        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (!isLoading) {
            setResult(returnCode);
            finish();
        }
    }
}
