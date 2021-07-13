package ru.pvapersonal.orders.activities;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.noowenz.customdatetimepicker.CustomDateTimePicker;

import org.jetbrains.annotations.NotNull;

import java.util.Calendar;
import java.util.Date;

import ru.pvapersonal.orders.R;

public class TestActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setup_room);
        findViewById(R.id.test_click).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CustomDateTimePicker customDateTimePicker = new CustomDateTimePicker(TestActivity.this, new CustomDateTimePicker.ICustomDateTimeListener() {
                    @Override
                    public void onSet(@NotNull Dialog dialog, @NotNull Calendar calendar, @NotNull Date date, int i, @NotNull String s, @NotNull String s1, int i1, int i2, @NotNull String s2, @NotNull String s3, int i3, int i4, int i5, int i6, @NotNull String s4) {

                    }

                    @Override
                    public void onCancel() {

                    }
                });
                customDateTimePicker.showDialog();

            }
        });
    }
}
