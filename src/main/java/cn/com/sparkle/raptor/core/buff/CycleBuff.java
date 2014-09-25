package cn.com.sparkle.raptor.core.buff;

public interface CycleBuff extends IoBuffer {
	public BuffPool getPool();

	public void incRef();
	
}
