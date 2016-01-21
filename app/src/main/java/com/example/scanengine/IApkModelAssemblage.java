package com.example.scanengine;


import java.io.File;

/**
 * @author locke
 */
public interface IApkModelAssemblage {

	/**
	 * 存入一个apk信息对象
	 */
	boolean putApkModel(File apkFile, APKModel apkModel);
}
