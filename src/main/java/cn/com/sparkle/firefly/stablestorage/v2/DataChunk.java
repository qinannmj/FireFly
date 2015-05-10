package cn.com.sparkle.firefly.stablestorage.v2;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.Callable;

import javax.management.RuntimeErrorException;

import org.apache.log4j.Logger;

import cn.com.sparkle.firefly.Context;
import cn.com.sparkle.firefly.checksum.ChecksumUtil.UnsupportedChecksumAlgorithm;
import cn.com.sparkle.firefly.model.Value.ValueType;
import cn.com.sparkle.firefly.stablestorage.ReadRecordCallback;
import cn.com.sparkle.firefly.stablestorage.io.RecordFileOut;
import cn.com.sparkle.firefly.stablestorage.io.RecordFileOutFactory;
import cn.com.sparkle.firefly.stablestorage.model.Record;
import cn.com.sparkle.firefly.stablestorage.model.RecordBody;
import cn.com.sparkle.firefly.stablestorage.model.RecordHead;
import cn.com.sparkle.firefly.stablestorage.model.RecordType;
import cn.com.sparkle.firefly.stablestorage.model.StoreModel.InstanceVoteRecord;
import cn.com.sparkle.firefly.stablestorage.model.StoreModel.SuccessfulRecord;
import cn.com.sparkle.firefly.util.IdComparator;
import cn.com.sparkle.firefly.util.LongUtil;

import com.google.protobuf.GeneratedMessage.Builder;

/**
 * 
 * @author qinan.qn
 * 1 vote record
 * 2 vote record
 * 3 vote record
 * 4 vote record
 * n vote record
 * 1 successful record
 * n successful record
 * 
 * successful record and isRedo flush to disk while this trunk was closed.
 */
public class DataChunk {
	private final static Logger logger = Logger.getLogger(DataChunk.class);

	private boolean isInited = false;
	private boolean closing = false;

	public boolean isClosing() {
		return closing;
	}

	public void setClosing(boolean closing) {
		this.closing = closing;
	}

	private RecordFileOutFactory factory;
	private long instanceId;
	private File file;
	private long maxVoteInstanceId;//the max instanceId this trunk has recorded
	private long successfullInstanceId;//the instanceId this trunk has recorded is ordered
	private long capacity;
	private long used;
	private RecordFileOut writeStream = null;

	public boolean isInited() {
		return isInited;
	}

	public void setInited(boolean isInited) {
		this.isInited = isInited;
	}

	private Context context;

	public DataChunk(RecordFileOutFactory factory, File f, Context context) {
		this.factory = factory;
		this.file = f;
		this.instanceId = Long.parseLong(f.getName());
		this.maxVoteInstanceId = instanceId - 1;
		this.successfullInstanceId = this.maxVoteInstanceId;
		this.context = context;
		capacity = f.length();
	}

	@SuppressWarnings("rawtypes")
	public ReadResult initRead(long startInstanceId, ReadRecordCallback<Builder<? extends Builder>> readCallback) throws IOException,
			UnsupportedChecksumAlgorithm {
		if (context.getConfiguration().isDebugLog()) {
			logger.debug(String.format("initRead from:%s file:%s", startInstanceId, file.getAbsoluteFile()));
		}
		ReadResult r = readRecord(startInstanceId, Long.MAX_VALUE, readCallback);
		used = r.pos;
		if (this.maxVoteInstanceId < r.maxVoteInstanceId) {
			this.maxVoteInstanceId = r.maxVoteInstanceId;
		}
		this.successfullInstanceId = r.maxSuccessInstanceId;
		return r;
	}

	public File getFile() {
		return file;
	}

	public void writeVote(long instanceId, Record record, Callable<Object> call) throws ChunkFullException, IOException {
		checkBufferout();
		int recordLen = record.getSerializeSize();
		if (maxVoteInstanceId >= instanceId || capacity >= (used + recordLen)) {
			record.writeToStream(writeStream, call, true);
			used += recordLen;
			if (instanceId > maxVoteInstanceId) {
				this.maxVoteInstanceId = instanceId;
			}
		} else {
			throw new ChunkFullException();
		}
	}

	public void writeSuccess(long instanceId, SuccessfulRecord.Builder successRecord, Record record,Callable<Object> realEvent) throws IOException, ChunkFullException {

		if (successfullInstanceId >= instanceId) {
			//the success has written and give up write,
			return;
		} else if (successfullInstanceId + 1 == instanceId) {
			checkBufferout();
			int recordLen = record.getSerializeSize();
			if (maxVoteInstanceId >= instanceId || capacity >= (used + recordLen)) {
				record.writeToStream(writeStream, realEvent, realEvent != null);
				++successfullInstanceId;
				if (successRecord.getV().getType() == ValueType.PLACE.getValue()) {
					long value = LongUtil.toLong(successRecord.getV().getValues().toByteArray(), 0);
					if (value > successfullInstanceId) {
						successfullInstanceId = value;
					}
				}
				used += recordLen;
			} else {
				throw new ChunkFullException();
			}
		} else {
			throw new RuntimeException(String.format("excepted successful instanceId %s , give instanceId %s", successfullInstanceId + 1, instanceId));
		}
		if (context.getConfiguration().isDebugLog()) {
			logger.debug("instanceId:" + instanceId + " isPlace " + (successRecord.getV().getType() == ValueType.PLACE.getValue()) + " successfullInstanceId "
					+ successfullInstanceId);
		}
	}

	public void close() throws IOException, UnsupportedChecksumAlgorithm {
		if (writeStream != null) {
			writeStream.close();
			writeStream = null;
		}
	}

	@SuppressWarnings("rawtypes")
	public ReadResult readRecord(long fromInstanceId, long toInstanceId, ReadRecordCallback<Builder<? extends Builder>> readCallback) throws IOException,
			UnsupportedChecksumAlgorithm {

		// read from read log
		long maxVoteInstanceId = this.instanceId - 1;
		long successInstanceId = this.instanceId - 1;

		long pos = 0;
		DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
		HashMap<Long, InstanceVoteRecord> voteRecordMap = new HashMap<Long, InstanceVoteRecord>(1000);
		try {
			while (true) {
				try {
					RecordHead head = RecordHead.readFromStream(in);
					if (head != null) {
						if (head.isValid()) {
							if (head.getInstanceId() > maxVoteInstanceId) {
								maxVoteInstanceId = head.getInstanceId();
							}
							if (head.getInstanceId() < fromInstanceId || head.getInstanceId() > toInstanceId) {
								in.skipBytes(head.getBodySize() + head.getBodyChecksumLength());
								pos += head.getSerializeSize() + head.getBodySize() + head.getBodyChecksumLength();
								if (head.getType() == RecordType.SUCCESS && head.getInstanceId() > successInstanceId) {
									successInstanceId = head.getInstanceId();
								}
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
									if (head.getInstanceId() > successInstanceId) {
										successInstanceId = head.getInstanceId();
									}
									if (record.getV().getType() == ValueType.PLACE.getValue()) {
										long value = LongUtil.toLong(record.getV().getValues().toByteArray(), 0);
										if (value > successInstanceId) {
											successInstanceId = value;
										}
									}
								} else {
									InstanceVoteRecord.Builder voteRecord = InstanceVoteRecord.newBuilder().mergeFrom(body.getBody());
									voteRecordMap.put(head.getInstanceId(), voteRecord.build());
									readCallback.read(head.getInstanceId(), voteRecord);
								}
								pos += head.getSerializeSize() + head.getBodySize() + head.getBodyChecksumLength();

							} else {
								logger.warn(String.format("checksum error file %s , pos: %s", file.getAbsolutePath(), pos));
								break;
							}

						} else {
							logger.warn(String.format("checksum error file %s , pos: %s", file.getAbsolutePath(), pos));
							break;

						}
					} else {
						break;
					}
				} catch (EOFException e) {
					break;
				}
			}
			return new ReadResult(voteRecordMap, pos, maxVoteInstanceId, successInstanceId);
		} finally {
			if (in != null) {
				in.close();
			}
		}
	}

	public long getInstanceId() {
		return this.instanceId;
	}

	public long getSuccessfullInstanceId() {
		return successfullInstanceId;
	}

	public long getMaxVoteInstanceId() {
		return maxVoteInstanceId;
	}

	public void setMaxVoteInstanceId(long maxVoteInstanceId) {
		this.maxVoteInstanceId = maxVoteInstanceId;
	}

	private void checkBufferout() throws IOException {
		if (!isInited) {
			throw new RuntimeException("uninited data chunk");
		}
		if (writeStream == null) {
			writeStream = factory.makeRecordFileOut(file, used);
		}
	}

	public final static class ReadResult {
		private long pos;
		private long maxVoteInstanceId;
		private long maxSuccessInstanceId;
		private HashMap<Long, InstanceVoteRecord> voteRecordMap;

		public HashMap<Long, InstanceVoteRecord> getVoteRecordMap() {
			return voteRecordMap;
		}

		public ReadResult(HashMap<Long, InstanceVoteRecord> voteRecordMap, long pos, long maxVoteInstanceId, long maxSuccessInstanceId) {
			super();
			this.pos = pos;
			this.maxVoteInstanceId = maxVoteInstanceId;
			this.maxSuccessInstanceId = maxSuccessInstanceId;
			this.voteRecordMap = voteRecordMap;
		}

		public long getPos() {
			return pos;
		}

		public long getMaxVoteInstanceId() {
			return maxVoteInstanceId;
		}

		public long getMaxSuccessInstanceId() {
			return maxSuccessInstanceId;
		}

	}
}
