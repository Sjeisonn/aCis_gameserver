package net.sf.l2j.gameserver.handler.chathandlers;

import net.sf.l2j.custom.voicedhandlers.IVoicedCommandHandler;
import net.sf.l2j.custom.voicedhandlers.VoicedCommandHandler;
import net.sf.l2j.gameserver.handler.IChatHandler;
import net.sf.l2j.gameserver.model.BlockList;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.network.FloodProtectors;
import net.sf.l2j.gameserver.network.FloodProtectors.Action;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;

public class ChatAll implements IChatHandler
{
	private static final int[] COMMAND_IDS =
	{
		0
	};
	
	@Override
	public void handleChat(int type, Player activeChar, String params, String text)
	{
		if (!FloodProtectors.performAction(activeChar.getClient(), Action.GLOBAL_CHAT))
			return;
		
		if (text.startsWith("."))
		{
			final String substring = text.substring(1).toLowerCase();
			final IVoicedCommandHandler voicedCommand = VoicedCommandHandler.getInstance().getVoicedCommand(substring);
			if (voicedCommand != null)
			{
				voicedCommand.useVoicedCommand(substring, activeChar);
				return;
			}
		}
		
		final CreatureSay cs = new CreatureSay(activeChar.getObjectId(), type, activeChar.getName(), text);
		for (Player player : activeChar.getKnownTypeInRadius(Player.class, 1250))
		{
			if (!BlockList.isBlocked(player, activeChar))
				player.sendPacket(cs);
		}
		activeChar.sendPacket(cs);
	}
	
	@Override
	public int[] getChatTypeList()
	{
		return COMMAND_IDS;
	}
}