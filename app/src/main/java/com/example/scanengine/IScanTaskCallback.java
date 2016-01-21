package com.example.scanengine;

/**
 * @author locke
 */
public interface IScanTaskCallback {
	/**
	 * 回调扫描相关信息
	 * @param what  IScanTask实现方定义信息类别值
	 * @param obj   数据对象
	 */
	void onMessage(int what, Object obj);
}
