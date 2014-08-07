package cn.com.sparkle.firefly.admin.processors;

import cn.com.sparkle.firefly.admin.AdminLookupHandler;
import cn.com.sparkle.firefly.model.AddRequest;
import cn.com.sparkle.firefly.net.netlayer.PaxosSession;

public abstract class AbstractAdminProcessor implements AdminProcessor{
	public void process(String[] command,AdminLookupHandler handler,PaxosSession session,AddRequest request){
		int commLen = commandLength();
		if(command.length < commLen){
			sendResponse("the " + commLen + " args is must needed!", handler, session, request);
		}else{
			try{
				String result = processComm(command, handler, session, request);
				sendResponse(result, handler, session, request);
			}catch(Throwable e){
				sendResponse("command has executed unsuccessfully! error info:" + e.getMessage(), handler, session, request);
			}
		}
	}
	public abstract int commandLength();
	public abstract String processComm(String[] command,AdminLookupHandler handler,PaxosSession session,AddRequest request);
	public void sendResponse(String response,AdminLookupHandler handler,PaxosSession session,AddRequest request){
		byte[] rb = response.getBytes();
		handler.sendResponseCommandResponse(session, request, rb);
	}
}
