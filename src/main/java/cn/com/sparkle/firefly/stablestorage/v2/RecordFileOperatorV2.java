package cn.com.sparkle.firefly.stablestorage.v2;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import cn.com.sparkle.firefly.Context;
import cn.com.sparkle.firefly.addprocess.AddRequestDealer;
import cn.com.sparkle.firefly.addprocess.AddRequestPackage;
import cn.com.sparkle.firefly.checksum.ChecksumUtil.UnsupportedChecksumAlgorithm;
import cn.com.sparkle.firefly.deamon.InstanceExecutor;
import cn.com.sparkle.firefly.model.Value.ValueType;
import cn.com.sparkle.firefly.stablestorage.FileDamageException;
import cn.com.sparkle.firefly.stablestorage.ReadRecordCallback;
import cn.com.sparkle.firefly.stablestorage.ReadSuccessRecordCallback;
import cn.com.sparkle.firefly.stablestorage.RecordFileOperator;
import cn.com.sparkle.firefly.stablestorage.event.PrepareRecordRealWriteEvent;
import cn.com.sparkle.firefly.stablestorage.io.RecordFileOutFactory;
import cn.com.sparkle.firefly.stablestorage.model.Record;
import cn.com.sparkle.firefly.stablestorage.model.RecordBody;
import cn.com.sparkle.firefly.stablestorage.model.RecordHead;
import cn.com.sparkle.firefly.stablestorage.model.RecordType;
import cn.com.sparkle.firefly.stablestorage.model.StoreModel.InstanceVoteRecord;
import cn.com.sparkle.firefly.stablestorage.model.StoreModel.SuccessfulRecord;
import cn.com.sparkle.firefly.stablestorage.model.SuccessfulRecordWrap;
import cn.com.sparkle.firefly.stablestorage.v2.DataChunk.ReadResult;
import cn.com.sparkle.firefly.util.IdComparator;
import cn.com.sparkle.firefly.util.LongUtil;

import com.google.protobuf.GeneratedMessage.Builder;

public class RecordFileOperatorV2 implements RecordFileOperator {
	private final static Logger logger = Logger.getLogger(RecordFileOperatorV2.class);

	private File dir;
	private InstanceExecutor instanceExecutor;
	private long lastExpectSafeInstanceId;
	private RecordFileOutFactory factory;
	private Context context;

	private FileIndexer fileIndexer;
	private AsyncAllocator allocator;

	private HashMap<Long, InstanceVoteRecord> votedInstanceRecordMap = new HashMap<Long, InstanceVoteRecord>();
	private volatile long maxVoteInstanceId = -1; //just for look up
	private volatile long maxKnowedInstanceId = -1; //just for look up

	private ReentrantLock writeLock = new ReentrantLock();

	private PriorityQueue<InstanceSaveContext> unsafeRecordQueue = new PriorityQueue<InstanceSaveContext>(1000);

	@Override
	public void initOperator(File dir, long lastExpectSafeInstanceId, final InstanceExecutor instanceExecutor, RecordFileOutFactory outFactory, Context context) {
		this.dir = dir;
		this.lastExpectSafeInstanceId = lastExpectSafeInstanceId;
		this.instanceExecutor = instanceExecutor;
		this.factory = outFactory;
		this.context = context;
		//start allocator
		allocator = new AsyncAllocator(outFactory, context.getConfiguration(), dir);
		File[] files = dir.listFiles(new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				return pathname.isFile();
			}
		});//only file
		Comparator<File> comparator = new Comparator<File>() {
			@Override
			public int compare(File o1, File o2) {
				return Long.valueOf(o1.getName()).compareTo(Long.valueOf(o2.getName()));
			}
		};
		Arrays.sort(files, comparator);
		fileIndexer = new FileIndexer(factory, files, context);
	}

	@Override
	public long loadData() throws IOException, ClassNotFoundException, UnsupportedChecksumAlgorithm {

		int i = fileIndexer.findDataChunkIdx(lastExpectSafeInstanceId);
		for (; i < fileIndexer.getEnd() && i >= 0; ++i) {

			try {
				DataChunk dataChunk = fileIndexer.getIndex()[i];
				while (true) {
					try {
						writeLock.lock();
						if(unsafeRecordQueue.size() <= AddRequestDealer.MAX_EXECUTING_INSTANCE_NUM){
							
							ReadResult r = dataChunk.initRead(lastExpectSafeInstanceId, new ReadSuccessRecordCallback() {
								@Override
								public void readSuccess(long instanceId,
										cn.com.sparkle.firefly.stablestorage.model.StoreModel.SuccessfulRecord.Builder successfulRecordBuilder) {
									
									if(successfulRecordBuilder.getV().getType() == ValueType.PLACE.getValue()){
										context.getConfiguration().checkArbitrator();
										long value = LongUtil.toLong(successfulRecordBuilder.getV().getValues().toByteArray(), 0);
										if(value > lastExpectSafeInstanceId){
											lastExpectSafeInstanceId = value + 1;
										}
									}else{
										SuccessfulRecordWrap recordWrap = new SuccessfulRecordWrap(instanceId, successfulRecordBuilder.build(), null);
										instanceExecutor.execute(recordWrap);
										if(lastExpectSafeInstanceId == instanceId){
											++lastExpectSafeInstanceId;
										}else if(instanceId > lastExpectSafeInstanceId){
											unsafeRecordQueue.add(new InstanceSaveContext(instanceId, successfulRecordBuilder, null));
										}
										
									}
								}
							});
							maxKnowedInstanceId = r.getMaxVoteInstanceId() > r.getMaxSuccessInstanceId() ? r.getMaxVoteInstanceId() : r.getMaxSuccessInstanceId();
							maxVoteInstanceId = maxKnowedInstanceId;
							votedInstanceRecordMap.putAll(r.getVoteRecordMap());
							//clear votemap for arbitrator
							Iterator<Entry<Long, InstanceVoteRecord>> iter = votedInstanceRecordMap.entrySet().iterator();
							while(iter.hasNext()){
								Entry<Long, InstanceVoteRecord> e = iter.next();
								if(e.getKey()<lastExpectSafeInstanceId){
									iter.remove();
								}
							}
							while(unsafeRecordQueue.size() != 0 && unsafeRecordQueue.peek().getInstanceId() < lastExpectSafeInstanceId){
								//poll repeated record
								InstanceSaveContext saveContext = unsafeRecordQueue.poll();
								if(saveContext.successfulRecord.getV().getType() == ValueType.PLACE.getValue()){
									long value = LongUtil.toLong(saveContext.successfulRecord.getV().getValues().toByteArray(), 0);
									if(value > lastExpectSafeInstanceId){
										for(long ll = lastExpectSafeInstanceId; ll < value + 1 ; ++ll ){
											this.votedInstanceRecordMap.remove(ll);
										}
										lastExpectSafeInstanceId = value + 1;
									}
								}
							}
							
							dataChunk.setInited(true);
							//write noredo hint
							if(dataChunk.getMaxVoteInstanceId() == dataChunk.getSuccessfullInstanceId()){
								checkHintNoRedo();
							}
							logger.debug(String.format("init load from [%s] votedInstanceRecordMap.size[%s] unsafeRecordQueue.size[%s],",dataChunk.getFile().getName(),votedInstanceRecordMap.size(),unsafeRecordQueue.size()));
							break;
						}
						
					} finally {
						writeLock.unlock();
					}
					//damage will lead to reach next code
					TimeUnit.MILLISECONDS.sleep(10);
				}

			} catch (Throwable e) {
				logger.error("fatal error", e);
				System.exit(1);
			}
		}
		return lastExpectSafeInstanceId - 1;
	}

	@Override
	public boolean writeSuccessfulRecord(long instanceId, SuccessfulRecord.Builder successfulRecord,
			LinkedList<AddRequestPackage> addRequestPackages, Callable<Object> realEvent) throws IOException, UnsupportedChecksumAlgorithm {
		try {
			writeLock.lock();

			while (true) { //for detect chunk full
				DataChunk dataChunk = null;
				try {
					
					if (instanceId > lastExpectSafeInstanceId) {
//						logger.info(String.format("waiting lastExpectSafeInstanceId %s instanceId %s", lastExpectSafeInstanceId, instanceId));
						//								catchUpWait.await();
						unsafeRecordQueue.add(new InstanceSaveContext(instanceId, successfulRecord, addRequestPackages));
						break;
					}else if(instanceId == lastExpectSafeInstanceId){
						
						dataChunk = fileIndexer.findDataChunk(instanceId);
						if (dataChunk == null && fileIndexer.getEnd() == 0) {
							throw new ChunkFullException();
						}
						if(!dataChunk.isInited()){
							return false;
						}
						
						InstanceVoteRecord voteRecord = votedInstanceRecordMap.remove(instanceId);
						//check successfulRecord.hasV
						boolean isVotedBySelf = voteRecord != null
								&& IdComparator.getInstance().compare(successfulRecord.getHighestVoteNum(), voteRecord.getHighestVotedNum()) == 0;
						//build a object to save
						RecordBody body = new RecordBody(successfulRecord.build().toByteArray(), context.getConfiguration().getFileChecksumType());
						RecordHead head = new RecordHead(body.getBody().length, instanceId, RecordType.SUCCESS, context.getConfiguration().getFileChecksumType());
						Record record = new Record(head, body);

						//build a execute record
						if (!successfulRecord.hasV()) {
							if (isVotedBySelf) {
								//is in order to reduce io of network,because for the node having 
								//voted in last vote round it has record real value ,the master only
								//notify the id to this node,we need to assemble the value to record.
								successfulRecord.setV(voteRecord.getHighestValue());
							} else {
								logger.warn("This node has not voted this instance!");
								return false;
							}
						}
						//build a object
						SuccessfulRecordWrap recordWrap = new SuccessfulRecordWrap(instanceId, successfulRecord.build(), addRequestPackages);
						
						//check if self is arbitrator
						if(successfulRecord.getV().getType() == ValueType.PLACE.getValue()){
							context.getConfiguration().checkArbitrator();
						}
						
						dataChunk.writeSuccess(instanceId,successfulRecord, record,realEvent);
						
						if(successfulRecord.getV().getType() == ValueType.PLACE.getValue()){
							long value = LongUtil.toLong(successfulRecord.getV().getValues().toByteArray(), 0);
							if(value > lastExpectSafeInstanceId){
								for(long i = lastExpectSafeInstanceId; i < value + 1 ; ++i ){
									this.votedInstanceRecordMap.remove(i);
								}
								lastExpectSafeInstanceId = value + 1;
							}
						}else{
							++lastExpectSafeInstanceId;
						}
						instanceExecutor.execute(recordWrap);
						
						if(dataChunk.isClosing() && dataChunk.getMaxVoteInstanceId() == dataChunk.getSuccessfullInstanceId()){
							dataChunk.close();
						}
					}
					
					while(unsafeRecordQueue.size() != 0 && unsafeRecordQueue.peek().getInstanceId() < lastExpectSafeInstanceId){
						//poll repeated record
						InstanceSaveContext saveContext = unsafeRecordQueue.poll();
						if(saveContext.successfulRecord.getV().getType() == ValueType.PLACE.getValue()){
							long value = LongUtil.toLong(saveContext.successfulRecord.getV().getValues().toByteArray(), 0);
							if(value > lastExpectSafeInstanceId){
								for(long i = lastExpectSafeInstanceId; i < value + 1 ; ++i ){
									this.votedInstanceRecordMap.remove(i);
								}
								lastExpectSafeInstanceId = value + 1;
							}
						}
					}
					
					if(unsafeRecordQueue.size() != 0 && unsafeRecordQueue.peek().getInstanceId() == lastExpectSafeInstanceId){
						//restore the context of instance be stored in queue
						InstanceSaveContext saveContext = unsafeRecordQueue.poll();
						instanceId = saveContext.getInstanceId();
						successfulRecord = saveContext.getSuccessfulRecord();
						addRequestPackages = saveContext.getAddRequestPackages();
					}else{
						break;
					}
				} catch (ChunkFullException e) {
					//fetch a new file from async allocator
					if (dataChunk != null) {
						dataChunk.close(); // this chunk is full.
						checkHintNoRedo();
					}
					try {
						DataChunk chunk = new DataChunk(factory, allocator.getIdle(dir.getAbsolutePath() + "/" + instanceId), context);
						chunk.setInited(true);
						if (context.getConfiguration().isDebugLog()) {
							logger.debug(String.format("allocate a file %s", chunk.getFile().getAbsoluteFile()));
						}
						fileIndexer.add(chunk);
					} catch (InterruptedException e1) {
						logger.error("unexcepted error", e);
					}
				}
			}
			
			if (lastExpectSafeInstanceId > maxKnowedInstanceId + 1) {
				maxKnowedInstanceId = lastExpectSafeInstanceId - 1;
			}
			return true;
		} finally {
			writeLock.unlock();
		}

	}

	@Override
	public boolean writeVoteRecord(final long instanceId, final InstanceVoteRecord record, final PrepareRecordRealWriteEvent realWriteEvent)
			throws IOException, UnsupportedChecksumAlgorithm {
		try {
			writeLock.lock();
			while (true) {
				DataChunk dataChunk = null;
				try {
					if (instanceId < lastExpectSafeInstanceId) {
						realWriteEvent.instanceExecuted(instanceId);
						return true;//has succeeded
					}
					dataChunk = fileIndexer.findDataChunk(instanceId);
					if (dataChunk == null && fileIndexer.getEnd() == 0) {
						throw new ChunkFullException();
					}

					RecordBody body = new RecordBody(record.toByteArray(), context.getConfiguration().getFileChecksumType());
					RecordHead head = new RecordHead(body.getBody().length, instanceId, RecordType.VOTE, context.getConfiguration().getFileChecksumType());
					final Record r = new Record(head, body);
					dataChunk.writeVote(instanceId, r, new Callable<Object>() {
						@Override
						public Object call() throws Exception {
							if (realWriteEvent != null) {
								realWriteEvent.successWrite(instanceId, record);
							}
							return null;
						}
					});
					// record vote log to map
					votedInstanceRecordMap.put(instanceId, record);
					//just for look up state
					if (instanceId > maxVoteInstanceId) {
						maxVoteInstanceId = instanceId;
					}
					if (instanceId > maxKnowedInstanceId) {
						maxKnowedInstanceId = instanceId;
					}
					break;
				} catch (ChunkFullException e) {
					//check file if has been finished
					if (dataChunk != null) {
						if (dataChunk.getMaxVoteInstanceId() == dataChunk.getSuccessfullInstanceId()) {
							dataChunk.close(); // this chunk is full.
							checkHintNoRedo();
						}else{
							dataChunk.setClosing(true);
						}
					}
					//fetch a new file from async allocator
					try {
						DataChunk chunk = new DataChunk(factory, allocator.getIdle(dir.getAbsolutePath() + "/"
								+ (dataChunk != null ? Math.max(dataChunk.getMaxVoteInstanceId(), dataChunk.getSuccessfullInstanceId()) + 1 : 0)), context);
						chunk.setInited(true);
						if (context.getConfiguration().isDebugLog()) {
							logger.debug(String.format("allocate a file %s", chunk.getFile().getAbsoluteFile()));
						}
						fileIndexer.add(chunk);
					} catch (InterruptedException e1) {
						logger.error("unexcepted error", e);
					}

				}
			}
			return true;
		} finally {
			writeLock.unlock();
		}
	}

	@Override
	public long getMinSuccessRecordInstanceId() {
		return fileIndexer.getEnd() != 0 ? fileIndexer.getIndex()[0].getInstanceId() : -1;
	}

	@Override
	public long getLastExpectSafeInstanceId() {
		return this.lastExpectSafeInstanceId;
	}

	@Override
	public HashMap<Long, InstanceVoteRecord> getVotedInstanceRecordMap() {
		return votedInstanceRecordMap;
	}

	@Override
	public long getMaxVoteInstanceId() {
		return this.maxVoteInstanceId;
	}

	@Override
	public long getKnowedMaxId() {
		return this.maxKnowedInstanceId;
	}

	@Override
	public boolean isSuccessful(long instanceId) {
		return lastExpectSafeInstanceId > instanceId;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void readRecord(long fromInstanceId, long toInstanceId, ReadRecordCallback<Builder<? extends Builder>> readCallback) throws IOException,
			UnsupportedChecksumAlgorithm, FileDamageException {
		long tempInstanceId = fromInstanceId;
		DataChunk endChunk = null;
		while (true) {
			DataChunk chunk = fileIndexer.findDataChunk(tempInstanceId);
			if (chunk != null) {
				ReadResult r = chunk.readRecord(tempInstanceId, toInstanceId, readCallback);
				tempInstanceId = r.getMaxSuccessInstanceId() + 1;
				if (chunk == endChunk) {
					break;
				}
				endChunk = chunk;
			} else {
				break;
			}
		}
	}

	@Override
	public long getFirstInstanceIdInUnsafe() {
		int i = fileIndexer.findDataChunkIdx(lastExpectSafeInstanceId);
		if (i != fileIndexer.getEnd() - 1) {
			return fileIndexer.getIndex()[i + 1].getInstanceId();
		}
		return -1;
	}

	@Override
	public boolean isDamaged() {
		return false;
	}

	@Override
	public void close() {
		int length = fileIndexer.getEnd();
		for (int i = 0; i < length; ++i) {
			try {
				fileIndexer.getIndex()[i].close();
			} catch (Throwable e) {
				logger.error("unexcepted error", e);
				System.exit(1);
			}
		}
		allocator.close();
	}

	@Override
	public void setExecutor(InstanceExecutor executor) {
		try {
			writeLock.lock();
			this.instanceExecutor = executor;
		} finally {
			writeLock.unlock();
		}

	}

	private void checkHintNoRedo() throws IOException, UnsupportedChecksumAlgorithm {
		long noRedoHint = context.getAccountBook().getInstanceIdNoRedoHint();
		DataChunk hintChunk = fileIndexer.findDataChunk(noRedoHint);
		if (hintChunk != null && hintChunk.getSuccessfullInstanceId() >= noRedoHint) {
			context.getAccountBook().writeExecuteLog(noRedoHint);
		}
	}

	private static class InstanceSaveContext implements Comparable<InstanceSaveContext> {
		private long instanceId;
		private SuccessfulRecord.Builder successfulRecord;
		LinkedList<AddRequestPackage> addRequestPackages;

		public InstanceSaveContext(long instanceId, cn.com.sparkle.firefly.stablestorage.model.StoreModel.SuccessfulRecord.Builder successfulRecord,
				LinkedList<AddRequestPackage> addRequestPackages) {
			super();
			this.instanceId = instanceId;
			this.successfulRecord = successfulRecord;
			this.addRequestPackages = addRequestPackages;
		}

		@Override
		public int compareTo(InstanceSaveContext o) {
			if (getInstanceId() == o.getInstanceId())
				return 0;
			return getInstanceId() > o.getInstanceId() ? 1 : -1;
		}

		public long getInstanceId() {
			return instanceId;
		}

		public SuccessfulRecord.Builder getSuccessfulRecord() {
			return successfulRecord;
		}

		public LinkedList<AddRequestPackage> getAddRequestPackages() {
			return addRequestPackages;
		}

	}
}
