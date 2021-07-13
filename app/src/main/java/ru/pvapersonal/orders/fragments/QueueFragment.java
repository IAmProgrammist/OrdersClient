package ru.pvapersonal.orders.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import ru.pvapersonal.orders.R;
import ru.pvapersonal.orders.activities.LoginActivity;
import ru.pvapersonal.orders.adapters.QueueAdapter;
import ru.pvapersonal.orders.model.QueueItem;
import ru.pvapersonal.orders.service.SaveSharedPreferences;
import ru.pvapersonal.orders.service.ServerUpdateListener;
import ru.pvapersonal.orders.service.listeners.GeneralEventListener;
import ru.pvapersonal.orders.service.listeners.ServerEventListener;

public class QueueFragment extends Fragment {
    ServerUpdateListener updateService;
    QueueAdapter adapter;
    View thisView;
    Long queryDate;
    GeneralEventListener generalEventListener;

    public QueueFragment(ServerUpdateListener updateService) {
        this.updateService = updateService;
        generalEventListener = null;
    }

    @Nullable
    @org.jetbrains.annotations.Nullable
    @Override
    public View onCreateView(@NonNull @NotNull LayoutInflater inflater, @Nullable @org.jetbrains.annotations.Nullable ViewGroup container, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        View resultView = inflater.inflate(R.layout.queue_screen, null);
        resultView.findViewById(R.id.loader).setVisibility(View.VISIBLE);
        thisView = resultView;
        thisView.findViewById(R.id.enter_queue).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                thisView.findViewById(R.id.enter_queue).setEnabled(false);
                updateService.toggleQueue(SaveSharedPreferences.getUserAccessKey(getActivity()),
                        new ServerEventListener() {
                            @Override
                            public void eventExecuted(int code, String response) {
                                if(code != 200){
                                    thisView.findViewById(R.id.enter_queue).setEnabled(true);
                                    Toast.makeText(getActivity(), R.string.app_error, Toast.LENGTH_LONG).show();
                                }
                            }
                        }, true);
            }
        });
        return resultView;
    }

    @Override
    public void onResume() {
        thisView.findViewById(R.id.loader).setVisibility(View.VISIBLE);
        tryToConnect(thisView);
        setupGeneralListener();
        super.onResume();
    }

    public void tryToConnect(View v) {
        updateService.getQueue(SaveSharedPreferences.getUserAccessKey(getActivity()), new ServerEventListener() {
            @Override
            public void eventExecuted(int code, String response) {
                if (code == 500) {
                    v.findViewById(R.id.loader).setVisibility(View.GONE);
                    Toast.makeText(getActivity(), getResources().getString(R.string.server_error), Toast.LENGTH_LONG).show();
                } else if (code == 422) {
                    v.findViewById(R.id.loader).setVisibility(View.GONE);
                    Toast.makeText(getActivity(), getResources().getString(R.string.app_error), Toast.LENGTH_LONG).show();
                } else if (code == 401) {
                    String phone = SaveSharedPreferences.getUserPhone(getActivity());
                    String password = SaveSharedPreferences.getUserPassword(getActivity());
                    if (phone == null || password == null) {
                        Intent loginIntent = new Intent(getActivity(), LoginActivity.class);
                        Toast.makeText(getActivity(),
                                getActivity().getResources().getString(R.string.key_timeout),
                                Toast.LENGTH_LONG).show();
                        getActivity().startActivity(loginIntent);
                        getActivity().finish();
                    } else {
                        updateService.loginQuery(phone, password, new ServerEventListener() {
                            @Override
                            public void eventExecuted(int code, String response) {
                                Intent launchIntent;
                                if (code == 200) {
                                    try {
                                        JSONObject res = new JSONObject(response);
                                        SaveSharedPreferences.setUserAccesskey(getActivity(), res.getString("key"));
                                        updateService.setAccessKey(res.getString("key"));
                                        launchIntent = null;
                                        tryToConnect(v);
                                    } catch (JSONException e) {
                                        Toast.makeText(getActivity(), getResources().getString(R.string.auth_unsuccessful), Toast.LENGTH_LONG).show();
                                        launchIntent = new Intent(getActivity(), LoginActivity.class);
                                    }
                                } else if (code == 401 || code == 422) {
                                    Toast.makeText(getActivity(), getResources().getString(R.string.auth_unsuccessful), Toast.LENGTH_LONG).show();
                                    launchIntent = new Intent(getActivity(), LoginActivity.class);
                                } else {
                                    Toast.makeText(getActivity(), getResources().getString(R.string.connecion_failed), Toast.LENGTH_LONG).show();
                                    tryToConnect(v);
                                    launchIntent = null;
                                }
                                if (launchIntent != null) {
                                    getActivity().startActivity(launchIntent);
                                    getActivity().finish();
                                }
                            }
                        }, true);
                    }
                } else if (code == 200) {
                    try {
                        v.findViewById(R.id.loader).setVisibility(View.GONE);
                        JSONObject resp = new JSONObject(response);
                        JSONArray dataArr = resp.getJSONArray("data");
                        List<QueueItem> queueList = new ArrayList<>();
                        for (int i = 0; i < dataArr.length(); i++) {
                            JSONObject obj = dataArr.getJSONObject(i);
                            QueueItem room = new QueueItem(obj);
                            queueList.add(room);
                        }
                        queryDate = resp.getLong("queryDate");
                        boolean isAdmin = resp.getBoolean("isAdmin");
                        if (isAdmin) {
                            v.findViewById(R.id.enter_queue).setEnabled(false);
                        }
                        adapter = new QueueAdapter(QueueFragment.this.getActivity(),
                                updateService);
                        LinearLayoutManager llm = new LinearLayoutManager(getActivity());
                        llm.setOrientation(LinearLayoutManager.VERTICAL);
                        RecyclerView recyclerView = v.findViewById(R.id.rooms_list);
                        recyclerView.setLayoutManager(llm);
                        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(
                                recyclerView.getContext(), llm.getOrientation());
                        recyclerView.addItemDecoration(dividerItemDecoration);
                        recyclerView.setAdapter(adapter);
                        adapter.setItems(queueList);
                        adapter.notifyDataSetChanged();
                        generalEventListener = new GeneralEventListener(queryDate, getActivity()) {
                            @Override
                            public void roomDeleted(JSONObject delete) throws JSONException {
                                //Do nothing
                            }

                            @Override
                            public void roomEdited(JSONObject edit) throws JSONException {
                                //Do nothing
                            }

                            @Override
                            public void roomCreated(JSONObject create) throws JSONException {
                                //Do nothing
                            }

                            @Override
                            public void roomUserExited(JSONObject userExited) throws JSONException {
                                //Do nothing
                            }

                            @Override
                            public void roomUserJoined(JSONObject userJoined) throws JSONException {
                                //Do nothing
                            }

                            @Override
                            public void roomStatusEdited(JSONObject statusChanged) throws JSONException {
                                //Do nothing
                            }

                            @Override
                            public void queueToggle(JSONObject queryToggle) throws JSONException {
                                if(queryToggle.getBoolean("self")){
                                    thisView.findViewById(R.id.enter_queue).setEnabled(true);
                                }
                                int userId = queryToggle.getInt("userId");
                                boolean toggle = queryToggle.getJSONObject("eventData").getBoolean("toggle");
                                Long date = null;
                                if (queryToggle.getJSONObject("eventData").has("exactDate")) {
                                    date = queryToggle.getJSONObject("eventData").getLong("exactDate");
                                }
                                adapter.toggleItem(userId, toggle, date);
                            }

                            @Override
                            public void queueAdded(JSONObject queryAdded) throws JSONException {
                                if(queryAdded.getBoolean("self")){
                                    thisView.findViewById(R.id.enter_queue).setEnabled(true);
                                }
                                int userId = queryAdded.getInt("userId");
                                String userName = queryAdded.getString("userName");
                                Long exactDate = queryAdded.getJSONObject("eventData").getLong("exactDate");
                                boolean enabled = queryAdded.getJSONObject("eventData").getBoolean("toggle");
                                boolean self = queryAdded.getBoolean("self");
                                QueueItem item = new QueueItem(userName, exactDate, userId, enabled, self);
                                adapter.addItem(item);
                            }

                            @Override
                            public void queueRemoved(JSONObject queryRemoved) throws JSONException {
                                int userId = queryRemoved.getInt("userId");
                                adapter.removeById(userId);
                            }
                        };
                        setupGeneralListener();
                    } catch (JSONException e) {

                    }
                } else {
                    Toast.makeText(getActivity(), getResources().getString(R.string.connecion_failed), Toast.LENGTH_LONG).show();
                    tryToConnect(v);
                }
            }
        }, true);
    }

    private void setupGeneralListener() {
        if(generalEventListener != null) {
            updateService.setGeneraListener(generalEventListener);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}
