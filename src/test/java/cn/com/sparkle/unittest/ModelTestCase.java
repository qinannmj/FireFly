package cn.com.sparkle.unittest;


import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import com.google.protobuf.ByteString;

import cn.com.sparkle.firefly.checksum.ChecksumUtil;
import cn.com.sparkle.firefly.checksum.ChecksumUtil.UnsupportedChecksumAlgorithm;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.PaxosMessages.MessagePackage;
import cn.com.sparkle.firefly.protocolprocessor.v0_0_1.PaxosMessages.ValueTrunk;
import cn.com.sparkle.firefly.stablestorage.model.RecordBody;
import cn.com.sparkle.firefly.util.ProtobufUtil;

public class ModelTestCase {
	@Test
	public void testRecord() throws UnsupportedChecksumAlgorithm{
		byte[][] buff = new byte[][]{new byte[16 *1024],new byte[7*1024]};
		RecordBody body = new RecordBody(buff, ChecksumUtil.PURE_JAVA_CRC32);
		Assert.assertTrue(body.isValid());
		Assert.assertEquals(buff[0].length + buff[1].length, body.getBodyLen());
		Assert.assertEquals(buff[0].length + buff[1].length + body.getChecksum().length, body.getSerializeSize());
	}
	
	@Test
	public void testProtobufTransform() throws IOException{
		byte[] bytes = new byte[16 * 1024 + 123];
		ValueTrunk.Builder v = ValueTrunk.newBuilder().setPart(ByteString.copyFrom(bytes));
		MessagePackage.Builder mp = MessagePackage.newBuilder().setId(123).setValueTrunk(v).setIsLast(false);
		byte[][] result = ProtobufUtil.transformTo(mp.build());
		mp = MessagePackage.newBuilder().mergeFrom(ByteString.copyFrom(ProtobufUtil.transformTo(result)));
		Assert.assertTrue(mp.hasValueTrunk());
		Assert.assertEquals(mp.getValueTrunk().getPart().toByteArray().length, bytes.length);
		Assert.assertEquals(mp.getId(), 123);
		Assert.assertFalse(mp.getIsLast());
	}
}
