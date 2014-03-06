package com.weemo.sdk.helper.util;

import java.lang.reflect.Field;

import android.app.Activity;
import android.view.ViewConfiguration;

/**
 * Small repository of utility functions
 */
public final class UIUtils {

	/**
	 * This class is a function repository and cannot be instanciated
	 */
	private UIUtils() {}

	/**
	 * This is a horrible hack whose purpose is to force the display of the overflow...
	 *
	 * http://stackoverflow.com/a/11438245/1269640
	 * https://code.google.com/p/android/issues/detail?id=38013
	 *
	 * @param activity The activity to hack
	 */
	public static void forceOverflowMenu(final Activity activity) {
		try {
			final ViewConfiguration config = ViewConfiguration.get(activity);
			final Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
			if (menuKeyField != null) {
				menuKeyField.setAccessible(true);
				menuKeyField.setBoolean(config, false);
			}
		}
		catch (Throwable exc) {
			exc.printStackTrace();
			// Ignore
		}
	}

}
