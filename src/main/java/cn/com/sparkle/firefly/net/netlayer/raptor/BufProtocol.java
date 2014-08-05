package cn.com.sparkle.firefly.net.netlayer.raptor;

import java.io.IOException;
import java.util.List;

import cn.com.sparkle.firefly.net.netlayer.buf.Buf;
import cn.com.sparkle.raptor.core.buff.BuffPool;
import cn.com.sparkle.raptor.core.buff.BuffPool.PoolEmptyException;
import cn.com.sparkle.raptor.core.buff.IoBuffer;
import cn.com.sparkle.raptor.core.io.BufferPoolOutputStream;
import cn.com.sparkle.raptor.core.protocol.MultiThreadProtecolHandler.ProtocolHandlerIoSession;
import cn.com.sparkle.raptor.core.protocol.Protocol;

public class BufProtocol implements Protocol {

	@Override
	public void init(ProtocolHandlerIoSession mySession) {
	}

	@Override
	public Object decode(ProtocolHandlerIoSession mySession, IoBuffer buff) throws IOException {
		return new RaptorBuf(buff);
	}

	@Override
	public IoBuffer[] encode(BuffPool buffpool, Object message) throws IOException, PoolEmptyException {
		return encode(buffpool, message, null);
	}

	@Override
	public IoBuffer[] encode(BuffPool buffpool, Object message, IoBuffer lastWaitSendBuff) throws IOException, PoolEmptyException {
		Buf[] bufs = (Buf[]) message;
		try{
			BufferPoolOutputStream bufferPoolOutputStream = new BufferPoolOutputStream(buffpool, 0, lastWaitSendBuff);
	
			for (IoBuffer b : bufs) {
				bufferPoolOutputStream.write(b);
			}
	
			for (IoBuffer b : bufs) {
				b.close();// release pooled buff
			}
	
			List<IoBuffer> list = bufferPoolOutputStream.getBuffArray();
			if (lastWaitSendBuff != null) {
				list.remove(0);
			}
			bufferPoolOutputStream.close();
			return list.toArray(new IoBuffer[list.size()]);
		}catch(PoolEmptyException e){
			//restore bufs position
			for(Buf b : bufs){
				b.getByteBuffer().position(0);
			}
			throw e;
		}
	}

	@Override
	public Object decode(ProtocolHandlerIoSession mySession) throws IOException {
		return null;
	}

}
