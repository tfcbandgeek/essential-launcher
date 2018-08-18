/*
 * Copyright (C) 2017  Clemens Bartz
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.clemensbartz.android.launcher;

import android.app.Activity;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.ViewSwitcher;

import java.util.ArrayList;
import java.util.List;

import de.clemensbartz.android.launcher.adapters.DrawerListAdapter;
import de.clemensbartz.android.launcher.models.ApplicationModel;
import de.clemensbartz.android.launcher.models.HomeModel;
import de.clemensbartz.android.launcher.tasks.LoadModelAsyncTask;
import de.clemensbartz.android.launcher.tasks.LoadMostUsedAppsAsyncTask;
import de.clemensbartz.android.launcher.tasks.ResetUsageAsyncTask;
import de.clemensbartz.android.launcher.tasks.ShowWidgetListAsPopupMenuTask;
import de.clemensbartz.android.launcher.tasks.ToggleDockAsyncTask;
import de.clemensbartz.android.launcher.tasks.ToggleStickyAsyncTask;
import de.clemensbartz.android.launcher.tasks.UpdateAsyncTask;
import de.clemensbartz.android.launcher.util.IntentUtil;

/**
 * Launcher class.
 *
 * @author Clemens Bartz
 * @since 1.0
 */
public final class Launcher extends Activity {

    /** A quarter. */
    private static final double QUARTER = 0.25;
    /** A half. */
    private static final double HALF = 0.5;
    /** Three quarter. */
    private static final double THREE_QUARTER = 0.75;
    /** Two third. */
    private static final double TWO_THIRD = 0.66;

    /** Default height in dp of the dock. */
    private static final int DOCK_HEIGHT = 60;

    /** Id to identify the home layout. */
    public static final int HOME_ID = 0;
    /** Id to identify the launcher layout. */
    private static final int DRAWER_ID = 1;

    /** Request code for binding widget. */
    private static final int REQUEST_BIND_APPWIDGET = 0;
    /** Request code for creating a widget. */
    private static final int REQUEST_CREATE_APPWIDGET = 1;

    /** Extra code for APP_WIGDET_CONFIGURE. */
    private static final String EXTRA_APP_WIDGET_CONFIGURE = "EL_APP_WIDGET_CONFIGURE";

    /** Request code for app info. */
    private static final int ITEM_APPINFO = 1;
    /** Request code for app uninstall. */
    private static final int ITEM_UNINSTALL = 2;
    /** Request code for reset app counter. */
    private static final int ITEM_RESET = 3;
    /** Request code for toggle disabling app. */
    private static final int ITEM_TOGGLE_DISABLED = 4;
    /** Request code for removing the widget. */
    private static final int ITEM_REMOVE_WIDGET = 5;
    /** Request code for choosing widget. */
    private static final int ITEM_CHOOSE_WIDGET = 6;
    /** Request code for toggle sticky app. */
    private static final int ITEM_TOGGLE_STICKY = 7;
    /** Request code for layouting widget. */
    private static final int ITEM_LAYOUT_WIDGET = 8;

    // Layout constants for widget
    /** Layout constant for default full screen layout. */
    public static final int WIDGET_LAYOUT_FULL_SCREEN = -1;
    /** Layout constant for top half widget. */
    private static final int WIDGET_LAYOUT_TOP_QUARTER = 0;
    /** Layout constant for top third widget. */
    private static final int WIDGET_LAYOUT_TOP_THIRD = 5;
    /** Layout constant for top half widget. */
    private static final int WIDGET_LAYOUT_TOP_HALF = 10;
    /** Layout constant for center widget reduced. */
    private static final int WIDGET_LAYOUT_CENTER = 15;
    /** Layout constant for bottom half widget. */
    private static final int WIDGET_LAYOUT_BOTTOM_HALF = 20;
    /** Layout constant for bottom third widget. */
    private static final int WIDGET_LAYOUT_BOTTOM_THIRD = 25;
    /** Layout constant for bottom quarter widget. */
    private static final int WIDGET_LAYOUT_BOTTOM_QUARTER = 30;

    /** The launcher drawable. */
    public Drawable ic_launcher;
    /** The view switcher of the launcher. */
    private ViewSwitcher vsLauncher;
    /** The view for holding the widget. */
    private FrameLayout frWidget;
    /** The view for holding the widget filler for top. */
    public View vTopFiller;
    /** The view for holding the widget filler for bottom. */
    private View vBottomFiller;
    /** The views for launching the most used apps. */
    public final List<ImageView> dockImageViews = new ArrayList<>(HomeModel.NUMBER_OF_APPS);

    /** The model for home. */
    private HomeModel model;
    /** The manager for widgets. */
    private AppWidgetManager appWidgetManager;
    /** The host for widgets. */
    private AppWidgetHost appWidgetHost;
    /** The adapter for applications. */
    private DrawerListAdapter lvApplicationsAdapter;
    /** The asynchronous task for updating the list view. */
    private UpdateAsyncTask updateAsyncTask;
    /** The list of installed applications. */
    public final List<ApplicationModel> applicationModels = new ArrayList<>(0);
    /** The broadcast receiver for package changes. */
    private final BroadcastReceiver packageChangedBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            updateApplications();
        }
    };
    /** The temporary application model for context menus. */
    private ApplicationModel contextMenuApplicationModel;
    /** The temporary configure component for widgets. */
    private ComponentName widgetConfigure;

    /**
     * Adjust StrictMode based on environment parameters.
     */
    private void adjustStrictMode() {
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build());
        }
    }

    /**
     * Adjust the theme.
     * <br>
     * This function is necessary, because API 21-25 do not
     * support theme inheritance in emulators (and I assume in real
     * devices as well.
     */
    private void setTheme() {
        switch (Build.VERSION.SDK_INT) {
            case Build.VERSION_CODES.LOLLIPOP:
            case Build.VERSION_CODES.LOLLIPOP_MR1:
            case Build.VERSION_CODES.M:
            case Build.VERSION_CODES.N:
            case Build.VERSION_CODES.N_MR1:
            case Build.VERSION_CODES.O:
            case Build.VERSION_CODES.O_MR1:
                setTheme(R.style.API21ActivityStyle);
                break;
            default:
                // leave highest default
        }
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        setTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.launcher);

        // Adjust strict mode
        adjustStrictMode();

        /*
         * Assign components.
         */
        vsLauncher = findViewById(R.id.vsLauncher);
        frWidget = findViewById(R.id.frWidget);
        vTopFiller = findViewById(R.id.topFiller);
        vBottomFiller = findViewById(R.id.bottomFiller);

        ic_launcher = getDrawable(R.drawable.ic_launcher);

        final GridView lvApplications = findViewById(R.id.lvApplications);
        final ImageView ivDrawer = findViewById(R.id.ivDrawer);

        dockImageViews.add((ImageView) findViewById(R.id.ivDock1));
        dockImageViews.add((ImageView) findViewById(R.id.ivDock2));
        dockImageViews.add((ImageView) findViewById(R.id.ivDock3));
        dockImageViews.add((ImageView) findViewById(R.id.ivDock4));
        dockImageViews.add((ImageView) findViewById(R.id.ivDock5));
        dockImageViews.add((ImageView) findViewById(R.id.ivDock6));

        /*
         * Set handlers.
         */
        if (hasAppWidgets(this)) {
            ivDrawer.setOnCreateContextMenuListener(new DrawerContextMenuListener());
        }

        ivDrawer.setOnClickListener(new DrawerOnClickListener());

        for (int i = 0, dockImageViewsSize = dockImageViews.size(); i < dockImageViewsSize; i++) {
            final ImageView imageView = dockImageViews.get(i);
            imageView.setOnCreateContextMenuListener(new DockContextMenuListener());
        }

        lvApplications.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(
                    final AdapterView<?> adapterView,
                    final View view,
                    final int i,
                    final long l) {

                openApp(applicationModels.get(i));
            }
        });
        registerForContextMenu(lvApplications);
        lvApplications.setOnCreateContextMenuListener(new ApplicationsContextMenuListener());

        /*
         * Initialize data.
         */
        // Animate the image of the drawer button.
        final RippleDrawable rd = new RippleDrawable(ColorStateList.valueOf(Color.GRAY), ivDrawer.getDrawable(), null);
        ivDrawer.setImageDrawable(rd);

        // Initialize widget handling.
        if (hasAppWidgets(this)) {
            appWidgetManager = AppWidgetManager.getInstance(this);
            appWidgetHost = new AppWidgetHost(this, R.id.frWidget);
            appWidgetHost.startListening();
        }

        // Initialize applications adapter and set it.
        lvApplicationsAdapter = new DrawerListAdapter(this, applicationModels);

        lvApplications.setAdapter(lvApplicationsAdapter);
    }

    @Override
    protected void onStart() {
        super.onStart();

        model = HomeModel.getInstance(this);

        // Listen for changes
        final IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_INSTALL_PACKAGE);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        filter.addAction(Intent.ACTION_LOCALE_CHANGED);
        filter.addAction(Intent.ACTION_PACKAGE_FULLY_REMOVED);
        filter.addDataScheme("package");

        registerReceiver(packageChangedBroadcastReceiver, filter);

        // Go
        new LoadModelAsyncTask(this, model).execute();
        updateApplications();
    }

    @Override
    public void onBackPressed() {
        switchTo(HOME_ID);
    }

    @Override
    protected void onResume() {
        super.onResume();

        switchTo(HOME_ID);

        updateDock();
    }

    @Override
    protected void onActivityResult(
            final int requestCode,
            final int resultCode,
            final Intent data) {

        if (resultCode == RESULT_OK) {
            final Integer appWidgetId = data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);

            if (requestCode == REQUEST_CREATE_APPWIDGET) {
                createWidget(appWidgetId);
            } else if (requestCode == REQUEST_BIND_APPWIDGET) {
                if (widgetConfigure != null) {
                    configureWidget(appWidgetId, widgetConfigure);
                } else {
                    createWidget(appWidgetId);
                }
            }
        }

        widgetConfigure = null;
    }

    /**
     * Configure a widget for a provider.
     * @param appWidgetId the appWidgetId
     * @param configure the configure component
     */
    private void configureWidget(final Integer appWidgetId, final ComponentName configure) {
        // Abort on invalid input
        if (appWidgetId == -1) {
            return;
        }

        if (configure != null) {
            final Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
            intent.setComponent(configure);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            intent.putExtra(EXTRA_APP_WIDGET_CONFIGURE, configure);

            if (IntentUtil.isCallable(getPackageManager(), intent)) {
                startActivityForResult(intent, REQUEST_CREATE_APPWIDGET);
            }
        } else {
            // Configuring not necessary, go strait to creation
            createWidget(appWidgetId);
        }

        // Reset widget configure
        widgetConfigure = null;
    }

    /**
     * Bind a widget for a provider.
     * @param provider the provider component
     * @param configure the configure component
     */
    public void bindWidget(final ComponentName provider, final ComponentName configure) {
        final int appWidgetId = appWidgetHost.allocateAppWidgetId();

        if (appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, provider)) {
            // Binding is allowed, go straight to configuring
            configureWidget(appWidgetId, configure);
        } else {
            // Ask for permission
            widgetConfigure = configure;

            final Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_BIND);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider);
            // This is the options bundle discussed above
            //intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_OPTIONS, options);
            startActivityForResult(intent, REQUEST_BIND_APPWIDGET);
        }
    }

    @Override
    public boolean onContextItemSelected(final MenuItem item) {
        if (item.getIntent() == null && contextMenuApplicationModel != null) {
            switch (item.getItemId()) {
                case ITEM_RESET:
                    new ResetUsageAsyncTask(this, model).execute(contextMenuApplicationModel);
                    break;
                case ITEM_TOGGLE_DISABLED:
                    new ToggleDockAsyncTask(this, model).execute(contextMenuApplicationModel);
                    break;
                case ITEM_TOGGLE_STICKY:
                    new ToggleStickyAsyncTask(this, model).execute(contextMenuApplicationModel);
                    break;
                default:
                    break;
            }

            // "Consume" the model
            contextMenuApplicationModel = null;
            return true;
        } else {
            switch (item.getItemId()) {

                case ITEM_REMOVE_WIDGET:
                    // Reset layout
                    model.setKeyAppwidgetLayout(WIDGET_LAYOUT_FULL_SCREEN);
                    adjustWidget(WIDGET_LAYOUT_FULL_SCREEN);
                    // Remove widget
                    createWidget(-1);
                    break;
                case ITEM_CHOOSE_WIDGET:
                    final ShowWidgetListAsPopupMenuTask showWidgetListAsPopupMenuTask = new ShowWidgetListAsPopupMenuTask(this, appWidgetManager);
                    showWidgetListAsPopupMenuTask.execute();
                    break;
                case ITEM_LAYOUT_WIDGET:
                    final PopupMenu popupMenu = new PopupMenu(this, vTopFiller);

                    final int currentLayout = model.getAppWidgetLayout();

                    addLayoutPopupMenuItem(popupMenu, WIDGET_LAYOUT_FULL_SCREEN, currentLayout, R.string.widgetLayoutFull);
                    addLayoutPopupMenuItem(popupMenu, WIDGET_LAYOUT_TOP_QUARTER, currentLayout, R.string.widgetLayoutTopQuarter);
                    addLayoutPopupMenuItem(popupMenu, WIDGET_LAYOUT_TOP_THIRD, currentLayout, R.string.widgetLayoutTopThird);
                    addLayoutPopupMenuItem(popupMenu, WIDGET_LAYOUT_TOP_HALF, currentLayout, R.string.widgetLayoutTopHalf);
                    addLayoutPopupMenuItem(popupMenu, WIDGET_LAYOUT_CENTER, currentLayout, R.string.widgetLayoutCenter);
                    addLayoutPopupMenuItem(popupMenu, WIDGET_LAYOUT_BOTTOM_HALF, currentLayout, R.string.widgetLayoutBottomHalf);
                    addLayoutPopupMenuItem(popupMenu, WIDGET_LAYOUT_BOTTOM_THIRD, currentLayout, R.string.widgetLayoutBottomThird);
                    addLayoutPopupMenuItem(popupMenu, WIDGET_LAYOUT_BOTTOM_QUARTER, currentLayout, R.string.widgetLayoutBottomQuarter);

                    popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(final MenuItem item) {
                            model.setKeyAppwidgetLayout(item.getItemId());
                            adjustWidget(item.getItemId());
                            return true;
                        }
                    });

                    popupMenu.show();

                    break;
                default:
                    break;
            }
        }

        return false;
    }

    /**
     * Add a menu item for the layout popup menu.
     * @param popupMenu the popup menu
     * @param widgetLayout the layout for the item
     * @param currentLayout the currently active item
     * @param resourceId the resource for the string
     */
    private void addLayoutPopupMenuItem(final PopupMenu popupMenu, final int widgetLayout, final int currentLayout, final int resourceId) {
        final MenuItem menuItem = popupMenu.getMenu().add(0, widgetLayout, 0, resourceId);
        menuItem.setCheckable(true);
        menuItem.setChecked(currentLayout == widgetLayout);
    }

    @Override
    protected void onDestroy() {
        appWidgetHost.stopListening();
        //model.close();

        // Prevent memory leaks for receiver
        try {
            unregisterReceiver(packageChangedBroadcastReceiver);
        } catch (final IllegalArgumentException e) {
            // do nothing here
        }

        super.onDestroy();
    }

    /**
     * Open an app from the model.
     * @param applicationModel the model
     */
    private void openApp(final ApplicationModel applicationModel) {
        final ComponentName component = new ComponentName(applicationModel.packageName, applicationModel.className);
        final Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setComponent(component);

        if (IntentUtil.isCallable(getPackageManager(), intent)) {
            startActivity(intent);
            new LoadMostUsedAppsAsyncTask(this, model).execute(applicationModel);
        }
    }

    /**
     * Switch to a layout.
     *
     * @param id the id of the layout
     */
    public void switchTo(final int id) {
        switch (vsLauncher.getDisplayedChild()) {
            case HOME_ID:
                if (id == DRAWER_ID) {
                    vsLauncher.showNext();
                }
                break;
            case DRAWER_ID:
                if (id == HOME_ID) {
                    vsLauncher.showPrevious();
                }
                break;
            default:
                break;
        }
    }

    /**
     * @param context the context to check for
     * @return returns <code>true</code>, if <code>PackageManager.FEATURE_APP_WIDGETS</code>
     * is supported, otherwise <code>false</code>.
     */
    private boolean hasAppWidgets(final Context context) {
        final PackageManager pm = context.getPackageManager();
        return pm.hasSystemFeature(PackageManager.FEATURE_APP_WIDGETS);
    }

    /**
     * Create a widget from an intent.
     * @param appWidgetId the appWidgetId
     */
    private void createWidget(final Integer appWidgetId) {
        if (model.getAppWidgetId() > -1) {
            appWidgetHost.deleteAppWidgetId(model.getAppWidgetId());
            frWidget.removeAllViews();
        }

        model.setAppWidgetId(appWidgetId);
        addHostView(appWidgetId);
    }

    /**
     * Adjust widget layout according to layout id.
     * @param appWidgetLayout the layout id.
     */
    public void adjustWidget(final int appWidgetLayout) {
        final ViewGroup.LayoutParams bottomLayout = vBottomFiller.getLayoutParams();
        final ViewGroup.LayoutParams topLayout = vTopFiller.getLayoutParams();

        final int screenHeightDp = vsLauncher.getHeight();

        // Check for consistent values
        if (screenHeightDp == Configuration.SCREEN_HEIGHT_DP_UNDEFINED) {
            bottomLayout.height = 0;
            topLayout.height = 0;
            return;
        }

        final double height = screenHeightDp - DOCK_HEIGHT;

        // Check for enough space to accommodate another dock
        if (height - DOCK_HEIGHT <= 0) {
            bottomLayout.height = 0;
            topLayout.height = 0;
            return;
        }

        double preciseBottomHeight = 0;
        double preciseTopHeight = 0;

        switch (appWidgetLayout) {
            case WIDGET_LAYOUT_TOP_QUARTER: // widget in top 1/4
                preciseBottomHeight = height * THREE_QUARTER;
                break;
            case WIDGET_LAYOUT_TOP_THIRD: // widget in top 1/3
                preciseBottomHeight = height * TWO_THIRD;
                break;
            case WIDGET_LAYOUT_TOP_HALF: // widget in top 1/2
                preciseBottomHeight = height * HALF;
                break;
            case WIDGET_LAYOUT_CENTER: // widget center with height adjusted
                preciseBottomHeight = height * QUARTER;
                preciseTopHeight = preciseBottomHeight;
                break;
            case WIDGET_LAYOUT_BOTTOM_HALF: // widget in bottom 1/2
                preciseTopHeight = height * HALF;
                break;
            case WIDGET_LAYOUT_BOTTOM_THIRD: // widget in bottom 1/3
                preciseTopHeight = height * TWO_THIRD;
                break;
            case WIDGET_LAYOUT_BOTTOM_QUARTER: // widget in bottom 1/4
                preciseTopHeight = height * THREE_QUARTER;
                break;
            default: // default: -1
                break;
        }

        bottomLayout.height = (int) Math.round(preciseBottomHeight);
        vBottomFiller.requestLayout();

        topLayout.height = (int) Math.round(preciseTopHeight);
        vTopFiller.requestLayout();



    }

    /**
     * Add a host view to the frame layout for a widget id.
     * @param appWidgetId the widget id
     */
    public void addHostView(final int appWidgetId) {
        frWidget.removeAllViews();

        if (hasAppWidgets(this)) {
            final AppWidgetProviderInfo appWidgetInfo = appWidgetManager.getAppWidgetInfo(appWidgetId);
            if (appWidgetInfo != null) {
                final AppWidgetHostView hostView = appWidgetHost.createView(this, appWidgetId, appWidgetInfo);
                hostView.setAppWidget(appWidgetId, appWidgetInfo);

                frWidget.addView(hostView);
            } else {
                model.setAppWidgetId(-1);
            }
        }
    }

    /**
     * Update applications.
     */
    private void updateApplications() {
        if (updateAsyncTask != null && !updateAsyncTask.isCancelled()) {
            updateAsyncTask.cancel(true);
        }

        updateAsyncTask = new UpdateAsyncTask(this, model);
        updateAsyncTask.execute();
    }

    /**
     * Update dock.
     */
    private void updateDock() {
        new LoadMostUsedAppsAsyncTask(this, model).execute();
    }

    /**
     * Handle click on dock image.
     * @param imageView the image view that was clicked
     */
    private void onDockClick(final ImageView imageView) {
        if (imageView.getTag() instanceof ApplicationModel) {
            openApp((ApplicationModel) imageView.getTag());
        }
    }

    /**
     * Update the dock image to feature the application model.
     * @param imageView the view
     * @param applicationModel the model, can be <code>null</code>
     */
    public void updateDock(final ImageView imageView, final ApplicationModel applicationModel) {
        if (applicationModel == null) {
            if (imageView.getTag() != null) {
                imageView.setTag(null);
                imageView.setImageDrawable(ic_launcher);
                imageView.setOnClickListener(null);
                imageView.setContentDescription(null);
            }
        } else {
            final Object tag = imageView.getTag();

            if (tag instanceof ApplicationModel) {
                final ApplicationModel tagModel = (ApplicationModel) tag;

                if (tagModel.packageName.equals(applicationModel.packageName)
                        && tagModel.className.equals(applicationModel.className)
                        && tagModel.label.equals(applicationModel.label)
                ) {
                    return;
                }
            }

            final RippleDrawable rd = new RippleDrawable(ColorStateList.valueOf(Color.GRAY), applicationModel.icon, null);
            imageView.setImageDrawable(rd);
            imageView.setTag(applicationModel);
            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View view) {
                    if (view instanceof ImageView) {
                        onDockClick((ImageView) view);
                    }
                }
            });
            imageView.setContentDescription(applicationModel.label);
        }
    }

    /**
     *
     * @return the applications adapter for the list view
     */
    public DrawerListAdapter getListViewApplicationsAdapter() {
        return lvApplicationsAdapter;
    }

    /**
     * Listener for all applications context menu.
     */
    private class ApplicationsContextMenuListener implements View.OnCreateContextMenuListener {
        @Override
        public void onCreateContextMenu(final ContextMenu contextMenu, final View view, final ContextMenu.ContextMenuInfo contextMenuInfo) {
            final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) contextMenuInfo;
            final ApplicationModel applicationModel = applicationModels.get(info.position);
            contextMenuApplicationModel = applicationModel;

            contextMenu.setHeaderTitle(applicationModel.label);
            final MenuItem itemAppInfo = contextMenu.add(0, ITEM_APPINFO, 0, R.string.appinfo);
            itemAppInfo.setIntent(IntentUtil.newAppDetailsIntent(applicationModel.packageName));

            contextMenu.add(0, ITEM_RESET, 0, R.string.resetcounter);

            final MenuItem toggleDisabledItem = contextMenu.add(0, ITEM_TOGGLE_DISABLED, 0, R.string.showInDock);
            toggleDisabledItem.setCheckable(true);
            toggleDisabledItem.setChecked(!applicationModel.disabled);

            final MenuItem toggleStickyItem = contextMenu.add(0, ITEM_TOGGLE_STICKY, 0, R.string.showInDockSticky);
            toggleStickyItem.setCheckable(true);
            toggleStickyItem.setChecked(applicationModel.sticky);

            // Check for system apps
            try {
                ApplicationInfo ai = getPackageManager().getApplicationInfo(applicationModel.packageName, 0);
                if ((ai.flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) == 0) {
                    final MenuItem itemUninstall = contextMenu.add(0, ITEM_UNINSTALL, 1, R.string.uninstall);
                    itemUninstall.setIntent(IntentUtil.uninstallAppIntent(applicationModel.packageName));
                }
            } catch (PackageManager.NameNotFoundException e) {
                // Do nothing here
            }
        }
    }

    /**
     * Listener for dock icon context menu.
     */
    private class DockContextMenuListener implements View.OnCreateContextMenuListener {

        @Override
        public void onCreateContextMenu(final ContextMenu contextMenu, final View view, final ContextMenu.ContextMenuInfo contextMenuInfo) {
            if (view instanceof ImageView) {
                final ImageView contextImageView = (ImageView) view;

                if (contextImageView.getTag() instanceof ApplicationModel) {
                    final ApplicationModel model = (ApplicationModel) contextImageView.getTag();
                    contextMenuApplicationModel = model;

                    contextMenu.add(0, ITEM_RESET, 0, R.string.resetcounter);

                    final MenuItem toggleDisabledItem = contextMenu.add(0, ITEM_TOGGLE_DISABLED, 0, R.string.showInDock);
                    toggleDisabledItem.setCheckable(true);
                    toggleDisabledItem.setChecked(!model.disabled);

                    final MenuItem toggleStickyItem = contextMenu.add(0, ITEM_TOGGLE_STICKY, 0, R.string.showInDockSticky);
                    toggleStickyItem.setCheckable(true);
                    toggleStickyItem.setChecked(model.sticky);
                }
            }
        }
    }

    /**
     * Listener for drawer icon context menu.
     */
    private class DrawerContextMenuListener implements View.OnCreateContextMenuListener {

        @Override
        public void onCreateContextMenu(final ContextMenu contextMenu, final View view, final ContextMenu.ContextMenuInfo contextMenuInfo) {
            contextMenu.add(0, ITEM_CHOOSE_WIDGET, 0, R.string.choose_widget);
            if (model.getAppWidgetId() != -1) {
                contextMenu.add(0, ITEM_LAYOUT_WIDGET, 0, R.string.adjustWidgetLayout);
                contextMenu.add(0, ITEM_REMOVE_WIDGET, 0, R.string.remove_widget);
            }
        }
    }

    /**
     * Lister for drawer icon.
     */
    private class DrawerOnClickListener implements View.OnClickListener {

        @Override
        public void onClick(final View view) {
            switchTo(DRAWER_ID);
        }
    }

}
