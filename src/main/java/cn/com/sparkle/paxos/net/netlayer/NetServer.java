package cn.com.sparkle.paxos.net.netlayer;

public interface NetServer {
	public void init(String confPath, int heartBeatInterval, NetHandler handler) throws Throwable;

	public void listen(String ip, int port) throws Throwable;
}
