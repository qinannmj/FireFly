package cn.com.sparkle.firefly.stablestorage;

import cn.com.sparkle.firefly.checksum.ChecksumUtil;
import cn.com.sparkle.firefly.checksum.ChecksumUtil.UnsupportedChecksumAlgorithm;
import cn.com.sparkle.firefly.stablestorage.model.RecordHead;
import cn.com.sparkle.firefly.stablestorage.model.RecordType;

public class TestHead {
	public static void main(String[] args) throws UnsupportedChecksumAlgorithm {
		RecordHead head = new RecordHead(134217727,122324555434l,RecordType.SUCCESS,3);
		head.getBytes()[0] = 44;
		RecordHead test = new RecordHead(head.getBytes(),head.getChecksum());
		System.out.println(test.isValid());
		System.out.println(test.getBodySize());
		System.out.println(test.getType()  == RecordType.SUCCESS);
		System.out.println(test.getInstanceId());
		
		long ct = System.currentTimeMillis();
		for(int i = 0 ;i < 1000000; ++i){
			head = new RecordHead(i,1234,RecordType.VOTE,ChecksumUtil.INBUILD_ALDER32);
		}
		System.out.println(System.currentTimeMillis() - ct);
	}
}
