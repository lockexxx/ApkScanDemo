package com.example.scanengine;

import java.util.LinkedList;
import java.util.Queue;

import android.os.SystemClock;

/**
 * @author locke
 */
public class TaskBus {

	public static final int TASK_BUS_WAIT_FINISHED = 0;  	// 正常结束
	public static final int TASK_BUS_WAIT_TIME_UP = 1;   	// 等待超时

	public static final int TASK_BUS_STATUS_READY = 0;		// 准备状态
	public static final int TASK_BUS_STATUS_WORKING = 1;	// 工作状态
	public static final int TASK_BUS_STATUS_FINISHED = 2;	// 完成状态

	public interface ITaskBusCallback {
		/**
		 * TaskBus状态变化时回调
		 * @param oldStatus		旧状态
		 * @param newStatus		新状态
		 */
		void changeTaskBusStatus(int oldStatus, int newStatus);

		/**
		 * 当某个Task因取消或超时而得不到调用它的scan()函数时，用本回调通知。
		 * @param task
		 */
		void notifySkipScan(IScanTask task);
	}

	protected int mTaskBusStatus = TASK_BUS_STATUS_READY;

	protected TaskCtrlImpl mTaskCtrl = new TaskCtrlImpl();

	protected Thread mTaskThread = null;

	protected Queue<TaskInfo> mTaskQueue = new LinkedList<TaskInfo>();

	protected ITaskBusCallback mCallback = null;
	private boolean hasTaskPushed = false;


	protected class TaskInfo {
		public IScanTask  mTask = null;	///< 扫描任务
		public boolean   mEssentialTask = false;	///< 是否必要任务。当提交异步任务时，此任务若为异步任务，则必须先等它完成后，才能启动后面的异步任务。

		public TaskInfo(IScanTask task) {
			mTask = task;
			mEssentialTask = false;
		}

		public TaskInfo(IScanTask task, boolean essentialTask) {
			mTask = task;
			mEssentialTask = essentialTask;
		}
	}


	/**
	 * 设定TaskBus回调对象
	 * @param cb	回调对象
	 */
	public void setCallback(ITaskBusCallback cb) {
		mCallback = cb;
	}

	/**
	 * 添加扫描任务(无优先级关系，按添加的先后顺序进行扫描)，startScan()之后不可再调用本函数。
	 * @param task 扫描任务
	 * @return 成功返回true，失败返回false。
	 */
	public boolean pushTask(IScanTask task) {
		if (null == task) {
			return false;
		}

		synchronized (this) {
			if (null != mTaskThread) {
				return false;
			}
			hasTaskPushed = true;
			mTaskQueue.offer(new TaskInfo(task));
		}

		return true;
	}

	public synchronized boolean hasTaskPushed() {
		return hasTaskPushed;
	}

	/**
	 * 添加扫描任务(无优先级关系，按添加的先后顺序进行扫描)，startScan()之后不可再调用本函数。
	 * @param task 扫描任务
	 * @param time 扫描任务时长约束(单位ms)，非负数，若为0表示无时长约束。
	 * @return 成功返回true，失败返回false。
	 */
	public boolean pushTask(IScanTask task, int time) {
		if (null == task || time < 0) {
			return false;
		}

		synchronized (this) {
			if (null != mTaskThread) {
				return false;
			}
			hasTaskPushed = true;
			mTaskQueue.offer(new TaskInfo(task));
		}

		return true;
	}

	/**
	 * 添加扫描任务(无优先级关系，按添加的先后顺序进行扫描)，startScan()之后不可再调用本函数。
	 * @param task 扫描任务
	 * @param time 扫描任务时长约束(单位ms)，非负数，若为0表示无时长约束。
	 * @param essentialTask 必要任务，在此任务完成或超时前，不启动后续添加的任务。
	 * @return 成功返回true，失败返回false。
	 */
	public boolean pushTask(IScanTask task, int time, boolean essentialTask) {
		if (null == task || time < 0) {
			return false;
		}

		synchronized (this) {
			if (null != mTaskThread) {
				return false;
			}
			hasTaskPushed = true;
			mTaskQueue.offer(new TaskInfo(task, essentialTask));
		}

		return true;
	}

	/**
	 * 开始异步扫描，本函数执行后，不能再使用pushTask()添加扫描任务。
	 * @return 成功返回true，失败返回false。
	 */
	public boolean startScan() {
		synchronized (this) {
			if (null != mTaskThread) {
				return false;
			}

			mTaskCtrl.reset();

			mTaskThread = new Thread() {
				@Override
				public void run() {
					try {
						if (mTaskQueue.isEmpty()) {
							return;
						}

						for (TaskInfo taskInfo = mTaskQueue.poll(); null != taskInfo; taskInfo = mTaskQueue.poll()) {
							if (null == taskInfo.mTask) {
								continue;
							}

							if (mTaskCtrl.checkStop()) {
								if (null != mCallback) {
									mCallback.notifySkipScan(taskInfo.mTask);
								}
								break;
							}

							taskInfo.mTask.scan(mTaskCtrl);
						}
					} finally {
						if ((!mTaskQueue.isEmpty()) && null != mCallback) {
							for (TaskInfo taskInfo = mTaskQueue.poll(); null != taskInfo; taskInfo = mTaskQueue.poll()) {
								if (null == taskInfo.mTask) {
									continue;
								}

								mCallback.notifySkipScan(taskInfo.mTask);
							}
						}
						if (null != mCallback) {
							mCallback.changeTaskBusStatus(mTaskBusStatus, TASK_BUS_STATUS_FINISHED);
						}
						mTaskBusStatus = TASK_BUS_STATUS_FINISHED;
					}
				}
			};

			if (null != mCallback) {
				mCallback.changeTaskBusStatus(mTaskBusStatus, TASK_BUS_STATUS_WORKING);
			}
			mTaskBusStatus = TASK_BUS_STATUS_WORKING;
			mTaskThread.start();
		}

		return true;
	}

	/**
	 * 通知正在进行的异步扫描流程停止。
	 * @return 成功返回true，失败返回false。
	 */
	public boolean notifyStop() {
		synchronized (this) {
			if (null == mTaskThread) {
				return false;
			}
			mTaskCtrl.notifyStop();
		}

		return true;
	}

	/**
	 * 等待异步扫描流程结束
	 * @param maxWaitTime 最长等待时间(单位ms)；若为0，表示永远等待；不能为负数。
	 * @return 成功结束返回TASK_BUS_WAIT_FINISHED，
	 *         等待超时返回TASK_BUS_WAIT_TIME_UP(极端情况下，有可能把成功结束的情况判为超时)，
	 *         其他值为失败错误码。
	 */
	public int waitForFinish(long maxWaitTime) {
		if (maxWaitTime < 0L) {
			return -1;
		}

		Thread taskThread = null;
		synchronized (this) {
			if (null == mTaskThread) {
				return -2;
			}

			taskThread = mTaskThread;
		}

		long time = 0L;
		try {
			time = SystemClock.uptimeMillis();
			taskThread.join(maxWaitTime);
			time = (SystemClock.uptimeMillis() - time);
		} catch (InterruptedException e) {
			e.printStackTrace();
			return -3;
		}

		time = maxWaitTime - time;

		if (maxWaitTime > 0L && time <= 0L) {
			// 超时。
			return TASK_BUS_WAIT_TIME_UP;
		}

		return TASK_BUS_WAIT_FINISHED;
	}

	/**
	 * 取得TaskBus当前状态
	 * @return TaskBus当前状态(TASK_BUS_STATUS_*)
	 */
	public int getTaskBusStatus() {
		return mTaskBusStatus;
	}

	/**
	 * 通知暂停
	 * @param millis 非负数，暂停指定时长后自动resume。(若为0，表示永远暂停，直到resumePause())
	 */
	public void notifyPause(long millis) {
		synchronized (this) {
			if (null == mTaskThread) {
				return;
			}
			mTaskCtrl.notifyPause(millis);
		}
	}

	/**
	 * 从暂停状态恢复
	 */
	public void resumePause() {
		synchronized (this) {
			if (null == mTaskThread) {
				return;
			}
			mTaskCtrl.resumePause();
		}
	}

	/**
	 * 正在处理任务中
	 * @return
	 */
	public boolean isWorking() {
	   return mTaskBusStatus == TASK_BUS_STATUS_WORKING;
    }
}
