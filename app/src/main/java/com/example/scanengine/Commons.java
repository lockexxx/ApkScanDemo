package com.example.scanengine;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;

/**
 * @author locke
 */
public class Commons {

	/**
	 * Get application name(OR application label) by specify package name
	 * 
	 * @param context
	 *            the context
	 * @param packageName
	 *            package name
	 * @return null or application name
	 */
	public static String getAppName(Context context, String packageName) {
		String label = null;
		if (context == null) {
			return null;
		}

		PackageManager pm = context.getPackageManager();
		try {
			ApplicationInfo info = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
			label = pm.getApplicationLabel(info).toString();
		} catch (NameNotFoundException e) {
			e.printStackTrace();
			label = packageName;
		}
		return label;
	}
	
}
