package cn.com.sparkle.raptor.test;

public class A {
	
	public static volatile long ttt = 0;
	public static void main(String[] args) {
//		new B().start();
//		new C().start();
		for(int i = 0; i < 100 ; i++){
			new D().start();
		}
		try {
			Thread.sleep(60000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("final:" + ttt);
	}
	static class  D extends Thread{
		public void run(){
			for(int i = 0 ; i < 10000 ; i++)
				System.out.println(ttt ++);
			}
		}
}
