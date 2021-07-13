package ru.pvapersonal.orders.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class SaveSharedPreferences {

    private static final String USER_PHONE = "user_phone";
    private static final String USER_PASSWORD = "user_password";
    private static final String USER_ACCESSKEY = "accesskey";

    static SharedPreferences getSharedPreferences(Context ctx) {
        return PreferenceManager.getDefaultSharedPreferences(ctx);
    }

    public static String getUserPhone(Context ctx){
        return getSharedPreferences(ctx).getString(USER_PHONE, "");
    }

    public static void setUserPhone(Context ctx, String userPhone){
        SharedPreferences.Editor editor = getSharedPreferences(ctx).edit();
        editor.putString(USER_PHONE, userPhone);
        editor.apply();
    }

    public static String getUserPassword(Context ctx){
        return getSharedPreferences(ctx).getString(USER_PASSWORD, "");
    }

    public static void setUserPassword(Context ctx, String userPassword){
        SharedPreferences.Editor editor = getSharedPreferences(ctx).edit();
        editor.putString(USER_PASSWORD, userPassword);
        editor.apply();
    }

    public static String getUserAccessKey(Context ctx){
        return getSharedPreferences(ctx).getString(USER_ACCESSKEY, null);
    }

    public static void setUserAccesskey(Context ctx, String userAccessKey){
        SharedPreferences.Editor editor = getSharedPreferences(ctx).edit();
        editor.putString(USER_ACCESSKEY, userAccessKey);
        editor.apply();
    }
}
