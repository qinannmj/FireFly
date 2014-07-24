package cn.com.sparkle.paxos.net.netlayer.raptor;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

import cn.com.sparkle.paxos.net.netlayer.CommonConf;

public class Conf extends CommonConf {
	private int recieveCell;
	private int sendCell;
	private int workthreadMinNum;
	private int workthreadMaxNum;

	public Conf(String path) throws FileNotFoundException, IOException {
		super(path);
		Properties prop = new Properties();
		prop.load(new InputStreamReader(new FileInputStream(path)));
		recieveCell = Integer.parseInt(prop.getProperty("recieve_cell_size"));
		sendCell = Integer.parseInt(prop.getProperty("send_mem_cell_size"));
		workthreadMinNum = Integer.parseInt(prop.getProperty("worker_thread_min_num"));
		workthreadMaxNum = Integer.parseInt(prop.getProperty("worker_thread_max_num"));
	}

	public int getRecieveCell() {
		return recieveCell;
	}

	public int getSendCell() {
		return sendCell;
	}

	public int getWorkthreadMinNum() {
		return workthreadMinNum;
	}

	public int getWorkthreadMaxNum() {
		return workthreadMaxNum;
	}

}
