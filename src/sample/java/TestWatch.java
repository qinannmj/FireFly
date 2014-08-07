import cn.com.sparkle.global.configcenter.client.ConfigClient;


public class TestWatch {
	public static void main(String[] args) throws Throwable {
		ConfigClient cc = new ConfigClient(new String[]{"127.0.0.1:11001","127.0.0.1:10001","127.0.0.1:9001"},"target/classes/configcenter/conf9000/service_in_net.prop");
		cc.get("1111");
	}
	
}
