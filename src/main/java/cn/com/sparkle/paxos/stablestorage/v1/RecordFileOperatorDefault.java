package cn.com.sparkle.paxos.stablestorage.v1;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import javax.management.RuntimeErrorException;

import org.apache.log4j.Logger;

import com.google.protobuf.GeneratedMessage.Builder;

import cn.com.sparkle.paxos.addprocess.AddRequestPackage;
import cn.com.sparkle.paxos.checksum.ChecksumUtil.UnsupportedChecksumAlgorithm;
import cn.com.sparkle.paxos.config.Configuration;
import cn.com.sparkle.paxos.deamon.InstanceExecutor;
import cn.com.sparkle.paxos.stablestorage.ReadRecordCallback;
import cn.com.sparkle.paxos.stablestorage.RecordFileOperator;
import cn.com.sparkle.paxos.stablestorage.event.PrepareRecordRealWriteEvent;
import cn.com.sparkle.paxos.stablestorage.io.BufferedFileOut;
import cn.com.sparkle.paxos.stablestorage.model.Record;
import cn.com.sparkle.paxos.stablestorage.model.RecordBody;
import cn.com.sparkle.paxos.stablestorage.model.RecordHead;
import cn.com.sparkle.paxos.stablestorage.model.RecordType;
import cn.com.sparkle.paxos.stablestorage.model.StoreModel.InstanceVoteRecord;
import cn.com.sparkle.paxos.stablestorage.model.StoreModel.SuccessfulRecord;
import cn.com.sparkle.paxos.stablestorage.model.SuccessfulRecordWrap;
import cn.com.sparkle.paxos.util.IdComparator;

public class RecordFileOperatorDefault implements RecordFileOperator {
	private final static Logger logger = Logger.getLogger(RecordFileOperatorDefault.class);

	private final static int DIR_FILE_NUM = 1000;
	public final static int SPLIT_SUCCESSFUL_RECORD_COUNT = 1000;
	private final static int MAX_UNSAFE_QUEUE_COUNT = 2 * SPLIT_SUCCESSFUL_RECORD_COUNT;

	private InstanceExecutor instanceExecutor;
	private PriorityQueue<SuccessfulRecordWrap> unsafeRecordQueue = new PriorityQueue<SuccessfulRecordWrap>();
	private HashMap<Long, SuccessfulRecordWrap> unsafeSet = new HashMap<Long, SuccessfulRecordWrap>();
	private HashMap<Long, InstanceVoteRecord> votedInstanceRecordMap = new HashMap<Long, InstanceVoteRecord>();

	private HashSet<Long> badFileSet = new HashSet<Long>();
	private ReentrantLock writeLock = new ReentrantLock();
	private File dir;
	private LinkedList<RecordFileBean> recordFileBeanList = new LinkedList<RecordFileBean>();
	private volatile long lastExpectSafeInstanceId;
	private volatile long maxVoteInstanceId = -1;
	private int preferChecksum;

	private Condition unsafeRecordQueueFull = writeLock.newCondition(); // unsafeRecordQueue
																		// max
																		// size
																		// =
																		// SPLIT_SUCCESSFUL_RECORD_COUNT

	private boolean debugLog = false;

	private static class RecordFileBean {
		private File file;
		private BufferedFileOut out;
		private long fileFlag;

		public RecordFileBean(File file) {
			super();
			this.file = file;
			fileFlag = Integer.valueOf(file.getName());
		}
	}

	@Override
	public void initOperator(File dir, long lastExpectSafeInstanceId, InstanceExecutor instanceExecutor, Configuration conf) {
		this.debugLog = conf.isDebugLog();
		this.dir = dir;
		this.lastExpectSafeInstanceId = lastExpectSafeInstanceId;
		this.maxVoteInstanceId = lastExpectSafeInstanceId - 1;
		this.instanceExecutor = instanceExecutor;
		this.preferChecksum = conf.getFileChecksumType();
		if (!dir.exists()) {
			dir.mkdirs();
		}
	}

	/**
	 * 
	 * @return the last instanceId that can be executed
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws UnsupportedChecksumAlgorithm 
	 */
	public long loadData() throws IOException, ClassNotFoundException, UnsupportedChecksumAlgorithm {
		try {
			writeLock.lock();
			//			File[] files = dir.listFiles();
			//			Comparator<File> comparator = new Comparator<File>() {
			//				@Override
			//				public int compare(File o1, File o2) {
			//					return Long.valueOf(o1.getName()).compareTo(Long.valueOf(o2.getName()));
			//				}
			//			};
			//			Arrays.sort(files, comparator);
			long successStartFileFlag = getFileFlagOfInstanceId(lastExpectSafeInstanceId);
			long dirFlag = getDirFlagOfFileFlag(successStartFileFlag);
			long pos = 0;
			long i = dirFlag;
			// read from read log
//			for (int i = dirFlag; i < Integer.MAX_VALUE; ++i) {
			while(true){
				long maxFlag = (i + 1) * DIR_FILE_NUM;
				for (; successStartFileFlag < maxFlag; ++successStartFileFlag) {
					pos = 0;
					File f = new File(dir + "/" + i + "/" + successStartFileFlag);
					if (!f.exists()) {
						break;
					}
					RecordFileBean recordFileBean = new RecordFileBean(f);
					recordFileBeanList.addLast(recordFileBean);
					long nextStartInstanceId = (recordFileBean.fileFlag + 1) * SPLIT_SUCCESSFUL_RECORD_COUNT;
					DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(f)));
					try {
						while (true) {
							try {
								RecordHead head = RecordHead.readFromStream(in);
								if (head != null) {
									if (head.isValid()) {
										if (head.getInstanceId() < lastExpectSafeInstanceId) {
											in.skipBytes(head.getBodySize() + head.getBodyChecksumLength());
											pos += head.getSerializeSize() + head.getBodySize() + head.getBodyChecksumLength();
											continue;
										}
										RecordBody body = RecordBody.readFromStream(in, head);
										if (body == null) {
											break;
										}
										if (body.isValid()) {
											if (head.getType() == RecordType.SUCCESS) {
												InstanceVoteRecord voteRecord = votedInstanceRecordMap.remove(head.getInstanceId());
												SuccessfulRecord.Builder record = SuccessfulRecord.newBuilder().mergeFrom(body.getBody());
												if (!record.hasV()) {
													if (IdComparator.getInstance().compare(record.getHighestVoteNum(), voteRecord.getHighestVotedNum()) == 0) {
														record.setV(voteRecord.getHighestValue());
													} else {
														logger.error("fatal error,the program logic is error ,please check code");
														throw new RuntimeErrorException(new Error("fatal error,the program logic is error ,please check code"));
													}
												}
												SuccessfulRecordWrap recordWrap = new SuccessfulRecordWrap(head.getInstanceId(), record.build(), null);

												unsafeRecordQueue.add(recordWrap);
												unsafeSet.put(head.getInstanceId(), recordWrap);
												while (recordWrap.getInstanceId() >= lastExpectSafeInstanceId + MAX_UNSAFE_QUEUE_COUNT) {
													// wait study from other node
													try {
														if (debugLog) {
															logger.debug("unsafeQueue full ,wait put forward record. instanceId:" + recordWrap.getInstanceId()
																	+ " lastExpectSafeInstanceId:" + lastExpectSafeInstanceId);
														}
														unsafeRecordQueueFull.await();
													} catch (InterruptedException e) {
														logger.error("unexception exception", e);
													}
												}
												while ((recordWrap = unsafeRecordQueue.peek()) != null
														&& unsafeRecordQueue.peek().getInstanceId() == lastExpectSafeInstanceId) {
													++lastExpectSafeInstanceId;
													instanceExecutor.execute(recordWrap);
													unsafeRecordQueue.poll();
													unsafeSet.remove(recordWrap.getInstanceId());
												}

											} else {
												InstanceVoteRecord instance = InstanceVoteRecord.newBuilder().mergeFrom(body.getBody()).build();
												votedInstanceRecordMap.put(head.getInstanceId(), instance);
												if (head.getInstanceId() > maxVoteInstanceId) {
													maxVoteInstanceId = head.getInstanceId();
												}
											}
										} else {
											badFileSet.add(recordFileBean.fileFlag);
											logger.warn("checksum error fileflag:" + recordFileBean.fileFlag + " file pos:" + pos);
											break;
										}

									} else {
										badFileSet.add(recordFileBean.fileFlag);
										logger.warn("checksum error fileflag:" + recordFileBean.fileFlag + " file pos:" + pos);
										break;
									}
									pos += head.getSerializeSize() + head.getBodySize() + head.getBodyChecksumLength();

								} else {
									break;
								}
							} catch (EOFException e) {
								break;
							}
						}
						// check file count
						if (lastExpectSafeInstanceId != nextStartInstanceId) {
							RandomAccessFile raff = new RandomAccessFile(f, "rws");
							raff.seek(pos);
							recordFileBean.out = new BufferedFileOut(raff);
						} else {
							while (recordFileBeanList.size() > 1) {
								if (recordFileBeanList.getFirst().out == null) {
									recordFileBeanList.removeFirst();
								} else {
									break;
								}
							}
						}
					} finally {
						close(in);
					}
				}
				if (successStartFileFlag != maxFlag) {//end cycle
					break;
				}
				++i;
			}

			if (recordFileBeanList.size() == 0) {
				File d = new File(dir + "/0");
				d.mkdir();
				File f = new File(dir + "/0/0");
				f.createNewFile();
				recordFileBeanList.add(new RecordFileBean(f));
			}
			return lastExpectSafeInstanceId - 1;
		} finally {
			writeLock.unlock();
		}
	}

	public boolean writeSuccessfulRecord(long instanceId, SuccessfulRecord.Builder successfulRecord, LinkedList<AddRequestPackage> addRequestPackages,
			final Callable<Object> realEvent) throws IOException, UnsupportedChecksumAlgorithm {

		try {
			writeLock.lock();

			while (instanceId >= lastExpectSafeInstanceId + MAX_UNSAFE_QUEUE_COUNT) {
				// wait study from other node
				try {
					unsafeRecordQueueFull.await();
				} catch (InterruptedException e) {
					logger.error("unexception exception", e);
				}
			}
			if (instanceId < lastExpectSafeInstanceId || unsafeSet.containsKey(instanceId)) {
				try {
					realEvent.call();
				} catch (Exception e) {
				}
				return true;
			}
			InstanceVoteRecord voteRecord = votedInstanceRecordMap.remove(instanceId);
			//check successfulRecord.hasV
			boolean isVotedBySelf = voteRecord != null
					&& IdComparator.getInstance().compare(successfulRecord.getHighestVoteNum(), voteRecord.getHighestVotedNum()) == 0;
			//build a object to save
			RecordBody body = new RecordBody(successfulRecord.build().toByteArray(), preferChecksum);
			RecordHead head = new RecordHead(body.getBody().length, instanceId, RecordType.SUCCESS, preferChecksum);
			Record record = new Record(head, body);

			//build a execute record
			if (!successfulRecord.hasV()) {
				if (isVotedBySelf) {
					//is in order to reduce io of network,because for the node having 
					//voted in last vote round it has record real value ,the master only
					//notify the id to this node,we need to assemble the value to record.
					successfulRecord.setV(voteRecord.getHighestValue());
				} else {
					logger.error("fatal error,the program logic is error ,please check code");
					throw new RuntimeErrorException(new Error("fatal error,the program logic is error ,please check code"));
				}
			}
			//build a object
			SuccessfulRecordWrap recordWrap = new SuccessfulRecordWrap(instanceId, successfulRecord.build(), addRequestPackages);

			boolean isSuccess = writeRecordLog(record, new Callable<Object>() {
				@Override
				public Object call() throws Exception {
					// notify
					if (realEvent != null) {
						realEvent.call();
					}
					return null;
				}
			});
			if (isSuccess) {
				if(instanceId > maxVoteInstanceId){
					maxVoteInstanceId = instanceId;
				}
				unsafeSet.put(recordWrap.getInstanceId(), recordWrap);
				unsafeRecordQueue.add(recordWrap);

				boolean isGoFoward = false;
				while ((recordWrap = unsafeRecordQueue.peek()) != null && recordWrap.getInstanceId() == lastExpectSafeInstanceId) {
					++lastExpectSafeInstanceId;
					instanceExecutor.execute(recordWrap);
					unsafeRecordQueue.poll();
					unsafeSet.remove(recordWrap.getInstanceId());
					isGoFoward = true;
				}
				if (isGoFoward) {
					unsafeRecordQueueFull.signalAll();
				}
				checkCloseFile(); //close file
			}

			return isSuccess;
		} finally {
			writeLock.unlock();
		}
	}

	/**
	 * 
	 * @param instanceId
	 * @param highestJoinNum
	 * @param highestVotedNum
	 * @param highestValue
	 * @param realWriteEvent
	 * @return is can write
	 * @throws IOException
	 * @throws UnsupportedChecksumAlgorithm 
	 */
	public boolean writeVoteRecord(final long instanceId, final InstanceVoteRecord record, final PrepareRecordRealWriteEvent realWriteEvent)
			throws IOException, UnsupportedChecksumAlgorithm {
		try {
			writeLock.lock();
			SuccessfulRecordWrap unsafeRecord = unsafeSet.get(instanceId);
			if (instanceId < lastExpectSafeInstanceId) {
				realWriteEvent.instanceExecuted(instanceId);
				return true;
			} else if (unsafeRecord != null) {
				try {
					realWriteEvent.instanceSucceeded(instanceId, unsafeRecord.getRecord());
				} catch (Exception e) {
				}
				return true;
			}
			//just for look up state
			if (instanceId > maxVoteInstanceId) {
				maxVoteInstanceId = instanceId;
			}

			RecordBody body = new RecordBody(record.toByteArray(), preferChecksum);
			RecordHead head = new RecordHead(body.getBody().length, instanceId, RecordType.VOTE, preferChecksum);
			Record r = new Record(head, body);
			boolean result = writeRecordLog(r, new Callable<Object>() {
				@Override
				public Object call() throws Exception {
					if (realWriteEvent != null) {
						realWriteEvent.successWrite(instanceId, record);
					}
					return null;
				}
			});
			if (result) {
				// record vote log to map
				votedInstanceRecordMap.put(instanceId, record);
				return true;
			} else {
				return false;
			}
		} finally {
			writeLock.unlock();
		}
	}

	private void checkCloseFile() throws IOException {

		while (recordFileBeanList.size() > 1) {//can't remove all
			RecordFileBean recordFileBean = recordFileBeanList.getFirst();
			long nextInstanceId = (recordFileBean.fileFlag + 1) * SPLIT_SUCCESSFUL_RECORD_COUNT;
			if (lastExpectSafeInstanceId >= nextInstanceId) {
				if (recordFileBean.out != null) {
					recordFileBean.out.close();
					recordFileBean.out = null;
				}
				recordFileBeanList.removeFirst();
			} else {
				break;
			}
		}
	}

	private boolean writeRecordLog(Record record, Callable<Object> callable) throws FileNotFoundException, IOException {
		RecordFileBean curFb = null;
		BufferedFileOut out = null;
		long fileFlag = getFileFlagOfInstanceId(record.getHead().getInstanceId());
		if (badFileSet.contains(fileFlag) && record.getHead().getType() != RecordType.SUCCESS) {// successful
																								// record
																								// must
																								// be
																								// write
			return false;
		}

		if (recordFileBeanList.getLast().fileFlag < fileFlag) {
			for (long i = recordFileBeanList.getLast().fileFlag + 1; i <= fileFlag; ++i) {
				long dirNum = getDirFlagOfFileFlag(i);
				if (i == dirNum * DIR_FILE_NUM) {
					File d = new File(dir + "/" + dirNum);
					d.mkdir();
				}
				File f = new File(dir + "/" + dirNum + "/" + i);
				f.createNewFile();
				RecordFileBean recordFileBean = new RecordFileBean(f);
				recordFileBeanList.addLast(recordFileBean);
			}
		}

		Iterator<RecordFileBean> iter = recordFileBeanList.descendingIterator();
		while (iter.hasNext()) {
			curFb = iter.next();
			if (curFb.fileFlag == fileFlag) {
				out = curFb.out;
				if (out == null) {
					curFb.out = new BufferedFileOut(new RandomAccessFile(curFb.file, "rws"));
					out = curFb.out;
				}
				break;
			}
		}
		record.writeToStream(out, callable);
		return true;
	}

	public long getLastExpectSafeInstanceId() {
		return lastExpectSafeInstanceId;
	}

	public HashMap<Long, InstanceVoteRecord> getVotedInstanceRecordMap() {
		return votedInstanceRecordMap;
	}

	public long getMaxVoteInstanceId() {
		return maxVoteInstanceId;
	}

	public boolean isSuccessful(long instanceId) {
		if (instanceId < lastExpectSafeInstanceId) {
			return true;
		}
		try {
			writeLock.lock();
			if (instanceId < lastExpectSafeInstanceId || unsafeSet.containsKey(instanceId)) {
				return true;
			} else {
				return false;
			}
		} finally {
			writeLock.unlock();
		}
	}
	@SuppressWarnings("rawtypes")
	@Override
	public void readRecord(long fromInstanceId, long toInstanceId, ReadRecordCallback<Builder<? extends Builder>> readCallback) throws IOException, UnsupportedChecksumAlgorithm {
		long startFile = getFileFlagOfInstanceId(fromInstanceId);
		long endFile = getFileFlagOfInstanceId(toInstanceId);
		long startDir = getDirFlagOfFileFlag(startFile);
		long endDir = getDirFlagOfFileFlag(endFile);
		// read from read log
		for (long i = startDir; i <= endDir; ++i) {
			long maxFile = (i + 1) * DIR_FILE_NUM;
			maxFile = maxFile > endFile ? endFile + 1 : maxFile;
			for (; startFile < maxFile; ++startFile) {
				long pos = 0;
				File f = new File(dir + "/" + i + "/" + startFile);
				if (!f.exists()) {
					break;
				}
				DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(f)));

				HashMap<Long, InstanceVoteRecord> voteRecordMap = new HashMap<Long, InstanceVoteRecord>(1000);
				try {
					while (true) {
						try {
							RecordHead head = RecordHead.readFromStream(in);
							if (head != null) {
								if (head.isValid()) {
									if (head.getInstanceId() < fromInstanceId || head.getInstanceId() > toInstanceId) {
										in.skipBytes(head.getBodySize() + head.getBodyChecksumLength());
										pos += head.getSerializeSize() + head.getBodySize() + head.getBodyChecksumLength();
										continue;
									}
									RecordBody body = RecordBody.readFromStream(in, head);
									if (body.isValid()) {
										if (head.getType() == RecordType.SUCCESS) {
											SuccessfulRecord.Builder record = SuccessfulRecord.newBuilder().mergeFrom(body.getBody());
											InstanceVoteRecord voteRecord = voteRecordMap.remove(head.getInstanceId());
											if (!record.hasV()) {
												if (IdComparator.getInstance().compare(record.getHighestVoteNum(), voteRecord.getHighestVotedNum()) == 0) {
													record.setV(voteRecord.getHighestValue());
												} else {
													logger.error("fatal error,the program logic is error ,please check code");
													throw new RuntimeErrorException(new Error("fatal error,the program logic is error ,please check code"));
												}
											}

											readCallback.read(head.getInstanceId(), record);
										} else {
											InstanceVoteRecord.Builder voteRecord = InstanceVoteRecord.newBuilder().mergeFrom(body.getBody());
											voteRecordMap.put(head.getInstanceId(), voteRecord.build());
											readCallback.read(head.getInstanceId(), voteRecord);
										}
										pos += head.getSerializeSize() + head.getBodySize() + head.getBodyChecksumLength();
									} else {
										logger.warn("checksum error fileflag:" + startFile + " file pos:" + pos);
										break;
									}

								} else {
									logger.warn("checksum error fileflag:" + startFile + " file pos:" + pos);
									break;

								}
							} else {
								break;
							}
						} catch (EOFException e) {
							break;
						}
					}
				} finally {
					close(in);
				}
			}
			if (startFile != maxFile) {
				break;
			}
		}
	}

	public long getFirstInstanceIdInUnsafe() {
		try {
			writeLock.lock();
			SuccessfulRecordWrap srw = unsafeRecordQueue.peek();
			return srw == null ? -1 : srw.getInstanceId();
		} finally {
			writeLock.unlock();
		}
	}

	public boolean isDamaged() {
		return badFileSet.size() != 0;
	}

	private long getFileFlagOfInstanceId(long instanceId) {
		return instanceId / SPLIT_SUCCESSFUL_RECORD_COUNT;
	}

	private long getDirFlagOfFileFlag(long fileFlag) {
		return fileFlag / DIR_FILE_NUM;
	}

	private void close(InputStream is) {
		if (is != null) {
			try {
				is.close();
			} catch (IOException e) {
			}
		}
	}

	@Override
	public long getMinSuccessRecordInstanceId() {
		File[] files = dir.listFiles();
		Comparator<File> comparator = new Comparator<File>() {
			@Override
			public int compare(File o1, File o2) {
				return Long.valueOf(o1.getName()).compareTo(Long.valueOf(o2.getName()));
			}
		};
		Arrays.sort(files, comparator);
		//		files
		if(files.length == 0){
			return 0;
		}else{
			return Integer.parseInt(files[0].getName()) * DIR_FILE_NUM * SPLIT_SUCCESSFUL_RECORD_COUNT;
		}
	}

}
