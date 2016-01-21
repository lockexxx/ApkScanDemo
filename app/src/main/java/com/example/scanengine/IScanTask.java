package com.example.scanengine;

/**
 * @author locke
 */
public interface IScanTask {
	/**
	 * 绑定一个数据回调对象
	 */
	void bindCallbackObj(IScanTaskCallback cb);

	/**
	 * 扫描
	 * @param ctrl 扫描流程控制对象
	 * @return 成功扫描(包括控制对象通知停止)返回为true，失败返回false。
	 */
	boolean scan(IScanTaskController ctrl);

	/**
	 * 获取任务的字符串描述
	 */
	String getTaskDesc();

	/**
	 * 为方便写IScanTask
	 */
	abstract class BaseStub implements IScanTask {

		@Override
		public void bindCallbackObj(IScanTaskCallback cb) {
			callBack = cb;
		}

		protected IScanTaskCallback callBack = null;
	}
}
