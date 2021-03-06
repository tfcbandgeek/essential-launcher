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

package de.clemensbartz.android.launcher.models;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.drawable.BitmapDrawable;

import java.util.ArrayList;
import java.util.List;

import de.clemensbartz.android.launcher.caches.IconCache;
import de.clemensbartz.android.launcher.db.ApplicationUsageDbHelper;
import de.clemensbartz.android.launcher.db.ApplicationUsageModel;
import de.clemensbartz.android.launcher.util.BitmapUtil;

/**
 * Model class for HomeActivity.
 *
 * @author Clemens Bartz
 * @since 1.0
 */
public final class HomeModel {

    /** The total cached number of apps. */
    public static final int NUMBER_OF_APPS = 6;
    /** Constant for descending sorting. */
    private static final String SPACE_DESC = " DESC";
    /** Columns of ApplicationUsage. */
    private static final String[] COLUMNS = {
            ApplicationUsageModel.ApplicationUsage.COLUMN_NAME_PACKAGE_NAME,
            ApplicationUsageModel.ApplicationUsage.COLUMN_NAME_CLASS_NAME,
            ApplicationUsageModel.ApplicationUsage.COLUMN_NAME_USAGE,
            ApplicationUsageModel.ApplicationUsage.COLUMN_NAME_DISABLED,
            ApplicationUsageModel.ApplicationUsage.COLUMN_NAME_STICKY
    };
    /** Order by sticky DESC, usage DESC, package name DESC, class name DESC constant. */
    private static final String ORDER_BY =
            ApplicationUsageModel.ApplicationUsage.COLUMN_NAME_STICKY
                    + SPACE_DESC
            + ", "
            + ApplicationUsageModel.ApplicationUsage.COLUMN_NAME_USAGE
                    + SPACE_DESC
            + ", "
            + ApplicationUsageModel.ApplicationUsage.COLUMN_NAME_PACKAGE_NAME
                    + SPACE_DESC
            + ", "
            + ApplicationUsageModel.ApplicationUsage.COLUMN_NAME_CLASS_NAME
                    + SPACE_DESC;
    /** Filter for package name and class name constant. */
    private static final String SELECTION =
            ApplicationUsageModel.ApplicationUsage.COLUMN_NAME_PACKAGE_NAME
                    + "=? AND "
            + ApplicationUsageModel.ApplicationUsage.COLUMN_NAME_CLASS_NAME
                    + "=?";
    /** Where statement for getting only enabled applications. */
    private static final String WHERE =
            ApplicationUsageModel.ApplicationUsage.COLUMN_NAME_DISABLED
                    + "=0 AND ("
            + ApplicationUsageModel.ApplicationUsage.COLUMN_NAME_USAGE
                    + ">0 OR "
            + ApplicationUsageModel.ApplicationUsage.COLUMN_NAME_STICKY
                    + ">0)";

    /** Database helper. */
    private final SQLiteOpenHelper dbHelper;
    /** Package manager. */
    private final PackageManager pm;

    /** Preferences value. */
    private final SharedPreferences preferences;

    /** Cache for most used applications. */
    private final List<ApplicationModel> mostUsedApplications =
            new ArrayList<>(NUMBER_OF_APPS);

    /** Writable SQLiteDatabase. */
    private SQLiteDatabase writableDatabase;

    /** The instance in during application life cycle. */
    private static HomeModel instance;

    /** Key for the appWidgetId property. */
    private static final String KEY_APPWIDGET_ID = "appWidgetId";
    ///** Key for the hide overlay property. */
    //This key has been removed as of version 1.3.
    //private static final String KEY_HIDE_OVERLAY_ID = "hideOverlay";
    /** Value for the appWidgetId property. */
    private int appWidgetId = -1;

    /**
     *
     * @param activity the Activity
     * @return the instance of the home model.
     */
    public static HomeModel getInstance(final Activity activity) {
        if (instance == null) {
            instance = new HomeModel(activity);
        }

        return instance;
    }


    /**
     * Create a new model in a context.
     * @param context the context
     */
    private HomeModel(final Activity context) {
        preferences = context.getPreferences(Context.MODE_PRIVATE);
        dbHelper = ApplicationUsageDbHelper.getInstance(context);
        pm = context.getApplicationContext().getPackageManager();
    }

    /**
     * Get a database for a mode.
     * @return the database or a readable database for unsupported modes
     */
    private SQLiteDatabase getDatabase() {
        if (writableDatabase == null || !(writableDatabase.isOpen())) {
            writableDatabase = dbHelper.getWritableDatabase();
        }

        return writableDatabase;
    }

    /**
     * Load preference values.
     * @param resources the activity this was called from
     * @param iconCache the icon cache to get the icons from
     */
    public void loadValues(final Resources resources, final IconCache iconCache) {
        updateApplications(resources, iconCache);

        appWidgetId = preferences.getInt(KEY_APPWIDGET_ID, -1);
    }

    /**
     *
     * @return the list of most used applications
     */
    public List<ApplicationModel> getMostUsedApplications() {
        return mostUsedApplications;
    }

    /**
     * Update the list of applications.
     * <p/>
     * This method has to be called from an async task.
     * @param resources the activity this was called from
     * @param iconCache the icon cache to get the icons from
     */
    public void updateApplications(final Resources resources, final IconCache iconCache) {
        final SQLiteDatabase db = getDatabase();

        mostUsedApplications.clear();

        boolean success;

        do {
            success = true;

            Cursor c = null;
            try {
                c = db.query(ApplicationUsageModel.ApplicationUsage.TABLE_NAME,
                        COLUMNS, WHERE, null, null, null,
                        ORDER_BY, Integer.toString(NUMBER_OF_APPS));

                if (c != null) {
                    for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
                        String packageName;
                        String className;
                        boolean disabled;
                        boolean sticky;
                        try {
                            packageName = c.getString(c.getColumnIndexOrThrow(ApplicationUsageModel.ApplicationUsage.COLUMN_NAME_PACKAGE_NAME));
                            className = c.getString(c.getColumnIndexOrThrow(ApplicationUsageModel.ApplicationUsage.COLUMN_NAME_CLASS_NAME));
                            disabled = c.getInt(c.getColumnIndexOrThrow(ApplicationUsageModel.ApplicationUsage.COLUMN_NAME_DISABLED)) > 0;
                            sticky = c.getInt(c.getColumnIndexOrThrow(ApplicationUsageModel.ApplicationUsage.COLUMN_NAME_STICKY)) > 0;
                        } catch (IllegalArgumentException e) {
                            continue;
                        }
                        try {
                            final ComponentName componentName = new ComponentName(packageName, className);
                            final ActivityInfo info = pm.getActivityInfo(componentName, 0);
                            if (!info.enabled) {
                                delete(packageName, className);
                            }
                            final ApplicationModel applicationModel = new ApplicationModel();
                            applicationModel.packageName = packageName;
                            applicationModel.className = className;
                            applicationModel.disabled = disabled;
                            applicationModel.sticky = sticky;

                            // Delete null-loaded values
                            if (packageName == null || className == null) {
                                delete(packageName, className);
                            }

                            applicationModel.label = info.loadLabel(pm);

                            final String key = BitmapUtil.createKey(applicationModel.packageName, applicationModel.className);
                            applicationModel.icon = iconCache.getIcon(key);
                            if (applicationModel.icon == null) {
                                final BitmapDrawable bitmapDrawable = BitmapUtil.resizeDrawable(resources, info.loadIcon(pm));
                                iconCache.create(key, bitmapDrawable);
                                applicationModel.icon = bitmapDrawable;
                            }

                            mostUsedApplications.add(applicationModel);
                        } catch (PackageManager.NameNotFoundException e) {
                            /* Although Android Studio claims,
                                these values can be null at this point!
                             */
                            if (packageName != null && className != null) {
                                delete(packageName, className);
                            }
                            success = false;
                            break;
                        }
                    }
                }
            } catch (final IllegalArgumentException e) {
                success = true;
            } finally {
                if (c != null) {
                    c.close();
                }
            }
        } while (!success);
    }

    /**
     * Toggle the sticky state for an application.
     * @param packageName the package name
     * @param className the class name
     */
    public void toggleSticky(final String packageName, final String className) {
        if (packageName == null || className == null) {
            delete(packageName, className);
            return;
        }

        final SQLiteDatabase db = getDatabase();

        db.beginTransaction();
        Cursor c = null;
        try {
            // Get entry
            c = db.query(ApplicationUsageModel.ApplicationUsage.TABLE_NAME,
                    new String[]{
                            ApplicationUsageModel.ApplicationUsage.COLUMN_NAME_STICKY
                    },
                    SELECTION, new String[]{packageName, className},
                    null, null, null);
            if (c != null) {
                if (c.getCount() > 1) {
                    delete(packageName, className);
                }
                if (c.moveToFirst()) {
                    final boolean sticky = c.getInt(c.getColumnIndexOrThrow(ApplicationUsageModel.ApplicationUsage.COLUMN_NAME_STICKY)) > 0;
                    // update
                    final ContentValues values = new ContentValues(3);
                    values.put(ApplicationUsageModel.ApplicationUsage.COLUMN_NAME_CLASS_NAME, className);
                    values.put(ApplicationUsageModel.ApplicationUsage.COLUMN_NAME_PACKAGE_NAME, packageName);
                    values.put(ApplicationUsageModel.ApplicationUsage.COLUMN_NAME_STICKY, !sticky);

                    db.update(ApplicationUsageModel.ApplicationUsage.TABLE_NAME,
                            values, SELECTION, new String[]{packageName, className});
                    db.setTransactionSuccessful();
                } else {
                    // insert
                    final ContentValues values = new ContentValues(4);
                    values.put(ApplicationUsageModel.ApplicationUsage.COLUMN_NAME_CLASS_NAME, className);
                    values.put(ApplicationUsageModel.ApplicationUsage.COLUMN_NAME_PACKAGE_NAME, packageName);
                    values.put(ApplicationUsageModel.ApplicationUsage.COLUMN_NAME_USAGE, 0);
                    values.put(ApplicationUsageModel.ApplicationUsage.COLUMN_NAME_DISABLED, false);
                    values.put(ApplicationUsageModel.ApplicationUsage.COLUMN_NAME_STICKY, true);

                    db.insertOrThrow(ApplicationUsageModel.ApplicationUsage.TABLE_NAME, null, values);
                    db.setTransactionSuccessful();
                }
            }
        } finally {
            db.endTransaction();
            if (c != null) {
                c.close();
            }
        }
    }

    /**
     * Toggle the disabled state for an application.
     * @param packageName the package name
     * @param className the class name
     */
    public void toggleDisabled(final String packageName, final String className) {
        if (packageName == null || className == null) {
            delete(packageName, className);
            return;
        }

        final SQLiteDatabase db = getDatabase();

        db.beginTransaction();
        Cursor c = null;
        try {
            // Get entry
            c = db.query(ApplicationUsageModel.ApplicationUsage.TABLE_NAME,
                    new String[]{
                            ApplicationUsageModel.ApplicationUsage.COLUMN_NAME_DISABLED
                    },
                    SELECTION, new String[]{packageName, className},
                    null, null, null);
            if (c != null) {
                if (c.getCount() > 1) {
                    delete(packageName, className);
                }
                if (c.moveToFirst()) {
                    final boolean disabled = c.getInt(c.getColumnIndexOrThrow(ApplicationUsageModel.ApplicationUsage.COLUMN_NAME_DISABLED)) > 0;
                    // update
                    final ContentValues values = new ContentValues(3);
                    values.put(ApplicationUsageModel.ApplicationUsage.COLUMN_NAME_CLASS_NAME, className);
                    values.put(ApplicationUsageModel.ApplicationUsage.COLUMN_NAME_PACKAGE_NAME, packageName);
                    values.put(ApplicationUsageModel.ApplicationUsage.COLUMN_NAME_DISABLED, !disabled);

                    db.update(ApplicationUsageModel.ApplicationUsage.TABLE_NAME,
                            values, SELECTION, new String[]{packageName, className});
                    db.setTransactionSuccessful();
                } else {
                    // insert
                    final ContentValues values = new ContentValues(4);
                    values.put(ApplicationUsageModel.ApplicationUsage.COLUMN_NAME_CLASS_NAME, className);
                    values.put(ApplicationUsageModel.ApplicationUsage.COLUMN_NAME_PACKAGE_NAME, packageName);
                    values.put(ApplicationUsageModel.ApplicationUsage.COLUMN_NAME_USAGE, 0);
                    values.put(ApplicationUsageModel.ApplicationUsage.COLUMN_NAME_DISABLED, true);
                    values.put(ApplicationUsageModel.ApplicationUsage.COLUMN_NAME_STICKY, false);

                    db.insertOrThrow(ApplicationUsageModel.ApplicationUsage.TABLE_NAME, null, values);
                    db.setTransactionSuccessful();
                }
            }
        } finally {
            db.endTransaction();
            if (c != null) {
                c.close();
            }
        }
    }

    /**
     * Check if an application is sticky.
     * @param packageName the package name
     * @param className the class name
     * @return if the application is sticky
     */
    public boolean isSticky(final String packageName, final String className) {
        if (packageName == null || className == null) {
            delete(packageName, className);
            return false;
        }

        final SQLiteDatabase db = getDatabase();

        boolean sticky = false;

        db.beginTransaction();
        Cursor c = null;
        try {
            // Get entry
            c = db.query(ApplicationUsageModel.ApplicationUsage.TABLE_NAME,
                    new String[]{
                            ApplicationUsageModel.ApplicationUsage.COLUMN_NAME_STICKY
                    },
                    SELECTION, new String[]{packageName, className},
                    null, null, null);
            if (c != null) {
                if (c.getCount() > 1) {
                    delete(packageName, className);
                }
                if (c.moveToFirst()) {
                    sticky = c.getInt(c.getColumnIndexOrThrow(ApplicationUsageModel.ApplicationUsage.COLUMN_NAME_STICKY)) > 0;
                }
            }
        } finally {
            db.endTransaction();
            if (c != null) {
                c.close();
            }
        }

        return sticky;
    }

    /**
     * Check if an application is disabled.
     * @param packageName the package name
     * @param className the class name
     * @return if the application is disabled
     */
    public boolean isDisabled(final String packageName, final String className) {
        if (packageName == null || className == null) {
            delete(packageName, className);
            return true;
        }

        final SQLiteDatabase db = getDatabase();

        boolean disabled = false;

        db.beginTransaction();
        Cursor c = null;
        try {
            // Get entry
            c = db.query(ApplicationUsageModel.ApplicationUsage.TABLE_NAME,
                    new String[]{
                            ApplicationUsageModel.ApplicationUsage.COLUMN_NAME_DISABLED
                    },
                    SELECTION, new String[]{packageName, className},
                    null, null, null);
            if (c != null) {
                if (c.getCount() > 1) {
                    delete(packageName, className);
                }
                if (c.moveToFirst()) {
                    disabled = c.getInt(c.getColumnIndexOrThrow(ApplicationUsageModel.ApplicationUsage.COLUMN_NAME_DISABLED)) > 0;
                }
            }
        } finally {
            db.endTransaction();
            if (c != null) {
                c.close();
            }
        }

        return disabled;
    }

    /**
     * Reset the counter for an application.
     * @param packageName the package name
     * @param className the class name
     * @param resources the activity this was called from
     * @param iconCache the icon cache to get the icons from
     */
    public void resetUsage(final String packageName, final String className, final Resources resources, final IconCache iconCache) {
        final SQLiteDatabase db = getDatabase();

        db.beginTransaction();
        Cursor c = null;
        try {
            // Get entry
            c = db.query(ApplicationUsageModel.ApplicationUsage.TABLE_NAME,
                    new String[]{
                            ApplicationUsageModel.ApplicationUsage.COLUMN_NAME_USAGE
                    },
                    SELECTION, new String[]{packageName, className},
                    null, null, null);
            if (c != null) {
                if (c.getCount() > 1) {
                    delete(packageName, className);
                }
                if (c.moveToFirst()) {
                    // update
                    final ContentValues values = new ContentValues(3);
                    values.put(ApplicationUsageModel.ApplicationUsage.COLUMN_NAME_CLASS_NAME, className);
                    values.put(ApplicationUsageModel.ApplicationUsage.COLUMN_NAME_PACKAGE_NAME, packageName);
                    values.put(ApplicationUsageModel.ApplicationUsage.COLUMN_NAME_USAGE, 0);

                    db.update(ApplicationUsageModel.ApplicationUsage.TABLE_NAME,
                            values, SELECTION, new String[]{packageName, className});
                    db.setTransactionSuccessful();
                } else {
                    // insert
                    final ContentValues values = new ContentValues(4);
                    values.put(ApplicationUsageModel.ApplicationUsage.COLUMN_NAME_CLASS_NAME, className);
                    values.put(ApplicationUsageModel.ApplicationUsage.COLUMN_NAME_PACKAGE_NAME, packageName);
                    values.put(ApplicationUsageModel.ApplicationUsage.COLUMN_NAME_USAGE, 0);
                    values.put(ApplicationUsageModel.ApplicationUsage.COLUMN_NAME_DISABLED, false);
                    values.put(ApplicationUsageModel.ApplicationUsage.COLUMN_NAME_STICKY, false);

                    db.insertOrThrow(ApplicationUsageModel.ApplicationUsage.TABLE_NAME, null, values);
                    db.setTransactionSuccessful();
                }
            }
        } finally {
            db.endTransaction();
            if (c != null) {
                c.close();
            }
        }

        updateApplications(resources, iconCache);
    }

    /**
     * Increase the counter of an app.
     * @param packageName the package name
     * @param className the class name
     * @param resources the activity this was called from
     * @param iconCache the icon cache to get the icons from
     */
    public void addUsage(final String packageName, final String className, final Resources resources, final IconCache iconCache) {
        if (packageName == null || className == null) {
            delete(packageName, className);
            return;
        }

        final SQLiteDatabase db = getDatabase();

        db.beginTransaction();
        Cursor c = null;
        try {
            // Get entry
            c = db.query(ApplicationUsageModel.ApplicationUsage.TABLE_NAME,
                    new String[]{
                            ApplicationUsageModel.ApplicationUsage.COLUMN_NAME_USAGE
                    },
                    SELECTION, new String[]{packageName, className},
                    null, null, null);
            if (c != null) {
                if (c.getCount() > 1) {
                    delete(packageName, className);
                }
                if (c.moveToFirst()) {
                    // update
                    final int usage = c.getInt(c.getColumnIndexOrThrow(ApplicationUsageModel.ApplicationUsage.COLUMN_NAME_USAGE));

                    if (usage < Integer.MAX_VALUE) {
                        final ContentValues values = new ContentValues(3);
                        values.put(ApplicationUsageModel.ApplicationUsage.COLUMN_NAME_CLASS_NAME, className);
                        values.put(ApplicationUsageModel.ApplicationUsage.COLUMN_NAME_PACKAGE_NAME, packageName);
                        values.put(ApplicationUsageModel.ApplicationUsage.COLUMN_NAME_USAGE, usage + 1);

                        db.update(ApplicationUsageModel.ApplicationUsage.TABLE_NAME,
                                values, SELECTION, new String[]{packageName, className});
                        db.setTransactionSuccessful();
                    } else {
                        resetUsage(packageName, className, resources, iconCache);
                    }
                } else {
                    // insert
                    final ContentValues values = new ContentValues(4);
                    values.put(ApplicationUsageModel.ApplicationUsage.COLUMN_NAME_CLASS_NAME, className);
                    values.put(ApplicationUsageModel.ApplicationUsage.COLUMN_NAME_PACKAGE_NAME, packageName);
                    values.put(ApplicationUsageModel.ApplicationUsage.COLUMN_NAME_USAGE, 1);
                    values.put(ApplicationUsageModel.ApplicationUsage.COLUMN_NAME_DISABLED, false);
                    values.put(ApplicationUsageModel.ApplicationUsage.COLUMN_NAME_STICKY, false);

                    db.insertOrThrow(ApplicationUsageModel.ApplicationUsage.TABLE_NAME, null, values);
                    db.setTransactionSuccessful();
                }
            }
        } finally {
            db.endTransaction();
            if (c != null) {
                c.close();
            }
        }

        updateApplications(resources, iconCache);
    }

    /**
     * Delete all entries for packageName and className.
     * @param packageName the package name
     * @param className the class name
     */
    private void delete(final String packageName, final String className) {
        final SQLiteDatabase db = getDatabase();
        db.delete(ApplicationUsageModel.ApplicationUsage.TABLE_NAME,
                SELECTION, new String[]{packageName, className});
    }

    /**
     *
     * @return the id of the app widget
     */
    public int getAppWidgetId() {
        return appWidgetId;
    }

    /**
     * Set the currently id of the app widget.
     * @param appWidgetId the id of the app widget
     */
    public void setAppWidgetId(final int appWidgetId) {
        preferences.edit().putInt(KEY_APPWIDGET_ID, appWidgetId).apply();
        this.appWidgetId = appWidgetId;
    }

}
