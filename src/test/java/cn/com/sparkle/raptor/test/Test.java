package cn.com.sparkle.raptor.test;

public class Test {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		int[] a = {1,2,3,4,5,6,7,8,9};
		int r = 4;
		r = r % a.length;
		
		int t;
		int i = 0;
		int k;
		while(i != a.length && r!=0){
			for(k = 0; i < a.length -r ; i++,k++){
				t = a[i];
				a[i] = a[i+r];
				a[i+r] = t;
			}
			r = (r - k%r)%r;
		}
		
		
		
		for(i = 0 ; i < a.length ;i++){
			System.out.println(a[i]);
		}
	}

}
