package com.example.scanengine;

/**
 * @author locke
 */
public interface IScanTaskController {

	int TASK_CTRL_NONE		= 0;	///< 无状态
	int TASK_CTRL_STOP		= 1;	///< 停止
	int TASK_CTRL_TIME_OUT	= 2;	///< 超时
	int TASK_CTRL_PAUSE		= 3;	///< 暂停

	/**
	 * 是否需要停止
	 * @return 返回为true表示需要停止扫描流程
	 */
	boolean checkStop();

	/**
	 * 取出当前任务状态
	 * @return 返回值定义见TASK_CTRL_*
	 */
	int getStatus();

	/**
	 * 添加注册一个观察对象，当控制状态发生变化时主动通知。注意，需要释放。
	 * @param o 观察对象
	 * @return 返回负数表示失败，非负数用于removeObserver()释放观察对象。
	 */
	int addObserver(IScanTaskControllerObserver o);

	/**
	 * 释放已注册的一个观察对象。
	 * @param observerIndex 待释放的观察对象索引，由addObserver()返回。
	 */
	void removeObserver(int observerIndex);
}
