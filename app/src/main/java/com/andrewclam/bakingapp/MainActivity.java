/*
 * Copyright (c) 2017 Andrew Chi Heng Lam
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.andrewclam.bakingapp;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.andrewclam.bakingapp.adapters.RecipeRecyclerViewAdapter;
import com.andrewclam.bakingapp.asyncTasks.DbMultiTableParsingAsyncTask;
import com.andrewclam.bakingapp.asyncTasks.FetchRecipeAsyncTask;
import com.andrewclam.bakingapp.models.Recipe;
import com.andrewclam.bakingapp.services.SyncDbIntentService;
import com.andrewclam.bakingapp.utils.NetworkUtils;
import com.andrewclam.bakingapp.widget.WidgetUtils;
import com.squareup.picasso.Picasso;

import org.parceler.Parcels;

import java.util.ArrayList;

import static com.andrewclam.bakingapp.Constants.ACTION_APPWIDGET_CONFIG;
import static com.andrewclam.bakingapp.Constants.DATA_URL;
import static com.andrewclam.bakingapp.Constants.EXTRA_RECIPE;
import static com.andrewclam.bakingapp.data.RecipeDbContract.RecipeEntry.CONTENT_URI_RECIPE;

public class MainActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor>,
        FetchRecipeAsyncTask.onFetchRecipeActionListener,
        RecipeRecyclerViewAdapter.OnRecipeItemClickedListener {
    /**
     * RecyclerView to show the list of recipes
     */
    private RecyclerView mRecipeRv;
    private RecyclerView.LayoutManager mLayoutManager;
    private RecipeRecyclerViewAdapter mAdapter;

    /**
     * Progress bar to show user the data is loading
     */
    private ProgressBar mProgressBar;

    /**
     * App Widget Configuration
     */
    private boolean mStartedForAppWidgetConfig;
    private int mAppWidgetId;

    /**
     * This ID will be used to identify the Loader responsible for loading our offline database. In
     * some cases, one Activity can deal with many Loaders. However, in our case, there is only one.
     * We will still use this ID to initialize the loader and create the loader for best practice.
    */
    private static final int RECIPE_LOADER_ID = 8888;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /* Check if started for and Setup AppWidget Configuration */
        initAppWidgetConfiguration();

        /* Recipes List Setup */
        mRecipeRv = findViewById(R.id.recipe_list_rv);
        mAdapter = new RecipeRecyclerViewAdapter(this, this);
        mRecipeRv.setAdapter(mAdapter);

        // Determine device's orientation and adjust layout type accordingly
        if (findViewById(R.id.recipe_list_container_land) == null) {
            // The device is not in landscape mode,
            // layout the recipe list in a linear layout
            mLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        } else {
            // The device is in landscape mode, use grid layout
            mLayoutManager = new GridLayoutManager(this, 3);
        }
        mRecipeRv.setLayoutManager(mLayoutManager);


        /* Check Network Connection */
        // TODO check network connection with resultReceiver
        if (NetworkUtils.getNetworkState(this)) {
            /* Async Load The Latest Recipe Data (NETWORK CONNECTED) */
            new FetchRecipeAsyncTask()
                    .setDataURL(DATA_URL)
                    .setListener(this)
                    .execute();
        }

        /* Load Data from Database using CursorLoader */
        // If offline, will still load the database cached data
        getSupportLoaderManager().restartLoader(RECIPE_LOADER_ID, null, this);


        /* Loading Progress Bar - Visible*/
        mProgressBar = findViewById(R.id.progress_bar);
        mProgressBar.setVisibility(View.VISIBLE);
    }

    /**
     * Callback from the FetchRecipeAsyncTask with a list of recipe ready to populate the
     * recycler view.
     *
     * @param recipes the list of recipes parsed from the data source
     */
    @Override
    public void onRecipesReady(ArrayList<Recipe> recipes) {
        // Call intent service to update the database with the latest recipes
        SyncDbIntentService.syncRecipes(this, recipes);
    }

    /**
     * Callback from the RecipeRecyclerViewAdapter when the user clicks a recipe from the list.
     *
     * @param recipe the user's clicked recipe
     */
    @Override
    public void onRecipeClicked(Recipe recipe) {
        if (mStartedForAppWidgetConfig) {
            // 1) Call create AppWidget to populate the RemoteView and create the widget with
            // AppWidgetManager
            Intent resultValue = WidgetUtils.createAppWidgetResult(this,mAppWidgetId,recipe);
            setResult(RESULT_OK, resultValue);

            // Finish the configuration activity once the result is set
            finish();

        } else {
            // 2) Otherwise, should just launch the detailActivity showing the recipe's full info
            Intent intent = new Intent(this, StepListActivity.class);
            intent.putExtra(EXTRA_RECIPE, Parcels.wrap(recipe));
            startActivity(intent);
        }
    }

    /**
     * App Widget Configuration
     * <p>
     * initAppWidgetConfiguration() gets the intent that started this Activity and initialize
     * the vars required for AppWidget configuration
     */
    private void initAppWidgetConfiguration() {
        Intent intent = getIntent();
        String action = intent.getAction();
        Bundle extras = intent.getExtras();

        if (action != null && extras != null && action.equals(ACTION_APPWIDGET_CONFIG)) {
            // Set the flag to true, this indicate the activity was started
            // with action for the companion app widget configuration
            mStartedForAppWidgetConfig = true;

            // Get the App Widget Id, this is used for appWidgetManager to update
            // a particular id
            mAppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);

            // Set title to select recipe
            setTitle(getString(R.string.app_widget_select_recipe_title));
        } else {
            // Set the flag to false, activity was started normally
            mStartedForAppWidgetConfig = false;
        }
    }

    /**
     * App Widget Configuration
     * <p>
     * createAppWidgetResult() creates the AppWidget user selecting a recipe from the list
     * new widget serves as the shortcut to the particular selected recipe.
     *
     * @param recipe the recipe object that the user clicked
     */
    private void createAppWidget(Recipe recipe) {
        // 1) If the app is started for AppWidget Configuration, upon user click the recipe
        // user is selecting the recipe to be displayed as the widget on the home screen
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);

        // Data - Create the pending intent, as the widget act as the shortcut to the recipe
        // the intent should launch the stepsListActivity by default with the recipe
        Intent intent = new Intent(MainActivity.this, StepListActivity.class);
        intent.putExtra(EXTRA_RECIPE, Parcels.wrap(recipe));
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                mAppWidgetId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        // UI - Find and Bind Views
        RemoteViews views = new RemoteViews(this.getPackageName(), R.layout.widget_recipe_small);

        views.setOnClickPendingIntent(R.id.widget_small_root_view, pendingIntent);
        views.setTextViewText(R.id.widget_small_recipe_name, recipe.getName());

        // UI - Image Icon Check if recipe has an image for icon
        String imageUrl = recipe.getImageURL();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            // Use picasso to load the image into the remoteView
            Picasso.with(this).load(imageUrl).into(
                    views,
                    R.id.widget_small_icon,
                    new int[]{mAppWidgetId}
            );
        } else {
            // default to cupcake icon
            views.setImageViewResource(R.id.widget_small_icon, R.drawable.ic_cupcake);
        }


        // Widget Update - Use appWidgetManager to update/create the particular widget by id
        appWidgetManager.updateAppWidget(mAppWidgetId, views);

        // Send out an intent with the resulting appWidgetId, with the result OK
        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        setResult(RESULT_OK, resultValue);

        // Finish the configuration activity
        finish();
    }

    /**
     * CursorLoader and LoaderManager Implementation
     * Do db query off the main thread and communicate via these callbacks
     */
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        /* Load ALL Cached Recipe Data from Database */
        return new CursorLoader(this,
                CONTENT_URI_RECIPE,
                null,
                null,
                null,
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data != null) {
            // Use an asyncTask to parse the multi-table db using the Cursor data
            new DbMultiTableParsingAsyncTask()
                    .setContentResolver(this.getContentResolver())
                    .setCursor(data)
                    .setListener(new DbMultiTableParsingAsyncTask.OnParsingActionComplete() {
                        @Override
                        public void onEntriesParsed(ArrayList<Recipe> recipes) {
                            mAdapter.setRecipeData(recipes);
                            mAdapter.notifyDataSetChanged();
                            /* Loading Progress Bar - Data Loaded, Be GONE */
                            mProgressBar.setVisibility(View.GONE);
                        }
            }).execute();
        }else
        {
            // Show empty view, no data available
            // TODO design a cuter empty view and show that instead
            Toast.makeText(this,getString(R.string.data_unavailable),Toast.LENGTH_SHORT).show();
        }

        /* Loading Progress Bar - Data Loaded, Be GONE */
        mProgressBar.setVisibility(View.GONE);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        /*
         * Since this Loader's data is now invalid, we need to clear the Adapter that is
         * displaying the data.
         */
        mAdapter.setRecipeData(null);
    }
}
