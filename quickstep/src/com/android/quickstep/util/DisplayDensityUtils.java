/*
 * Copyright (C) 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.quickstep.util;

import android.app.ActivityManager;
import android.app.ActivityTaskManager;
import android.content.res.Configuration;
import android.os.RemoteException;
import android.util.Log;
import android.view.IWindowManager;
import android.view.WindowManagerGlobal;
import android.window.WindowContainerTransaction;
import android.window.WindowOrganizer;

public final class DisplayDensityUtils {

    private static final String TAG = "DisplayDensityUtils";

    private DisplayDensityUtils() {
    }

    /**
     * Toggles a density override on the task identified by {@code taskId}.
     *
     * @return {@code true} if the task was found and the override was applied.
     */
    public static boolean toggleTaskDensity(int taskId) {
        final ActivityManager.RunningTaskInfo taskInfo = findTask(taskId);
        if (taskInfo == null) {
            Log.w(TAG, "Unable to find task " + taskId + " while toggling density");
            return false;
        }

        final IWindowManager windowManager = WindowManagerGlobal.getWindowManagerService();
        try {
            final int regularDensity = windowManager.getInitialDisplayDensity(
                    taskInfo.displayId);
            if (regularDensity <= 0) {
                Log.e(TAG, "Invalid initial density " + regularDensity
                        + " for display " + taskInfo.displayId);
                return false;
            }

            final int compactDensity = Math.max(1, Math.round(regularDensity * 2f / 3f));
            final int currentDensity = taskInfo.configuration.densityDpi;
            final int targetDensity = currentDensity == compactDensity
                    ? Configuration.DENSITY_DPI_UNDEFINED
                    : compactDensity;

            final WindowContainerTransaction transaction = new WindowContainerTransaction()
                    .setDensityDpi(taskInfo.token, targetDensity);
            new WindowOrganizer().applyTransaction(transaction);
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to read the initial density for task " + taskId, e);
        } catch (RuntimeException e) {
            Log.e(TAG, "Failed to toggle density for task " + taskId, e);
        }
        return false;
    }

    private static ActivityManager.RunningTaskInfo findTask(int taskId) {
        for (ActivityManager.RunningTaskInfo taskInfo
                : ActivityTaskManager.getInstance().getTasks(Integer.MAX_VALUE)) {
            if (taskInfo.taskId == taskId) {
                return taskInfo;
            }
        }
        return null;
    }
}
