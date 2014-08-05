package cn.com.sparkle.firefly.net.netlayer.raptor;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

import cn.com.sparkle.firefly.net.netlayer.CommonConf;

public class Conf extends CommonConf {
	private int cycleRecieveCell;
	private int cycleSendCell;
	private int cycleRecieveSize;
	private int cycleSendBuffSize;
	private int workthreadMinNum;
	private int workthreadMaxNum;

	public Conf(String path) throws FileNotFoundException, IOException {
		super(path);
		Properties prop = new Properties();
		prop.load(new InputStreamReader(new FileInputStream(path)));
		cycleRecieveCell = Integer.parseInt(prop.getProperty("cycle_recv_cell_size"));
		cycleRecieveSize = Integer.parseInt(prop.getProperty("cycle_recv_mem_size"));
		
		cycleSendCell = Integer.parseInt(prop.getProperty("cycle_recv_cell_size"));
		cycleSendBuffSize = Integer.parseInt(prop.getProperty("cycle_send_mem_size"));
		workthreadMinNum = Integer.parseInt(prop.getProperty("worker_thread_min_num"));
		workthreadMaxNum = Integer.parseInt(prop.getProperty("worker_thread_max_num"));
	}

	public int getCycleRecieveCell() {
		return cycleRecieveCell;
	}

	public int getCycleSendCell() {
		return cycleSendCell;
	}

	public int getCycleRecieveSize() {
		return cycleRecieveSize;
	}

	public int getCycleSendBuffSize() {
		return cycleSendBuffSize;
	}

	public int getWorkthreadMinNum() {
		return workthreadMinNum;
	}

	public int getWorkthreadMaxNum() {
		return workthreadMaxNum;
	}

}
