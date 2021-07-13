package ru.pvapersonal.orders.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputLayout;
import com.google.zxing.client.android.Intents;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.nambimobile.widgets.efab.FabOption;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import ru.pvapersonal.orders.R;
import ru.pvapersonal.orders.activities.DetailActivity;
import ru.pvapersonal.orders.activities.LoginActivity;
import ru.pvapersonal.orders.activities.SetupRoomActivity;
import ru.pvapersonal.orders.adapters.RoomsAdapter;
import ru.pvapersonal.orders.adapters.RoomsAdapterTypes;
import ru.pvapersonal.orders.model.Room;
import ru.pvapersonal.orders.model.Status;
import ru.pvapersonal.orders.service.SaveSharedPreferences;
import ru.pvapersonal.orders.service.ServerUpdateListener;
import ru.pvapersonal.orders.service.listeners.GeneralEventListener;
import ru.pvapersonal.orders.service.listeners.ServerEventListener;

public class RoomsFragment extends Fragment {
    ServerUpdateListener updateService;
    RoomsAdapter adapterAdmin;
    RoomsAdapter adapterMember;
    RoomsAdapter adapterNotMember;
    View thisView;
    Long queryDate;

    public RoomsFragment(ServerUpdateListener updateService) {
        this.updateService = updateService;
    }

    @Nullable
    @org.jetbrains.annotations.Nullable
    @Override
    public View onCreateView(@NonNull @NotNull LayoutInflater inflater, @Nullable @org.jetbrains.annotations.Nullable ViewGroup container, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        View resultView = inflater.inflate(R.layout.rooms_screen, null);
        resultView.findViewById(R.id.loader).setVisibility(View.VISIBLE);
        ((SearchView) resultView.findViewById(R.id.searcher)).setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (adapterAdmin != null) {
                    adapterAdmin.filter(query);
                }
                if(adapterMember != null){
                    adapterMember.filter(query);
                }
                if(adapterNotMember != null){
                    adapterNotMember.filter(query);
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String query) {
                if (adapterAdmin != null) {
                    adapterAdmin.filter(query);
                }
                if(adapterMember != null){
                    adapterMember.filter(query);
                }
                if(adapterNotMember != null){
                    adapterNotMember.filter(query);
                }
                return true;
            }
        });
        resultView.findViewById(R.id.create_fab).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(getResources().getString(R.string.create_room));
                View createView = LayoutInflater.from(getContext()).inflate(R.layout.create_room_dialog, null);
                createView.findViewById(R.id.comment_til).setVisibility(View.GONE);
                builder.setView(createView);
                builder.setPositiveButton(R.string.create_room, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });

                builder.setNegativeButton(R.string.cancel, (dialog, which) -> {
                    dialog.cancel();
                });
                final AlertDialog dialog = builder.create();
                dialog.show();
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        TextInputLayout nameTil = createView.findViewById(R.id.room_name_til);
                        TextInputLayout maxMembersTil = createView.findViewById(R.id.room_maxmembers_til);
                        nameTil.setError(null);
                        maxMembersTil.setError(null);
                        EditText nameEditText = createView.findViewById(R.id.room_name_edittext);
                        EditText maxMembersEditText = createView.findViewById(R.id.room_maxmembers_edittext);
                        String nameText = nameEditText.getText().toString().trim();
                        String maxMembersText = maxMembersEditText.getText().toString();
                        boolean isValid = true;
                        if (nameText.isEmpty()) {
                            nameTil.setError(getResources().getString(R.string.roomname_empty));
                            isValid = false;
                        }
                        if (!TextUtils.isEmpty(maxMembersText) && TextUtils.isDigitsOnly(maxMembersText)) {
                            int maxMembers = Integer.parseInt(maxMembersText);
                            if (maxMembers < 1) {
                                maxMembersTil.setError(getResources().getString(R.string.number_is_too_small));
                                isValid = false;
                            }
                        } else {
                            maxMembersTil.setError(getResources().getString(R.string.not_a_number));
                            isValid = false;
                        }
                        if (isValid) {
                            dialog.cancel();
                            tryToConnect(nameText, maxMembersText, dialog);
                        }
                    }
                });
            }

            public void tryToConnect(String name, String maxMembers, DialogInterface dialog) {
                updateService.createRoom(SaveSharedPreferences.getUserAccessKey(getContext()),
                        name, maxMembers, new ServerEventListener() {
                            @Override
                            public void eventExecuted(int code, String response) {
                                if (code == 200) {
                                    //Update searcher will handle everything
                                    dialog.cancel();
                                } else if (code == 422) {
                                    Toast.makeText(getContext(),
                                            getResources().getString(R.string.server_error),
                                            Toast.LENGTH_LONG).show();
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
                                                        tryToConnect(name, maxMembers, dialog);
                                                    } catch (JSONException e) {
                                                        Toast.makeText(getActivity(), getResources().getString(R.string.auth_unsuccessful), Toast.LENGTH_LONG).show();
                                                        launchIntent = new Intent(getActivity(), LoginActivity.class);
                                                    }
                                                } else if (code == 401 || code == 422) {
                                                    Toast.makeText(getActivity(), getResources().getString(R.string.auth_unsuccessful), Toast.LENGTH_LONG).show();
                                                    launchIntent = new Intent(getActivity(), LoginActivity.class);
                                                } else {
                                                    Toast.makeText(getActivity(), getResources().getString(R.string.connecion_failed), Toast.LENGTH_LONG).show();
                                                    Toast.makeText(getContext(),
                                                            getResources().getString(R.string.room_fail_connection),
                                                            Toast.LENGTH_LONG).show();
                                                    launchIntent = null;
                                                }
                                                if (launchIntent != null) {
                                                    getActivity().startActivity(launchIntent);
                                                    getActivity().finish();
                                                }
                                            }
                                        }, true);
                                    }
                                } else if (code == 500) {
                                    Toast.makeText(getContext(),
                                            getResources().getString(R.string.server_error),
                                            Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(getContext(),
                                            getResources().getString(R.string.room_fail_connection),
                                            Toast.LENGTH_LONG).show();
                                }
                            }
                        }, true);
            }
        });
        resultView.findViewById(R.id.join_fab).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(getResources().getString(R.string.enter_room));
                View joinView = LayoutInflater.from(getContext()).inflate(R.layout.join_room_dialog_first, null);

                builder.setView(joinView);
                builder.setPositiveButton(R.string.enter, (dialog, which) -> {
                });
                builder.setNegativeButton(R.string.cancel, (dialog, which) -> {
                    dialog.cancel();
                });
                final AlertDialog dialog = builder.create();
                dialog.show();
                joinView.findViewById(R.id.scan_qr).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.cancel();
                        IntentIntegrator.forSupportFragment(RoomsFragment.this)
                                .setOrientationLocked(false)
                                .setDesiredBarcodeFormats(IntentIntegrator.QR_CODE).initiateScan();
                    }
                });
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        TextInputLayout idTil = joinView.findViewById(R.id.room_join_id_til);
                        //TextInputLayout passwordTil = joinView.findViewById(R.id.room_password_til);
                        idTil.setError(null);
                        EditText idEditText = joinView.findViewById(R.id.room_join_id);
                        //EditText passwordEditText = joinView.findViewById(R.id.room_password_edittext);
                        String idText = idEditText.getText().toString();
                        //String passwordText = passwordEditText.getText().toString();
                        boolean isValid = true;
                        if (idText.isEmpty() || !isNumber(idText)) {
                            idTil.setError(getResources().getString(R.string.room_id_invalid));
                            isValid = false;
                        }
                        thisView.findViewById(R.id.loader).setVisibility(View.VISIBLE);
                        if (isValid) {
                            Intent launch = new Intent(getActivity(), DetailActivity.class);
                            launch.putExtra(DetailActivity.ROOM_ID, Integer.parseInt(idText));
                            getActivity().startActivity(launch);
                        }
                    }
                });
            }
        });
        thisView = resultView;
        return resultView;
    }

    public boolean isNumber(String a) {
        try {
            Integer.parseInt(a);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable @org.jetbrains.annotations.Nullable Intent data) {
        if (requestCode != IntentIntegrator.REQUEST_CODE) {
            // This is important, otherwise the result will not be passed to the fragment
            super.onActivityResult(requestCode, resultCode, data);
            return;
        }

        IntentResult result = IntentIntegrator.parseActivityResult(resultCode, data);

        if (result.getContents() == null) {
            Intent originalIntent = result.getOriginalIntent();
            if (originalIntent == null) {
                //Log.d("MainActivity", "Cancelled scan");
            } else if (originalIntent.hasExtra(Intents.Scan.MISSING_CAMERA_PERMISSION)) {
                //Log.d("MainActivity", "Cancelled scan due to missing camera permission");
            }
        } else {
            //Toast.makeText(this, "Scanned: " + result.getContents(), Toast.LENGTH_LONG).show();
            try {
                JSONObject resultScan = new JSONObject(result.getContents());
                thisView.findViewById(R.id.loader).setVisibility(View.VISIBLE);
                if(resultScan.has("id")){
                    int roomId = resultScan.getInt("id");
                    Intent launch = new Intent(getActivity(), DetailActivity.class);
                    launch.putExtra(DetailActivity.ROOM_ID, roomId);
                    startActivity(launch);
                }
            } catch (JSONException e) {
                Toast.makeText(getActivity(), getResources().getString(R.string.scan_error), Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onResume() {
        if(isAdded()) {
            thisView.findViewById(R.id.loader).setVisibility(View.VISIBLE);
            tryToConnect(thisView);
        }
        super.onResume();
    }

    private void setupGeneralListener() {
        updateService.setGeneraListener(new GeneralEventListener(queryDate,
                getActivity()) {
            @Override
            public void roomDeleted(JSONObject delete) throws JSONException {
                int roomId = delete.getInt("roomId");
                adapterAdmin.deleteRoomByID(roomId);
                adapterMember.deleteRoomByID(roomId);
                adapterNotMember.deleteRoomByID(roomId);
            }

            @Override
            public void roomEdited(JSONObject edit) throws JSONException {
                int roomId = edit.getInt("roomId");
                adapterAdmin.editRoom(roomId, edit.getJSONObject("eventData"));
                adapterMember.editRoom(roomId, edit.getJSONObject("eventData"));
                adapterNotMember.editRoom(roomId, edit.getJSONObject("eventData"));
            }

            @Override
            public void roomCreated(JSONObject create) throws JSONException {
                int roomId = create.getInt("roomId");
                Room room = new Room(create.getJSONObject("eventData"));
                boolean duplicate = adapterAdmin.add(room) & adapterMember.add(room)
                        & adapterNotMember.add(room);
                if(room.isAdmin && !duplicate) {
                    Intent launch = new Intent(getActivity(), SetupRoomActivity.class);
                    launch.putExtra(SetupRoomActivity.ROOM_ID, roomId);
                    launch.putExtra(SetupRoomActivity.MAX_MEMBERS, room.maxMembers);
                    getContext().startActivity(launch);
                }
            }

            @Override
            public void roomUserExited(JSONObject userExited) throws JSONException {
                int roomId = userExited.getInt("roomId");
                adapterAdmin.userExited(roomId, userExited.getBoolean("self"));
                adapterMember.userExited(roomId, userExited.getBoolean("self"));
                adapterNotMember.userExited(roomId, userExited.getBoolean("self"));
                //Fuck this, just want to finish this already
            }

            @Override
            public void roomUserJoined(JSONObject userJoined) throws JSONException {
                int roomId = userJoined.getInt("roomId");
                adapterAdmin.userAdd(roomId, userJoined.getBoolean("self"));
                adapterMember.userAdd(roomId, userJoined.getBoolean("self"));
                adapterNotMember.userAdd(roomId, userJoined.getBoolean("self"));
            }

            @Override
            public void roomStatusEdited(JSONObject statusChanged) throws JSONException {
                int roomId = statusChanged.getInt("roomId");
                Status st = Status.valueOf(statusChanged
                        .getJSONObject("eventData")
                        .getString("status"));
                Long start = null;
                if(statusChanged.getJSONObject("eventData").has("start")){
                    start = statusChanged.getJSONObject("eventData").getLong("start");
                }
                Long end = null;
                if(statusChanged.getJSONObject("eventData").has("end")){
                    start = statusChanged.getJSONObject("eventData").getLong("end");
                }
                adapterAdmin.changeStatus(roomId, st, start, end);
                adapterMember.changeStatus(roomId, st, start, end);
                adapterNotMember.changeStatus(roomId, st, start, end);
                //Just nevermind okay?
            }

            @Override
            public void queueToggle(JSONObject queryToggle) throws JSONException {
                //Do nothing
            }

            @Override
            public void queueAdded(JSONObject queryAdded) throws JSONException {
                //Do nothing
            }

            @Override
            public void queueRemoved(JSONObject queryRemoved) throws JSONException {
                //Do nothing
            }
        });

    }

    public void tryToConnect(View v) {
        updateService.getAllRoomsQuery(SaveSharedPreferences.getUserAccessKey(getContext()),
                new ServerEventListener() {
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
                                List<Room> roomsList = new ArrayList<>();
                                for (int i = 0; i < dataArr.length(); i++) {
                                    JSONObject obj = dataArr.getJSONObject(i);
                                    Room room = new Room(obj);
                                    roomsList.add(room);
                                }
                                queryDate = resp.getLong("queryDate");
                                boolean isAdmin = resp.getBoolean("isAdmin");
                                if (isAdmin) {
                                    ((FabOption)v.findViewById(R.id.join_fab)).setFabOptionEnabled(false);
                                } else {
                                    ((FabOption)v.findViewById(R.id.create_fab)).setFabOptionEnabled(false);
                                }
                                adapterAdmin = new RoomsAdapter(RoomsFragment.this.getActivity(),
                                        updateService, isAdmin, RoomsAdapterTypes.ADMIN, v.findViewById(R.id.header_adm_cont));
                                adapterMember = new RoomsAdapter(RoomsFragment.this.getActivity(),
                                        updateService, isAdmin, RoomsAdapterTypes.MEMBER,v.findViewById(R.id.header_mem_cont));
                                adapterNotMember = new RoomsAdapter(RoomsFragment.this.getActivity(),
                                        updateService, isAdmin, RoomsAdapterTypes.NOT_MEMBER, v.findViewById(R.id.header_not_mem_cont));
                                LinearLayoutManager llm = new LinearLayoutManager(getActivity());
                                llm.setOrientation(LinearLayoutManager.VERTICAL);
                                LinearLayoutManager llm2 = new LinearLayoutManager(getActivity());
                                llm.setOrientation(LinearLayoutManager.VERTICAL);
                                LinearLayoutManager llm3 = new LinearLayoutManager(getActivity());
                                llm.setOrientation(LinearLayoutManager.VERTICAL);
                                RecyclerView recyclerView = v.findViewById(R.id.rooms_list_admin);
                                RecyclerView recyclerView2 = v.findViewById(R.id.rooms_list_member);
                                RecyclerView recyclerView3 = v.findViewById(R.id.rooms_list_not_member);
                                recyclerView.setLayoutManager(llm);
                                recyclerView2.setLayoutManager(llm2);
                                recyclerView3.setLayoutManager(llm3);
                                adapterAdmin.setItems(roomsList);
                                recyclerView.setAdapter(adapterAdmin);
                                adapterAdmin.notifyDataSetChanged();
                                adapterMember.setItems(roomsList);
                                recyclerView2.setAdapter(adapterMember);
                                adapterMember.notifyDataSetChanged();
                                adapterNotMember.setItems(roomsList);
                                recyclerView3.setAdapter(adapterNotMember);
                                adapterNotMember.notifyDataSetChanged();
                                //I dont fucking now why, but only after these lines everything works... It doesn't makes any sense to me :/
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }


}
