package net.sf.l2j.custom.voicedhandlers.command;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Set;
import java.util.stream.Collectors;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.custom.voicedhandlers.IVoicedCommandHandler;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

public class Menu implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
		"menu",
		"exp",
		"silence",
		"trade",
		"party",
		"repair"
	};
	
	@Override
	public void useVoicedCommand(final String command, final Player player)
	{
		if (command.equals("menu"))
		{
			sendMenu(player);
		}
		else
		{
			if (command.equals("exp"))
				player.setAcquireExp(!player.canAcquireExp());
			else if (command.equals("silence"))
				player.setInRefusalMode(!player.isInRefusalMode());
			else if (command.equals("trade"))
				player.setTradeRefusal(!player.getTradeRefusal());
			else if (command.equals("party"))
				player.setPartyRefusal(!player.getPartyRefusal());
			else if (command.equals("repair"))
				handleRepair(player);
			
			sendMenu(player);
		}
		
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	private static final void sendMenu(Player player)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile("data/html/mods/menu/menu.htm");
		html.replace("%online%", World.getInstance().getPlayers().size());
		html.replace("%exp%", player.canAcquireExp() ? "<font color=ff0000>FALSE</font>" : "<font color=33cc33>ON</font>");
		html.replace("%silence%", player.isInRefusalMode() ? "<font color=33cc33>ON</font>" : "<font color=ff0000>FALSE</font>");
		html.replace("%trade%", player.getTradeRefusal() ? "<font color=33cc33>ON</font>" : "<font color=ff0000>FALSE</font>");
		html.replace("%party%", player.getPartyRefusal() ? "<font color=33cc33>ON</font>" : "<font color=ff0000>FALSE</font>");
		player.sendPacket(html);
	}
	
	private static final void handleRepair(Player player)
	{
		final Location TELE_LOC = new Location(-84318, 244579, -3730);
		final Set<Integer> chars = player.getAccountChars().keySet();
		
		if (chars.isEmpty())
			player.sendMessage("You have no other characters on this account.");
		else
		{
			final String charIds = chars.stream().map(String::valueOf).collect(Collectors.joining(", "));
			try (Connection con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement ps = con.prepareStatement("UPDATE characters SET x=?, y=?, z=? WHERE obj_Id IN (" + charIds + ")"))
			{
				ps.setInt(1, TELE_LOC.getX());
				ps.setInt(2, TELE_LOC.getY());
				ps.setInt(3, TELE_LOC.getZ());
				ps.execute();
				
				player.sendMessage("Other characters fixed.");
			}
			catch (SQLException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public String[] getVoicedCommands()
	{
		return VOICED_COMMANDS;
	}
}