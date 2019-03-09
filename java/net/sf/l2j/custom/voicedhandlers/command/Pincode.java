package net.sf.l2j.custom.voicedhandlers.command;

import net.sf.l2j.custom.pincode.PincodeTable;
import net.sf.l2j.custom.voicedhandlers.IVoicedCommandHandler;
import net.sf.l2j.gameserver.model.actor.instance.Player;

public class Pincode implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
		"changepin"
	};
	
	@Override
	public void useVoicedCommand(final String command, final Player player)
	{
		PincodeTable.sendPinCodeWindow(player.getClient(), 1, player.getObjectId(), -1, "");
	}
	
	@Override
	public String[] getVoicedCommands()
	{
		return VOICED_COMMANDS;
	}
}