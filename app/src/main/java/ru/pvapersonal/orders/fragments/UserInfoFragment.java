package ru.pvapersonal.orders.fragments;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.fragment.app.Fragment;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.InputStream;

import ru.pvapersonal.orders.R;
import ru.pvapersonal.orders.activities.ActiveUsersActivity;
import ru.pvapersonal.orders.activities.ChangeUserInfoActivity;
import ru.pvapersonal.orders.activities.DetailActivity;
import ru.pvapersonal.orders.activities.LoginActivity;
import ru.pvapersonal.orders.activities.TransactionHistoryActivity;
import ru.pvapersonal.orders.service.SaveSharedPreferences;
import ru.pvapersonal.orders.service.ServerUpdateListener;
import ru.pvapersonal.orders.service.listeners.ServerEventListener;
import static ru.pvapersonal.orders.other.App.URL;

public class UserInfoFragment extends Fragment {
    ServerUpdateListener updateService;
    Uri resultUri;
    View viewPhoto;
    View thisView;

    public UserInfoFragment(ServerUpdateListener updateService){
        this.updateService = updateService;
        updateService.removeGeneralListener();
    }

    @Nullable
    @org.jetbrains.annotations.Nullable
    @Override
    public View onCreateView(@NonNull @NotNull LayoutInflater inflater, @Nullable @org.jetbrains.annotations.Nullable ViewGroup container, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        View resultView = inflater.inflate(R.layout.account_screen, null);
        viewPhoto = resultView.findViewById(R.id.avatar);
        thisView = resultView;
        thisView.findViewById(R.id.trans_hist).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent launch = new Intent(getActivity(), TransactionHistoryActivity.class);
                startActivity(launch);
            }
        });
        updateService.removeGeneralListener();
        return resultView;
    }

    @Override
    public void onViewCreated(@NonNull @NotNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tryToConnect(view);
        view.findViewById(R.id.change_photo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CropImage.activity()
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .setAspectRatio(1, 1)
                        .setRequestedSize(500, 500)
                        .start(UserInfoFragment.this.getContext(), UserInfoFragment.this);
            }
        });
    }

    ActivityResultLauncher<Intent> launcherChangeInfo = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == ChangeUserInfoActivity.CHANGE_INFO_SUCCESS) {
                        Intent data = result.getData();
                        String resString = data.getExtras().getString("middlename").equals("") ?
                                String.format("%s %s", data.getExtras().getString("surname"),
                                        data.getExtras().getString("name")) :
                                String.format("%s %s %s", data.getExtras().getString("surname"),
                                        data.getExtras().getString("name"),
                                        data.getExtras().getString("middlename"));
                        ((TextView) thisView.findViewById(R.id.name)).setText(resString);
                        try {
                            PhoneNumberUtil util = PhoneNumberUtil.getInstance();
                            Phonenumber.PhoneNumber number = util.parse(data.getExtras().getString("phone"), "RU");
                            String pre = util.format(number, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL);
                            ((TextView) thisView.findViewById(R.id.phone)).setText(pre);
                        } catch (NumberParseException e) {
                            ((TextView) thisView.findViewById(R.id.phone)).setText(data.getExtras().getString("phone"));
                        }
                        thisView.findViewById(R.id.change_info).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Context ctx = getContext();
                                Intent intent = new Intent(getActivity(), ChangeUserInfoActivity.class);
                                intent.putExtra("name", data.getExtras().getString("name"));
                                intent.putExtra("phone", data.getExtras().getString("phone"));
                                intent.putExtra("surname", data.getExtras().getString("surname"));
                                intent.putExtra("middlename", data.getExtras().getString("middlename"));
                                launcherChangeInfo.launch(intent);
                            }
                        });
                    } else if (result.getResultCode() == ChangeUserInfoActivity.CHANGE_INFO_LAUNCH_LOGIN) {
                        Intent intent = new Intent(getActivity(), LoginActivity.class);
                        getActivity().startActivity(intent);
                        getActivity().finish();
                    }
                }
            });

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable @org.jetbrains.annotations.Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == getActivity().RESULT_OK) {
                resultUri = result.getUri();
                if (viewPhoto != null) {
                    Drawable chosen;
                    try {
                        InputStream inputStream = getActivity().getContentResolver().openInputStream(resultUri);
                        chosen = Drawable.createFromStream(inputStream, resultUri.toString());
                    } catch (FileNotFoundException e) {
                        chosen = getResources().getDrawable(R.drawable.ic_avatar_empty);
                    }
                    ((ImageView) viewPhoto).setImageDrawable(chosen);
                    getActivity().findViewById(R.id.loader).setVisibility(View.VISIBLE);
                    try {
                        Drawable finalChosen = chosen;
                        updateService.setUserImage(
                                SaveSharedPreferences.getUserAccessKey(getActivity()),
                                resultUri, new ServerEventListener() {
                                    @Override
                                    public void eventExecuted(int code, String response) {
                                        getActivity().findViewById(R.id.loader).setVisibility(View.GONE);
                                        if (code == 200) {
                                            ((ImageView) viewPhoto).setImageDrawable(finalChosen);
                                            return;
                                        } else {
                                            Toast.makeText(getActivity(),
                                                    getResources().getString(R.string.server_error),
                                                    Toast.LENGTH_LONG).show();
                                            return;
                                        }
                                    }
                                }, true);
                    } catch (FileNotFoundException e) {
                        getActivity().findViewById(R.id.loader).setVisibility(View.GONE);
                        Toast.makeText(getActivity(),
                                getResources().getString(R.string.file_not_found),
                                Toast.LENGTH_LONG).show();
                        resultUri = null;
                    }
                }
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Toast.makeText(getActivity(), getResources().getString(R.string.crop_image_error),
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    public void tryToConnect(View v) {
        v.findViewById(R.id.loader).setVisibility(View.VISIBLE);
        updateService.getUserInfo(SaveSharedPreferences.getUserAccessKey(getActivity()), new ServerEventListener() {
            @Override
            public void eventExecuted(int code, String response) {
                if (code == 200) {
                    try {
                        v.findViewById(R.id.loader).setVisibility(View.GONE);
                        JSONObject res = new JSONObject(response);
                        String name = res.getString("name");
                        String surname = res.getString("surname");
                        String middlename = res.has("middlename") ? res.getString("middlename") : null;
                        String phone = res.getString("telNumber");
                        String image = res.has("fileName") ? res.getString("fileName") : null;
                        boolean isAdmin = res.getBoolean("isAdmin");
                        if(isAdmin){
                            v.findViewById(R.id.active_users).setVisibility(View.VISIBLE);
                            ((AppCompatTextView)v.findViewById(R.id.role)).setText(R.string.role_admin);
                            v.findViewById(R.id.active_users).setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Intent intent  = new Intent(getActivity(), ActiveUsersActivity.class);
                                    startActivity(intent);
                                }
                            });
                        }else{
                            v.findViewById(R.id.active_users).setVisibility(View.GONE);
                            ((AppCompatTextView)v.findViewById(R.id.role)).setText(R.string.role_member);
                        }
                        thisView.findViewById(R.id.change_info).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Context ctx = getContext();
                                Intent intent = new Intent(getActivity(), ChangeUserInfoActivity.class);
                                intent.putExtra("name", name);
                                intent.putExtra("phone", phone);
                                intent.putExtra("surname", surname);
                                intent.putExtra("middlename", middlename == null ? "" : middlename);
                                launcherChangeInfo.launch(intent);
                            }
                        });
                        String resText = middlename == null ? String.format("%s %s", surname, name) :
                                String.format("%s %s %s", surname, name, middlename);
                        ((TextView) v.findViewById(R.id.name)).setText(resText);
                        try {
                            PhoneNumberUtil util = PhoneNumberUtil.getInstance();
                            Phonenumber.PhoneNumber number = util.parse(phone, "RU");
                            String pre = util.format(number, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL);
                            ((TextView) v.findViewById(R.id.phone)).setText(pre);
                        } catch (NumberParseException e) {
                            ((TextView) v.findViewById(R.id.phone)).setText(phone);
                        }
                        if (image != null && isAdded()) {
                            Picasso.get().load(URL + "images/" + image).into((ImageView) v.findViewById(R.id.avatar));
                        } else {
                            ((ImageView) v.findViewById(R.id.avatar)).setImageResource(R.drawable.ic_avatar_empty);
                        }
                    } catch (JSONException e) {
                        v.findViewById(R.id.loader).setVisibility(View.GONE);
                        ((TextView) v.findViewById(R.id.name)).setText(getResources().getString(R.string.user_not_found));
                        ((TextView) v.findViewById(R.id.phone)).setText("");
                        v.findViewById(R.id.change_info).setVisibility(View.GONE);
                        v.findViewById(R.id.change_photo).setVisibility(View.GONE);
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
                    ((TextView) v.findViewById(R.id.name)).setText(getResources().getString(R.string.user_not_found));
                    ((TextView) v.findViewById(R.id.phone)).setText("");
                    Picasso.get().load(R.drawable.ic_avatar_empty).into((ImageView) v.findViewById(R.id.avatar));
                    v.findViewById(R.id.change_info).setVisibility(View.GONE);
                    v.findViewById(R.id.change_photo).setVisibility(View.GONE);
                } else {
                    Toast.makeText(getActivity(), getResources().getString(R.string.connecion_failed), Toast.LENGTH_LONG).show();
                    tryToConnect(v);
                }
            }
        }, true);
    }
}
