package cn.com.sparkle.raptor.core.collections;

public interface Queue<T> {
	public void poll();

	public T peek();

	public void push(T obj) throws Exception;

}
