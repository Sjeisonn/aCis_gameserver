package net.sf.l2j.custom.voicedhandlers;

import net.sf.l2j.gameserver.model.actor.instance.Player;

public interface IVoicedCommandHandler
{
	public void useVoicedCommand(String command, Player activeChar);
	
	public String[] getVoicedCommands();
}