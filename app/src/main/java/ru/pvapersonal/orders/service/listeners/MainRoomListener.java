package ru.pvapersonal.orders.service.listeners;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import androidx.fragment.app.FragmentActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import ru.pvapersonal.orders.other.Notifier;

public class MainRoomListener {
    private Map<Integer, List<RoomListener>> listenerMap;
    private final Object syncKey = new Object();

    public MainRoomListener() {
        listenerMap = new ConcurrentHashMap<>();
    }

    public void addListener(int roomId, RoomListener listener){
        synchronized (syncKey){
            List<RoomListener> listeners = listenerMap.containsKey(roomId) ? listenerMap.get(roomId) :
                    Collections.synchronizedList(new ArrayList<>());
            listeners.add(listener);
            listenerMap.put(roomId, listeners);
        }
    }

    public void removeListener(RoomListener listener){
        synchronized (syncKey){
            for(Map.Entry<Integer, List<RoomListener>> ent : listenerMap.entrySet()){
                ent.getValue().remove(listener);
            }
        }
    }

    public void executeUpdates(Context ctx, JSONArray array) {
        new Handler(ctx.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                synchronized (syncKey) {
                    try {
                        for (int i = 0; i < array.length(); i++) {
                            JSONArray updoots = array.getJSONArray(i);
                            for (int j = 0; j < updoots.length(); j++) {
                                JSONObject update = updoots.getJSONObject(j);
                                int roomId = update.getInt("roomId");
                                if (listenerMap.containsKey(roomId)) {
                                    List<RoomListener> listeners = listenerMap.get(roomId);
                                    if (listeners.size() == 0) {
                                        Notifier.executeRoomUpdates(ctx, update);
                                    } else {
                                        for (RoomListener r : listeners) {
                                            r.executeUpdate(ctx, update);
                                        }
                                    }
                                } else {
                                    Notifier.executeRoomUpdates(ctx, update);
                                }
                            }
                        }
                    } catch (JSONException e) {
                        Log.e("Orders", "Error while executing updates");
                    }
                }
            }
        });
    }

    public static void executeUpdates(Context ctx, JSONObject upds) throws JSONException {
        if(upds.has("queueUpdates")) {
            JSONArray queueUpds = upds.getJSONArray("queueUpdates");
            for (int i = 0; i < queueUpds.length(); i++) {
                JSONArray pre = queueUpds.getJSONArray(i);
                for (int j = 0; j < pre.length(); j++) {
                    Notifier.notifyQueueAdded(ctx, pre.getJSONObject(i));
                }
            }
        }
        if(upds.has("updates")) {
            JSONArray mainUpdates = upds.getJSONArray("updates");
            for (int i = 0; i < mainUpdates.length(); i++) {
                JSONArray pre = mainUpdates.getJSONArray(i);
                for (int j = 0; j < pre.length(); j++) {
                    Notifier.executeRoomUpdates(ctx, upds);
                }
            }
        }
    }

    public List<Integer> getRegisteredIds() {
        synchronized (syncKey) {
            List<Integer> observs = new ArrayList<>();
            for (Map.Entry<Integer, List<RoomListener>> it : listenerMap.entrySet()) {
                observs.add(it.getKey());
            }
            return observs;
        }
    }

    public void removeAllListeners() {
        synchronized (syncKey){
            listenerMap = new ConcurrentHashMap<>();
        }
    }
}
