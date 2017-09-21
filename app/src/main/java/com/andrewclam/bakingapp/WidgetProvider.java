package com.andrewclam.bakingapp;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.andrewclam.bakingapp.services.WidgetRemoteViewService;

/**
 * Implementation of App Widget functionality.
 */
public class WidgetProvider extends AppWidgetProvider {

    /**
     * Debug Tag
     */
    private static final String TAG = WidgetProvider.class.getSimpleName();

    /**
     * updateAppWidget() is where UI bind the data
     *
     * @param context          the application context
     * @param appWidgetManager the widget manager used to update the appwidget by id
     * @param appWidgetId      the particular widget id to update
     */
    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

        // Construct the RemoteViews
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_recipe_start);

        Intent adapterIntent = new Intent(context, WidgetRemoteViewService.class);
        views.setRemoteAdapter(R.id.widget_ingredient_list_lv,adapterIntent);

        // Handle empty recipe
        views.setEmptyView(R.id.widget_start_recipe, R.id.widget_empty_view);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }


    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // Start the intent service update widget action, the service takes care of updating
        // the widgets UI. There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}

