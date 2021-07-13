package ru.pvapersonal.orders.activities;

import android.app.ActivityManager;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatSpinner;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;

import ru.pvapersonal.orders.R;
import ru.pvapersonal.orders.service.SaveSharedPreferences;
import ru.pvapersonal.orders.service.ServerUpdateListener;
import ru.pvapersonal.orders.service.listeners.ServerEventListener;

public class SetupRoomActivity extends AppCompatActivity {

    public static final String ROOM_ID = "ROOM_ID";
    public static final String MAX_MEMBERS = "MAX_MEMBERS";
    Intent serviceIntent;
    ServerUpdateListener updateService;
    private Integer roomId;
    private Long loadTime;
    private Long startTime = null;
    private Long finishTime = null;
    private static int maxMembers = 1;

    @Override
    protected void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setup_room);
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
        findViewById(R.id.loader).setVisibility(View.VISIBLE);
        try {
            roomId = getIntent().getExtras().getInt(ROOM_ID);
            maxMembers = getIntent().getExtras().getInt(MAX_MEMBERS);
        } catch (NullPointerException e) {
            Toast.makeText(this, R.string.app_error, Toast.LENGTH_LONG).show();
            finish();
        }
        loadTime = System.currentTimeMillis();

        findViewById(R.id.start_min_pick).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickDate(loadTime, findViewById(R.id.start_min_root), true);
            }
        });
        findViewById(R.id.end_max_pick).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickDate(startTime, findViewById(R.id.end_max_root), false);
            }
        });
        EditText fullPrice = findViewById(R.id.full_budget_edittext);
        EditText pricePerHour = findViewById(R.id.budget_per_hour_edittext);
        EditText priceRoomPerUser = findViewById(R.id.budget_full_room_per_user_edittext);
        AppCompatSpinner spinner = findViewById(R.id.spinner);
        findViewById(R.id.setup_room_info_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    updateService.setupRoom(SaveSharedPreferences.getUserAccessKey(SetupRoomActivity.this),
                            roomId, ((EditText) findViewById(R.id.comment_edittext)).getText().toString(),
                            startTime, finishTime, spinner.getSelectedItemPosition(),
                            Double.parseDouble(spinner.getSelectedItemPosition() == 0 ?
                                    pricePerHour.getText().toString() : fullPrice.getText().toString()),
                            new ServerEventListener() {
                                @Override
                                public void eventExecuted(int code, String response) {
                                    if (code == 201) {
                                        Toast.makeText(SetupRoomActivity.this,
                                                R.string.room_already_set_up, Toast.LENGTH_LONG).show();
                                    } else if (code != 200) {
                                        Toast.makeText(SetupRoomActivity.this,
                                                R.string.app_error, Toast.LENGTH_LONG).show();
                                    }
                                    finish();
                                }
                            }, true);
                } catch (Exception d) {
                    Toast.makeText(SetupRoomActivity.this, R.string.app_error, Toast.LENGTH_LONG);
                }
            }
        });

        fullPrice.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (fullPrice.getTag() == null) {
                    recalculatePrices(0);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        pricePerHour.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (pricePerHour.getTag() == null) {
                    recalculatePrices(1);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        priceRoomPerUser.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (priceRoomPerUser.getTag() == null) {
                    recalculatePrices(2);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
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
            findViewById(R.id.loader).setVisibility(View.GONE);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            updateService = null;
        }
    };

    private void pickDate(Long min, View root, boolean start) {
        if (start || SetupRoomActivity.this.startTime != null) {
            Calendar cal = new GregorianCalendar();
            cal.setTimeInMillis(min);
            DatePickerDialog datePickerDialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
                @Override
                public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                    cal.set(year, month, dayOfMonth);
                    TimePickerDialog timePickerDialog = new TimePickerDialog(SetupRoomActivity.this,
                            new TimePickerDialog.OnTimeSetListener() {
                                @Override
                                public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                                    cal.set(GregorianCalendar.HOUR_OF_DAY, hourOfDay);
                                    cal.set(GregorianCalendar.MINUTE, minute);
                                    cal.set(GregorianCalendar.SECOND, 0);
                                    cal.set(Calendar.MILLISECOND, 0);
                                    SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", new Locale("RU"));
                                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy года", new Locale("RU"));
                                    if (start) {
                                        if (SetupRoomActivity.this.finishTime != null &&
                                                (cal.getTimeInMillis() >=
                                                        SetupRoomActivity.this.finishTime)) {
                                            Toast.makeText(SetupRoomActivity.this,
                                                    R.string.start_time_is_too_big,
                                                    Toast.LENGTH_LONG).show();
                                        } else {
                                            SetupRoomActivity.this.startTime = cal.getTimeInMillis();
                                            TextView timeTV = ((TextView) root.findViewById(R.id.start_min_time));
                                            TextView dateTV = ((TextView) root.findViewById(R.id.start_min_date));
                                            if (timeTV != null) {
                                                timeTV.setText(timeFormat.format(cal.getTimeInMillis()));
                                            }
                                            if (dateTV != null) {
                                                dateTV.setText(dateFormat.format(cal.getTimeInMillis()));
                                            }
                                            recalculatePrices(0);
                                        }
                                    } else {
                                        if (SetupRoomActivity.this.startTime != null &&
                                                (cal.getTimeInMillis() <=
                                                        SetupRoomActivity.this.startTime)) {
                                            Toast.makeText(SetupRoomActivity.this,
                                                    R.string.time_is_too_small,
                                                    Toast.LENGTH_LONG).show();
                                        } else {
                                            SetupRoomActivity.this.finishTime = cal.getTimeInMillis();
                                            TextView timeTV = ((TextView) root.findViewById(R.id.end_max_time));
                                            TextView dateTV = ((TextView) root.findViewById(R.id.end_max_date));
                                            if (timeTV != null) {
                                                timeTV.setText(timeFormat.format(cal.getTimeInMillis()));
                                            }
                                            if (dateTV != null) {
                                                dateTV.setText(dateFormat.format(cal.getTimeInMillis()));
                                            }
                                            recalculatePrices(0);
                                        }
                                    }
                                }
                            }, cal.get(GregorianCalendar.HOUR_OF_DAY), cal.get(GregorianCalendar.MINUTE),
                            true);
                    timePickerDialog.show();
                }
            }, cal.get(GregorianCalendar.YEAR),
                    cal.get(GregorianCalendar.MONTH),
                    cal.get(GregorianCalendar.DAY_OF_MONTH));
            datePickerDialog.getDatePicker().setMinDate(min);
            datePickerDialog.show();
        } else {
            Toast.makeText(this, R.string.pick_start_first, Toast.LENGTH_LONG).show();
        }
    }

    private void recalculatePrices(int changed) {
        EditText fullPrice = findViewById(R.id.full_budget_edittext);
        EditText pricePerHour = findViewById(R.id.budget_per_hour_edittext);
        EditText priceRoomPerUser = findViewById(R.id.budget_full_room_per_user_edittext);
        try {
            switch (changed) {
                case 0:
                    //changed full price of date
                    if (finishTime != null && startTime != null && finishTime - startTime > 0) {
                        BigDecimal fullPriceNum = new BigDecimal(fullPrice.getText().toString());
                        BigDecimal pricePerHourNum = fullPriceNum
                                .divide(new BigDecimal(String.valueOf(maxMembers)), 10, RoundingMode.HALF_UP)
                                .multiply(new BigDecimal("3600000.0"))
                                .divide(new BigDecimal(String.valueOf(finishTime - startTime)), 10, RoundingMode.HALF_UP);
                        pricePerHourNum = pricePerHourNum.setScale(2, RoundingMode.HALF_UP);
                        BigDecimal priceRoomPerUserNum = fullPriceNum
                                .divide(new BigDecimal(String.valueOf(maxMembers)), 2, RoundingMode.HALF_UP);
                        priceRoomPerUserNum = priceRoomPerUserNum.setScale(2, RoundingMode.HALF_UP);
                        setText(pricePerHour, pricePerHourNum.toString());
                        setText(priceRoomPerUser, priceRoomPerUserNum.toString());
                    }
                    break;
                case 1:
                    //change perhour
                    if (finishTime != null && startTime != null && finishTime - startTime > 0) {
                        BigDecimal pricePerHourNum = new BigDecimal(pricePerHour.getText().toString());
                        BigDecimal fullPriceNum = pricePerHourNum
                                .multiply(new BigDecimal(String.valueOf(finishTime - startTime)))
                                .multiply(new BigDecimal(String.valueOf(maxMembers)))
                                .divide(new BigDecimal("3600000.0"), 10, RoundingMode.HALF_UP);
                        fullPriceNum = fullPriceNum.setScale(2, RoundingMode.HALF_UP);
                        BigDecimal priceRoomPerUserNum = fullPriceNum
                                .divide(new BigDecimal(String.valueOf(maxMembers)), 2, RoundingMode.HALF_UP);
                        priceRoomPerUserNum = priceRoomPerUserNum.setScale(2, RoundingMode.HALF_UP);
                        setText(fullPrice, fullPriceNum.toString());
                        setText(priceRoomPerUser, priceRoomPerUserNum.toString());
                    }
                    break;
                case 2:
                    if (finishTime != null && startTime != null && finishTime - startTime > 0) {
                        BigDecimal priceRoomPerUserNum = new BigDecimal(priceRoomPerUser.getText().toString());
                        BigDecimal fullPriceNum = priceRoomPerUserNum
                                .multiply(new BigDecimal(String.valueOf(maxMembers)));
                        fullPriceNum = fullPriceNum.setScale(2, RoundingMode.HALF_UP);
                        BigDecimal pricePerHourNum = priceRoomPerUserNum
                                .multiply(new BigDecimal("3600000.0"))
                                .divide(new BigDecimal(String.valueOf(finishTime - startTime)), 10, RoundingMode.HALF_UP);
                        pricePerHourNum = pricePerHourNum.setScale(2, RoundingMode.HALF_UP);
                        setText(fullPrice, fullPriceNum.toString());
                        setText(pricePerHour, pricePerHourNum.toString());
                    }
                    break;
            }
        } catch (Exception e) {
            setText(fullPrice, "");
            setText(pricePerHour, "");
            setText(priceRoomPerUser, "");
            Log.e("Orders", "Error while parsing num");
        }
    }

    private static final String TAG_PROGRAMM_CHANGE = "EDITTEXT_PROGRAMMATICALLY_CHANGED";

    private void setText(EditText editText, String s) {
        editText.setTag(TAG_PROGRAMM_CHANGE);
        editText.setText(s);
        editText.setTag(null);
    }
}
