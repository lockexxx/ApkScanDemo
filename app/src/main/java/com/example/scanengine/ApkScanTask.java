package com.example.scanengine;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

/**
 * @author locke
 */
public class ApkScanTask extends IScanTask.BaseStub implements IApkModelAssemblage {
	public static final int TASK_SCAN_FAILED = -1;
	public static final int TASK_SCAN_RUNNING = 1;
	public static final int TASK_SCAN_FINISHED = 2;
	public static final int TASK_SCAN_UPDATE_DIRS = 3;
	public static final int TASK_SCAN_UPDATE_MODEL = 4;

	private static final int MAX_SCAN_LEVEL = 3;
	private boolean mShouldCompoundScan = true;
	private boolean mShouldMediaScan = false;

	private List<APKModel> mNotInstalledApks;
	private List<APKModel> mInstalledApks;
	private ApkScanPolicy mApkScanPolicy;
	private ApkParseThread mParseThread;
	private IScanTaskController mCtrl;
	private Set<String> mApkSet;

	private StorageHelper mStorageHelper;
	private Context mContext;

	public ApkScanTask(Context context) {
		mStorageHelper = new StorageHelper(context);
		mApkScanPolicy = new ApkScanPolicy();
		mContext = context;
	}

	@Override
	public boolean putApkModel(File apkFile, APKModel apkModel) {
		if (apkFile == null) return false;

		String path = apkFile.getAbsolutePath();

		if (!TextUtils.isEmpty(path)) {
			if (mApkSet == null) {
				mApkSet = new HashSet<String>();
			}

			if (!mApkSet.contains(path.toLowerCase())) {
				mApkSet.add(path.toLowerCase());
			} else {
				return false;
			}
		}

		if (null != apkModel) {
			if (apkModel.isInstalled()) {
				mInstalledApks.add(apkModel);
			} else {
				mNotInstalledApks.add(apkModel);
			}
		}

		if (null != callBack) {
			callBack.onMessage(TASK_SCAN_UPDATE_MODEL, apkModel);
		}

		return true;
	}

	@Override
	public String getTaskDesc() {
		return ApkScanTask.class.getSimpleName();
	}

	@Override
	public boolean scan(IScanTaskController ctrl) {
		mCtrl = ctrl;

		try {
			scanApk();

		} finally {
			updateCache();
		}

		return true;
	}

	private void updateCache() {
		if(mParseThread != null && !mParseThread.apkParser.isUpdateBlock()) {
			mParseThread.apkParser.updateCache();
		}
	}

	private void findApkInFolder(String strFolderPath, String[] strExceptionFileArr) {
		if (TextUtils.isEmpty(strFolderPath)) {
			return;
		}

		File nowFolder = new File(strFolderPath);
		if (!nowFolder.exists() || !nowFolder.isDirectory()) {
			return;
		}

		if (null != mCtrl && mCtrl.checkStop()) {
			return;
		}

		String[] thisLevelPathName = nowFolder.list();

		if (null != thisLevelPathName) {
			for (int i = 0; i < thisLevelPathName.length; ++i) {
				if (null != mCtrl && mCtrl.checkStop()) {
					break;
				}

				if (null != strExceptionFileArr) {
					boolean bSkip = false;
					for (int z = 0; z < strExceptionFileArr.length; z++) {
						if (strExceptionFileArr[z].length() == thisLevelPathName[i].length() &&
								strExceptionFileArr[z].equalsIgnoreCase(thisLevelPathName[i])) {
							bSkip = true;
							break;
						}
					}
					if (bSkip) {
						continue;
					}
				}

				File sub = new File(FileUtils.addSlash(nowFolder.getPath()) + thisLevelPathName[i]);
				if (sub.isFile() && sub.getName().toLowerCase().endsWith(".apk")) {
					mParseThread.putApkFile(sub);
					thisLevelPathName[i] = null;
				}
			}
		}
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void scanApkByMediaStore() {
		ContentResolver cr = mContext.getContentResolver();
		Cursor cursor = null;
		try {
			cursor = cr.query(MediaStore.Files.getContentUri("external"), null,
					MediaStore.Files.FileColumns.DATA + " like ?", new String[]{"%.apk"}, null);
			if (cursor != null) {
				final int nColPath = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA);
				if (cursor.moveToFirst()) {
					do {
						if (mCtrl.checkStop()) {
							break;
						}
						String path = cursor.getString(nColPath);
						File sub = new File(path);
						if (sub.exists() && sub.isFile()) {
							mParseThread.putApkFile(sub);
							//find more apk under the same folder
							findApkInFolder(sub.getParent(), new String[] {sub.getName()});
						}
					} while (cursor.moveToNext());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	private void scanApkByEnumDisk() {
		// 外部存储设备路径
		ArrayList<String> externalStoragePaths = mStorageHelper.getMountedVolumePaths();
		ArrayList<ScanTargetDir> scanTargetDirs = buildTargetDatas(externalStoragePaths, MAX_SCAN_LEVEL);

		// 添加一些指定需要扫描的路径
		appendMoreTargetPaths(scanTargetDirs);

		if (null != scanTargetDirs) {
			Collections.sort(scanTargetDirs, new Comparator<ScanTargetDir>() {
				@Override
				public int compare(ScanTargetDir obj1, ScanTargetDir obj2) {
					return obj1.targetPath.compareTo(obj2.targetPath);
				}
			});
			for (int idx = 0; idx < scanTargetDirs.size(); ++idx) {
				ScanTargetDir target = scanTargetDirs.get(idx);
				if (target.needScan) {
					listApkFilesInPath(new File(target.targetPath), target.maxScanLevel, getSubFolders(scanTargetDirs, idx));
				}
				if (null != mCtrl && mCtrl.checkStop()) {
					break;
				}
			}
		}
	}

	private void notifyCallBack(int taskState, Object obj) {
		if (null != callBack) {
			callBack.onMessage(taskState, obj);
		}
	}

	private void scanApk() {
		notifyCallBack(TASK_SCAN_RUNNING, null);
		if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
			notifyCallBack(TASK_SCAN_FAILED, null);
			return;
		}

		mInstalledApks = new ArrayList<APKModel>();
		mNotInstalledApks = new ArrayList<APKModel>();

		mParseThread = new ApkParseThread(mContext, this);
		mParseThread.start();

		if (Build.VERSION.SDK_INT > 10 && mShouldMediaScan && !mShouldCompoundScan) {
			scanApkByMediaStore();

		} else {
			if (Build.VERSION.SDK_INT > 10 && mShouldCompoundScan) {
				scanApkByMediaStore();
			}
			scanApkByEnumDisk();
		}

		mParseThread.notifyStartWaitForFinish();
		try {
			mParseThread.join();
		} catch (InterruptedException e) {}

		mergeSortList(mInstalledApks, mNotInstalledApks);
		notifyCallBack(TASK_SCAN_FINISHED, new ApkModels(mInstalledApks, mNotInstalledApks));
	}

	private void mergeSortList(List... list) {
		System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");
		for (List l : list) {
			if (null != l && !l.isEmpty()) {
				Collections.sort(l);
			}
		}
	}

	private ArrayList<ScanTargetDir> buildTargetDatas(ArrayList<String> externalStoragePaths, int maxLevel) {
		if (null == externalStoragePaths || externalStoragePaths.isEmpty()) {
			return null;
		}

		ArrayList<ScanTargetDir> rst = new ArrayList<ScanTargetDir>();
		ScanTargetDir target = null;

		for (String name : externalStoragePaths) {
			target = new ScanTargetDir();
			target.targetPath = name;
			target.maxScanLevel = maxLevel;
			target.needScan = true;
			rst.add(target);
		}

		return rst;
	}

	private void appendMoreTargetPaths(ArrayList<ScanTargetDir> scanTargetDirs) {
		if (null == scanTargetDirs || scanTargetDirs.isEmpty()) {
			return;
		}

		if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
			return;
		}

		String sdCardPath = Environment.getExternalStorageDirectory().getPath();
		if (null == sdCardPath) {
			return;
		}

		ScanTargetDir[] scanTargets = mApkScanPolicy.getScanTargetDirs();
		for (ScanTargetDir scanTarget : scanTargets) {
			String targetPath = sdCardPath + scanTarget.targetPath;
			if (new File(targetPath).exists()) {
				ScanTargetDir target = new ScanTargetDir();
				target.targetPath = targetPath;
				target.maxScanLevel = scanTarget.maxScanLevel;
				target.needScan = scanTarget.needScan;
				scanTargetDirs.add(target);
			}
		}
	}

	private ArrayList<ScanTargetDir> getSubFolders(List<ScanTargetDir> sortedScanTargetDirs, int targetFolder) {
		if (null == sortedScanTargetDirs || sortedScanTargetDirs.isEmpty() || targetFolder >= sortedScanTargetDirs.size()) {
			return null;
		}

		ScanTargetDir targetItem = sortedScanTargetDirs.get(targetFolder);
		if (null == targetItem || null == targetItem.targetPath) {
			return null;
		}

		ArrayList<ScanTargetDir> subFolders = new ArrayList<ScanTargetDir>();
		String nameWithSlash = FileUtils.addSlash(targetItem.targetPath);
		if (null == nameWithSlash) {
			return null;
		}

		for (int i = targetFolder + 1; i < sortedScanTargetDirs.size(); ++i) {
			ScanTargetDir subTargetItem = sortedScanTargetDirs.get(i);
			if (null == subTargetItem || null == subTargetItem.targetPath) {
				continue;
			}

			if (subTargetItem.targetPath.startsWith(nameWithSlash)) {
				subFolders.add(subTargetItem);
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

	private void listApkFilesInPath(File nowFolder, int maxLevel, ArrayList<ScanTargetDir> filterFolders) {

		if (null != mCtrl && mCtrl.checkStop()) {
			return;
		}

		String[] thisLevelPathName = null;

		try {
			if (null != callBack) {
				callBack.onMessage(TASK_SCAN_UPDATE_DIRS, nowFolder.getName());
			}

			thisLevelPathName = nowFolder.list();
		} catch (Exception e) {
			thisLevelPathName = null;
		}

		if (null != thisLevelPathName) {
			int folderNum = 0;
			for (int i = 0; i < thisLevelPathName.length; ++i) {
				if (null != mCtrl && mCtrl.checkStop()) {
					break;
				}

				File sub = new File(FileUtils.addSlash(nowFolder.getPath()) + thisLevelPathName[i]);
				if (sub.isDirectory()) {
					if (maxLevel > 0) {
						if (folderNum != i) {
							thisLevelPathName[folderNum] = thisLevelPathName[i];
							thisLevelPathName[i] = null;
						}
						++folderNum;
					} else {
						thisLevelPathName[i] = null;
					}
				}
				else if (sub.getName().toLowerCase().endsWith(".apk")) {
					mParseThread.putApkFile(sub);
					thisLevelPathName[i] = null;
				}
			}

			if ((null == mCtrl || !mCtrl.checkStop()) && folderNum > 0) {
				if (folderNum < (thisLevelPathName.length / 2)) {
					// 在进行下一级文件夹枚举前，释放不必要的空间
					String[] arrayOfSubFolder = new String[folderNum];
					System.arraycopy(thisLevelPathName, 0, arrayOfSubFolder, 0, folderNum);
					thisLevelPathName = arrayOfSubFolder;
				}

				for (int i = 0; i < folderNum; ++i) {
					if (null != mCtrl && mCtrl.checkStop()) {
						break;
					}

					// 过滤掉指定过滤的文件夹
					File sub = new File(FileUtils.addSlash(nowFolder.getPath()) + thisLevelPathName[i]);
					int idx = -1;
					if (null != filterFolders) {
						ScanTargetDir temp = new ScanTargetDir();
						temp.targetPath = sub.getPath();
						idx = Collections.binarySearch(filterFolders, temp, new Comparator<ScanTargetDir>() {
							@Override
							public int compare(ScanTargetDir obj1,
											   ScanTargetDir obj2) {
								return obj1.targetPath.compareTo(obj2.targetPath);
							}
						});
					}
					if (idx >= 0) {
						ScanTargetDir temp = filterFolders.get(idx);
						if (temp.needScan) {
							if (temp.maxScanLevel > (maxLevel - 1)) {
								// 目标的扫描层数如果比子目录的扫描层数多, 就不在此处扫描这个文件夹
								thisLevelPathName[i] = null;
								continue;
							} else {
								// 本地扫描, 后续不需要再扫描了
								temp.needScan = false;
							}
						}
					}

					listApkFilesInPath(sub, maxLevel - 1, filterFolders);
					thisLevelPathName[i] = null;
				}
			}
		}
	}

	private class ApkParseThread extends Thread implements IApkFileAssemblage {
		private IApkModelAssemblage apkAssemblage;
		private Stack<File> apkFileStack;
		private ApkParser apkParser;
		private boolean waitForFinish;

		public ApkParseThread(Context context, IApkModelAssemblage apkModelAssemblage) {
			apkAssemblage = apkModelAssemblage;
			apkParser = new ApkParser(context);
			apkFileStack = new Stack<File>();
			waitForFinish = false;
		}

		@Override
		public boolean putApkFile(File apkFile) {
			synchronized(apkFileStack) {
				apkFileStack.push(apkFile);
				apkFileStack.notifyAll();
			}
			return true;
		}

		@Override
		public File getApkFile() {
			synchronized(apkFileStack) {
				if (apkFileStack.empty()) {
					return null;
				}
				return apkFileStack.pop();
			}
		}

		@Override
		public void run() {
			apkParser.initApkParser();

			int obsIdx = -1;
			if (null != mCtrl) {
				obsIdx = mCtrl.addObserver(new IScanTaskControllerObserver() {

					@Override
					public void timeout() {
					}

					@Override
					public void stop() {
						apkParser.notifyStop();
						synchronized(apkFileStack) {
							apkFileStack.notifyAll();
						}
					}

					@Override
					public void resume() {
					}

					@Override
					public void reset() {
					}

					@Override
					public void pause(long millis) {
					}
				});
			}

			while (true) {
				if (null != mCtrl && mCtrl.checkStop()) {
					break;
				}

				File apkFile = getApkFile();
				if (null == apkFile) {
					if (waitForFinish) {
						break;
					}
					synchronized(apkFileStack) {
						try {
							apkFileStack.wait();
						} catch (InterruptedException e) {
							break;
						}
					}

					if (null != mCtrl && mCtrl.checkStop()) {
						break;
					}

					continue;
				}

				if (null != callBack) {
					callBack.onMessage(TASK_SCAN_UPDATE_DIRS, apkFile.getName());
				}

				APKModel apkModel = apkParser.parseApkFile(apkFile);

				if (null != apkAssemblage) {
					apkAssemblage.putApkModel(apkFile, apkModel);
				}
			}

			if (null != mCtrl && obsIdx >= 0) {
				mCtrl.removeObserver(obsIdx);
				obsIdx = -1;
			}
		}

		public void notifyStartWaitForFinish() {
			waitForFinish = true;
			synchronized(apkFileStack) {
				apkFileStack.notifyAll();
			}
		}
	}

	public static class ScanTargetDir {
		public String targetPath = null;
		public int maxScanLevel = 0;
		public boolean needScan = false;

		public ScanTargetDir() {}

		public ScanTargetDir(String targetPath, int maxScanLevel, boolean needScan) {
			this.targetPath = targetPath;
			this.needScan = needScan;
			this.maxScanLevel = maxScanLevel;
		}
	}

	public static class ApkModels {
		public List<APKModel> installedApk;
		public List<APKModel> uninstalledApk;

		public ApkModels(List<APKModel> installedApk, List<APKModel> uninstalledApk) {
			this.installedApk = installedApk;
			this.uninstalledApk = uninstalledApk;
		}
	}

}
