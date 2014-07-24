package cn.com.sparkle.paxos.util;

public class QuorumCalcUtil {
	public static int calcQuorumNum(int senatorSize, int memDiskLost) {
		return Math.min(senatorSize / 2 + 1 + memDiskLost, senatorSize);
	}
}
