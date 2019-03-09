package net.sf.l2j.custom.cafepoint.admincommand;

import java.text.NumberFormat;
import java.util.Collection;
import java.util.Locale;
import java.util.StringTokenizer;

import net.sf.l2j.gameserver.data.cache.HtmCache;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * @author SweeTs
 */
public class AdminPcCafePoints implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_pccafe",
	};
	
	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		final StringTokenizer st = new StringTokenizer(command, " ");
		final String actualCommand = st.nextToken();
		
		if (actualCommand.equals("admin_pccafe"))
		{
			if (st.hasMoreTokens())
			{
				final String action = st.nextToken();
				
				final Player target = getTarget(activeChar);
				if ((target == null) || !st.hasMoreTokens())
					return false;
				
				int value = 0;
				try
				{
					value = Integer.parseInt(st.nextToken());
				}
				catch (Exception e)
				{
					showMenuHtml(activeChar);
					activeChar.sendMessage("Invalid Value!");
					return false;
				}
				
				switch (action)
				{
					case "increase":
					{
						if (target.getPcCafePoints() >= 200_000)
						{
							showMenuHtml(activeChar);
							activeChar.sendMessage(target.getName() + " already have max count of Pc Cafe Points!");
							return false;
						}
						
						activeChar.increasePcCafePoints(value);
						target.sendMessage("Admin increase your Pc Cafe Point(s) by " + value + "!");
						activeChar.sendMessage("You increased Pc Cafe Point(s) of " + target.getName() + " by " + value);
						break;
					}
					case "decrease":
					{
						if (target.getPcCafePoints() == 0)
						{
							showMenuHtml(activeChar);
							activeChar.sendMessage(target.getName() + " already have min count of Pc Cafe Points!");
							return false;
						}
						
						activeChar.decreasePcCafePoints(value);
						target.sendMessage("Admin decreased your Pc Cafe Point(s) by " + value + "!");
						activeChar.sendMessage("You decreased Pc Cafe Point(s) of " + target.getName() + " by " + value);
						break;
					}
					case "rewardOnline":
					{
						int range = 0;
						try
						{
							range = Integer.parseInt(st.nextToken());
						}
						catch (Exception e)
						{
						}
						
						if (range <= 0)
						{
							final int count = increaseForAll(World.getInstance().getPlayers(), value);
							activeChar.sendMessage("You increased Pc Cafe Point(s) of all online players (" + count + ") by " + value + ".");
						}
						else if (range > 0)
						{
							final int count = increaseForAll(activeChar.getKnownTypeInRadius(Player.class, range), value);
							activeChar.sendMessage("You increased Pc Cafe Point(s) of all players (" + count + ") in range " + range + " by " + value + ".");
						}
						break;
					}
				}
				showMenuHtml(activeChar);
			}
			else
				showMenuHtml(activeChar);
		}
		return true;
	}
	
	private static int increaseForAll(Collection<Player> playerList, int value)
	{
		int counter = 0;
		for (Player temp : playerList)
		{
			if ((temp != null) && (temp.isOnlineInt() == 1))
			{
				if (temp.getPcCafePoints() >= 200_000)
					continue;
				
				temp.increasePcCafePoints(value);
				temp.sendMessage("Admin increase your Pc Cafe Point(s) by " + value + "!");
				counter++;
			}
		}
		return counter;
	}
	
	private static Player getTarget(Player activeChar)
	{
		return ((activeChar.getTarget() != null) && (activeChar.getTarget().getActingPlayer() != null)) ? activeChar.getTarget().getActingPlayer() : activeChar;
	}
	
	private static void showMenuHtml(Player activeChar)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		final Player target = getTarget(activeChar);
		final int points = target.getPcCafePoints();
		html.setHtml(HtmCache.getInstance().getHtm("data/html/admin/pccafepoints.htm"));
		html.replace("%points%", NumberFormat.getInstance(Locale.ITALY).format(points));
		html.replace("%targetName%", target.getName());
		activeChar.sendPacket(html);
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}