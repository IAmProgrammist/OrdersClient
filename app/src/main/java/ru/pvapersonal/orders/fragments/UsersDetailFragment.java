package ru.pvapersonal.orders.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import ru.pvapersonal.orders.R;
import ru.pvapersonal.orders.activities.LoginActivity;
import ru.pvapersonal.orders.adapters.FullUsersAdapter;
import ru.pvapersonal.orders.model.FullUserItem;
import ru.pvapersonal.orders.model.FullUsersModel;
import ru.pvapersonal.orders.service.SaveSharedPreferences;
import ru.pvapersonal.orders.service.ServerUpdateListener;
import ru.pvapersonal.orders.service.listeners.GeneralEventListener;
import ru.pvapersonal.orders.service.listeners.RoomListener;
import ru.pvapersonal.orders.service.listeners.ServerEventListener;

import static ru.pvapersonal.orders.other.App.URL;

public class UsersDetailFragment extends Fragment {
    ServerUpdateListener updateService;
    int roomId;
    View v;
    private RoomListener roomListener;
    private FullUsersModel mUsers;
    private FullUsersAdapter adapter;

    public UsersDetailFragment(ServerUpdateListener updateService, int roomId) {
        this.updateService = updateService;
        this.roomId = roomId;
        updateService.removeGeneralListener();
    }

    @Override
    public void onResume() {
        if (isAdded()) {
            adapter = new FullUsersAdapter(getActivity());
            v.findViewById(R.id.loader).setVisibility(View.VISIBLE);
            tryToConnect(v);
        }
        super.onResume();
    }

    public void setupGeneralListener() {
        updateService.setGeneraListener(new GeneralEventListener(mUsers.qDate, getActivity()) {
            @Override
            public void roomDeleted(JSONObject delete) throws JSONException {
                if (delete.getInt("roomId") == mUsers.id) {
                    getActivity().finish();
                }
            }

            @Override
            public void roomEdited(JSONObject edit) throws JSONException {
                //Not requiered
            }

            @Override
            public void roomCreated(JSONObject create) throws JSONException {
                //You are already in this room dum dum
            }

            @Override
            public void roomUserExited(JSONObject userExited) throws JSONException {
                if (userExited.getInt("roomId") == mUsers.id) {
                    mUsers.removeUser(userExited.getInt("userId"));
                    modelUpdated();
                }
            }

            @Override
            public void roomUserJoined(JSONObject userJoined) throws JSONException {
                if (userJoined.getInt("roomId") == mUsers.id) {
                    mUsers.addUser(userJoined.getJSONObject("eventData"), userJoined.getBoolean("self"));
                    modelUpdated();
                }
            }

            @Override
            public void roomStatusEdited(JSONObject statusChanged) throws JSONException {
                //Not requiered
            }

            @Override
            public void queueToggle(JSONObject queryToggle) throws JSONException {
                //Not requiered
            }

            @Override
            public void queueAdded(JSONObject queryAdded) throws JSONException {
                //Do not need
            }

            @Override
            public void queueRemoved(JSONObject queryRemoved) throws JSONException {
                //Still no need
            }
        });
    }

    @Override
    public void onStop() {
        super.onStop();
        removeUpdateListener();
    }

    public void removeUpdateListener() {
        if (roomListener != null) {
            updateService.removeRoomUpdateListener(roomListener);
        }
        roomListener = null;
    }

    @Nullable
    @org.jetbrains.annotations.Nullable
    @Override
    public View onCreateView(@NonNull @NotNull LayoutInflater inflater, @Nullable @org.jetbrains.annotations.Nullable ViewGroup container, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        View resultView = inflater.inflate(R.layout.full_room_users_fragment, null);
        v = resultView;
        updateService.removeGeneralListener();
        return resultView;
    }

    @Override
    public void onViewCreated(@NonNull @NotNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tryToConnect(view);
    }

    public void tryToConnect(View v) {
        v.findViewById(R.id.loader).setVisibility(View.VISIBLE);
        updateService.getDetailUsers(SaveSharedPreferences.getUserAccessKey(getActivity()), roomId,
                new ServerEventListener() {
                    @Override
                    public void eventExecuted(int code, String response) {
                        if (code == 200) {
                            try {
                                v.findViewById(R.id.loader).setVisibility(View.GONE);
                                JSONObject res = new JSONObject(response);
                                mUsers = new FullUsersModel(res);
                                LinearLayoutManager llm = new LinearLayoutManager(getActivity());
                                llm.setOrientation(LinearLayoutManager.VERTICAL);
                                RecyclerView recyclerView = v.findViewById(R.id.full_users_list);
                                recyclerView.setLayoutManager(llm);
                                DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(
                                        recyclerView.getContext(), llm.getOrientation());
                                recyclerView.addItemDecoration(dividerItemDecoration);
                                recyclerView.setAdapter(adapter);
                                adapter.updateItems(mUsers.items);
                                adapter.notifyDataSetChanged();
                                setupGeneralListener();
                                roomListener = new RoomListener(roomId, mUsers.qDate) {
                                    @Override
                                    public boolean userJoined(JSONObject userJoined) {
                                        return true;
                                    }

                                    @Override
                                    public boolean userExited(JSONObject userExited) {
                                        return true;
                                    }

                                    @Override
                                    public boolean roomEdited(JSONObject roomEdited) throws JSONException {
                                        return false;
                                    }

                                    @Override
                                    public boolean roomDeleted(JSONObject delete) {
                                        return true;
                                    }

                                    @Override
                                    public boolean statusChanged(JSONObject statusChange) throws JSONException {
                                        return false;
                                    }
                                };
                                addUpdateListener();
                                modelUpdated();
                            } catch (JSONException e) {
                                v.findViewById(R.id.loader).setVisibility(View.GONE);
                                Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        } else if (code == 403) {
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
                        } else if (code == 404 || code == 422 || code == 500) {
                            v.findViewById(R.id.loader).setVisibility(View.GONE);
                            //Did not succeed
                        } else {
                            Toast.makeText(getActivity(), getResources().getString(R.string.connecion_failed), Toast.LENGTH_LONG).show();
                            tryToConnect(v);
                        }
                    }
                }, true);
    }

    private void addUpdateListener() {
        if (roomListener != null) {
            updateService.addRoomUpdateListener(roomId, roomListener);
        }
    }

    private void modelUpdated() {
        View admin = v.findViewById(R.id.admin);
        TextView name = admin.findViewById(R.id.userName);
        ImageView avatar = admin.findViewById(R.id.avatar);
        FullUserItem item = mUsers.admin;
        if(item.name == null){
            name.setText(R.string.you);
        }else{
            name.setText(item.name);
        }
        if(item.image == null || !isAdded()){
            avatar.setImageResource(R.drawable.ic_avatar_empty);
        }else{
            Picasso.get().load(URL + "images/" + item.image).into(avatar);
        }
        if(adapter != null){
            adapter.updateItems(mUsers.items);
        }
        ImageButton phone = admin.findViewById(R.id.call_phone);
        ImageButton whatsapp = admin.findViewById(R.id.call_whatsapp);
        ImageButton viber = admin.findViewById(R.id.call_viber);
        phone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent intent = new Intent(Intent.ACTION_DIAL, item.getPhoneCallUri());
                    startActivity(intent);
                }catch (Exception e){
                    Toast.makeText(getActivity(), R.string.not_found_requiered_apps, Toast.LENGTH_LONG).show();
                }
            }
        });
        whatsapp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, item.getWhatsAppCallUri());
                    startActivity(intent);
                }catch (Exception e){
                    Toast.makeText(getActivity(), R.string.not_found_requiered_apps, Toast.LENGTH_LONG).show();
                }
            }
        });
        viber.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent intent = new Intent("android.intent.action.VIEW");
                    intent.setClassName("com.viber.voip", "com.viber.voip.WelcomeActivity");
                    intent.setData(item.getPhoneCallUri());
                    startActivity(intent);
                }catch (Exception e){
                    Toast.makeText(getActivity(), R.string.not_found_requiered_apps, Toast.LENGTH_LONG).show();
                }
            }
        });
    }
}
