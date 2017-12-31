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

package de.clemensbartz.android.launcher.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Helper class for opening a SQLite database.
 *
 * @author Clemens Bartz
 * @since 1.0
 */
public final class ApplicationUsageDbHelper extends SQLiteOpenHelper {
    /** The instance for static lookup. */
    private static ApplicationUsageDbHelper instance;
    /** The instance for application context. */
    private static Context applicationContext;
    /** The version of the database. */
    private static final int DATABASE_VERSION = 3;
    /** The database name. */
    private static final String DATABASE_NAME = "ApplicationUsage.db";

    /**
     *
     * @return the application context
     */
    public static Context getApplicationContext() {
        return applicationContext;
    }

    /**
     *
     * @param context initialize with a context
     * @return the current instance
     */
    public static SQLiteOpenHelper getInstance(final Context context) {
        if (instance == null) {
            applicationContext = context.getApplicationContext();
            instance = new ApplicationUsageDbHelper(applicationContext);
        }

        return instance;
    }

    /**
     * Create a new helper class in a context.
     * @param context the context
     */
    private ApplicationUsageDbHelper(final Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(final SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(ApplicationUsageModel.CREATE_SQL);
    }

    @Override
    public void onUpgrade(final SQLiteDatabase sqLiteDatabase,
                          final int oldVersion,
                          final int newVersion) {
        if (newVersion - oldVersion != 1) {
            recreateDatabase(sqLiteDatabase);
        } else {
            switch (oldVersion) {
                case 2:
                    sqLiteDatabase.beginTransaction();

                    sqLiteDatabase.execSQL(ApplicationUsageModel.ALTER_TABLE_2);
                    sqLiteDatabase.execSQL(ApplicationUsageModel.UPDATE_CONTENT_2);

                    sqLiteDatabase.endTransaction();
                default:
                    recreateDatabase(sqLiteDatabase);
                    break;
            }
        }
    }

    /**
     * Drop database and recreate a new one.
     * @param sqLiteDatabase the database to use
     */
    private void recreateDatabase(final SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(ApplicationUsageModel.DROP_SQL);
        onCreate(sqLiteDatabase);
    }
}
