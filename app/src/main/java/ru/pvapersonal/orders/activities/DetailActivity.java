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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.jetbrains.annotations.NotNull;

import ru.pvapersonal.orders.R;
import ru.pvapersonal.orders.fragments.RoomDetailFragment;
import ru.pvapersonal.orders.fragments.UserInfoFragment;
import ru.pvapersonal.orders.fragments.UsersDetailFragment;
import ru.pvapersonal.orders.service.ServerUpdateListener;
import ru.pvapersonal.orders.service.MainBroadcastReceiver;

public class DetailActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener {
    public static final String ROOM_ID = "ROOM_ID_EXTRA";
    private int roomId = 0;
    Intent serviceIntent;
    ServerUpdateListener updateService;
    Fragment userInfoFragment;
    Fragment roomDetail;
    Fragment usersFragment;
    public BottomNavigationView navigationView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        roomId = getIntent().getExtras().getInt(ROOM_ID);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.main_application);
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.getMenu().clear();
        bottomNavigationView.inflateMenu(R.menu.detail_room_menu);
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
        if(item.getItemId() == R.id.full_info_room){
            if (roomDetail == null) {
                roomDetail = new RoomDetailFragment(updateService, roomId);
            }
            getSupportFragmentManager().beginTransaction().replace(R.id.room_fragment, roomDetail).commit();
            return true;
        }else if(item.getItemId() == R.id.menu_acc){
            if (userInfoFragment == null) {
                userInfoFragment = new UserInfoFragment(updateService);
            }
            getSupportFragmentManager().beginTransaction().replace(R.id.room_fragment, userInfoFragment).commit();
            return true;
        }else if(item.getItemId() == R.id.all_users){
            if(usersFragment == null){
                usersFragment = new UsersDetailFragment(updateService, roomId);
            }
            getSupportFragmentManager().beginTransaction().replace(R.id.room_fragment, usersFragment).commit();

            return true;
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
            roomDetail = new RoomDetailFragment(updateService, roomId);
            usersFragment = new UsersDetailFragment(updateService, roomId);
            navigationView = (BottomNavigationView) findViewById(R.id.bottomNavigationView);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.room_fragment, roomDetail).commit();
            navigationView.setOnNavigationItemSelectedListener(DetailActivity.this);
            navigationView.setSelectedItemId(R.id.full_info_room);
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
