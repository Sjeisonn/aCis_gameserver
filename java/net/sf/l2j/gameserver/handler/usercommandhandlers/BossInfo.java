package net.sf.l2j.gameserver.handler.usercommandhandlers;

import java.text.SimpleDateFormat;

import net.sf.l2j.gameserver.data.xml.NpcData;
import net.sf.l2j.gameserver.handler.IUserCommandHandler;
import net.sf.l2j.gameserver.instancemanager.GrandBossManager;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.templates.StatsSet;

public class BossInfo implements IUserCommandHandler
{
	private static final int[] raidbosses = new int[]
		{
			51006 // Zaken
		};
	
	private static final int[] GRAND = new int[]
		{
			29001, // Queen Ant
			29006, // Core
			29014 // Orfen
		};
	
	private static final int[] GRAND2 = new int[]
		{
			29019, // Antharas
			29028, // Valakas
			29047 // Halisha
		};
	
	private static final int BAIUM = 29020;
	
	private static final int[] COMMAND_IDS =
	{
		90
	};
	
	@Override
	public boolean useUserCommand(int id, Player activeChar)
	{
		final StringBuilder sb = new StringBuilder();
		NpcHtmlMessage html = new NpcHtmlMessage(0);
		
		/**
		for (int raidboss : raidbosses)
		{
			String name = NpcData.getInstance().getTemplate(raidboss).getName();
			long delay = RaidBossManager.getInstance().getRespawntime(raidboss);
			sb.append("<html><head><title>Epic Boss Manager</title></head><body>");
			sb.append("<center>");
			sb.append("<img src=\"L2UI.SquareGray\" width=300 height=1><br>");
			
			if (delay <= System.currentTimeMillis())
			{
				sb.append("" + name + ":&nbsp;<font color=\"4d94ff\">Is Alive!</font><br1>");
			}
			else
			{
				sb.append("" + name + ":&nbsp;<br1>");
				sb.append("&nbsp;<font color=\"FFFFFF\">" + " " + "Respawn at:</font>" + "" + "<font color=\"FF9900\"> " + new SimpleDateFormat("dd-MM-yyyy HH:mm").format(delay) + "</font><br>");
			}
		}
		*/
		
		// Case of Queen/Core/Orfen
		for (int grandboss : GRAND)
		{
			StatsSet info = GrandBossManager.getInstance().getStatsSet(grandboss);
			long temp = info.getLong("respawn_time");
			String Grand = NpcData.getInstance().getTemplate(grandboss).getName();
			
			sb.append("<center>");
			sb.append("<img src=\"L2UI.SquareGray\" width=300 height=1><br>");
			if (temp <= System.currentTimeMillis())
			{
				sb.append("" + Grand + ":&nbsp;<font color=\"4d94ff\">Is Alive!</font><br1>");
			}
			else
			{
				sb.append("" + Grand + ":&nbsp;<br1>");
				sb.append("&nbsp;<font color=\"FFFFFF\">" + " " + "Respawn at:</font>" + "" + "<font color=\"FF9900\"> " + new SimpleDateFormat("dd-MM-yyyy HH:mm").format(temp) + "</font><br>");
			}
		}
		
		// Case of Baium
		StatsSet infobaium = GrandBossManager.getInstance().getStatsSet(BAIUM);
		long tempbaium = infobaium.getLong("respawn_time");
		String Baium = NpcData.getInstance().getTemplate(BAIUM).getName();
		int BaiumStatus = GrandBossManager.getInstance().getBossStatus(BAIUM);
		
		sb.append("<center>");
		sb.append("<img src=\"L2UI.SquareGray\" width=300 height=1><br>");
		if (tempbaium <= System.currentTimeMillis() && BaiumStatus == 0)
		{
			sb.append("" + Baium + ":&nbsp;<font color=\"ff4d4d\">Is Asleep!</font><br1>");
		}
		else if (BaiumStatus == 1)
		{
			sb.append("" + Baium + ":&nbsp;<font color=\"ff4d4d\">Is Awake and fighting. Entry is locked.</font><br1>");
		}
		else
		{
			sb.append("" + Baium + ":&nbsp;<br1>");
			sb.append("&nbsp;<font color=\"FFFFFF\">" + " " + "Respawn at:</font>" + "" + "<font color=\"FF9900\"> " + new SimpleDateFormat("dd-MM-yyyy HH:mm").format(tempbaium) + "</font><br>");
		}
		
		// Case of Antharas/Valakas/Halisha
		for (int grandboss : GRAND2)
		{
			StatsSet infogrand = GrandBossManager.getInstance().getStatsSet(grandboss);
			long tempgrand = infogrand.getLong("respawn_time");
			String Grand = NpcData.getInstance().getTemplate(grandboss).getName();
			int BossStatus = GrandBossManager.getInstance().getBossStatus(grandboss);
			
			sb.append("<center>");
			sb.append("<img src=\"L2UI.SquareGray\" width=300 height=1><br>");
			if (tempgrand <= System.currentTimeMillis() && BossStatus == 0)
			{
				sb.append("" + Grand + ":&nbsp;<font color=\"4d94ff\">Is spawned. Entry is unlocked.</font><br1>");
			}
			else if (BossStatus == 1)
			{
				sb.append("" + Grand + ":&nbsp;<font color=\"ff4d4d\">Someone has entered. Hurry!</font><br1>");
			}
			else if (BossStatus == 2)
			{
				sb.append("" + Grand + ":&nbsp;<font color=\"ff4d4d\">Is engaged in battle. Entry is locked.</font><br1>");
			}
			else
			{
				sb.append("" + Grand + ":&nbsp;<br1>");
				sb.append("&nbsp;<font color=\"FFFFFF\">" + " " + "Respawn at:</font>" + "" + "<font color=\"FF9900\"> " + new SimpleDateFormat("dd-MM-yyyy HH:mm").format(tempgrand) + "</font><br>");
			}
		}
		
		html.setHtml(sb.toString());
		html.replace("%bosslist%", sb.toString());
		activeChar.sendPacket(html);
		return true;
	}
	
	@Override
	public int[] getUserCommandList()
	{
		return COMMAND_IDS;
	}
}