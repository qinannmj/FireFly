package cn.com.sparkle.firefly.util;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import cn.com.sparkle.raptor.core.io.BytesArraysOutputStream;

import com.google.protobuf.ByteString;
import com.google.protobuf.GeneratedMessage;

public class ProtobufUtil {
	public final static byte[][] transformTo(GeneratedMessage message) throws IOException{
		byte[][] bytes = BytesArrayMaker.makeBytesArray(message.getSerializedSize());
		BytesArraysOutputStream baos = new BytesArraysOutputStream(bytes);
		message.writeTo(baos);
		return bytes;
	}
	
	public final static List<ByteString> transformTo(byte[][] bytes){
		LinkedList<ByteString> list = new LinkedList<ByteString>();
		for(byte[] bs : bytes){
			ByteString byteString = ByteString.copyFrom(bs, 0, bs.length);
			list.add(byteString);
		}
		return list;
	}
}
