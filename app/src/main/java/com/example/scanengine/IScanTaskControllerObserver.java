package com.example.scanengine;

/**
 * @author locke
 */
public interface IScanTaskControllerObserver {
	/**
	 * stop事件触发
	 */
	void stop();

	/**
	 * reset事件触发
	 */
	void reset();

	/**
	 * timeout事件触发
	 */
	void timeout();

	/**
	 * 暂停事件触发
	 * @param millis 非负数，暂停指定时长后自动resume。(若为0，表示永远暂停，直到resume()事件触发)
	 */
	void pause(long millis);

	/**
	 * 恢复暂停事件触发
	 */
	void resume();
}
