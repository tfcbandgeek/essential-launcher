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

import java.lang.ref.WeakReference;

import de.clemensbartz.android.launcher.Launcher;
import de.clemensbartz.android.launcher.models.ApplicationModel;
import de.clemensbartz.android.launcher.models.HomeModel;

/**
 * Toggle sticky visibility for an application.
 * @since 1.5
 * @author Clemens Bartz
 */
public final class ToggleStickyAsyncTask extends AsyncTask<ApplicationModel, Integer, Integer> {

    /** Weak reference to {@link Launcher}. */
    private final WeakReference<Launcher> launcherWeakReference;
    /** Weak reference to {@link HomeModel}. */
    private final WeakReference<HomeModel> homeModelWeakReference;

    /**
     * Create a new task for toggling the visibility.
     * @param launcher the launcher
     * @param model the model
     */
    public ToggleStickyAsyncTask(final Launcher launcher, final HomeModel model) {
        launcherWeakReference = new WeakReference<>(launcher);
        homeModelWeakReference = new WeakReference<>(model);
    }

    @Override
    protected Integer doInBackground(final ApplicationModel... applicationModels) {
        final HomeModel model = homeModelWeakReference.get();

        if (model != null) {
            for (ApplicationModel applicationModel : applicationModels) {
                model.toggleSticky(applicationModel.packageName, applicationModel.className);
            }
        }

        return 0;
    }

    @Override
    protected void onPostExecute(final Integer result) {
        final Launcher launcher = launcherWeakReference.get();
        final HomeModel model = homeModelWeakReference.get();

        if (launcher != null && model != null) {
            new UpdateAsyncTask(launcher, model).execute();
            new LoadMostUsedAppsAsyncTask(launcher, model).execute();
            launcher.switchTo(Launcher.HOME_ID);
        }
    }
}
