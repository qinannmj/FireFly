package cn.com.sparkle.firefly.stablestorage.v2;

import java.io.File;

import cn.com.sparkle.firefly.Context;
import cn.com.sparkle.firefly.stablestorage.io.RecordFileOutFactory;

public class FileIndexer {

	private DataChunk[] index;
	private volatile int end = 0;

	public FileIndexer(RecordFileOutFactory factory, File[] files,Context context) {
		index = new DataChunk[files.length == 0 ? 1 : files.length * 2];
		int index_idx = 0;
		DataChunk prevChunk = null;
		for (File f : files) {
			DataChunk chunk = new DataChunk(factory, f,context); 
			index[index_idx++] = chunk;
			if(prevChunk != null){
				prevChunk.setClosing(true);
				prevChunk.setMaxVoteInstanceId(chunk.getMaxVoteInstanceId());
			}
			prevChunk = chunk;
		}
		end = files.length;
	}

	public DataChunk[] getIndex() {
		return index;
	}

	public int getEnd() {
		return end;
	}

	public synchronized void add(DataChunk dataChunk) {
		checkCapacity(end + 1);
		if (end == 0 || index[end - 1].getInstanceId() < dataChunk.getInstanceId()) {
			index[end] = dataChunk;
		} else {
			int i = binarySearch(dataChunk.getInstanceId(), 0, end - 1) + 1;
			System.arraycopy(index, i, index, i + 1, end - i);
			index[i] = dataChunk;
		}
		++end;
	}

	private void checkCapacity(int newCapacity) {
		if (newCapacity > index.length) {
			DataChunk[] newIndex = new DataChunk[index.length * 2];
			System.arraycopy(index, 0, newIndex, 0, index.length);
			index = newIndex;
		}
	}

	public DataChunk findDataChunk(long instanceId) {
		//binary search
		int i = findDataChunkIdx(instanceId);
		return i == -1 ? null : index[i];
	}

	public synchronized int findDataChunkIdx(long instanceId) {
		if (end == 0) {
			return -1;
		} else if (instanceId >= index[end - 1].getInstanceId()) {
			return end - 1;
		} else {
			//binary search
			return binarySearch(instanceId, 0, end - 1);
		}
	}

	private int binarySearch(long instanceId, int lp, int rp) {
		int size = rp - lp + 1;
		if (size == 1) {
			if (index[rp].getInstanceId() <= instanceId) {
				return rp;
			} else {
				return -1;
			}
		} else if (size == 2) {
			if (index[lp].getInstanceId() > instanceId) {
				return -1;
			} else if (index[rp].getInstanceId() > instanceId) {
				return lp;
			} else {
				return rp;
			}
		}
		int mid = (rp + lp) / 2;
		if (index[mid].getInstanceId() < instanceId) {
			//right search
			return binarySearch(instanceId, mid, rp);
		} else {
			return binarySearch(instanceId, lp, mid);
		}
	}

	public static void main(String[] args) {
		File[] f = new File[] { new File("c:\\123"), new File("c:\\234"), new File("c:\\334"), new File("c:\\434"), new File("c:\\534") };
		FileIndexer indexer = new FileIndexer(null, f,null);
		indexer.add(new DataChunk(null, new File("c:\\535"),null));
		for (int i = 0; i < indexer.getEnd(); ++i) {
			DataChunk dc = indexer.getIndex()[i];
			System.out.println(dc.getInstanceId());
		}

	}
}
