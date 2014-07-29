import cn.com.sparkle.global.configcenter.client.ConfigClient;


public class TestModify {
	public static void main(String[] args) throws Throwable {
		ConfigClient cc = new ConfigClient(new String[]{"127.0.0.1:11001","127.0.0.1:10001","127.0.0.1:9001"},"target/classes/configcenter/conf9000/service_in_net.prop");
		
		for(int i = 0 ; i < 100 ; i++){
			boolean r = cc.set("1111", "都收到了呀" + i);
			System.out.println("modify result:" + r);
		}
		
		System.exit(0);
	}
}
