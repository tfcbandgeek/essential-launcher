/*
 * Copyright (C) 2018  Clemens Bartz
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

package de.clemensbartz.android.launcher.tasks;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.view.MenuItem;
import android.widget.PopupMenu;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import de.clemensbartz.android.launcher.Launcher;
import de.clemensbartz.android.launcher.util.IntentUtil;

/**
 * This task will list all widget providers and show them in a popup menu for an activity.
 * @author Clemens Bartz
 * @since 1.5
 */
public class ShowWidgetListAsPopupMenuTask extends AsyncTask<Integer, Integer, List<ShowWidgetListAsPopupMenuTask.FilledAppWidgetProviderInfo>> {

    /** The weak reference to our activity where the widget list should be shown. */
    private final WeakReference<Launcher> launcherWeakReference;
    /** The weak reference to the app widget manager. */
    private final WeakReference<AppWidgetManager> appWidgetManagerWeakReference;

    /**
     * Create a new widget list task. When the listing is done, show the popup menu in
     * the activity.
     * @param activity the activity to show the popup menu
     * @param appWidgetManager the app widget manager
     */
    public ShowWidgetListAsPopupMenuTask(
            final Launcher activity,
            final AppWidgetManager appWidgetManager) {

        this.launcherWeakReference = new WeakReference<>(activity);
        this.appWidgetManagerWeakReference = new WeakReference<>(appWidgetManager);
    }

    @Override
    protected List<FilledAppWidgetProviderInfo> doInBackground(final Integer... integers) {

        final AppWidgetManager appWidgetManager = appWidgetManagerWeakReference.get();
        final Launcher launcher = launcherWeakReference.get();

        if (appWidgetManager == null || launcher == null) {
            return null;
        }

        final PackageManager pm = launcher.getPackageManager();

        final List<AppWidgetProviderInfo> appWidgetProviderInfos = appWidgetManager.getInstalledProviders();
        final List<FilledAppWidgetProviderInfo> infoList = new ArrayList<>(appWidgetProviderInfos.size());

        for (AppWidgetProviderInfo appWidgetProviderInfo : appWidgetProviderInfos) {

            // Check if configure activity is exported, i. e. callable
            if (appWidgetProviderInfo.configure != null) {
                final Intent intent = IntentUtil.createWidgetConfigureIntent(appWidgetProviderInfo.configure);

                if (!IntentUtil.isCallable(pm, intent)) {
                    continue;
                }
            }

            // Fill info
            final FilledAppWidgetProviderInfo info = new FilledAppWidgetProviderInfo();
            info.label = appWidgetProviderInfo.loadLabel(pm);
            info.provider = appWidgetProviderInfo.provider;
            info.configure = appWidgetProviderInfo.configure;

            infoList.add(info);
        }

        Collections.sort(infoList, new Comparator<FilledAppWidgetProviderInfo>() {
            @Override
            public int compare(final FilledAppWidgetProviderInfo o1, final FilledAppWidgetProviderInfo o2) {
                return o1.label.compareTo(o2.label);
            }
        });

        return infoList;
    }

    @Override
    protected void onPostExecute(final List<FilledAppWidgetProviderInfo> appWidgetProviderInfos) {

        final Launcher launcher = launcherWeakReference.get();

        if (appWidgetProviderInfos != null && launcher != null && appWidgetProviderInfos.size() > 0) {
            final PopupMenu popupMenu = new PopupMenu(launcher, launcher.vTopFiller);

            for (ShowWidgetListAsPopupMenuTask.FilledAppWidgetProviderInfo info : appWidgetProviderInfos) {
                final MenuItem menuItem = popupMenu.getMenu().add(info.label);

                final Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
                intent.setComponent(info.configure);
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider);

                menuItem.setIntent(intent);
            }

            popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(final MenuItem item) {
                    final ComponentName provider = item.getIntent().getParcelableExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER);

                    if (provider != null) {
                        launcher.bindWidget(provider, item.getIntent().getComponent());
                    }

                    return true;
                }
            });

            popupMenu.show();
        }
    }

    /**
     * Holds filled AppWidgetProviderInfo.
     */
    public class FilledAppWidgetProviderInfo {
        /** The label for the provider. */
        public String label;
        /** The provider component. */
        public ComponentName provider;
        /** The configure component. */
        public ComponentName configure;
    }
}
