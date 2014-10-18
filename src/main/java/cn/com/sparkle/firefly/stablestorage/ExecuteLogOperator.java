package cn.com.sparkle.firefly.stablestorage;

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

import org.apache.log4j.Logger;

import cn.com.sparkle.firefly.checksum.ChecksumUtil.UnsupportedChecksumAlgorithm;
import cn.com.sparkle.firefly.stablestorage.io.RecordFileOut;
import cn.com.sparkle.firefly.stablestorage.io.rwsbuffered.BufferedFileOut;
import cn.com.sparkle.firefly.stablestorage.io.rwsbuffered.FlushThreadGroup;
import cn.com.sparkle.firefly.stablestorage.model.ExecuteRecord;
import cn.com.sparkle.firefly.stablestorage.util.FileUtil;

public class ExecuteLogOperator {
	private final static Logger logger = Logger.getLogger(ExecuteLogOperator.class);
	private final static int SPLIT_EXECUTE_LOG_COUNT = 500000;
	private File dir;
	private int lastLogCount = 0;
	private RecordFileOut executeOutputStream;
	private LinkedList<File> executeFileList = new LinkedList<File>();
	private FlushThreadGroup flushThreadGroup;
	private long lastWaitExecuteInstanceId = -1;
	
	public ExecuteLogOperator(File dir) throws IOException {
		this.dir = dir;
		flushThreadGroup = new FlushThreadGroup(1024, 3, "executelog", false);
	}

	public long init() throws IOException, UnsupportedChecksumAlgorithm {
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
			File f = FileUtil.getFile(dir + "/0");
			executeFileList.add(f);
		}
		RandomAccessFile rs = new RandomAccessFile(executeFileList.getLast(), "rws");
		rs.seek(pos);
		executeOutputStream = new BufferedFileOut(executeFileList.getLast().getName(),rs,flushThreadGroup);
		
		return lastWaitExecuteInstanceId + 1;
	}

	public void writeExecuteLog(long instanceId) throws IOException, UnsupportedChecksumAlgorithm {
		if(instanceId < lastWaitExecuteInstanceId){
			return;
		}
		ExecuteRecord record = new ExecuteRecord(instanceId);
		record.writeToStream(executeOutputStream, null);

		if (++lastLogCount >= SPLIT_EXECUTE_LOG_COUNT) {
			lastLogCount = 0;
			File f = FileUtil.getFile(dir + "/" + (Long.valueOf(executeFileList.getLast().getName()) + 1));
			executeFileList.addLast(f);
			executeOutputStream.close();
			RandomAccessFile rs = new RandomAccessFile(executeFileList.getLast(), "rws");
			executeOutputStream = new BufferedFileOut(executeFileList.getLast().getName(),rs,flushThreadGroup);
			
			
			// check mount of file
			while (executeFileList.size() > 2) {
				File ff = executeFileList.removeFirst();
				FileUtil.deleteFile(ff);
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

}
