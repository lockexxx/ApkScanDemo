package com.example.scanengine;

import java.io.File;
import java.io.Serializable;

import android.content.Context;

/**
 * 安装包信息
 * @author zhangmin
 */

public class APKModel implements Serializable, Comparable<APKModel>{

	private static final long serialVersionUID = -3498260819936152076L;

	public static final int APK_STATUS_OLD = 0;
	public static final int APK_STATUS_CUR = 1;
	public static final int APK_STATUS_NEW = 2;

	private String packageName;
	private String title;
	private long size;
	private String version;
	private String path;
	private long modifyTime;
	private boolean isInstalled;
	private int apkInstallStatus = APK_STATUS_OLD;
	private String fileName;
	private int versionCode = 0;
	private int appVersionCode = 0;

	public int getAppVersionCode() {
		return appVersionCode;
	}
	public void setAppVersionCode(int appVersionCode) {
		this.appVersionCode = appVersionCode;
	}
	/**
	 * @param packageName the apkName to set
	 */
	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}
	/**
	 * @return the apkName
	 */
	public String getPackageName() {
		return packageName;
	}
	/**
	 * @param title the title to set
	 */
	public void setTitle(String title) {
		this.title = title;
	}
	/**
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}
	/**
	 * @param size the size to set
	 */
	public void setSize(long size) {
		this.size = size;
	}
	/**
	 * @return the size
	 */
	public long getSize() {
		return size;
	}
	/**
	 * @param version the version to set
	 */
	public void setVersion(String version) {
		this.version = version;
	}
	/**
	 * @return the version
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * @param path the path to set
	 */
	public void setPath(String path) {
		this.path = path;
	}
	/**
	 * @return the path
	 */
	public String getPath() {
		return path;
	}
	/**
	 * @param modifyTime the modifyTime to set
	 */
	public void setModifyTime(long modifyTime) {
		this.modifyTime = modifyTime;
	}
	/**
	 * @return the modifyTime
	 */
	public long getModifyTime() {
		return modifyTime;
	}
	/**
	 * @param installed 此名字的apk包是否已经安装(未比较版本)
	 */
	public void setInstalled(boolean installed) {
		this.isInstalled = installed;
	}
	/**
	 * @return 此名字的apk包是否已经安装(未比较版本)
	 */
	public boolean isInstalled() {
		return isInstalled;
	}
	/**
	 * @param apkStatus 此名字的apk包是否已经安装，或者是否新旧版本。
	 */
	public void setApkInstallStatus(int apkStatus) {
		this.apkInstallStatus = apkStatus;
	}
	/**
	 * @return 此名字的apk包是否已经安装，或者是否新旧版本。
	 */
	public int getApkInstallStatus() {
		return apkInstallStatus;
	}
	/**
	 * @param fileName the fileName to set
	 */
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	/**
	 * @return the fileName
	 */
	public String getFileName() {
		return fileName;
	}

	public void setVersionCode(int verCode) {
		versionCode = verCode;
	}

	public int getVersionCode() {
		return versionCode;
	}

	//自定义排序
	@Override
	public int compareTo(APKModel another) {
		if(null == this || null == another || this == another || this.title.length() == 0 || another.title.length() == 0)
			return 0;

		int nResult = this.title.compareToIgnoreCase(another.title);
		return nResult;
	}
	@Override
	public String toString() {
		return "APKModel [packageName=" + packageName + ", title=" + title + ", size=" + size + ", version=" + version
				+ ", path=" + path + ", modifyTime=" + modifyTime + ", isInstalled="
				+ isInstalled + ", apkInstallStatus=" + apkInstallStatus + ", fileName="
				+ fileName + ", versionCode=" + versionCode + "]";
	}

	public static APKModel create(Context context, File target) {
		ApkParser parser = new ApkParser(context);
		APKModel x = parser.parseApkFile(target);
		if(null == x){
			return null;
		}
		if(x.isInstalled()) {
			x.setTitle(Commons.getAppName(context, x.packageName));
		}
		return x;
	}

}
