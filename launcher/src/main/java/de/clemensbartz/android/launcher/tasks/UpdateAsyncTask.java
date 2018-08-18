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

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.AsyncTask;

import java.lang.ref.WeakReference;
import java.text.Collator;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import de.clemensbartz.android.launcher.Launcher;
import de.clemensbartz.android.launcher.models.ApplicationModel;
import de.clemensbartz.android.launcher.models.HomeModel;

/**
 * Async task to update applications of the list view.
 * @since 1.5
 * @author Clemens Bartz
 */
public final class UpdateAsyncTask extends AsyncTask<Integer, Integer, Integer> {

    /** Weak reference to {@link Launcher}. */
    private final WeakReference<Launcher> launcherWeakReference;
    /** Weak reference to {@link HomeModel}. */
    private final WeakReference<HomeModel> homeModelWeakReference;

    /**
     * Create a new task updating application of the list view.
     * @param launcher the launcher
     * @param model the model
     */
    public UpdateAsyncTask(final Launcher launcher, final HomeModel model) {
        launcherWeakReference = new WeakReference<>(launcher);
        homeModelWeakReference = new WeakReference<>(model);
    }

    @Override
    protected Integer doInBackground(final Integer... integers) {
        final Launcher launcher = launcherWeakReference.get();
        final HomeModel model = homeModelWeakReference.get();

        if (launcher != null && model != null) {
            final Intent intent = new Intent();
            intent.setAction(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);

            final PackageManager pm = launcher.getPackageManager();
            final List<ResolveInfo> resolveInfoList = pm.queryIntentActivities(intent, 0);

            for (int i1 = 0, resolveInfoListSize = resolveInfoList.size(); i1 < resolveInfoListSize; i1++) {
                final ResolveInfo resolveInfo = resolveInfoList.get(i1);

                // Skip for non-launchable activities
                if (!resolveInfo.activityInfo.exported) {
                    continue;
                }

                final boolean disabled = model.isDisabled(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name);
                final boolean sticky = model.isSticky(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name);
                final ApplicationModel applicationModel = new ApplicationModel();
                applicationModel.packageName = resolveInfo.activityInfo.packageName;
                applicationModel.className = resolveInfo.activityInfo.name;
                applicationModel.disabled = disabled;
                applicationModel.sticky = sticky;

                if (applicationModel.packageName == null || applicationModel.className == null) {
                    continue;
                }

                final CharSequence label = resolveInfo.loadLabel(pm);

                applicationModel.label = (label != null) ? label.toString() : resolveInfo.activityInfo.name;

                if (applicationModel.label == null) {
                    applicationModel.label = "";
                }

                applicationModel.icon = resolveInfo.loadIcon(pm);

                // Check for when icon can become null (e. g. on Huawei Nexus 6p angler).
                if (applicationModel.icon == null) {
                    applicationModel.icon = launcher.ic_launcher;
                }

                launcher.applicationModels.add(applicationModel);
            }

            // Sort
            Collections.sort(launcher.applicationModels, new Comparator<ApplicationModel>() {
                @Override
                public int compare(final ApplicationModel o1, final ApplicationModel o2) {
                    return Collator.getInstance().compare(o1.label, o2.label);
                }
            });
        }

        return 0;
    }

    @Override
    protected void onPostExecute(final Integer result) {
        final Launcher launcher = launcherWeakReference.get();

        if (launcher != null) {
            launcher.lvApplicationsAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onPreExecute() {
        final Launcher launcher = launcherWeakReference.get();

        if (launcher != null) {
            launcher.applicationModels.clear();
            launcher.lvApplicationsAdapter.notifyDataSetChanged();
        }
    }
}
