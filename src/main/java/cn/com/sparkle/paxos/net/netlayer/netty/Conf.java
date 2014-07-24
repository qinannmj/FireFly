package cn.com.sparkle.paxos.net.netlayer.netty;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

import cn.com.sparkle.paxos.net.netlayer.CommonConf;

public class Conf extends CommonConf {
	public int getRecvBuf() {
		return recvBuf;
	}

	private int workthreadNum;
	private int sendBuf;
	private int recvBuf;

	public Conf(String path) throws FileNotFoundException, IOException {
		super(path);
		Properties prop = new Properties();
		prop.load(new InputStreamReader(new FileInputStream(path)));
		workthreadNum = Integer.parseInt(prop.getProperty("worker_thread_num"));
		sendBuf = Integer.parseInt(prop.getProperty("send_mem_size"));
		recvBuf = Integer.parseInt(prop.getProperty("recieve_mem_size"));
	}

	public int getWorkthreadNum() {
		return workthreadNum;
	}

	public int getSendBuf() {
		return sendBuf;
	}

}
