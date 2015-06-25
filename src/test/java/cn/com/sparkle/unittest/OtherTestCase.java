package cn.com.sparkle.unittest;

import static org.junit.Assert.*;

import org.junit.Test;

import cn.com.sparkle.firefly.util.BytesArrayMaker;

public class OtherTestCase {

	@Test
	public void testBytesArrayMaker() {
		byte[][] b = BytesArrayMaker.makeBytesArray(12345);
		int l = 0;
		for(byte[] bs : b){
			l += bs.length;
		}
		assertEquals(12345, l);
		byte[][] bb = BytesArrayMaker.makeBytesArray(8 * 1024 * 4);
		int ll = 0;
		for(byte[] bs : bb){
			ll += bs.length;
		}
		assertEquals(8 * 1024 * 4, ll);
	}
	

}
