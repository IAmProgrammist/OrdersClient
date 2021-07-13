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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import ru.pvapersonal.orders.R;
import ru.pvapersonal.orders.adapters.FullUsersAdapter;
import ru.pvapersonal.orders.model.FullUserItem;
import ru.pvapersonal.orders.service.MainBroadcastReceiver;
import ru.pvapersonal.orders.service.SaveSharedPreferences;
import ru.pvapersonal.orders.service.ServerUpdateListener;
import ru.pvapersonal.orders.service.listeners.ServerEventListener;

public class ActiveUsersActivity extends AppCompatActivity {

    Intent serviceIntent;
    ServerUpdateListener updateService;
    @Override
    protected void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.active_users);
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
        updateService.getActiveUsers(SaveSharedPreferences.getUserAccessKey(this), new ServerEventListener() {
            @Override
            public void eventExecuted(int code, String response) {
                if (code == 200) {
                    try {
                        findViewById(R.id.loader).setVisibility(View.GONE);
                        JSONObject res = new JSONObject(response);
                        JSONArray array = res.getJSONArray("data");
                        Long qDate = res.getLong("queryTime");
                        List<FullUserItem> list = new ArrayList<>();
                        for(int i = 0; i < array.length(); i++){
                            JSONObject obj = array.getJSONObject(i);
                            list.add(new FullUserItem(obj, qDate));
                        }
                        LinearLayoutManager llm = new LinearLayoutManager(ActiveUsersActivity.this);
                        llm.setOrientation(LinearLayoutManager.VERTICAL);
                        RecyclerView recyclerView = findViewById(R.id.full_users_list);
                        recyclerView.setLayoutManager(llm);
                        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(
                                recyclerView.getContext(), llm.getOrientation());
                        recyclerView.addItemDecoration(dividerItemDecoration);
                        FullUsersAdapter adapter = new FullUsersAdapter(ActiveUsersActivity.this);
                        recyclerView.setAdapter(adapter);
                        adapter.updateItems(list);
                        adapter.notifyDataSetChanged();
                    } catch (JSONException e) {
                        Toast.makeText(ActiveUsersActivity.this, R.string.app_error, Toast.LENGTH_LONG).show();
                        finish();
                    }
                } else{
                    Toast.makeText(ActiveUsersActivity.this, R.string.you_are_not_admin, Toast.LENGTH_LONG).show();
                    finish();
                }
            }
        }, true);
    }
}
