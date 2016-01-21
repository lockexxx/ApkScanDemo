package com.example.scanengine;

import java.io.File;

/**
 * @author locke
 */
public interface IApkFileAssemblage {
	/*
	 * 存入一个文件对象
	 */
	boolean putApkFile(File apkFile);

	/*
	 * 取出一个文件对象。取出后，本集合内不再存有此对象。
	 */
	File getApkFile();
}
