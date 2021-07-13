package ru.pvapersonal.orders.service;

import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import ru.pvapersonal.orders.R;
import ru.pvapersonal.orders.service.listeners.MainRoomListener;

public class LookForUpdatesSingle extends JobIntentService {

    @Override
    protected void onHandleWork(@NonNull @NotNull Intent intent) {

    }

}
