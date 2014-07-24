package cn.com.sparkle.paxos.net.netlayer;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

public class CommonConf {
	private int iothreadnum;
	private int recieveBuffSize;
	private int sendBuffSize;
	private int backlog;
	
	public CommonConf(String path) throws FileNotFoundException, IOException {
		Properties prop = new Properties();
		prop.load(new InputStreamReader(new FileInputStream(path)));
		iothreadnum = Integer.parseInt(prop.getProperty("io_thread_num"));
		recieveBuffSize = Integer.parseInt(prop.getProperty("recieve_mem_size"));
		sendBuffSize = Integer.parseInt(prop.getProperty("send_mem_size"));
		backlog = Integer.parseInt(prop.getProperty("backlog"));
	}

	public int getIothreadnum() {
		return iothreadnum;
	}


	public int getRecieveBuffSize() {
		return recieveBuffSize;
	}


	public int getSendBuffSize() {
		return sendBuffSize;
	}

	public int getBacklog() {
		return backlog;
	}
	
}
