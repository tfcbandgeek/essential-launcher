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

package de.clemensbartz.android.launcher.util;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;

import java.util.List;

/**
 * Utility class for constructing intents.
 */
public final class IntentUtil {

    /**
     * Hidden constructor.
     */
    private IntentUtil() {
        // Hidden constructor
    }

    /**
     * <p>Intent to show an applications details page in (Settings) com.android.settings.</p>
     *
     * @param packageName   The package name of the application
     * @return the intent to open the application info screen.
     */
    public static Intent newAppDetailsIntent(final String packageName) {
        final Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setData(Uri.parse("package:" + packageName));
        return intent;
    }

    /**
     * Intent to uninstall applications.
     *
     * @param packageName the package name of the application
     * @return the intent to uninstall application.
     */
    public static Intent uninstallAppIntent(final String packageName) {
        Intent intent = new Intent(Intent.ACTION_DELETE);
        intent.setData(Uri.parse("package:" + packageName));
        return intent;
    }

    /**
     * Create an intent for calling the ACTION_APPWIDGET_CONFIGURE activity.
     * @param componentName the component
     * @return the intent
     */
    public static Intent createWidgetConfigureIntent(final ComponentName componentName) {
        final Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);

        intent.setComponent(componentName);

        return intent;
    }

    /**
     * Create an intent for calling the ACTION_APPWIDGET_CONFIGURE activity for a specific
     * appWidgetId
     * @param componentName the component
     * @param appWidgetId the appWidgetId
     * @return the intent
     */
    public static Intent createWidgetConfigureIntent(final ComponentName componentName, final Integer appWidgetId) {
        final Intent intent = createWidgetConfigureIntent(componentName);

        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);

        return intent;
    }

    /**
     * Check if an intent is callable.
     * @param intent the intent
     * @return if it is callable.
     */
    public static boolean isCallable(final PackageManager pm, final Intent intent) {
        // Thank you to Google Keep for ruining the show: java.lang.SecurityException: Permission Denial: starting Intent [...] not exported from uid
        final List<ResolveInfo> resolveInfos = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);

        for (ResolveInfo resolveInfo : resolveInfos) {
            if (resolveInfo.activityInfo.exported) {
                return true;
            }
        }

        return false;
    }
}
