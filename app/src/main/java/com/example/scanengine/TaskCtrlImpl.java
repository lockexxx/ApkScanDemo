package com.example.scanengine;

import java.util.ArrayList;
import java.util.List;

/**
 * @author locke
 */
public class TaskCtrlImpl implements IScanTaskController {
	
	private int mStatus = IScanTaskController.TASK_CTRL_NONE;
	private List<IScanTaskControllerObserver> mObserver = null;
	private Object mSyncObj = new Object();
	private long mPauseTime = 0L;

//	checkStop():
//		N:		状态无变化，返回false
//		S:		状态无变化，返回true
//		T:		状态无变化，返回true
//		P:		在本函数内暂停，直到恢复后根据状态返回true或false，自动恢复暂状态时，本函数会改变状态为N，其它情况不修改状态
//	getStatus():
//		N:		状态无变化，返回当前记录的状态
//		S:		状态无变化，返回当前记录的状态
//		T:		状态无变化，返回当前记录的状态
//		P:		状态无变化，返回当前记录的状态
//	notifyStop():
//		N:		状态记录改为S
//		S:		状态记录不变
//		T:		状态记录不变
//		P:		状态记录改为S，并通知所有已暂停项恢复
//	reset():
//		N:		状态记录不变
//		S:		状态记录改为N
//		T:		状态记录改为N
//		P:		先resumePause，然后状态记录改为N
//	notifyTimeOut():
//		N:		状态记录改为T
//		S:		状态记录不变
//		T:		状态记录不变
//		P:		状态记录改为T，并通知所有已暂停项恢复
//	notifyPause():
//		N:		状态记录改为P
//		S:		状态记录不变
//		T:		状态记录不变
//		P:		状态记录不变
//	resumePause():
//		N:		状态记录不变，并通知所有已暂停项恢复
//		S:		状态记录不变，并通知所有已暂停项恢复
//		T:		状态记录不变，并通知所有已暂停项恢复
//		P:		状态记录改为N，并通知所有已暂停项恢复
	
	@Override
	public boolean checkStop() {
		boolean pause = false;
		synchronized (mSyncObj) {
			while (true) {
				switch (mStatus) {
				case IScanTaskController.TASK_CTRL_NONE:
					return false;

				case IScanTaskController.TASK_CTRL_STOP:
				case IScanTaskController.TASK_CTRL_TIME_OUT:
					return true;

				case IScanTaskController.TASK_CTRL_PAUSE:
					if (!pause) {
						try {
							mSyncObj.wait(mPauseTime);
						} catch (InterruptedException e) {
						}
						pause = true;
					} else {
						mStatus = IScanTaskController.TASK_CTRL_NONE;
					}
					break;
				}
			}
		}
	}
	
	@Override
	public int getStatus() {
		synchronized (mSyncObj) {
			return mStatus;
		}
	}

	public void notifyStop() {

		IScanTaskControllerObserver[] observer = null;
		
		synchronized (mSyncObj) {
			switch (mStatus) {
			case IScanTaskController.TASK_CTRL_NONE:
				mStatus = IScanTaskController.TASK_CTRL_STOP;
				break;

			case IScanTaskController.TASK_CTRL_STOP:
			case IScanTaskController.TASK_CTRL_TIME_OUT:
				break;

			case IScanTaskController.TASK_CTRL_PAUSE:
				mStatus = IScanTaskController.TASK_CTRL_STOP;
				mSyncObj.notifyAll();
				break;
			}

			if (null != mObserver && !mObserver.isEmpty()) {
				observer = mObserver.toArray(new IScanTaskControllerObserver[mObserver.size()]);
			}
		}

		if (null != observer) {
			for (IScanTaskControllerObserver o : observer) {
				if (null == o) {
					continue;
				}

				o.stop();
			}
		}
	}

	public void reset() {

		IScanTaskControllerObserver[] observer = null;

		synchronized (mSyncObj) {
			switch (mStatus) {
			case IScanTaskController.TASK_CTRL_NONE:
				break;

			case IScanTaskController.TASK_CTRL_STOP:
			case IScanTaskController.TASK_CTRL_TIME_OUT:
				mStatus = IScanTaskController.TASK_CTRL_NONE;
				break;

			case IScanTaskController.TASK_CTRL_PAUSE:
				mSyncObj.notifyAll();
				mStatus = IScanTaskController.TASK_CTRL_NONE;
				break;
			}

			if (null != mObserver && !mObserver.isEmpty()) {
				observer = mObserver.toArray(new IScanTaskControllerObserver[mObserver.size()]);
			}
		}

		if (null != observer) {
			for (IScanTaskControllerObserver o : observer) {
				if (null == o) {
					continue;
				}

				o.reset();
			}
		}
	}
	
	public void notifyTimeOut() {

		IScanTaskControllerObserver[] observer = null;
		
		synchronized (mSyncObj) {
			switch (mStatus) {
			case IScanTaskController.TASK_CTRL_NONE:
				mStatus = IScanTaskController.TASK_CTRL_TIME_OUT;
				break;

			case IScanTaskController.TASK_CTRL_STOP:
			case IScanTaskController.TASK_CTRL_TIME_OUT:
				break;

			case IScanTaskController.TASK_CTRL_PAUSE:
				mStatus = IScanTaskController.TASK_CTRL_TIME_OUT;
				mSyncObj.notifyAll();
				break;
			}

			if (null != mObserver && !mObserver.isEmpty()) {
				observer = mObserver.toArray(new IScanTaskControllerObserver[mObserver.size()]);
			}
		}

		if (null != observer) {
			for (IScanTaskControllerObserver o : observer) {
				if (null == o) {
					continue;
				}

				o.timeout();
			}
		}
	}
	

	public void notifyPause(long millis) {
		
		if (millis < 0L) {
			return;
		}

		IScanTaskControllerObserver[] observer = null;
		
		synchronized (mSyncObj) {
			
			if (IScanTaskController.TASK_CTRL_STOP == mStatus || 
					IScanTaskController.TASK_CTRL_TIME_OUT == mStatus) {
				// 停止状态时不能暂停，否则有可能造成停不下来。
				return;
			}
			
			mStatus = IScanTaskController.TASK_CTRL_PAUSE;
			mPauseTime = millis;

			if (null != mObserver && !mObserver.isEmpty()) {
				observer = mObserver.toArray(new IScanTaskControllerObserver[mObserver.size()]);
			}
		}

		if (null != observer) {
			for (IScanTaskControllerObserver o : observer) {
				if (null == o) {
					continue;
				}

				o.pause(millis);
			}
		}
	}
	
	public void resumePause() {

		IScanTaskControllerObserver[] observer = null;

		synchronized (mSyncObj) {
			
			if (IScanTaskController.TASK_CTRL_PAUSE == mStatus) {
				mStatus = IScanTaskController.TASK_CTRL_NONE;
			}

			mSyncObj.notifyAll();
			
			if (null != mObserver && !mObserver.isEmpty()) {
				observer = mObserver.toArray(new IScanTaskControllerObserver[mObserver.size()]);
			}
		}

		if (null != observer) {
			for (IScanTaskControllerObserver o : observer) {
				if (null == o) {
					continue;
				}

				o.resume();
			}
		}
	}

	@Override
	public int addObserver(IScanTaskControllerObserver o) {
		if (null == o) {
			return -1;
		}

		int idx = 0;
		synchronized (mSyncObj) {
			if (null == mObserver) {
				mObserver = new ArrayList<IScanTaskControllerObserver>();
			}

			for (idx = 0; idx < mObserver.size(); ++idx) {
				if (null == mObserver.get(idx)) {
					mObserver.set(idx, o);
					break;
				}
			}

			if (mObserver.size() == idx) {
				mObserver.add(o);
			}
		}

		return idx;
	}

	@Override
	public void removeObserver(int observerIndex) {
		if (observerIndex < 0) {
			return;
		}

		synchronized (mSyncObj) {
			if (observerIndex >= mObserver.size()) {
				return;
			}

			mObserver.set(observerIndex, null);
		}
	}
}
	