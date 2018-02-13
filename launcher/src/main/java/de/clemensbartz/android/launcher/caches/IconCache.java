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

package de.clemensbartz.android.launcher.caches;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.util.LruCache;

/**
 * A cache to hold icons.
 *
 * @author Clemens Bartz
 * @since 1.4
 */
public final class IconCache {

    /** The eviction count when we are running out of memory. */
    private int evictionCount = 0;

    /** The key for the ic_launcher icon. */
    public static final String IC_LAUNCHER_KEY = "ic_launcher";

    /** How much of total memory we are going to use. */
    private static final int CACHE_SHARE = 3;
    /** The factor to convert byte to MiB. */
    private static final int MEBI_FACTOR = 1024 * 1024;
    /** The default cache size (= 1/3rd of 16 MiB). */
    private static final int DEFAULT_CACHE_SIZE = CACHE_SHARE * MEBI_FACTOR;

    /**
     * The lru cache for icons.
     */
    private final LruCache<String, BitmapDrawable> cache;

    /**
     * The instance.
     */
    private static IconCache instance;

    /**
     * @param activity the activity to get the memory from
     * @return the instace of the cache
     */
    public static IconCache getInstance(final Activity activity) {
        if (instance == null) {
            instance = new IconCache(activity);
        }

        return instance;
    }

    /**
     * Create a new cache.
     *
     * @param activity the activity to get the memory from
     */
    private IconCache(final Activity activity) {
        // The maximum memory of this icon cache.
        int maxMemory = DEFAULT_CACHE_SIZE;

        // If the device tells us how much we can, we will actually use more
        final Object service = activity.getSystemService(Context.ACTIVITY_SERVICE);
        if (service instanceof ActivityManager) {
            final int memClass = ((ActivityManager) service).getMemoryClass();
            maxMemory = MEBI_FACTOR * memClass / CACHE_SHARE;
        }

        cache = new LruCache<String, BitmapDrawable>(maxMemory) {
            @Override
            protected int sizeOf(final String key, final BitmapDrawable value) {
                return value.getBitmap().getByteCount();
            }
        };
    }

    /**
     * Get the icon for a key.
     * @param key the icon
     * @return an icon or <code>null</code>, if no value was saved
     */
    public synchronized BitmapDrawable getIcon(final String key) {
        return cache.get(key);
    }

    /**
     * Put an icon in the cache.
     * @param key the key
     * @param bitmapDrawable the drawable
     */
    public synchronized void create(final String key, final BitmapDrawable bitmapDrawable) {
        // Only cache when we are not already full
        if (cache.evictionCount() - evictionCount <= 0) {
            cache.put(key, bitmapDrawable);
        }
    }

    /**
     * Clear the cache.
     */
    public synchronized void invalidate() {
        cache.evictAll();
        evictionCount = cache.evictionCount();
    }
}
