package cn.com.sparkle.firefly.stablestorage.v2;

import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import cn.com.sparkle.firefly.config.Configuration;
import cn.com.sparkle.firefly.stablestorage.io.PriorChangeable;
import cn.com.sparkle.firefly.stablestorage.io.RecordFileOut;
import cn.com.sparkle.firefly.stablestorage.io.RecordFileOutFactory;
import cn.com.sparkle.firefly.stablestorage.io.rwsbuffered.RwsBufferedRecordFileOutFactory;
import cn.com.sparkle.firefly.stablestorage.util.FileUtil;

public final class AsyncAllocator{
	private final static Logger logger = Logger.getLogger(AsyncAllocator.class);

	private final long fileCapacity;
	private final RwsBufferedRecordFileOutFactory factory;
	
	private final ArrayBlockingQueue<File> idleList;
	private final ArrayBlockingQueue<File> safeIdleList;
	
	private final long fileBlockNum;
	private final byte[] zeroBytes = new byte[1024 * 1024];
	private final Random random = new Random();
	private final Thread unsafeAllocatorThread;
	private final Thread safeAllocatorThread;
	private final int idleSize;
	private final File workspace;

	public AsyncAllocator(RecordFileOutFactory factory, Configuration conf, File dir) {
		if (conf.getConfigValue("async-file-allocator-idle-size") == null) {
			throw new RuntimeException("must set async-file-allocator-idle-size in conf.prop!");
		}
		if (conf.getConfigValue("async-file-allocator-fileblock-num") == null) {
			throw new RuntimeException("must set async-file-allocator-fileblock-num in conf.prop!");
		}
		fileBlockNum = Long.parseLong(conf.getConfigValue("async-file-allocator-fileblock-num"));
		fileCapacity = fileBlockNum * 1024 * 1024 * 8;
		int idleSizeTemp = Integer.parseInt(conf.getConfigValue("async-file-allocator-idle-size"));
		idleSize = idleSizeTemp < 6 ? 6 : idleSizeTemp;
		idleList = new ArrayBlockingQueue<File>(idleSize -idleSize/3 - 1);
		safeIdleList = new ArrayBlockingQueue<File>(idleSize/3 - 1);
		this.factory = (RwsBufferedRecordFileOutFactory) factory;
		
		workspace = FileUtil.getDir(dir.getAbsoluteFile() + "/allocator");
		File[] files = workspace.listFiles();
		LinkedBlockingQueue<File> waitFinishList = new LinkedBlockingQueue<File>();
		LinkedBlockingQueue<File> finishedList = new LinkedBlockingQueue<File>();
		for(File f : files){
			if(f.length() == fileCapacity){
				finishedList.add(f);
			}else{
				waitFinishList.add(f);
			}
		}
		safeAllocatorThread = new Thread(new SafeIdleFileMaker(finishedList));
		safeAllocatorThread.setName("async-file-safe-allocator");
		safeAllocatorThread.start();
		
		unsafeAllocatorThread = new Thread(new IdleFileMaker(waitFinishList,finishedList));
		unsafeAllocatorThread.setName("async-file-allocator");
		unsafeAllocatorThread.start();
	}

	public File getIdle(String newPath) throws InterruptedException {
		while (true) {
			File f = null;
			if(idleList.size() != 0){
				f = idleList.poll();
			}else{
				f = safeIdleList.poll(1,TimeUnit.SECONDS);
			}
			if (f != null) {
				File newfile = new File(newPath);
				FileUtil.rename(f, newfile);
				return newfile;
			}
		}
	}
	
	public void close() {
		safeAllocatorThread.interrupt();
		unsafeAllocatorThread.interrupt();
		synchronized (this) {
			return; //for be sure the thread has exited
		}
	}

	private void ensureCapacity(File f,boolean isHighPrior) throws IOException, InterruptedException {
		long curLen = f.length();
		int wBlkNum = (int) Math.ceil(((double) (fileCapacity - curLen)) / zeroBytes.length);
		RecordFileOut out = null;
		try {
			out = factory.makeRwsRecordFileOut(f, f.length());
			if (out instanceof PriorChangeable) {
				((PriorChangeable) out).setIsHighPrior(isHighPrior);
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
	
	private class IdleFileMaker implements Runnable {
		private BlockingQueue<File> waitFinishList;
		private BlockingQueue<File> finishedList;
		
		public IdleFileMaker(BlockingQueue<File> waitFinishList,
				BlockingQueue<File> finishedList) {
			super();
			this.waitFinishList = waitFinishList;
			this.finishedList = finishedList;
		}

		public void run() {
			synchronized (this) {
				try {
					if (unsafeAllocatorThread.isInterrupted()) {
						return;
					}
					while(finishedList.peek() != null){
						File f = finishedList.poll();
						if(f != null){
							idleList.put(f);
						}
					}
					for (File f : waitFinishList) {
						if (f.length() < fileCapacity) {
							ensureCapacity(f,false);
						}
						idleList.put(f);
					}
					while (true) {
							File f;
							while (true) {
								int randomNum = random.nextInt();
								f = new File(workspace.getAbsoluteFile() + "/" + String.valueOf(randomNum));
								if (!f.exists()) {
									break;
								}
							}
							f.createNewFile();
							ensureCapacity(f,false);
							idleList.put(f);
					}
				} catch (InterruptedException e) {
					logger.info("shutdown file allocator!");
				} catch (Throwable e) {
					logger.error("unexcepted error", e);
				}
			}
		
		}
	};
	private class SafeIdleFileMaker implements Runnable {
		private BlockingQueue<File> finishedList;
		
		public SafeIdleFileMaker(BlockingQueue<File> finishedList) {
			super();
			this.finishedList = finishedList;
		}

		public void run() {
			synchronized (this) {
				try {
					if (safeAllocatorThread.isInterrupted()) {
						return;
					}
					while(finishedList.peek() != null){
						File f = finishedList.poll();
						if(f != null){
							safeIdleList.put(f);
						}
					}
					while (true) {
							File f;
							while (true) {
								int randomNum = random.nextInt();
								f = new File(workspace.getAbsoluteFile() + "/" + String.valueOf(randomNum));
								if (!f.exists()) {
									break;
								}
							}
							f.createNewFile();
							ensureCapacity(f,true);
							safeIdleList.put(f);
					}
				} catch (InterruptedException e) {
					logger.info("shutdown file allocator!");
				} catch (Throwable e) {
					logger.error("unexcepted error", e);
				}
			}
		
		}
	};

}
