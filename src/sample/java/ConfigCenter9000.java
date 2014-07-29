

import cn.com.sparkle.global.configcenter.server.ConfigServer;



public class ConfigCenter9000 {
	public static void main(String[] args) throws Throwable{
		ConfigServer.start("target/classes/configcenter/conf9000");
	}
}
