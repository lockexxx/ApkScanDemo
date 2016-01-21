package com.example.scanengine;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.support.v4.util.ArrayMap;
import android.text.TextUtils;

public class ApkParser {
	//	private ApkParserBaseDaoImp apkParserBaseDaoImp;
	private ArrayMap<String , APKModel> mCacheArrayMap;
	private List<PackageInfo> mInstalledPackageInfo;
	private PackageManager mPackageManager;
	private List<String> mUpdateList;
	private Context mContext;
	private boolean mStop;

	public ApkParser(Context context) {
		if (context == null) {
			throw new IllegalArgumentException("Context is null!");
		}
		mContext = context;
	}

	public void initApkParser() {
		mPackageManager = mContext.getPackageManager();
		mInstalledPackageInfo = mPackageManager.getInstalledPackages(0);
//		apkParserBaseDaoImp = DaoFactory.getApkParserBaseDao(mContext);
		mStop = false;
	}

	private boolean compare(APKModel newModel, APKModel oldModel) {
		if (newModel != null && oldModel != null) {
			if (newModel.getPath().equals(oldModel.getPath())) {
				if (newModel.getSize() == oldModel.getSize()) {
					if (newModel.getModifyTime() == oldModel.getModifyTime()) {
						return true;
					}
				}
			}
		}

		return false;
	}

	private boolean setPackageInfo(APKModel apkModel) {
		ApplicationInfo appInfo = null;
		PackageInfo packageInfo = mPackageManager.getPackageArchiveInfo(apkModel.getPath(), 0);
		if (packageInfo != null) {
			appInfo = packageInfo.applicationInfo;
			appInfo.sourceDir = apkModel.getPath();
			appInfo.publicSourceDir = apkModel.getPath();
			CharSequence label = appInfo.loadLabel(mPackageManager);
			if (label != null) {
				apkModel.setTitle(label.toString());
				apkModel.setVersion(packageInfo.versionName);
				apkModel.setVersionCode(packageInfo.versionCode);
				apkModel.setPackageName(packageInfo.packageName);
				return true;
			}
		}

		return false;
	}

	public boolean isUpdateBlock() {
//		return apkParserBaseDaoImp.isUpdateBlock;
		return false;
	}

	public void updateCache() {
//		apkParserBaseDaoImp.updateCahce(mCacheArrayMap, mUpdateList);
	}

	public void notifyStop() {
		mStop = true;
	}

	public APKModel parseApkFile(File apkFile) {
		if (mCacheArrayMap == null) {
//			if(apkParserBaseDaoImp != null){
//				mCacheArrayMap = apkParserBaseDaoImp.getAllApkCache();
//			}

			if (mCacheArrayMap == null) {
				mCacheArrayMap = new ArrayMap<String, APKModel>();
			}
		}

		APKModel apkModel = new APKModel();
		apkModel.setSize(apkFile.length());
		apkModel.setPath(apkFile.getAbsolutePath());
		apkModel.setFileName(apkFile.getName());
		apkModel.setModifyTime(apkFile.lastModified());

		APKModel cacheModel =  mCacheArrayMap.get(apkModel.getPath());
		boolean isUpdateCache = !compare(apkModel, cacheModel);
		if(cacheModel == null || isUpdateCache) {
			if(!setPackageInfo(apkModel)) {
				return null;
			}
		} else {
			apkModel.setTitle(cacheModel.getTitle());
			apkModel.setVersion(cacheModel.getVersion());
			apkModel.setVersionCode(cacheModel.getVersionCode());
			apkModel.setPackageName(cacheModel.getPackageName());
		}

		if (mStop || TextUtils.isEmpty(apkModel.getPackageName())) {
			return null;
		}

		if (null != mInstalledPackageInfo) {
			for (PackageInfo packageInfo : mInstalledPackageInfo) {

				if (mStop) break;

				if (packageInfo.applicationInfo.packageName.equals(apkModel.getPackageName())) {
					apkModel.setInstalled(true);
					apkModel.setAppVersionCode(packageInfo.versionCode);
					if (apkModel.getVersionCode() < packageInfo.versionCode) {
						apkModel.setApkInstallStatus(APKModel.APK_STATUS_OLD);
					} else if (apkModel.getVersionCode() > packageInfo.versionCode) {
						apkModel.setApkInstallStatus(APKModel.APK_STATUS_NEW);
					} else if (apkModel.getVersion().compareToIgnoreCase(packageInfo.versionName) == 0){
						apkModel.setApkInstallStatus(APKModel.APK_STATUS_CUR);
					} else if (apkModel.getVersion().compareToIgnoreCase(packageInfo.versionName) < 0){
						apkModel.setApkInstallStatus(APKModel.APK_STATUS_OLD);
					} else {
						apkModel.setApkInstallStatus(APKModel.APK_STATUS_NEW);
					}

					break;
				}
			}

			if (isUpdateCache) {
				if(mUpdateList == null) {
					mUpdateList = new ArrayList<String>();
				}
				mCacheArrayMap.put(apkModel.getPath(), apkModel);
				mUpdateList.add(apkModel.getPath());
			}
		}

		return apkModel;
	}
}
