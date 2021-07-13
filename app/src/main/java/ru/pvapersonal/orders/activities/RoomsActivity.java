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
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.jetbrains.annotations.NotNull;

import ru.pvapersonal.orders.R;
import ru.pvapersonal.orders.fragments.QueueFragment;
import ru.pvapersonal.orders.fragments.RoomsFragment;
import ru.pvapersonal.orders.fragments.UserInfoFragment;
import ru.pvapersonal.orders.service.SaveSharedPreferences;
import ru.pvapersonal.orders.service.ServerUpdateListener;
import ru.pvapersonal.orders.service.MainBroadcastReceiver;

public class RoomsActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener {
    Intent serviceIntent;
    ServerUpdateListener updateService;
    Fragment userInfoFragment;
    Fragment roomsFragment;
    Fragment queueFragment;
    public BottomNavigationView navigationView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_application);
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

    @Override
    public boolean onNavigationItemSelected(@NonNull @NotNull MenuItem item) {
        if(item.getItemId() == R.id.menu_room){
            if (roomsFragment == null) {
                roomsFragment = new RoomsFragment(updateService);
            }
            getSupportFragmentManager().beginTransaction().replace(R.id.room_fragment, roomsFragment).commit();
            return true;
        }else if(item.getItemId() == R.id.menu_acc){
            if (userInfoFragment == null) {
                userInfoFragment = new UserInfoFragment(updateService);
            }
            getSupportFragmentManager().beginTransaction().replace(R.id.room_fragment, userInfoFragment).commit();
            return true;
        }else if(item.getItemId() == R.id.queue_room){
            if(queueFragment == null){
                queueFragment = new QueueFragment(updateService);
            }
            getSupportFragmentManager().beginTransaction().replace(R.id.room_fragment, queueFragment).commit();
            return true;
        }
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.orders_room, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == R.id.exit){
            SaveSharedPreferences.setUserPhone(RoomsActivity.this, null);
            SaveSharedPreferences.setUserPassword(RoomsActivity.this, null);
            SaveSharedPreferences.setUserAccesskey(RoomsActivity.this, null);
            updateService.setAccessKey("");
            updateService.setPassword("");
            updateService.setPhone("");
            Intent intent = new Intent(RoomsActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
        return super.onOptionsItemSelected(item);
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
            userInfoFragment = new UserInfoFragment(updateService);
            roomsFragment = new RoomsFragment(updateService);
            queueFragment = new QueueFragment(updateService);
            navigationView = (BottomNavigationView) findViewById(R.id.bottomNavigationView);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.room_fragment, queueFragment).commit();
            navigationView.setOnNavigationItemSelectedListener(RoomsActivity.this);
                navigationView.setSelectedItemId(R.id.queue_room);
            findViewById(R.id.loader).setVisibility(View.GONE);
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
}
