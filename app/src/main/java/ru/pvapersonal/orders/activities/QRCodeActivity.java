package ru.pvapersonal.orders.activities;

import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.squareup.picasso.Picasso;

import org.json.JSONObject;

import java.util.Base64;

import ru.pvapersonal.orders.R;
import ru.pvapersonal.orders.service.SaveSharedPreferences;

import static ru.pvapersonal.orders.other.App.URL;

public class QRCodeActivity extends AppCompatActivity {

    public static final String ROOM_ID = "EXTRA_ROOM_ID";

    @Override
    protected void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scan_qr);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        String key = SaveSharedPreferences.getUserAccessKey(this);
        Integer roomId = getIntent().getExtras().getInt(ROOM_ID);
        if (key == null || roomId == 0) {
            Toast.makeText(this, getResources().getString(R.string.app_error), Toast.LENGTH_LONG).show();
            finish();
        }else{
            Picasso.get().load(String.format(URL + "generateroomqr?key=%s&roomId=%s",
                            key, roomId)).into((ImageView) findViewById(R.id.qr_image));
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }
}
