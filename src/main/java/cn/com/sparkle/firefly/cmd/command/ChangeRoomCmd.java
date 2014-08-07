package cn.com.sparkle.firefly.cmd.command;

import cn.com.sparkle.firefly.admin.Commands;

public class ChangeRoomCmd extends CommonCmd {
	public final static ChangeRoomCmd DEFAULT = new ChangeRoomCmd();
	@Override
	public String cmdInfo() {
		return Commands.CH_ROOM + "/" + Commands.CH_ROOM1 + " id room: change the room of a senator";
	}

	@Override
	public String helpInfo() {
		return "";
	}

	@Override
	public String[] cmd() {
		return new String[]{Commands.CH_ROOM , Commands.CH_ROOM1};
	}

}
