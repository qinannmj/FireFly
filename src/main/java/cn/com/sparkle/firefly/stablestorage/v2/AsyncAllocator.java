package cn.com.sparkle.firefly.stablestorage.v2;

import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import cn.com.sparkle.firefly.config.Configuration;
import cn.com.sparkle.firefly.stablestorage.io.PriorChangeable;
import cn.com.sparkle.firefly.stablestorage.io.RecordFileOut;
import cn.com.sparkle.firefly.stablestorage.io.RecordFileOutFactory;
import cn.com.sparkle.firefly.stablestorage.io.rwsbuffered.RwsBufferedRecordFileOutFactory;
import cn.com.sparkle.firefly.stablestorage.util.FileUtil;

public class AsyncAllocator implements Runnable {
	private final static Logger logger = Logger.getLogger(AsyncAllocator.class);

	private final long fileCapacity;
	private final RwsBufferedRecordFileOutFactory factory;
	private final ArrayBlockingQueue<File> idleList;
	private final File dir;
	private final long fileBlockNum;
	private final byte[] zeroBytes = new byte[1024 * 1024];
	private final Random random = new Random();
	private final Thread thread;
	private volatile Throwable isAllocateError = null;

	public AsyncAllocator(RecordFileOutFactory factory, Configuration conf, File dir) {
		if (conf.getConfigValue("async-file-allocator-idle-size") == null) {
			throw new RuntimeException("must set async-file-allocator-idle-size in conf.prop!");
		}
		if (conf.getConfigValue("async-file-allocator-fileblock-num") == null) {
			throw new RuntimeException("must set async-file-allocator-fileblock-num in conf.prop!");
		}
		fileBlockNum = Long.parseLong(conf.getConfigValue("async-file-allocator-fileblock-num"));
		fileCapacity = fileBlockNum * 1024 * 1024 * 8;
		int idleSize = Integer.parseInt(conf.getConfigValue("async-file-allocator-idle-size"));

		idleList = new ArrayBlockingQueue<File>(idleSize < 2 ? 1 : idleSize - 1);
		this.dir = dir;
		//		this.factory = new RwsBufferedRecordFileOutFactory();
		//		this.factory.init(conf);
		this.factory = (RwsBufferedRecordFileOutFactory) factory;
		thread = new Thread(this);
		thread.setName("async-file-allocator");
		thread.start();
	}

	public File getIdle(String newPath) throws InterruptedException {
		while (true) {
			File f = idleList.poll(5, TimeUnit.SECONDS);
			if (f != null) {
				File newfile = new File(newPath);
				FileUtil.rename(f, newfile);
				return newfile;
			} else if (isAllocateError != null) {
				logger.error("async allocator error", isAllocateError);
			}
		}
	}

	public void run() {
		synchronized (this) {
			try {
				if (thread.isInterrupted()) {
					return;
				}
				File workspace = FileUtil.getDir(dir.getAbsoluteFile() + "/allocator");
				File[] files = workspace.listFiles();
				for (File f : files) {
					if (f.length() < fileCapacity) {
						ensureCapacity(f);
					}
					idleList.put(f);
				}
				while (true) {
					File f;
					while (true) {
						int random = this.random.nextInt();
						f = new File(workspace.getAbsoluteFile() + "/" + String.valueOf(random));
						if (!f.exists()) {
							break;
						}
					}
					f.createNewFile();
					ensureCapacity(f);

					idleList.put(f);
				}
			} catch (InterruptedException e) {
				logger.info("shutdown file allocator!");
			} catch (Throwable e) {
				logger.error("unexcepted error", e);
				this.isAllocateError = e;
			}
		}

	}

	public void close() {
		thread.interrupt();
		synchronized (this) {
			return; //for be sure the thread has exited
		}
	}

	private void ensureCapacity(File f) throws IOException, InterruptedException {
		long curLen = f.length();
		int wBlkNum = (int) Math.ceil(((double) (fileCapacity - curLen)) / zeroBytes.length);
		RecordFileOut out = null;
		try {
			out = factory.makeRwsRecordFileOut(f, f.length());
			if (out instanceof PriorChangeable) {
				((PriorChangeable) out).setIsHighPrior(false);
			}

			for (int i = 0; i < wBlkNum; ++i) {
				final CountDownLatch finish = new CountDownLatch(1);
				out.write(zeroBytes, 0, zeroBytes.length, new Callable<Object>() {
					@Override
					public Object call() throws Exception {
						finish.countDown();
						return null;
					}
				}, true);
				//					out.skip(BLOCK_SIZE - zeroBytes.length);

				finish.await();
			}

		} finally {
			if (out != null) {
				out.close();
			}
		}

	}

}
