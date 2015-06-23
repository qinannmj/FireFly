package cn.com.sparkle.firefly;

public class Constants {
	public final static long PAXOS_FAIL_TIME_OUT = -100000;
	public final static long PAXOS_FAIL_FILE_DAMAGED = -99900;
	public final static long PAXOS_FAIL_INSTANCE_SUCCEEDED = -99800;
	public final static long VOTE_OK = -1;
	public final static long FILE_WRITE_SUCCESS = -1;
	public final static int MAX_HEART_BEAT_INTERVAL = 10000;//ten minutes
	public final static int CATCH_STOP_NUM = 100000;
	public final static long ELECTION_VOTE_ID_TOLERATION = 200; // for master can catch up fast
	public final static int MAX_MASTER_DISTANCE = Integer.MAX_VALUE;
	public final static int MAX_ACTIVE_HEART_BEAT_LIFE_CYCLE = 10;
	
	//error code
	public final static String ERROR_NEGOTIATE_ARBITRATOR = "0001";
	public final static String ERROR_NEGOTIATE_UNCOMPATIBLE = "0002";
	public final static String ERROR_NEGOTIATE_UNACCEPT_IP = "0003";
}
