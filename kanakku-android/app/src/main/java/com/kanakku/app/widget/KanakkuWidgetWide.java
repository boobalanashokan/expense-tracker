package com.kanakku.app.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.RemoteViews;
import com.kanakku.app.MainActivity;
import com.kanakku.app.QuickAddActivity;
import com.kanakku.app.R;

public class KanakkuWidgetWide extends AppWidgetProvider {

    @Override
    public void onUpdate(Context ctx, AppWidgetManager mgr, int[] ids) {
        for (int id : ids) updateWidget(ctx, mgr, id);
        ctx.startService(new Intent(ctx, WidgetUpdateService.class));
    }

    @Override
    public void onReceive(Context ctx, Intent intent) {
        super.onReceive(ctx, intent);
        if ("com.kanakku.WIDGET_REFRESH".equals(intent.getAction())) {
            ctx.startService(new Intent(ctx, WidgetUpdateService.class));
        }
    }

    public static void updateWidget(Context ctx, AppWidgetManager mgr, int widgetId) {
        SharedPreferences prefs = ctx.getSharedPreferences(WidgetUpdateService.PREFS, Context.MODE_PRIVATE);

        String monthTotal = prefs.getString(WidgetUpdateService.KEY_MONTH_TOTAL, "—");
        String todayTotal = prefs.getString(WidgetUpdateService.KEY_TODAY_TOTAL, "—");
        String lastUpdate = prefs.getString(WidgetUpdateService.KEY_LAST_UPDATE, "");
        String t1n = prefs.getString(WidgetUpdateService.KEY_TXN_1_NAME, "");
        String t1a = prefs.getString(WidgetUpdateService.KEY_TXN_1_AMT,  "");
        String t2n = prefs.getString(WidgetUpdateService.KEY_TXN_2_NAME, "");
        String t2a = prefs.getString(WidgetUpdateService.KEY_TXN_2_AMT,  "");
        String t3n = prefs.getString(WidgetUpdateService.KEY_TXN_3_NAME, "");
        String t3a = prefs.getString(WidgetUpdateService.KEY_TXN_3_AMT,  "");

        RemoteViews views = new RemoteViews(ctx.getPackageName(), R.layout.widget_wide);

        // Stats
        views.setTextViewText(R.id.wwMonth, monthTotal);
        views.setTextViewText(R.id.wwToday, todayTotal);
        views.setTextViewText(R.id.wwUpdated, lastUpdate);

        // Recent transactions
        views.setTextViewText(R.id.wwT1Name, t1n.isEmpty() ? "No entries yet" : t1n);
        views.setTextViewText(R.id.wwT1Amt,  t1a);
        views.setViewVisibility(R.id.wwRow1, t1n.isEmpty() ? View.GONE : View.VISIBLE);

        views.setTextViewText(R.id.wwT2Name, t2n);
        views.setTextViewText(R.id.wwT2Amt,  t2a);
        views.setViewVisibility(R.id.wwRow2, t2n.isEmpty() ? View.GONE : View.VISIBLE);

        views.setTextViewText(R.id.wwT3Name, t3n);
        views.setTextViewText(R.id.wwT3Amt,  t3a);
        views.setViewVisibility(R.id.wwRow3, t3n.isEmpty() ? View.GONE : View.VISIBLE);

        // Tap anywhere on header → open app
        PendingIntent openApp = PendingIntent.getActivity(ctx, 0,
            new Intent(ctx, MainActivity.class),
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.wwHeader, openApp);
        views.setOnClickPendingIntent(R.id.wwTxnSection, openApp);

        // ➕ Add expense
        Intent addIntent = new Intent(ctx, QuickAddActivity.class);
        addIntent.setAction("com.kanakku.QUICK_ADD_WIDE_" + widgetId);
        PendingIntent addPi = PendingIntent.getActivity(ctx, widgetId + 100,
            addIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.wwAddBtn, addPi);

        // ↩ Refund
        Intent refundIntent = new Intent(ctx, QuickAddActivity.class);
        refundIntent.putExtra("is_refund", true);
        refundIntent.setAction("com.kanakku.REFUND_WIDE_" + widgetId);
        PendingIntent refundPi = PendingIntent.getActivity(ctx, widgetId + 1100,
            refundIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.wwRefundBtn, refundPi);

        // 🔄 Refresh
        Intent refreshIntent = new Intent(ctx, WidgetUpdateService.class);
        PendingIntent refreshPi = PendingIntent.getService(ctx, widgetId + 2100,
            refreshIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.wwRefreshBtn, refreshPi);

        mgr.updateAppWidget(widgetId, views);
    }
}
