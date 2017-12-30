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

import android.graphics.drawable.Drawable;

/**
 * Model for applications. Out of performance reasons, this class will be excluded from
 * CheckStyle checks.
 *
 * @author Clemens Bartz
 * @since 1.0
 */
public final class ApplicationModel {
    /** The localized label. */
    @SuppressWarnings("CheckStyle")
    public CharSequence label;
    /** The icon. */
    @SuppressWarnings("CheckStyle")
    public Drawable icon;
    /** The package name. */
    @SuppressWarnings("CheckStyle")
    public String packageName;
    /** The full class name. */
    @SuppressWarnings("CheckStyle")
    public String className;
    /** The disabled flag. */
    @SuppressWarnings("CheckStyle")
    public boolean disabled;
    /** The sticky flag. */
    @SuppressWarnings("CheckStyle")
    public boolean sticky;
}
