package cn.com.sparkle.firefly.net.netlayer;

public interface NetServer {
	public void init(String confPath, int heartBeatInterval, NetHandler handler,String ip,int port,String name) throws Throwable;

	public void listen() throws Throwable;
}
