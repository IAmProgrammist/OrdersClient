package ru.pvapersonal.orders.fragments;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputLayout;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;

import ru.pvapersonal.orders.R;
import ru.pvapersonal.orders.activities.LoginActivity;
import ru.pvapersonal.orders.activities.QRCodeActivity;
import ru.pvapersonal.orders.activities.UserInfoActivity;
import ru.pvapersonal.orders.adapters.AvatarsAdapter;
import ru.pvapersonal.orders.model.RoomFull;
import ru.pvapersonal.orders.model.Status;
import ru.pvapersonal.orders.service.SaveSharedPreferences;
import ru.pvapersonal.orders.service.ServerUpdateListener;
import ru.pvapersonal.orders.service.listeners.GeneralEventListener;
import ru.pvapersonal.orders.service.listeners.RoomListener;
import ru.pvapersonal.orders.service.listeners.ServerEventListener;

public class RoomDetailFragment extends Fragment {
    ServerUpdateListener updateService;
    int roomId;
    View v;
    private static Timer timer = null;
    private CustomTimerTask current = null;
    private RoomFull mRoom;

    private static class CustomTimerTask extends TimerTask {

        private Long delta;
        private Long until;
        private View v;

        protected CustomTimerTask(Long delta, Long until, View thisView) {
            super();
            this.delta = delta;
            this.v = thisView;
            this.until = until;
            timer = new Timer();
        }

        @Override
        public void run() {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Long nowFixed = System.currentTimeMillis() - delta;
                    long diff = until - nowFixed;
                    if (diff > 0) {
                        TextView timerTxt = v.findViewById(R.id.room_timer);
                        Long hours = diff / 3600000L;
                        long minutes = diff % 3600000L / 60000L;
                        long seconds = diff % 3600000L % 60000L / 1000;
                        timerTxt.setText((String.valueOf(hours).length() == 1 ? "0" : "") + hours
                                + ":" + (String.valueOf(minutes).length() == 1 ? "0" : "")
                                + minutes + ":" + (String.valueOf(seconds).length() == 1 ? "0" : "")
                                + seconds);
                    }
                }
            });
        }
    }

    private RoomListener roomListener;

    public RoomDetailFragment(ServerUpdateListener updateService, int roomId) {
        this.updateService = updateService;
        this.roomId = roomId;
        updateService.removeGeneralListener();
    }

    @Override
    public void onResume() {
        if (isAdded()) {
            v.findViewById(R.id.loader).setVisibility(View.VISIBLE);
            tryToConnect(v);
        }
        super.onResume();
    }

    public void setupGeneralListener() {
        updateService.setGeneraListener(new GeneralEventListener(mRoom.qDate, getActivity()) {
            @Override
            public void roomDeleted(JSONObject delete) throws JSONException {
                if (delete.getInt("roomId") == mRoom.id) {
                    getActivity().finish();
                }
            }

            @Override
            public void roomEdited(JSONObject edit) throws JSONException {
                if (edit.getInt("roomId") == mRoom.id) {
                    mRoom.roomName = edit.getJSONObject("eventData").getString("name");
                    mRoom.passworded = edit.getJSONObject("eventData").getBoolean("isLocked");
                    if(edit.getJSONObject("eventData").has("comment")){
                        mRoom.comment = edit.getJSONObject("eventData").getString("comment");
                    }else{
                        mRoom.comment = null;
                    }
                    modelUpdated();
                }
            }

            @Override
            public void roomCreated(JSONObject create) throws JSONException {
                //You are already in this room dum dum
            }

            @Override
            public void roomUserExited(JSONObject userExited) throws JSONException {
                if (userExited.getInt("roomId") == mRoom.id) {
                    mRoom.removeMember(userExited.getInt("userId"));
                    if (userExited.getBoolean("self")) {
                        mRoom.shouldShowExpandedKeyboard = false;
                        v.findViewById(R.id.enter_exit_room).setEnabled(true);
                        mRoom.participiantType = 0;
                    }
                    modelUpdated();
                }
            }

            @Override
            public void roomUserJoined(JSONObject userJoined) throws JSONException {
                if (userJoined.getInt("roomId") == mRoom.id) {
                    mRoom.addMember(userJoined.getJSONObject("eventData"),
                            userJoined.getBoolean("self"));
                    if (userJoined.getBoolean("self")) {
                        mRoom.shouldShowExpandedKeyboard = false;
                        v.findViewById(R.id.enter_exit_room).setEnabled(true);
                        mRoom.participiantType = 1;
                    }
                    modelUpdated();
                }
            }

            @Override
            public void roomStatusEdited(JSONObject statusChanged) throws JSONException {
                if (statusChanged.getInt("roomId") == mRoom.id) {
                    mRoom.st = Status.valueOf(statusChanged.getJSONObject("eventData").getString("status"));
                    if (statusChanged.getJSONObject("eventData").has("start")) {
                        mRoom.start = statusChanged.getJSONObject("eventData").getLong("start");
                    }
                    if (statusChanged.getJSONObject("eventData").has("end")) {
                        mRoom.end = statusChanged.getJSONObject("eventData").getLong("end");
                    }
                    if (statusChanged.getJSONObject("eventData").has("payType")) {
                        mRoom.transType = statusChanged.getJSONObject("eventData").getInt("payType");
                    }
                    if (statusChanged.getJSONObject("eventData").has("payVal")) {
                        mRoom.transVal = statusChanged.getJSONObject("eventData").getInt("payVal");
                    }
                    modelUpdated();
                }
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

    private AvatarsAdapter adapter = null;

    @Nullable
    @org.jetbrains.annotations.Nullable
    @Override
    public View onCreateView(@NonNull @NotNull LayoutInflater inflater, @Nullable @org.jetbrains.annotations.Nullable ViewGroup container, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        View resultView = inflater.inflate(R.layout.full_room_info_fragment, null);
        v = resultView;
        updateService.removeGeneralListener();
        v.findViewById(R.id.qrcode_room_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mRoom != null) {
                    Intent intent = new Intent(getActivity(), QRCodeActivity.class);
                    intent.putExtra(QRCodeActivity.ROOM_ID, mRoom.id);
                    startActivity(intent);
                }
            }
        });
        v.findViewById(R.id.edit_room_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mRoom != null) {
                    int rId = mRoom.id;
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle(R.string.create_room);
                    View createView = LayoutInflater.from(getActivity()).inflate(R.layout.create_room_dialog, null);
                    createView.findViewById(R.id.room_maxmembers_til).setVisibility(View.GONE);
                    ((EditText) createView.findViewById(R.id.room_name_edittext)).setText(mRoom.roomName);
                    if (mRoom.comment != null) {
                        ((EditText) createView.findViewById(R.id.comment_edittext)).setText(mRoom.comment);
                    }
                    //((TextView)createView.findViewById(R.id.textView2)).setText(R.string.roomname_additionalinfo_edit);
                    builder.setView(createView);
                    builder.setPositiveButton(R.string.edit_room_text, new DialogInterface.OnClickListener() {
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
                            //TextInputLayout passwordTil = createView.findViewById(R.id.room_password_til);
                            nameTil.setError(null);
                            EditText commentText = createView.findViewById(R.id.comment_edittext);
                            //passwordTil.setError(null);
                            EditText nameEditText = createView.findViewById(R.id.room_name_edittext);
                            //EditText passwordEditText = createView.findViewById(R.id.room_password_edittext);
                            String nameText = nameEditText.getText().toString().trim();
                            //String passwordText = passwordEditText.getText().toString();
                            boolean isValid = true;
                            if (nameText.isEmpty()) {
                                nameTil.setError(getString(R.string.roomname_empty));
                                isValid = false;
                            }
                            //if (passwordText.length() > 0 && passwordText.length() < 6) {
                            //    passwordTil.setError(ctx.getString(R.string.password_incorrect));
                            //    isValid = false;
                            //}
                            if (isValid) {
                                dialog.cancel();
                                updateService.editRoom(SaveSharedPreferences.getUserAccessKey(getActivity()),
                                        rId, nameText.trim(), /*passwordText*/ "", commentText.getText().toString().trim(), new ServerEventListener() {
                                            @Override
                                            public void eventExecuted(int code, String response) {
                                                if (code != 200) {
                                                    Toast.makeText(getActivity(), R.string.failed_to_edit_room, Toast.LENGTH_LONG).show();
                                                }
                                            }
                                        }, true);
                            }
                        }
                    });
                }
            }
        });
        v.findViewById(R.id.delete_room_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mRoom != null) {
                    int rId = mRoom.id;
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle(getResources().getString(R.string.are_you_sure));
                    builder.setMessage(getResources().getString(R.string.delete_room_desc));
                    builder.setPositiveButton(R.string.remove, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            updateService.removeRoom(SaveSharedPreferences.getUserAccessKey(getActivity()),
                                    rId, new ServerEventListener() {
                                        @Override
                                        public void eventExecuted(int code, String response) {
                                            if (code != 200) {
                                                Toast.makeText(getActivity(), R.string.failed_to_remove_room, Toast.LENGTH_LONG).show();
                                            }
                                        }
                                    }, true);
                        }
                    });
                    builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    });
                    builder.create().show();
                }
            }
        });
        LinearLayoutManager llm = new LinearLayoutManager(getActivity());
        llm.setOrientation(LinearLayoutManager.HORIZONTAL);
        RecyclerView recyclerView = v.findViewById(R.id.full_users_list);
        recyclerView.setLayoutManager(llm);
        adapter = new AvatarsAdapter(getActivity());
        recyclerView.setAdapter(adapter);
        return resultView;
    }

    @Override
    public void onViewCreated(@NonNull @NotNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tryToConnect(view);
        view.findViewById(R.id.enter_exit_room).setOnClickListener(v -> {
            view.findViewById(R.id.enter_exit_room).setEnabled(false);
            updateService.toggleParticipiance(SaveSharedPreferences.getUserAccessKey(getActivity()),
                    roomId, new ServerEventListener() {
                        @Override
                        public void eventExecuted(int code, String response) {
                            if (code != 200) {
                                view.findViewById(R.id.enter_exit_room).setEnabled(true);
                                Toast.makeText(getActivity(), R.string.app_error, Toast.LENGTH_LONG).show();
                            }
                        }
                    }, true);

        });
        view.findViewById(R.id.accept).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                view.findViewById(R.id.accept).setEnabled(false);
                view.findViewById(R.id.cancel).setEnabled(false);
                updateService.acceptRoom(SaveSharedPreferences.getUserAccessKey(getActivity()), roomId, new ServerEventListener() {
                    @Override
                    public void eventExecuted(int code, String response) {
                        if(code != 200){
                            view.findViewById(R.id.accept).setEnabled(true);
                            view.findViewById(R.id.cancel).setEnabled(true);
                            Toast.makeText(getContext(), getString(R.string.app_error), Toast.LENGTH_LONG).show();
                        }
                    }
                }, true);
            }
        });
        view.findViewById(R.id.cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                view.findViewById(R.id.accept).setEnabled(false);
                view.findViewById(R.id.cancel).setEnabled(false);
                updateService.denyRoom(SaveSharedPreferences.getUserAccessKey(getActivity()), roomId, new ServerEventListener() {
                    @Override
                    public void eventExecuted(int code, String response) {
                        if(code != 200){
                            view.findViewById(R.id.accept).setEnabled(true);
                            view.findViewById(R.id.cancel).setEnabled(true);
                            Toast.makeText(getContext(), getString(R.string.app_error), Toast.LENGTH_LONG).show();
                        }else{
                            getActivity().finish();
                        }
                    }
                }, true);
            }
        });
    }

    public void tryToConnect(View v) {
        v.findViewById(R.id.loader).setVisibility(View.VISIBLE);
        updateService.getDetailRoom(SaveSharedPreferences.getUserAccessKey(getActivity()), roomId,
                new ServerEventListener() {
                    @Override
                    public void eventExecuted(int code, String response) {
                        if (code == 200) {
                            try {
                                v.findViewById(R.id.loader).setVisibility(View.GONE);
                                JSONObject res = new JSONObject(response);
                                mRoom = new RoomFull(res);
                                setupGeneralListener();
                                roomListener = new RoomListener(roomId, mRoom.qDate) {
                                    @Override
                                    public boolean userJoined(JSONObject userJoined) {
                                        return false;
                                    }

                                    @Override
                                    public boolean userExited(JSONObject userExited) {
                                        return false;
                                    }

                                    @Override
                                    public boolean roomEdited(JSONObject roomEdited) throws JSONException {
                                        return true;
                                    }

                                    @Override
                                    public boolean roomDeleted(JSONObject delete) {
                                        return true;
                                    }

                                    @Override
                                    public boolean statusChanged(JSONObject statusChange) throws JSONException {
                                        return true;
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
        ((TextView) v.findViewById(R.id.id_text_room)).setText(String.valueOf(mRoom.id));
        ((TextView) v.findViewById(R.id.id_name_room)).setText(mRoom.roomName);
        ((TextView) v.findViewById(R.id.members_text_room)).setText(String.valueOf(mRoom.members.size()));
        ((TextView) v.findViewById(R.id.max_members_text_room)).setText(String.valueOf(mRoom.maxMembers));
        ((TextView) v.findViewById(R.id.id_room_creator_text_room)).setText(mRoom.creatorName);
        v.findViewById(R.id.id_room_creator_text_room).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mRoom != null) {
                    Intent userInfoIntent = new Intent(getActivity(), UserInfoActivity.class);
                    userInfoIntent.putExtra(UserInfoActivity.USER_ID, mRoom.creatorId);
                    startActivity(userInfoIntent);
                }
            }
        });
        if(mRoom.getMembersSize() == 0){
            v.findViewById(R.id.full_users_list_cont).setVisibility(View.GONE);
        }else{
            v.findViewById(R.id.full_users_list_cont).setVisibility(View.VISIBLE);
            adapter.updateItems(mRoom.getMembers());
        }
        TextView par = v.findViewById(R.id.id_stat_room);
        switch (mRoom.participiantType) {
            case 0:
                par.setText(R.string.role_nobody);
                break;
            case 1:
                par.setText(R.string.role_member);
                break;
            case 2:
                par.setText(R.string.role_admin);
                break;
        }
        CustomTimerTask customTimerTask = null;
        v.findViewById(R.id.room_execute_time_cont).setVisibility(View.VISIBLE);
        v.findViewById(R.id.pay_type_cont).setVisibility(View.VISIBLE);
        v.findViewById(R.id.pay_val_cont).setVisibility(View.VISIBLE);
        if (mRoom.comment != null) {
            v.findViewById(R.id.comment_cont).setVisibility(View.VISIBLE);
            ((TextView) v.findViewById(R.id.comment_text)).setText(mRoom.comment);
        } else {
            v.findViewById(R.id.comment_cont).setVisibility(View.GONE);
        }
        ((TextView) v.findViewById(R.id.id_room_execute_time_text_room)).setText(mRoom.getExecutionDate());
        ((TextView) v.findViewById(R.id.pay_type_text)).setText(mRoom.getPayType(getResources().getStringArray(R.array.spinner_pay_types)));
        ((TextView) v.findViewById(R.id.pay_val_text)).setText(mRoom.getPayVal(getResources().getStringArray(R.array.suffix_pay_types)));
        v.findViewById(R.id.enter_exit_room).setEnabled(false);

        if (mRoom.participiantType == 0) {
            if(mRoom.shouldShowExpandedKeyboard){
                v.findViewById(R.id.enter_exit_room).setVisibility(View.GONE);
                v.findViewById(R.id.cancel).setVisibility(View.VISIBLE);
                v.findViewById(R.id.accept).setVisibility(View.VISIBLE);
            }else{
                v.findViewById(R.id.enter_exit_room).setVisibility(View.VISIBLE);
                v.findViewById(R.id.cancel).setVisibility(View.GONE);
                v.findViewById(R.id.accept).setVisibility(View.GONE);
            }
            v.findViewById(R.id.qrcode_room_button).setVisibility(View.GONE);
            v.findViewById(R.id.edit_room_button).setVisibility(View.GONE);
            v.findViewById(R.id.delete_room_button).setVisibility(View.GONE);
        } else if (!mRoom.isAdmin) {
            v.findViewById(R.id.enter_exit_room).setVisibility(View.VISIBLE);
            v.findViewById(R.id.cancel).setVisibility(View.GONE);
            v.findViewById(R.id.accept).setVisibility(View.GONE);
            v.findViewById(R.id.edit_room_button).setVisibility(View.GONE);
            v.findViewById(R.id.delete_room_button).setVisibility(View.GONE);
            v.findViewById(R.id.qrcode_room_button).setVisibility(View.VISIBLE);
        } else {
            v.findViewById(R.id.enter_exit_room).setVisibility(View.GONE);
            v.findViewById(R.id.cancel).setVisibility(View.GONE);
            v.findViewById(R.id.accept).setVisibility(View.GONE);
            v.findViewById(R.id.edit_room_button).setVisibility(View.VISIBLE);
            v.findViewById(R.id.delete_room_button).setVisibility(View.VISIBLE);
            v.findViewById(R.id.qrcode_room_button).setVisibility(View.VISIBLE);
        }
        ((TextView)v.findViewById(R.id.id_stat_room)).setTextColor(ContextCompat.getColor(getContext(), R.color.reddish));
        switch (mRoom.st) {
            case EXECUTED:
                v.findViewById(R.id.enter_exit_room).setVisibility(View.GONE);
                v.findViewById(R.id.cancel).setVisibility(View.GONE);
                v.findViewById(R.id.accept).setVisibility(View.GONE);
                v.findViewById(R.id.enter_exit_room).setVisibility(View.GONE);
                ((TextView)v.findViewById(R.id.id_stat_room)).setText(R.string.room_status_executed);
                ((TextView) v.findViewById(R.id.room_status)).setText(R.string.room_status_executed);
                break;
            case NOT_SET_UP:
                v.findViewById(R.id.enter_exit_room).setVisibility(View.GONE);
                v.findViewById(R.id.cancel).setVisibility(View.GONE);
                v.findViewById(R.id.accept).setVisibility(View.GONE);
                ((TextView)v.findViewById(R.id.id_stat_room)).setText(R.string.room_status_not_set_up);
                ((TextView) v.findViewById(R.id.room_status)).setText(R.string.room_status_not_set_up);
                v.findViewById(R.id.room_execute_time_cont).setVisibility(View.GONE);
                v.findViewById(R.id.pay_type_cont).setVisibility(View.GONE);
                v.findViewById(R.id.pay_val_cont).setVisibility(View.GONE);
                break;
            case WAIT:
                ((TextView)v.findViewById(R.id.id_stat_room)).setTextColor(ContextCompat.getColor(getContext(), R.color.greenish));
                ((TextView)v.findViewById(R.id.id_stat_room)).setText(R.string.room_status_wait);
                if (!mRoom.isAdmin) {
                    v.findViewById(R.id.enter_exit_room).setEnabled(true);
                }
                ((TextView) v.findViewById(R.id.room_status)).setText(R.string.room_status_wait);
                if (mRoom.start != -1) {
                    customTimerTask = new CustomTimerTask(mRoom.delta, mRoom.start, v);
                }
                break;
            case EXECUTING:
                v.findViewById(R.id.enter_exit_room).setVisibility(View.GONE);
                v.findViewById(R.id.cancel).setVisibility(View.GONE);
                v.findViewById(R.id.accept).setVisibility(View.GONE);
                ((TextView)v.findViewById(R.id.id_stat_room)).setText(R.string.room_status_executing);
                ((TextView) v.findViewById(R.id.room_status)).setText(R.string.room_status_executing);
                if (mRoom.end != -1) {
                    customTimerTask = new CustomTimerTask(mRoom.delta, mRoom.end, v);
                }
                break;
        }
        if (customTimerTask != null) {
            if (current != null) {
                current.cancel();
            }
            if (timer == null) {
                timer = new Timer();
            }
            current = customTimerTask;
            timer.schedule(current, 0, 1000L);
        }
    }
}

