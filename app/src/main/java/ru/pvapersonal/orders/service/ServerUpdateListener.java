package ru.pvapersonal.orders.service;

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.SyncHttpClient;
import com.loopj.android.http.TextHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Timer;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import ru.pvapersonal.orders.service.listeners.GeneralEventListener;
import ru.pvapersonal.orders.service.listeners.MainRoomListener;
import ru.pvapersonal.orders.service.listeners.RoomListener;
import ru.pvapersonal.orders.service.listeners.ServerEventListener;

import static ru.pvapersonal.orders.other.App.URL;

public class ServerUpdateListener extends Service {
    private final IBinder binder = new MyBinder();
    private Timer timer;

    private static final String KEY_ARG = "KEY_ARG";
    private static final String PHONE_ARG = "PHONE_ARG";
    private static final String PASSWORD_ARG = "PASSWORD_ARG";

    private static String accessKey;
    private static String phone;
    private static OkHttpClient okHttpClient = null;
    private static boolean shouldRun = true;
    private static String password;
    private static ServerUpdateListener thisService;
    private static final String UPDATES_TAG = "LookForUpdatesTag";
    private static boolean doesThreadWorks = false;
    public static GeneralEventListener generalListener = null;
    private static final MainRoomListener mainRoomListener = new MainRoomListener();

    private final Thread mainThread = new Thread(new Runnable() {
        @Override
        public void run() {
            doesThreadWorks = true;
            try {
                while (shouldRun) {
                    try {
                        if(accessKey != null) {
                            Log.d("Orders", "Still looking...");
                            okHttpClient = new OkHttpClient();
                            String observables = "";
                            List<Integer> registeredRooms = mainRoomListener.getRegisteredIds();
                            for(int i = 0 ; i < registeredRooms.size(); i++){
                                observables += "&observe" + i + "=" + registeredRooms.get(i);
                            }
                            Request request = new Request.Builder()
                                    .url(String.format(URL + "updates" + "?key=%s&includeGeneral=%s" + (!observables.equals("") ? observables : ""),
                                            accessKey, generalListener != null))
                                    .tag(UPDATES_TAG)
                                    .build();
                            final JSONArray[] genUpdates = {null, null};
                            try {
                                try (Response resp = okHttpClient.newCall(request).execute()) {
                                    int code = resp.code();
                                    String responseString = resp.body().string();
                                    if (code / 100 == 2) {
                                        JSONObject updates = new JSONObject(responseString);
                                        if (updates.has("generalUpdates")) {
                                            JSONArray genUpds = updates.getJSONArray("generalUpdates");
                                            genUpdates[0] = genUpds;
                                        } else {
                                            genUpdates[0] = new JSONArray();
                                        }
                                        if (updates.has("queueUpdates")) {
                                            Log.d("Orders", "Queue update lol");
                                            JSONArray queueUpds = updates.getJSONArray("queueUpdates");
                                            genUpdates[1] = queueUpds;
                                        } else {
                                            genUpdates[1] = new JSONArray();
                                        }
                                        if(updates.has("updates")){
                                            mainRoomListener.executeUpdates(ServerUpdateListener.this,
                                                    updates.getJSONArray("updates"));
                                        }
                                        if (generalListener != null && genUpdates[0] != null && genUpdates[1] != null) {
                                            generalListener.executeUpdates(genUpdates[0], genUpdates[1],
                                                    ServerUpdateListener.thisService);
                                        }
                                    } else {

                                    }
                                } catch (IOException e) {
                                    Log.d("Orders", "Cancelled working");
                                }
                            }catch (Exception e){
                                Log.d("Orders", "Null ha-ha");
                            }
                        }
                    } catch (Exception e) {
                        Log.e("Orders", "Couldn't get updates - very bad");
                    }
                }
                doesThreadWorks = false;
            }catch (Exception e){
                doesThreadWorks = false;
            }
        }
    });

    public static void cancelCallWithTag(OkHttpClient client, String tag) {
        for(Call call : client.dispatcher().queuedCalls()) {
            if(call.request().tag().equals(tag))
                call.cancel();
        }
        for(Call call : client.dispatcher().runningCalls()) {
            if(call.request().tag().equals(tag))
                call.cancel();
        }
    }

    public void setGeneraListener(GeneralEventListener listener) {
        ServerUpdateListener.generalListener = listener;
        mainRoomListener.removeAllListeners();
        if(okHttpClient != null){
            cancelCallWithTag(okHttpClient, UPDATES_TAG);
        }
    }

    public void removeGeneralListener() {
        ServerUpdateListener.generalListener = null;
    }

    public ServerUpdateListener() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        thisService = this;
        Log.d("Orders", "Started looking for updates");
        try {
            accessKey = intent.getExtras().getString(KEY_ARG, "");
        } catch (NullPointerException ignored) {
        }
        try {
            phone = intent.getExtras().getString(PHONE_ARG, "");
        } catch (NullPointerException ignored) {
        }
        try {
            password = intent.getExtras().getString(PASSWORD_ARG, "");
        } catch (NullPointerException ignored) {
        }
        if(!doesThreadWorks) {
            mainThread.start();
        }

        return START_REDELIVER_INTENT;
    }

    public void setAccessKey(String accessKey) {
        ServerUpdateListener.accessKey = accessKey;
    }

    public void setPhone(String phone) {
        ServerUpdateListener.phone = phone;
    }

    public void setPassword(String password) {
        ServerUpdateListener.password = password;
    }

    @Override
    public IBinder onBind(Intent intent) {
        //Toast.makeText(this, "On bind", Toast.LENGTH_SHORT).show();
        return binder;
    }

    public void checkForKeyValidness(String key, ServerEventListener serverEventListener, boolean async) {
        AsyncHttpClient client;
        if (async) {
            client = new AsyncHttpClient();
        } else {
            client = new SyncHttpClient();
        }
        RequestParams params = new RequestParams();
        params.put("key", key);
        client.setTimeout(15000);
        params.setContentEncoding("UTF-8");
        client.get(URL + "iskeyactive", params, new TextHttpResponseHandler() {
                    @Override
                    public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, String responseString, Throwable throwable) {
                        serverEventListener.eventExecuted(statusCode, responseString);
                    }

                    @Override
                    public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, String responseString) {
                        serverEventListener.eventExecuted(statusCode, responseString);
                    }
                }
        );
    }

    public void loginQuery(String userPhone, String userPassword, ServerEventListener serverEventListener, boolean async) {
        AsyncHttpClient client;
        if (async) {
            client = new AsyncHttpClient();
        } else {
            client = new SyncHttpClient();
        }
        RequestParams params = new RequestParams();
        params.put("telNumber", userPhone);
        params.put("password", userPassword);
        client.setTimeout(15000);
        params.setContentEncoding("UTF-8");
        client.get(URL + "login", params, new TextHttpResponseHandler() {
                    @Override
                    public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, String responseString, Throwable throwable) {
                        serverEventListener.eventExecuted(statusCode, responseString);
                    }

                    @Override
                    public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, String responseString) {
                        serverEventListener.eventExecuted(statusCode, responseString);
                    }
                }
        );
    }

    public void toggleParticipiance(String accessKey, int roomId, ServerEventListener serverEventListener, boolean async){
        AsyncHttpClient client;
        if (async) {
            client = new AsyncHttpClient();
        } else {
            client = new SyncHttpClient();
        }
        RequestParams params = new RequestParams();
        params.put("key", accessKey);
        params.put("roomId", roomId);
        client.setTimeout(15000);
        params.setContentEncoding("UTF-8");
        client.get(URL + "toggleroompart", params, new TextHttpResponseHandler() {
                    @Override
                    public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, String responseString, Throwable throwable) {
                        serverEventListener.eventExecuted(statusCode, responseString);
                    }

                    @Override
                    public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, String responseString) {
                        serverEventListener.eventExecuted(statusCode, responseString);
                    }
                }
        );
    }

    public void registerQuery(String name, String surName, String middlename, String phoneNum,
                              String password, ServerEventListener serverEventListener, boolean async) throws JSONException,
            UnsupportedEncodingException {
        AsyncHttpClient client;
        if (async) {
            client = new AsyncHttpClient();
        } else {
            client = new SyncHttpClient();
        }
        JSONObject reqBody = new JSONObject();
        reqBody.put("name", name);
        reqBody.put("surname", surName);
        if (!middlename.equals("")) {
            reqBody.put("middlename", middlename);
        }
        reqBody.put("telNumber", phoneNum);
        reqBody.put("password", password);
        StringEntity body = new StringEntity(reqBody.toString(), "UTF-8");
        client.post(this, URL + "register", body,
                "application/json; charset=UTF-8", new TextHttpResponseHandler() {
                    @Override
                    public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                        serverEventListener.eventExecuted(statusCode, responseString);
                    }

                    @Override
                    public void onSuccess(int statusCode, Header[] headers, String responseString) {
                        serverEventListener.eventExecuted(statusCode, responseString);
                    }
                });
    }

    public void setUserImage(String accessKey, Uri image, ServerEventListener serverEventListener, boolean async) throws FileNotFoundException {
        AsyncHttpClient client;
        if (async) {
            client = new AsyncHttpClient();
        } else {
            client = new SyncHttpClient();
        }
        RequestParams requestParams = new RequestParams();
        requestParams.setForceMultipartEntityContentType(true);
        File req = new File(image.getPath());
        requestParams.put("avatar", req);
        requestParams.put("key", accessKey);
        requestParams.setContentEncoding("UTF-8");
        client.post(this, URL + "setuserimage", requestParams, new TextHttpResponseHandler() {
            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                serverEventListener.eventExecuted(statusCode, responseString);
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, String responseString) {
                serverEventListener.eventExecuted(statusCode, responseString);
            }
        });
    }

    public void getUserInfo(String userAccessKey, ServerEventListener serverEventListener, boolean async) {
        AsyncHttpClient client;
        if (async) {
            client = new AsyncHttpClient();
        } else {
            client = new SyncHttpClient();
        }
        RequestParams params = new RequestParams();
        params.put("key", userAccessKey);
        client.setTimeout(15000);
        params.setContentEncoding("UTF-8");
        client.get(URL + "getuserinfo", params, new TextHttpResponseHandler() {
                    @Override
                    public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, String responseString, Throwable throwable) {
                        serverEventListener.eventExecuted(statusCode, responseString);
                    }

                    @Override
                    public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, String responseString) {
                        serverEventListener.eventExecuted(statusCode, responseString);
                    }
                }
        );
    }

    public void getUserInfo(String userAccessKey, int userId, ServerEventListener serverEventListener, boolean async) {
        AsyncHttpClient client;
        if (async) {
            client = new AsyncHttpClient();
        } else {
            client = new SyncHttpClient();
        }
        RequestParams params = new RequestParams();
        params.put("user", userId);
        params.put("key", userAccessKey);
        client.setTimeout(15000);
        params.setContentEncoding("UTF-8");
        client.get(URL + "getuserinfo", params, new TextHttpResponseHandler() {
                    @Override
                    public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, String responseString, Throwable throwable) {
                        serverEventListener.eventExecuted(statusCode, responseString);
                    }

                    @Override
                    public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, String responseString) {
                        serverEventListener.eventExecuted(statusCode, responseString);
                    }
                }
        );
    }

    public void updateUserInfo(String phone, String name, String surName, String middleName,
                               String key, ServerEventListener serverEventListener, boolean async) {
        AsyncHttpClient client;
        if (async) {
            client = new AsyncHttpClient();
        } else {
            client = new SyncHttpClient();
        }
        try {
            JSONObject reqBody = new JSONObject();
            reqBody.put("name", name);
            reqBody.put("surname", surName);
            if (!middleName.equals("")) {
                reqBody.put("middlename", middleName);
            }
            reqBody.put("telNumber", phone);
            reqBody.put("key", key);
            StringEntity body = new StringEntity(reqBody.toString(), "UTF-8");
            client.post(this, URL + "updateuser", body,
                    "application/json; charset=UTF-8", new TextHttpResponseHandler() {
                        @Override
                        public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                            serverEventListener.eventExecuted(statusCode, responseString);
                        }

                        @Override
                        public void onSuccess(int statusCode, Header[] headers, String responseString) {
                            serverEventListener.eventExecuted(statusCode, responseString);
                        }
                    });
        } catch (JSONException e) {
            //what the fuck man
            updateUserInfo(phone, name, surName, middleName, key, serverEventListener, async);
        }
    }

    public void getAllRoomsQuery(String userAccessKey, ServerEventListener serverEventListener, boolean async) {
        AsyncHttpClient client;
        if (async) {
            client = new AsyncHttpClient();
        } else {
            client = new SyncHttpClient();
        }
        RequestParams params = new RequestParams();
        params.put("key", userAccessKey);
        client.setTimeout(15000);
        params.setContentEncoding("UTF-8");
        client.get(URL + "allrooms", params, new TextHttpResponseHandler() {
                    @Override
                    public void onFailure(int statusCode,
                                          cz.msebera.android.httpclient.Header[] headers,
                                          String responseString, Throwable throwable) {
                        serverEventListener.eventExecuted(statusCode, responseString);
                    }

                    @Override
                    public void onSuccess(int statusCode,
                                          cz.msebera.android.httpclient.Header[] headers,
                                          String responseString) {
                        serverEventListener.eventExecuted(statusCode, responseString);
                    }
                }
        );
    }

    public void createRoom(String userAccessKey, String name, String maxMembers, ServerEventListener serverEventListener, boolean async) {
        AsyncHttpClient client;
        if (async) {
            client = new AsyncHttpClient();
        } else {
            client = new SyncHttpClient();
        }
        try {
            JSONObject reqBody = new JSONObject();
            reqBody.put("name", name);
            reqBody.put("maxMembers", maxMembers);
            reqBody.put("accessKey", userAccessKey);
            StringEntity body = new StringEntity(reqBody.toString(), "UTF-8");
            client.post(this, URL + "createroom", body,
                    "application/json; charset=UTF-8", new TextHttpResponseHandler() {
                        @Override
                        public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                            serverEventListener.eventExecuted(statusCode, responseString);
                        }

                        @Override
                        public void onSuccess(int statusCode, Header[] headers, String responseString) {
                            serverEventListener.eventExecuted(statusCode, responseString);
                        }
                    });
        } catch (JSONException e) {
            //what the fuck man
            createRoom(userAccessKey, name, password, serverEventListener, async);
        }
    }

    public void removeRoom(String userAccessKey, int id, ServerEventListener serverEventListener, boolean async) {
        AsyncHttpClient client;
        if (async) {
            client = new AsyncHttpClient();
        } else {
            client = new SyncHttpClient();
        }
        RequestParams params = new RequestParams();
        params.put("key", userAccessKey);
        params.put("roomId", id);
        client.setTimeout(15000);
        params.setContentEncoding("UTF-8");
        client.get(this, URL + "deleteroom", params, new TextHttpResponseHandler() {
            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                serverEventListener.eventExecuted(statusCode, responseString);
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, String responseString) {
                serverEventListener.eventExecuted(statusCode, responseString);
            }
        });
    }

    public void editRoom(String userAccessKey, Integer roomId, String name, String passwordText, String comment, ServerEventListener serverEventListener, boolean async) {
        AsyncHttpClient client;
        if (async) {
            client = new AsyncHttpClient();
        } else {
            client = new SyncHttpClient();
        }
        RequestParams params = new RequestParams();
        params.put("key", userAccessKey);
        params.put("roomId", roomId);
        params.put("name", name);
        if (!passwordText.isEmpty()) {
            params.put("password", passwordText);
        }
        if(!comment.isEmpty()){
            params.put("comment", comment);
        }
        client.setTimeout(15000);
        params.setContentEncoding("UTF-8");
        client.get(this, URL + "editroom", params, new TextHttpResponseHandler() {
            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                serverEventListener.eventExecuted(statusCode, responseString);
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, String responseString) {
                serverEventListener.eventExecuted(statusCode, responseString);
            }
        });
    }

    public void setupRoom(String userAccessKey, Integer roomId, String comment, Long startTime, Long finishTime, int payType, double v, ServerEventListener serverEventListener, boolean async) {
        AsyncHttpClient client;
        if (async) {
            client = new AsyncHttpClient();
        } else {
            client = new SyncHttpClient();
        }
        RequestParams params = new RequestParams();
        params.put("key", userAccessKey);
        params.put("roomId", roomId);
        params.put("start", startTime);
        params.put("end", finishTime);
        if (!comment.trim().isEmpty()) {
            params.put("comment", comment.trim());
        }
        params.put("payType", payType);
        params.put("payVal", v);
        client.setTimeout(15000);
        params.setContentEncoding("UTF-8");
        client.get(this, URL + "setuproom", params, new TextHttpResponseHandler() {
            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                serverEventListener.eventExecuted(statusCode, responseString);
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, String responseString) {
                serverEventListener.eventExecuted(statusCode, responseString);
            }
        });
    }

    public void getQueue(String userAccessKey, ServerEventListener serverEventListener, boolean async) {
        AsyncHttpClient client;
        if (async) {
            client = new AsyncHttpClient();
        } else {
            client = new SyncHttpClient();
        }
        RequestParams params = new RequestParams();
        params.put("key", userAccessKey);
        client.setTimeout(15000);
        params.setContentEncoding("UTF-8");
        client.get(this, URL + "getqueue", params, new TextHttpResponseHandler() {
            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                serverEventListener.eventExecuted(statusCode, responseString);
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, String responseString) {
                serverEventListener.eventExecuted(statusCode, responseString);
            }
        });
    }

    public void toggleQueue(String userAccessKey, ServerEventListener serverEventListener, boolean async) {
        AsyncHttpClient client;
        if (async) {
            client = new AsyncHttpClient();
        } else {
            client = new SyncHttpClient();
        }
        RequestParams params = new RequestParams();
        params.put("key", userAccessKey);
        client.setTimeout(15000);
        params.setContentEncoding("UTF-8");
        client.get(this, URL + "querysetter", params, new TextHttpResponseHandler() {
            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                serverEventListener.eventExecuted(statusCode, responseString);
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, String responseString) {
                serverEventListener.eventExecuted(statusCode, responseString);
            }
        });
    }

    public void getDetailRoom(String userAccessKey, int roomId, ServerEventListener serverEventListener, boolean async) {
        AsyncHttpClient client;
        if (async) {
            client = new AsyncHttpClient();
        } else {
            client = new SyncHttpClient();
        }
        RequestParams params = new RequestParams();
        params.put("key", userAccessKey);
        params.put("roomId", roomId);
        params.put("method", 0);
        client.setTimeout(15000);
        params.setContentEncoding("UTF-8");
        client.get(this, URL + "detailroom", params, new TextHttpResponseHandler() {
            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                serverEventListener.eventExecuted(statusCode, responseString);
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, String responseString) {
                serverEventListener.eventExecuted(statusCode, responseString);
            }
        });
    }

    public void getDetailUsers(String userAccessKey, int roomId, ServerEventListener serverEventListener, boolean async) {
        AsyncHttpClient client;
        if (async) {
            client = new AsyncHttpClient();
        } else {
            client = new SyncHttpClient();
        }
        RequestParams params = new RequestParams();
        params.put("key", userAccessKey);
        params.put("roomId", roomId);
        params.put("method", 1);
        client.setTimeout(15000);
        params.setContentEncoding("UTF-8");
        client.get(this, URL + "detailroom", params, new TextHttpResponseHandler() {
            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                serverEventListener.eventExecuted(statusCode, responseString);
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, String responseString) {
                serverEventListener.eventExecuted(statusCode, responseString);
            }
        });
    }

    public void getPayHistory(String key, ServerEventListener serverEventListener, boolean async) {
        AsyncHttpClient client;
        if (async) {
            client = new AsyncHttpClient();
        } else {
            client = new SyncHttpClient();
        }
        RequestParams params = new RequestParams();
        params.put("key", key);
        client.setTimeout(15000);
        params.setContentEncoding("UTF-8");
        client.get(this, URL + "transhist", params, new TextHttpResponseHandler() {
            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                serverEventListener.eventExecuted(statusCode, responseString);
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, String responseString) {
                serverEventListener.eventExecuted(statusCode, responseString);
            }
        });
    }

    public void acceptRoom(String key, int roomId, ServerEventListener serverEventListener, boolean async){
        AsyncHttpClient client;
        if (async) {
            client = new AsyncHttpClient();
        } else {
            client = new SyncHttpClient();
        }
        RequestParams params = new RequestParams();
        params.put("key", key);
        params.put("roomId", roomId);
        params.put("method", 0);
        client.setTimeout(15000);
        params.setContentEncoding("UTF-8");
        client.get(this, URL + "acceptdeny", params, new TextHttpResponseHandler() {
            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                serverEventListener.eventExecuted(statusCode, responseString);
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, String responseString) {
                serverEventListener.eventExecuted(statusCode, responseString);
            }
        });
    }

    public void denyRoom(String key, int roomId, ServerEventListener serverEventListener, boolean async){
        AsyncHttpClient client;
        if (async) {
            client = new AsyncHttpClient();
        } else {
            client = new SyncHttpClient();
        }
        RequestParams params = new RequestParams();
        params.put("key", key);
        params.put("roomId", roomId);
        params.put("method", 1);
        client.setTimeout(15000);
        params.setContentEncoding("UTF-8");
        client.get(this, URL + "acceptdeny", params, new TextHttpResponseHandler() {
            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                serverEventListener.eventExecuted(statusCode, responseString);
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, String responseString) {
                serverEventListener.eventExecuted(statusCode, responseString);
            }
        });
    }

    public void removeRoomUpdateListener(RoomListener listener) {
        mainRoomListener.removeListener(listener);
    }

    public void addRoomUpdateListener(int roomId, RoomListener listener){
        mainRoomListener.addListener(roomId, listener);
    }

    public void getActiveUsers(String accessKey, ServerEventListener serverEventListener, boolean async) {
        AsyncHttpClient client;
        if (async) {
            client = new AsyncHttpClient();
        } else {
            client = new SyncHttpClient();
        }
        RequestParams params = new RequestParams();
        params.put("key", accessKey);
        client.setTimeout(15000);
        params.setContentEncoding("UTF-8");
        client.get(this, URL + "getactiveusers", params, new TextHttpResponseHandler() {
            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                serverEventListener.eventExecuted(statusCode, responseString);
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, String responseString) {
                serverEventListener.eventExecuted(statusCode, responseString);
            }
        });
    }

    // create an inner Binder class
    public class MyBinder extends Binder {
        public ServerUpdateListener getService() {
            return ServerUpdateListener.this;
        }
    }

    @Override
    public void onDestroy() {
        shouldRun = false;
        super.onDestroy();

        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(MainBroadcastReceiver.RESTART_SERVICE);
        broadcastIntent.setClass(this, MainBroadcastReceiver.class);
        this.sendBroadcast(broadcastIntent);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        shouldRun = false;
        super.onTaskRemoved(rootIntent);

        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(MainBroadcastReceiver.RESTART_SERVICE);
        broadcastIntent.setClass(this, MainBroadcastReceiver.class);
        this.sendBroadcast(broadcastIntent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        //Toast.makeText(this, "On unbinded", Toast.LENGTH_SHORT).show();
        return super.onUnbind(intent);
    }
}
