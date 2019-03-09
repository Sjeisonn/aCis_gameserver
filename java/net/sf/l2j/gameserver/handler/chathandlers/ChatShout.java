package net.sf.l2j.gameserver.handler.chathandlers;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.xml.MapRegionData;
import net.sf.l2j.gameserver.handler.IChatHandler;
import net.sf.l2j.gameserver.model.BlockList;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.network.FloodProtectors;
import net.sf.l2j.gameserver.network.FloodProtectors.Action;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;

public class ChatShout implements IChatHandler
{
	private static final int[] COMMAND_IDS =
	{
		1
	};
	
	@Override
	public void handleChat(int type, Player activeChar, String target, String text)
	{
		if (!FloodProtectors.performAction(activeChar.getClient(), Action.GLOBAL_CHAT))
			return;
		
		if (activeChar.getLevel() >= Config.CHAT_MIN_LEVEL || activeChar.getPvpKills() >= Config.CHAT_MIN_PVP)
		{
			final CreatureSay cs = new CreatureSay(activeChar.getObjectId(), type, activeChar.getName(), text);
			final int region = MapRegionData.getInstance().getMapRegion(activeChar.getX(), activeChar.getY());
			
			for (Player player : World.getInstance().getPlayers())
			{
				if (!BlockList.isBlocked(player, activeChar) && region == MapRegionData.getInstance().getMapRegion(player.getX(), player.getY()))
					player.sendPacket(cs);
			}
		}
		else
			activeChar.sendMessage("You can not use the chat, unless you reach " + Config.CHAT_MIN_LEVEL + " level or " + Config.CHAT_MIN_PVP + " pvp kills.");
	}
	
	@Override
	public int[] getChatTypeList()
	{
		return COMMAND_IDS;
	}
}