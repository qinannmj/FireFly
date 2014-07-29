package cn.com.sparkle.firefly.admin.processors;

import cn.com.sparkle.firefly.Context;
import cn.com.sparkle.firefly.admin.AbstractAdminProcessor;
import cn.com.sparkle.firefly.admin.AdminLookupHandler;
import cn.com.sparkle.firefly.model.AddRequest;
import cn.com.sparkle.firefly.net.netlayer.PaxosSession;

public class ChangeRoomProcessor extends AbstractAdminProcessor {
	private Context context;

	public ChangeRoomProcessor(Context context) {
		this.context = context;
	}

	@Override
	public int commandLength() {
		return 2;
	}

	@Override
	public String processComm(String[] command, AdminLookupHandler handler, PaxosSession session, AddRequest request) {
		context.getConfiguration().changeRoom(command[2]);
		return "changed!";
	}
}
