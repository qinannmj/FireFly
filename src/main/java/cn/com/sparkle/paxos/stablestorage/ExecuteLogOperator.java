package cn.com.sparkle.paxos.stablestorage;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

import cn.com.sparkle.paxos.checksum.ChecksumUtil.UnsupportedChecksumAlgorithm;
import cn.com.sparkle.paxos.stablestorage.io.BufferedFileOut;
import cn.com.sparkle.paxos.stablestorage.model.ExecuteRecord;

public class ExecuteLogOperator {
	private final static Logger logger = Logger.getLogger(ExecuteLogOperator.class);
	private final static int SPLIT_EXECUTE_LOG_COUNT = 500000;
	private File dir;
	private int lastLogCount = 0;
	private BufferedFileOut executeOutputStream;
	private LinkedList<File> executeFileList = new LinkedList<File>();

	public ExecuteLogOperator(File dir) throws IOException {
		this.dir = dir;
		if (!dir.exists()) {
			dir.mkdirs();
		}
	}

	public long init() throws IOException, UnsupportedChecksumAlgorithm {
		long lastWaitExecuteInstanceId = -1;
		File[] files = dir.listFiles();
		Comparator<File> comparator = new Comparator<File>() {
			@Override
			public int compare(File o1, File o2) {
				return Long.valueOf(o1.getName()).compareTo(Long.valueOf(o2.getName()));
			}
		};
		Arrays.sort(files, comparator);
		// read from execute log
		long pos = 0;
		byte[] recordBytes = new byte[16];
		for (int i = 0; i < files.length; i++) {
			pos = 0;
			File f = files[i];
			DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(f)));
			executeFileList.add(f);
			try {
				while (true) {
					try {
						// lastWaitExecuteInstanceId = in.readLong();
						int size = in.read(recordBytes);
						if (size != recordBytes.length) {
							break;
						}
						ExecuteRecord record = new ExecuteRecord(recordBytes);
						if (record.isValid()) {
							lastWaitExecuteInstanceId = record.getInstanceId();
						} else {
							logger.warn("executed log checksum error");
						}
						pos += 16;
						if (i == files.length - 1) {
							++lastLogCount;
						}
					} catch (EOFException e) {
						break;
					}
				}
			} finally {
				close(in);
			}
		}

		if (executeFileList.size() == 0) {
			File f = new File(dir + "/0");
			f.createNewFile();
			executeFileList.add(f);
		}
		RandomAccessFile rs = new RandomAccessFile(executeFileList.getLast(), "rws");
		rs.seek(pos);
		executeOutputStream = new BufferedFileOut(rs);
		return lastWaitExecuteInstanceId + 1;
	}

	public void writeExecuteLog(long instanceId, Callable<Object> callable) throws IOException, UnsupportedChecksumAlgorithm {

		ExecuteRecord record = new ExecuteRecord(instanceId);
		record.writeToStream(executeOutputStream, callable);
		// executeOutputStream.writeLong(instanceId, callable);

		// ++lastWaitExecuteInstanceId;

		if (++lastLogCount >= SPLIT_EXECUTE_LOG_COUNT) {
			lastLogCount = 0;
			File f = new File(dir + "/" + (Long.valueOf(executeFileList.getLast().getName()) + 1));
			f.createNewFile();
			executeFileList.addLast(f);
			executeOutputStream.close();
			RandomAccessFile rs = new RandomAccessFile(executeFileList.getLast(), "rws");
			executeOutputStream = new BufferedFileOut(rs);
			// check mount of file
			while (executeFileList.size() > 2) {
				File ff = executeFileList.removeFirst();
				ff.delete();
			}
		}
	}

	private void close(InputStream is) {
		if (is != null) {
			try {
				is.close();
			} catch (IOException e) {
			}
		}
	}

	public static void main(String[] args) throws IOException, UnsupportedChecksumAlgorithm {
		ExecuteLogOperator eo = new ExecuteLogOperator(new File("C:\\jbpaxos\\127.0.0.1-9000\\executelog"));
		System.out.println(eo.init());
		// int testSize = 1000;
		// long ct = System.currentTimeMillis();
		// for (int i = 0; i < testSize; i++) {
		// eo.writeExecuteLog(i, null);
		// }
		// System.out.println(testSize
		// / ((double) (System.currentTimeMillis() - ct) / 1000) + "/s");
	}
}
