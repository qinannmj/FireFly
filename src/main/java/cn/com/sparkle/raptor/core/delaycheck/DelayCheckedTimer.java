package cn.com.sparkle.raptor.core.delaycheck;

import java.util.LinkedList;

import cn.com.sparkle.raptor.core.collections.MaximumSizeArrayCycleQueue;
import cn.com.sparkle.raptor.core.collections.MaximumSizeArrayCycleQueue.QueueFullException;
import cn.com.sparkle.raptor.core.util.TimeUtil;

public class DelayCheckedTimer {
	private static LinkedList<DelayChecked> l = new LinkedList<DelayChecked>();
	private static MaximumSizeArrayCycleQueue<DelayChecked> waitAddDelayCheck = new MaximumSizeArrayCycleQueue<DelayChecked>(DelayChecked.class, 100);

	public static void work() {
		DelayChecked dc = null;
		while ((dc = waitAddDelayCheck.peek()) != null) {
			l.add(dc);
			waitAddDelayCheck.poll();
		}
		for (DelayChecked d : l) {
			long t = TimeUtil.currentTimeMillis();
			if (d.isRun && (t - d.lastCheckTime) > d.delaytime) {
				if (!d.isCycle) {
					d.isRun = false;
				}
				d.goToRun();
				d.lastCheckTime = t;
			}

		}
	}

	public static void addDelayCheck(DelayChecked delayChecked) {
		while (true) {
			try {
				waitAddDelayCheck.push(delayChecked);
				break;
			} catch (QueueFullException e) {
				e.printStackTrace();
				try {
					Thread.sleep(100);
				} catch (InterruptedException e1) {
				}
			}
		}
	}

}
