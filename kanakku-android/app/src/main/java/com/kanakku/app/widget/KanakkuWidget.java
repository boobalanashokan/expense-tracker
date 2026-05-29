package com.kanakku.app.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.RemoteViews;
import com.kanakku.app.MainActivity;
import com.kanakku.app.QuickAddActivity;
import com.kanakku.app.R;

public class KanakkuWidget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context ctx, AppWidgetManager mgr, int[] ids) {
        for (int id : ids) updateWidget(ctx, mgr, id);
        // Kick off data refresh
        ctx.startService(new Intent(ctx, WidgetUpdateService.class));
    }

    @Override
    public void onReceive(Context ctx, Intent intent) {
        super.onReceive(ctx, intent);
        if ("com.kanakku.WIDGET_REFRESH".equals(intent.getAction()) ||
            "com.kanakku.QUICK_ADD".equals(intent.getAction())) {
            ctx.startService(new Intent(ctx, WidgetUpdateService.class));
        }
    }

    public static void updateWidget(Context ctx, AppWidgetManager mgr, int widgetId) {
        SharedPreferences prefs = ctx.getSharedPreferences(WidgetUpdateService.PREFS, Context.MODE_PRIVATE);

        String monthTotal = prefs.getString(WidgetUpdateService.KEY_MONTH_TOTAL, "—");
        String todayTotal = prefs.getString(WidgetUpdateService.KEY_TODAY_TOTAL, "—");
        String lastUpdate = prefs.getString(WidgetUpdateService.KEY_LAST_UPDATE, "");

        RemoteViews views = new RemoteViews(ctx.getPackageName(), R.layout.widget_small);

        views.setTextViewText(R.id.wMonth, monthTotal);
        views.setTextViewText(R.id.wToday, todayTotal);
        views.setTextViewText(R.id.wUpdated, lastUpdate);

        // Tap widget body → open app
        PendingIntent openApp = PendingIntent.getActivity(ctx, 0,
            new Intent(ctx, MainActivity.class),
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.wRoot, openApp);

        // Tap ➕ button → open QuickAdd dialog
        Intent addIntent = new Intent(ctx, QuickAddActivity.class);
        addIntent.setAction("com.kanakku.QUICK_ADD_SMALL_" + widgetId);
        PendingIntent addPi = PendingIntent.getActivity(ctx, widgetId,
            addIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.wAddBtn, addPi);

        // Tap refund button → open QuickAdd in refund mode
        Intent refundIntent = new Intent(ctx, QuickAddActivity.class);
        refundIntent.putExtra("is_refund", true);
        refundIntent.setAction("com.kanakku.REFUND_SMALL_" + widgetId);
        PendingIntent refundPi = PendingIntent.getActivity(ctx, widgetId + 1000,
            refundIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.wRefundBtn, refundPi);

        // Tap refresh → trigger data update
        Intent refreshIntent = new Intent(ctx, WidgetUpdateService.class);
        PendingIntent refreshPi = PendingIntent.getService(ctx, widgetId + 2000,
            refreshIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.wRefreshBtn, refreshPi);

        mgr.updateAppWidget(widgetId, views);
    }
}
