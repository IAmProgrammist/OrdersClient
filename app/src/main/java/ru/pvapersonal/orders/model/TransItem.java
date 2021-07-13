package ru.pvapersonal.orders.model;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.ColorRes;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Objects;

public class TransItem {
    private static final String R = "₽";
    long transVal;
    String initiatorName;
    int transactionType;
    public Long transDate;

    public TransItem(JSONObject obj) throws JSONException {
        transVal = obj.getLong("transVal");
        if(obj.has("initiatorName")){
            initiatorName = obj.getString("initiatorName");
        }
        transactionType = obj.getInt("transType");
        transDate = obj.getLong("transDate");
    }

    public String getPayString(){
        long tmp = transVal;
        String res = "." + tmp % 100 + " " + R;
        tmp = tmp / 100;
        return (transactionType == 2 ? "- " : "+ ") + String.valueOf(tmp).replace("/\\B(?=(\\d{3})+(?!\\d))/g", " ") + res;
    }

    @ColorRes
    public int getColor(){
        if(transactionType == 2){
            return ru.pvapersonal.orders.R.color.reddish;
        }else{
            return ru.pvapersonal.orders.R.color.greenish;
        }
    }

    public String getDate(){
        return new SimpleDateFormat("dd MMMM yyyy HH:mm:ss", new Locale("RU"))
                .format(transDate);
    }

    public String getComment(Resources res){
        switch (transactionType){
            case 0:
                return String.format(res.getString(ru.pvapersonal.orders.R.string.per_hour_trans), initiatorName == null ? "" : initiatorName);
            case 1:
                return String.format(res.getString(ru.pvapersonal.orders.R.string.full_payment_trans), initiatorName != null ? initiatorName : "");
            default:
                return res.getString(ru.pvapersonal.orders.R.string.admin_payout);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransItem transItem = (TransItem) o;
        return transVal == transItem.transVal &&
                transactionType == transItem.transactionType &&
                Objects.equals(initiatorName, transItem.initiatorName) &&
                transDate.equals(transItem.transDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transVal, initiatorName, transactionType, transDate);
    }
}
