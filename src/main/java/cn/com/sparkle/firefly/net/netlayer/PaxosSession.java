package cn.com.sparkle.firefly.net.netlayer;

import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import cn.com.sparkle.firefly.checksum.ChecksumUtil;
import cn.com.sparkle.firefly.checksum.ChecksumUtil.UnsupportedChecksumAlgorithm;
import cn.com.sparkle.firefly.net.frame.FrameBody;
import cn.com.sparkle.firefly.net.frame.FrameHead;
import cn.com.sparkle.firefly.net.netlayer.buf.Buf;

public abstract class PaxosSession {

	private final static Logger logger = Logger.getLogger(PaxosSession.class);

	private ObjectProtocolCacheBean protocolAttachment = new ObjectProtocolCacheBean();
	private ConcurrentHashMap<AttributeKey<?>, Object> attachment = new ConcurrentHashMap<AttributeKey<?>, Object>();
	private LinkedList<FrameBody> frameList = new LinkedList<FrameBody>();
	private int curPackageSize = 0;
	private int curPackageSizeLength = 0;

	private int checksumType = ChecksumUtil.PURE_JAVA_CRC32;

	@SuppressWarnings("unchecked")
	public <T> T get(AttributeKey<T> key) {
		return (T) attachment.get(key);
	}

	public ObjectProtocolCacheBean getProtocolAttachment() {
		return protocolAttachment;
	}

	public void setProtocolAttachment(ObjectProtocolCacheBean protocolAttachment) {
		this.protocolAttachment = protocolAttachment;
	}

	public void put(AttributeKey<?> key, Object obj) {
		attachment.put(key, obj);
	}
	public void remove(AttributeKey<?> key){
		attachment.remove(key);
	}

	public int getChecksumType() {
		return checksumType;
	}

	public void setChecksumType(int checksumType) {
		this.checksumType = checksumType;
	}

	public int getCurPackageSize() {
		return curPackageSize;
	}

	public int getCurPackageSizeLength() {
		return curPackageSizeLength;
	}

	public void setCurPackageSizeLength(int curPackageSizeLength) {
		this.curPackageSizeLength = curPackageSizeLength;
	}

	public void setCurPackageSize(int curPackageSize) {
		this.curPackageSize = curPackageSize;
	}

	public LinkedList<FrameBody> getFrameList() {
		return frameList;
	}

	public final synchronized void write(FrameBody message) throws NetCloseException {
		try {
			FrameHead head = new FrameHead(message.getChecksumType(), message.getBodySize());
			Buf[] bufArray = new Buf[3 + message.getBody().length];
			bufArray[0] = wrap(head.getHead());
			bufArray[1] = wrap(head.getChecksum());
			for(int i = 0 ; i < message.getBody().length ; ++i){
				bufArray[2 + i] = wrap(message.getBody()[i]);
			}
			bufArray[2 + message.getBody().length] = wrap(message.getChecksum());
			write(bufArray);
		} catch (UnsupportedChecksumAlgorithm e) {
			logger.error("fatal error", e);
			closeSession();
		}
	}

	public abstract Buf wrap(byte[] bytes);

	public abstract void write(Buf[] buf) throws NetCloseException;

	public abstract void closeSession();
	
	public abstract boolean isClose();
	
	public abstract String getRemoteAddress();
	
	public abstract String getLocalAddress();
	
	public static class ObjectProtocolCacheBean {
		public volatile FrameHead head = null;
		public volatile int recieveSize = 0;
		public LinkedList<Buf> buff = new LinkedList<Buf>();
	}
}
