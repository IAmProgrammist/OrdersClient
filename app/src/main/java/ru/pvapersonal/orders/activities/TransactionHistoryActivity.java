package ru.pvapersonal.orders.activities;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
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
import ru.pvapersonal.orders.adapters.TransactionAdapter;
import ru.pvapersonal.orders.model.TransItem;
import ru.pvapersonal.orders.service.SaveSharedPreferences;
import ru.pvapersonal.orders.service.ServerUpdateListener;
import ru.pvapersonal.orders.service.MainBroadcastReceiver;
import ru.pvapersonal.orders.service.listeners.ServerEventListener;

public class TransactionHistoryActivity extends AppCompatActivity {
    Intent serviceIntent;
    ServerUpdateListener updateService;
    TransactionAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.transaction_history_screen);
        serviceIntent = new Intent(this, ServerUpdateListener.class);
        if (isMyServiceRunning()) {
            bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        } else if (!isMyServiceRunning()) {
            updateService = new ServerUpdateListener();
            startService(serviceIntent);
        }
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void tryToConnect() {
        String key = SaveSharedPreferences.getUserAccessKey(this);
        updateService.getPayHistory(key, new ServerEventListener() {

            public String getPayString(long transVal){
                long tmp = transVal;
                String res = "." + tmp % 100 + " " + getString(R.string.ruble);
                tmp = tmp / 100;
                return String.valueOf(tmp).replace("/\\B(?=(\\d{3})+(?!\\d))/g", " ") + res;
            }

            @Override
            public void eventExecuted(int code, String response) {
                Intent launchIntent = null;
                if (code == 200) {
                    try {
                        JSONObject res = new JSONObject(response);
                        ((TextView)findViewById(R.id.budget)).setText(getPayString(res.getLong("budget")));
                        List<TransItem> items = new ArrayList<>();
                        JSONArray data = res.getJSONArray("data");
                        for(int i = 0; i < data.length(); i++){
                            items.add(new TransItem(data.getJSONObject(i)));
                        }
                        adapter = new TransactionAdapter(getResources());
                        LinearLayoutManager llm = new LinearLayoutManager(TransactionHistoryActivity.this);
                        llm.setOrientation(LinearLayoutManager.VERTICAL);
                        RecyclerView recyclerView = findViewById(R.id.trans_hist);
                        recyclerView.setLayoutManager(llm);
                        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(
                                recyclerView.getContext(), llm.getOrientation());
                        recyclerView.addItemDecoration(dividerItemDecoration);
                        recyclerView.setAdapter(adapter);
                        adapter.setItems(items);
                    } catch (JSONException e) {
                        SaveSharedPreferences.setUserAccesskey(TransactionHistoryActivity.this, null);
                        launchIntent = new Intent(TransactionHistoryActivity.this, LoginActivity.class);
                    }
                } else if (code == 401 || code == 422) {
                    launchIntent = new Intent(TransactionHistoryActivity.this, LoginActivity.class);
                } else if (code == 403) {
                    SaveSharedPreferences.setUserAccesskey(TransactionHistoryActivity.this, null);
                    String userPhone = SaveSharedPreferences.getUserPhone(TransactionHistoryActivity.this);
                    String userPassword = SaveSharedPreferences.getUserPassword(TransactionHistoryActivity.this);
                    updateService.loginQuery(userPhone, userPassword, new ServerEventListener() {
                        @Override
                        public void eventExecuted(int code, String response) {
                            Intent launchIntent;
                            if (code == 200) {
                                try {
                                    JSONObject res = new JSONObject(response);
                                    SaveSharedPreferences.setUserAccesskey(TransactionHistoryActivity.this, res.getString("key"));
                                    updateService.setAccessKey(res.getString("key"));
                                    launchIntent = null;
                                    tryToConnect();
                                } catch (JSONException e) {
                                    launchIntent = new Intent(TransactionHistoryActivity.this, LoginActivity.class);
                                }
                            } else if (code == 401 || code == 422) {
                                launchIntent = new Intent(TransactionHistoryActivity.this, LoginActivity.class);
                            } else {
                                Toast.makeText(TransactionHistoryActivity.this, TransactionHistoryActivity.this.getResources().getString(R.string.connecion_failed), Toast.LENGTH_LONG).show();
                                TransactionHistoryActivity.this.tryToConnect();
                                launchIntent = null;
                            }
                            if (launchIntent != null) {
                                TransactionHistoryActivity.this.startActivity(launchIntent);
                                TransactionHistoryActivity.this.finish();
                            }
                        }
                    }, true);
                } else {
                    Toast.makeText(TransactionHistoryActivity.this, TransactionHistoryActivity.this.getResources().getString(R.string.connecion_failed), Toast.LENGTH_LONG).show();
                    TransactionHistoryActivity.this.tryToConnect();
                    launchIntent = null;
                }
                if (launchIntent != null) {
                    TransactionHistoryActivity.this.startActivity(launchIntent);
                    TransactionHistoryActivity.this.finish();
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.trans_hist_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        if(item.getItemId() == R.id.reverse){
            if(adapter != null){
                adapter.reverse();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
