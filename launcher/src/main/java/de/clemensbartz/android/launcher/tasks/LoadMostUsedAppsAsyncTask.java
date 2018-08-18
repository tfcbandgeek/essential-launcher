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
import java.util.List;

import de.clemensbartz.android.launcher.Launcher;
import de.clemensbartz.android.launcher.models.ApplicationModel;
import de.clemensbartz.android.launcher.models.DockUpdateModel;
import de.clemensbartz.android.launcher.models.HomeModel;

/**
 * Async task for loading the most used applications.
 * @since 1.5
 * @author Clemens Bartz
 */
public final class LoadMostUsedAppsAsyncTask extends AsyncTask<ApplicationModel, DockUpdateModel, Integer> {
    /** Weak reference to {@link Launcher}. */
    private final WeakReference<Launcher> launcherWeakReference;
    /** Weak reference to {@link HomeModel}. */
    private final WeakReference<HomeModel> homeModelWeakReference;

    /**
     * Create a new task for loading the most used applications.
     * @param launcher the launcher
     * @param model the model
     */
    public LoadMostUsedAppsAsyncTask(final Launcher launcher, final HomeModel model) {
        launcherWeakReference = new WeakReference<>(launcher);
        homeModelWeakReference = new WeakReference<>(model);
    }

    @Override
    protected Integer doInBackground(final ApplicationModel... applicationModels) {
        final HomeModel model = homeModelWeakReference.get();
        final Launcher launcher = launcherWeakReference.get();

        if (model != null && launcher != null) {
            for (ApplicationModel applicationModel : applicationModels) {
                model.addUsage(applicationModel.packageName, applicationModel.className);
            }

            if (applicationModels.length == 0) {
                model.updateApplications();
            }

            final List<ApplicationModel> mostUsedApplications = model.getMostUsedApplications();

            for (int i = 0; i < launcher.dockImageViews.size(); i++) {
                if (i >= mostUsedApplications.size()) {
                    publishProgress(new DockUpdateModel(launcher.dockImageViews.get(i), null));
                } else {
                    publishProgress(new DockUpdateModel(launcher.dockImageViews.get(i), mostUsedApplications.get(i)));
                }
            }
        }

        return 0;
    }

    @Override
    protected void onProgressUpdate(final DockUpdateModel... values) {
        final Launcher launcher = launcherWeakReference.get();

        if (launcher != null) {
            for (DockUpdateModel dockUpdateModel : values) {
                launcher.updateDock(dockUpdateModel.getImageView(), dockUpdateModel.getApplicationModel());
            }
        }
    }
}
