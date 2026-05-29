package com.kanakku.app.widget;

import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;
import com.kanakku.app.MainActivity;
import com.kanakku.app.QuickAddActivity;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class WidgetUpdateService extends Service {

    private static final String TAG = "WidgetUpdateService";
    private static final String SCRIPT_URL =
        "https://script.google.com/macros/s/AKfycbzAAKTyxRnu_nidhAZjnLMGT_WrnfR6R4nO-_xuqDukvOWVg8Ns7eOzvIvrnuktvbM/exec";

    // SharedPrefs keys for widget data cache
    public static final String PREFS          = QuickAddActivity.PREFS;
    public static final String KEY_MONTH_TOTAL= "month_total";
    public static final String KEY_TODAY_TOTAL= "today_total";
    public static final String KEY_TXN_1_NAME = "txn1_name";
    public static final String KEY_TXN_1_AMT  = "txn1_amt";
    public static final String KEY_TXN_2_NAME = "txn2_name";
    public static final String KEY_TXN_2_AMT  = "txn2_amt";
    public static final String KEY_TXN_3_NAME = "txn3_name";
    public static final String KEY_TXN_3_AMT  = "txn3_amt";
    public static final String KEY_LAST_UPDATE = "last_update";
    public static final String KEY_USER_EMAIL  = QuickAddActivity.KEY_USER_EMAIL;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String email = getSharedPreferences(PREFS, MODE_PRIVATE)
            .getString(KEY_USER_EMAIL, "");

        if (email.isEmpty()) {
            stopSelf(startId);
            return START_NOT_STICKY;
        }

        fetchAndUpdate(email, startId);
        return START_NOT_STICKY;
    }

    private void fetchAndUpdate(String email, int startId) {
        OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

        HttpUrl url = HttpUrl.parse(SCRIPT_URL).newBuilder()
            .addQueryParameter("action", "get_expenses")
            .addQueryParameter("user", email)
            .build();

        client.newCall(new Request.Builder().url(url).build()).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Fetch failed: " + e.getMessage());
                pushWidgetUpdate();
                stopSelf(startId);
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                try {
                    String body = response.body().string();
                    JSONObject json = new JSONObject(body);
                    JSONArray data = json.optJSONArray("data");
                    if (data == null) { stopSelf(startId); return; }

                    String today = java.time.LocalDate.now().toString(); // yyyy-MM-dd
                    String thisMonth = today.substring(0, 7); // yyyy-MM

                    double monthTotal = 0, todayTotal = 0;
                    // Collect last 3 non-excluded txns
                    String[] names = {"", "", ""};
                    String[] amts  = {"", "", ""};
                    int txnCount = 0;

                    // iterate newest → oldest (reverse)
                    for (int i = data.length() - 1; i >= 0; i--) {
                        JSONObject r = data.getJSONObject(i);
                        double amt = r.optDouble("amount", 0);
                        String date = r.optString("date", "");
                        String type = r.optString("type", "");
                        String cat  = r.optString("category", "");

                        // Effective amount
                        double eff = amt;
                        double others = r.optDouble("others_total", 0);
                        if ("bill".equalsIgnoreCase(type.trim()) && others > 0)
                            eff = Math.max(0, amt - others);

                        if (date.startsWith(thisMonth)) monthTotal += eff;
                        if (date.equals(today))         todayTotal += eff;

                        if (txnCount < 3 && !type.isEmpty()) {
                            String display = amt < 0 ? "+₹" + fmt(Math.abs(eff)) : "₹" + fmt(Math.abs(eff));
                            names[txnCount] = type;
                            amts[txnCount]  = display;
                            txnCount++;
                        }
                    }

                    // Save to SharedPrefs
                    SharedPreferences.Editor ed = getSharedPreferences(PREFS, MODE_PRIVATE).edit();
                    ed.putString(KEY_MONTH_TOTAL,  "₹" + fmt(monthTotal));
                    ed.putString(KEY_TODAY_TOTAL,  "₹" + fmt(todayTotal));
                    ed.putString(KEY_TXN_1_NAME,   names[0]);
                    ed.putString(KEY_TXN_1_AMT,    amts[0]);
                    ed.putString(KEY_TXN_2_NAME,   names[1]);
                    ed.putString(KEY_TXN_2_AMT,    amts[1]);
                    ed.putString(KEY_TXN_3_NAME,   names[2]);
                    ed.putString(KEY_TXN_3_AMT,    amts[2]);
                    ed.putString(KEY_LAST_UPDATE,  "Updated " + java.time.LocalTime.now()
                        .toString().substring(0, 5));
                    ed.apply();

                    pushWidgetUpdate();
                } catch (Exception e) {
                    Log.e(TAG, "Parse error: " + e.getMessage());
                    pushWidgetUpdate();
                }
                stopSelf(startId);
            }
        });
    }

    private void pushWidgetUpdate() {
        AppWidgetManager mgr = AppWidgetManager.getInstance(this);
        // Update small widget
        int[] ids1 = mgr.getAppWidgetIds(new ComponentName(this, KanakkuWidget.class));
        for (int id : ids1) KanakkuWidget.updateWidget(this, mgr, id);
        // Update wide widget
        int[] ids2 = mgr.getAppWidgetIds(new ComponentName(this, KanakkuWidgetWide.class));
        for (int id : ids2) KanakkuWidgetWide.updateWidget(this, mgr, id);
    }

    private String fmt(double v) {
        if (v >= 100000) return String.format("%.1fL", v / 100000);
        if (v >= 1000)   return String.format("%.1fK", v / 1000);
        return String.valueOf(Math.round(v));
    }

    @Override public IBinder onBind(Intent i) { return null; }
}
