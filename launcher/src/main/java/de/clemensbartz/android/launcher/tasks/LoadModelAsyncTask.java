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

import android.os.AsyncTask;
import android.util.Pair;

import java.lang.ref.WeakReference;

import de.clemensbartz.android.launcher.Launcher;
import de.clemensbartz.android.launcher.models.HomeModel;

/**
 * Async task for loading the model on start.
 * @author Clemens Bartz
 * @since 1.5
 */
public class LoadModelAsyncTask extends AsyncTask<Integer, Integer, Pair<Integer, Integer>> {

    /** Weak reference to {@link Launcher}. */
    private final WeakReference<Launcher> launcherWeakReference;

    /** Weak reference to {@link HomeModel}. */
    private final WeakReference<HomeModel> homeModelWeakReference;

    /**
     * Create a new task for loading the model.
     * @param launcher the launcher
     * @param model the model
     */
    public LoadModelAsyncTask(final Launcher launcher, final HomeModel model) {
        launcherWeakReference = new WeakReference<>(launcher);
        homeModelWeakReference = new WeakReference<>(model);
    }

    @Override
    protected Pair<Integer, Integer> doInBackground(final Integer... params) {
        final HomeModel model = homeModelWeakReference.get();

        if (model != null) {
            model.loadValues();

            return new Pair<>(model.getAppWidgetId(), model.getAppWidgetLayout());
        }

        return null;
    }

    @Override
    protected void onPostExecute(final Pair<Integer, Integer> result) {
        final Launcher launcher = launcherWeakReference.get();

        if (result != null && launcher != null) {
            // Show last selected widget.
            if (result.first > -1) {
                launcher.addHostView(result.first);
            }

            // Layout widget
            launcher.adjustWidget(result.second);
        }
    }
}
