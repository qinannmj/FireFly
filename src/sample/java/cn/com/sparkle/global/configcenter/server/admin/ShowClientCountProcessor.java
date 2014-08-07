package cn.com.sparkle.global.configcenter.server.admin;

import cn.com.sparkle.firefly.admin.AdminLookupHandler;
import cn.com.sparkle.firefly.admin.processors.AbstractAdminProcessor;
import cn.com.sparkle.firefly.model.AddRequest;
import cn.com.sparkle.firefly.net.netlayer.PaxosSession;
import cn.com.sparkle.global.configcenter.cmd.ShowClientCountCmd;
import cn.com.sparkle.global.configcenter.server.ConfigServerHandler;

public class ShowClientCountProcessor extends AbstractAdminProcessor {
	private ConfigServerHandler handler;
	public ShowClientCountProcessor(ConfigServerHandler handler){
		this.handler = handler;
	}
	@Override
	public String[] getName() {
		return ShowClientCountCmd.DEFAULT.cmd();
	}

	@Override
	public int commandLength() {
		return 2;
	}

	@Override
	public String processComm(String[] command, AdminLookupHandler handler, PaxosSession session, AddRequest request) {
		StringBuffer sb = new StringBuffer();
		sb.append(String.valueOf(this.handler.clientCount)).append("\r\n");
		for(PaxosSession s : this.handler.clientset.keySet()){
			try{
			sb.append(String.format("remote %s local %s", s.getRemoteAddress(),s.getLocalAddress())).append("\r\n");
			}catch(Throwable e){}
		}
		return sb.toString();
	}
	

}
