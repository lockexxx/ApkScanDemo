package com.example.scanengine;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Pattern;

import android.content.Context;
import android.os.Build;
import android.os.Environment;

/**
 * @author locke
 */
public class StorageHelper {
	private Context mContext;
	private Object mStorageManager;
	private Method mMethodGetVolumeList;
	private Method mMethodGetPaths;
	private Method mMethodGetVolumeState;
	private Pattern mFilterPattern00;
	private Pattern mFilterPattern01 ;
	private Pattern mFilterPattern02;
	private Pattern mFilterPattern03;
	private Pattern mFilterPattern04;
	private Pattern mFilterPattern05;
	private Pattern mFilterPattern06;
	private Pattern mFilterPattern07;
	private Pattern mFilterPattern08;
	private Pattern mFilterPattern09;

	public StorageHelper(Context context) {
		mContext = context;
		if (mContext != null && Build.VERSION.SDK_INT >= 14) {
			mStorageManager = mContext.getSystemService(Context.STORAGE_SERVICE);
			try {
				mMethodGetPaths = mStorageManager.getClass().getMethod("getVolumePaths");
				mMethodGetVolumeList = mStorageManager.getClass().getMethod("getVolumeList");
				mMethodGetVolumeState = mStorageManager.getClass().getMethod("getVolumeState", new Class[] { String.class });
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 从已序表中取出sortedFolders中targetFolder所在下标位的路径名下的子路径名。
	 */
	public static ArrayList<String> getSubFolders(ArrayList<String> sortedFolders, int targetFolder) {
		if (null == sortedFolders || sortedFolders.isEmpty() || targetFolder >= sortedFolders.size()) {
			return null;
		}

		String targetFolderPath = sortedFolders.get(targetFolder);
		if (null == targetFolderPath) {
			return null;
		}

		ArrayList<String> subFolders = new ArrayList<String>();
		String nameWithSlash = FileUtils.addSlash(targetFolderPath);
		if (null == nameWithSlash) {
			return null;
		}

		for (int i = targetFolder + 1; i < sortedFolders.size(); ++i) {
			String name = sortedFolders.get(i);
			if (null == name) {
				continue;
			}

			if (name.startsWith(nameWithSlash)) {
				subFolders.add(name);
				continue;
			} else {
				break;
			}
		}

		if (subFolders.isEmpty()) {
			subFolders = null;
		}

		return subFolders;
	}

	/**
	 * 取出已挂载的所有存储器路径，要排除子路径(例如：返回结果中如果有/mnt/sdcard，就不会有/mnt/sdcard/external_sd)
	 */
	public ArrayList<String> getMountedVolumePathsWithoutSubFolders() {
		if (Build.VERSION.SDK_INT < 14) {
			return getDefualtMountedStoragePathsWithoutSubFolders();
		}

		if (null == mMethodGetVolumeState) {
			return getDefualtMountedStoragePathsWithoutSubFolders();
		}

		String[] volumePaths = getVolumePaths();
		if (null == volumePaths || 0 == volumePaths.length) {
			return getDefualtMountedStoragePathsWithoutSubFolders();
		}

		ArrayList<String> mountedVolumePaths = new ArrayList<String>();

		try {
			for (int idx = 0; idx < volumePaths.length; ++idx) {
				if (null == volumePaths[idx]) {
					continue;
				}

				if (mMethodGetVolumeState.invoke(mStorageManager, new Object[] { volumePaths[idx] }).equals("mounted")) {
					mountedVolumePaths.add(volumePaths[idx]);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (mountedVolumePaths.isEmpty()) {
			return getDefualtMountedStoragePathsWithoutSubFolders();
		}

		return filterSubFolders(mountedVolumePaths);
	}

	/**
	 * 取出已挂载的所有存储器路径
	 */
	public ArrayList<String> getMountedVolumePaths() {
		if (Build.VERSION.SDK_INT < 14) {
			return getDefualtMountedStoragePaths(null);
		}

		if (null == mMethodGetVolumeState) {
			return getDefualtMountedStoragePaths(null);
		}

		String[] volumePaths = getVolumePaths();
		if (null == volumePaths || 0 == volumePaths.length) {
			return getDefualtMountedStoragePaths(null);
		}

		ArrayList<String> mountedVolumePaths = new ArrayList<String>();

		try {
			for (int idx = 0; idx < volumePaths.length; ++idx) {
				if (null == volumePaths[idx]) {
					continue;
				}

				if (mMethodGetVolumeState.invoke(mStorageManager, new Object[] { volumePaths[idx] }).equals("mounted")) {
					mountedVolumePaths.add(volumePaths[idx]);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (mountedVolumePaths.isEmpty()) {
			return getDefualtMountedStoragePaths(null);
		}

		return mountedVolumePaths;
	}

	/**
	 * 取出已挂载的电话内部存储器路径
	 */
	public ArrayList<String> getMountedPhoneVolumePaths() {
		return getMountedVolumePaths(false);
	}

	/**
	 * 取出已挂载的外插SD卡存储器路径
	 */
	public ArrayList<String> getMountedSdCardVolumePaths() {
		return getMountedVolumePaths(true);
	}

	private String[] getVolumePaths() {
		if (null == mMethodGetPaths || null == mStorageManager) {
			return null;
		}

		String[] paths = null;
		try {
			paths = (String[]) mMethodGetPaths.invoke(mStorageManager);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		return paths;
	}

	/**
	 * 排除folders中的子目录
	 *
	 * @param folders
	 *            排除子目录前的数据
	 * @return 排除子目录后的数据
	 */
	private ArrayList<String> filterSubFolders(ArrayList<String> folders) {
		if (null == folders) {
			return null;
		}

		if (folders.isEmpty()) {
			return folders;
		}

		ArrayList<String> filteredResult = new ArrayList<String>();
		String nameWithSlash = null;
		Collections.sort(folders);
		for (String name : folders) {
			if (null == name) {
				continue;
			}

			if (null != nameWithSlash) {
				if (name.startsWith(nameWithSlash)) {
					continue;
				} else {
					nameWithSlash = FileUtils.addSlash(name);
					filteredResult.add(name);
					continue;
				}
			} else {
				nameWithSlash = FileUtils.addSlash(name);
				filteredResult.add(name);
				continue;
			}
		}

		if (filteredResult.isEmpty()) {
			filteredResult = null;
		}

		return filteredResult;
	}

	/**
	 * 用最原始的方法获取已挂载的存储器路径，并读取/proc/mounts文件中有可能是存储器的挂载点路径，排除被其它路径包含的子路径后返回。
	 */
	private ArrayList<String> getDefualtMountedStoragePathsWithoutSubFolders() {
		ArrayList<String> result = getDefualtMountedStoragePaths(null);
		return filterSubFolders(result);
	}

	/**
	 * 用最原始的方法获取已挂载的存储器路径
	 */
	private ArrayList<String> getDefualtMountedStoragePaths() {
		ArrayList<String> result = new ArrayList<String>();

		if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
			result.add(Environment.getExternalStorageDirectory().getPath());
		}

		if (result.isEmpty()) {
			return null;
		}

		return result;
	}

	/**
	 * 判断api level低于14的默认SD卡路径是否外插，如果api level低于9，则当作内置处理。
	 */
	private boolean isDefaultMountedStorageRemovable() {

		if (Build.VERSION.SDK_INT < 9) {
			return false;
		}

		boolean removable = false;

		try {
			Method methodIsExternalStorageRemovable = Environment.class.getMethod("isExternalStorageRemovable");
			if (null == methodIsExternalStorageRemovable) {
				return removable;
			}

			removable = ((Boolean) methodIsExternalStorageRemovable.invoke(Environment.class)).booleanValue();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return removable;
	}

	/**
	 * 用最原始的方法获取已挂载的存储器路径，并读取/proc/mounts文件中有可能是存储器的挂载点路径，叠加在result中返回。
	 */
	private ArrayList<String> getDefualtMountedStoragePaths(ArrayList<String> result) {
		if (null == result) {
			result = new ArrayList<String>();
		}

		ArrayList<String> resultDefault = getDefualtMountedStoragePaths();
		if (null != resultDefault && !resultDefault.isEmpty()) {
			if (result.isEmpty()) {
				result = resultDefault;
			} else {
				result.addAll(resultDefault);
			}
		}

		ArrayList<String> resultFromMountsFile = getMountedStoragePathsFromMountsFile(result);
		if (null != resultFromMountsFile && !resultFromMountsFile.isEmpty()) {
			if (result.isEmpty()) {
				result = resultFromMountsFile;
			} else {
				result.addAll(resultFromMountsFile);
			}
		}

		if (result.isEmpty()) {
			return null;
		}

		return result;
	}

	/**
	 * 对于不低于2.3的系统，区别判断是否可移除的存储器叠加在result中返回。
	 */
	private ArrayList<String> getDefualtMountedStoragePaths(ArrayList<String> result, boolean removable) {

		ArrayList<String> filterPaths = getDefualtMountedStoragePaths();

		if (removable == isDefaultMountedStorageRemovable()) {
			if (null == result) {
				result = new ArrayList<String>();
			}

			if (null != filterPaths && !filterPaths.isEmpty()) {
				result.addAll(filterPaths);
			}

			if (null != result && result.isEmpty()) {
				result = null;
			}
		}

		if (removable) {
			// 从/proc/mounts中读取判为可能的存储设备，算做外部存储。
			// 要把我们可以正常取到的路径排除在外
			if (null == filterPaths || filterPaths.isEmpty()) {
				filterPaths = result;
			} else {
				if (null != result && !result.isEmpty()) {
					for (int idx = 0; idx < result.size(); ++idx) {
						if (!filterPaths.contains(result.get(idx))) {
							filterPaths.add(result.get(idx));
						}
					}
				}
			}

			ArrayList<String> resultFromMountsFile = getMountedStoragePathsFromMountsFile(filterPaths);
			if (null != resultFromMountsFile && !resultFromMountsFile.isEmpty()) {
				if (null == result || result.isEmpty()) {
					result = resultFromMountsFile;
				} else {
					result.addAll(resultFromMountsFile);
				}
			}
		}

		return result;
	}

	private ArrayList<String> getMountedVolumePaths(boolean removable) {
		ArrayList<String> result = new ArrayList<String>();
		if (Build.VERSION.SDK_INT < 14) {
			return getDefualtMountedStoragePaths(result, removable);
		}

		if (null == mStorageManager || null == mMethodGetVolumeList || null == mMethodGetVolumeState) {
			return null;
		}

		try {
			Object[] arrayOfStorageVolume = (Object[]) mMethodGetVolumeList.invoke(mStorageManager);

			if (null == arrayOfStorageVolume || 0 == arrayOfStorageVolume.length) {
				return null;
			}

			Method methodGetPath = arrayOfStorageVolume[0].getClass().getMethod("getPath");
			Method methodIsRemovable = arrayOfStorageVolume[0].getClass().getMethod("isRemovable");

			if (null == methodGetPath || null == methodIsRemovable) {
				return null;
			}

			String volumePath = null;
			for (int idx = 0; idx < arrayOfStorageVolume.length; ++idx) {
				if (removable != ((Boolean) methodIsRemovable.invoke(arrayOfStorageVolume[idx])).booleanValue()) {
					continue;
				}

				volumePath = (String) methodGetPath.invoke(arrayOfStorageVolume[idx]);

				if (mMethodGetVolumeState.invoke(mStorageManager, new Object[] { volumePath }).equals("mounted")) {
					result.add(volumePath);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (result.isEmpty()) {
			return null;
		}

		return result;
	}

	/**
	 * 从/proc/mounts中读取判断有可能是存储设备，且不在filterPaths当中的挂载路径。
	 * @param filterPaths 要过滤掉的地址
	 */
	private ArrayList<String> getMountedStoragePathsFromMountsFile(ArrayList<String> filterPaths) {
		ArrayList<String> result = new ArrayList<String>();

		BufferedReader mountsFileReader = null;
		try {
			mountsFileReader = new BufferedReader(new InputStreamReader(new FileInputStream(new File("/proc/mounts"))));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			mountsFileReader = null;
		}

		try {
			String lineInfo = null;

			while (null != mountsFileReader) {
				lineInfo = mountsFileReader.readLine();
				if (null == lineInfo) {
					mountsFileReader.close();
					mountsFileReader = null;
					continue;
				}

				if (shouldBeFiltered(lineInfo)) {
					continue;
				}

				String[] arrayOfString = lineInfo.split(" ");
				if (arrayOfString.length >= 4) {
					String pathName = arrayOfString[1];
					if ((null == filterPaths || !filterPaths.contains(pathName)) && !result.contains(pathName)) {
						result.add(pathName);
					}
				}
			}
		} catch (Exception e) {
		}

		try {
			if (null != mountsFileReader) {
				mountsFileReader.close();
				mountsFileReader = null;
			}
		} catch (Exception e) {
		}

		if (result.isEmpty()) {
			result = null;
		}

		return result;
	}

	private boolean shouldBeFiltered(String lineInfo) {
		if (null == lineInfo) {
			return true;
		}

		if (!initRegexPattern()) {
			return true;
		}

		if (mFilterPattern00.matcher(lineInfo).find())
			if (mFilterPattern01.matcher(lineInfo).find())
				if (mFilterPattern02.matcher(lineInfo).find())
					if (!mFilterPattern03.matcher(lineInfo).find())
						if (!mFilterPattern04.matcher(lineInfo).find())
							if (!mFilterPattern05.matcher(lineInfo).find())
								if (!mFilterPattern06.matcher(lineInfo).find())
									if (!mFilterPattern07.matcher(lineInfo).find())
										if (!mFilterPattern08.matcher(lineInfo).find())
											if (!mFilterPattern09.matcher(lineInfo).find()) {
//												KInfocClientAssist.getInstance().reportData("cm_sl_ml", "li=" + lineInfo);
												return false;
											}

		return true;
	}

	private boolean initRegexPattern() {
		try {
			if (null == mFilterPattern00) {
				mFilterPattern00 = Pattern.compile("^\\/");
			}
			if (null == mFilterPattern01) {
				mFilterPattern01 = Pattern.compile("\\s(vfat)|(fuse)\\s");
			}
			if (null == mFilterPattern02) {
				mFilterPattern02 = Pattern.compile("\\brw\\b");
			}
			if (null == mFilterPattern03) {
				mFilterPattern03 = Pattern.compile("\\bnoauto_da_alloc\\b");
			}
			if (null == mFilterPattern04) {
				mFilterPattern04 = Pattern.compile("(\\basec)|(asec\\b)");
			}
			if (null == mFilterPattern05) {
				mFilterPattern05 = Pattern.compile("\\buser_id=0\\b");
			}
			if (null == mFilterPattern06) {
				mFilterPattern06 = Pattern.compile("\\bgroup_id=0\\b");
			}
			if (null == mFilterPattern07) {
				mFilterPattern07 = Pattern.compile("\\buid=0\\b");
			}
			if (null == mFilterPattern08) {
				mFilterPattern08 = Pattern.compile("\\bgid=0\\b");
			}
			if (null == mFilterPattern09) {
				mFilterPattern09 = Pattern.compile("\\bbarrier=1\\b");
			}
		} catch (Exception e) {
			return false;
		}

		return true;
	}

	public static boolean isSystemRO() {
		BufferedReader mountsFileReader = null;
		try {
			mountsFileReader = new BufferedReader(new InputStreamReader(new FileInputStream(new File("/proc/mounts"))));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			mountsFileReader = null;
		}
		try {
			String lineInfo = null;
			while (null != mountsFileReader) {
				lineInfo = mountsFileReader.readLine();
				if (null == lineInfo) {
					mountsFileReader.close();
					mountsFileReader = null;
					continue;
				}
				if (lineInfo.contains("system") && lineInfo.contains("ro,")) {
					return true;
				}
			}
		} catch (Exception e) {
		}

		try {
			if (null != mountsFileReader) {
				mountsFileReader.close();
				mountsFileReader = null;
			}
		} catch (Exception e) {
		}

		return false;
	}

	/**
	 * 获取挂载点对应的设备路径
	 *
	 * @param mountpoint
	 * @return device file path of the specify mountpoint
	 */
	public static String getDeviceByMountPoint(final String mountpoint) {
		String device = null;
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader("/proc/mounts"));

			String line = null;
			while ((line = br.readLine()) != null) {
				String[] cols = line.split(" ");
				if (cols != null && cols.length > 2) {
					if (mountpoint.equals(cols[1])) {
						device = cols[0];
						break;
					}
				}
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return device;
	}
}
