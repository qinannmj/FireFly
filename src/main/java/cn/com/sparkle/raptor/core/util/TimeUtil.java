package cn.com.sparkle.raptor.core.util;

import cn.com.sparkle.raptor.core.delaycheck.DelayCheckedTimer;

public class TimeUtil {
	private static volatile long curtime;

	private static class TimerThread extends Thread {
		public void run() {
			while (true) {
				curtime = System.currentTimeMillis();
				// check timer task
				DelayCheckedTimer.work();
				try {
					Thread.sleep(1);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	static {
		curtime = System.currentTimeMillis();
		TimerThread t = new TimerThread();
		t.setName("Raptor-Timer-Clock");
		t.setDaemon(true);
		t.start();
	}

	public static long currentTimeMillis() {
		return curtime;
	}

}
