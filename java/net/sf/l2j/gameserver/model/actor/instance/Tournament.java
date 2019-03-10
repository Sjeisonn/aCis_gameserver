package net.sf.l2j.gameserver.model.actor.instance;

import net.sf.l2j.gameserver.event.Match;
import net.sf.l2j.gameserver.event.TournamentManager;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * @author Kara`
 */
public class Tournament extends Folk
{
	public Tournament(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public void onBypassFeedback(Player player, String command)
	{
		if (command.startsWith("match"))
		{
			final Match match = TournamentManager.getInstance().getMatch(Integer.parseInt(command.substring(6)));
			
			if (match == null)
			{
				player.sendMessage("Match is not running.");
				return;
			}
			
			if (match.getPlayers().contains(player))
			{
				match.onRegister(player);
			}
			else
			{
				match.onRegister(player);
			}
		}
	}
	
	@Override
	public void showChatWindow(final Player player, final int val)
	{
		final StringBuilder tb = new StringBuilder();
		
		tb.append("<html><title>Tournament Event</title><body>");
		tb.append("<center><img src=\"L2UI_CH3.herotower_deco\" width=256 height=32></center><br>");
		tb.append("<center><font color=\"LEVEL\">Tournament Event</font></center><br>");
		tb.append("<font color=\"00CCFF\"><b>Event Information</b></font><br1>");
		tb.append("Team vs team <font color=\"00FF00\">full buffs</font> event.<br1>");
		tb.append("The goal is to defeat the enemy team.<br1>");
		tb.append("<font color=\"FF0000\">Create your party and enjoy!</font><br><br>");
		tb.append("<font color=\"FF0000\">Event disabled, we're finishing the system!</font><br><br>");
		
		for (final Match match : TournamentManager.getInstance().getMatches())
		{
			tb.append("<center><button action=\"bypass -h npc_" + getObjectId() + "_match "+match.getId()+" \" value=\""+match.getName()+"\" width=75 height=21 back=\"L2UI_ch3.Btn1_normalOn\" fore=\"L2UI_ch3.Btn1_normal\">");
		}
		
		tb.append("<html><body>");
		tb.append("<center><img src=\"L2UI_CH3.herotower_deco\" width=256 height=32></center><br>");
		tb.append("<center><font color=\"LEVEL\">Tournament Event</font></center><br>");
		tb.append("<br><br><br>");
		tb.append("</body></html>");
		
		final NpcHtmlMessage msg = new NpcHtmlMessage(getObjectId());
		msg.setHtml(tb.toString());
		msg.replace("%name%", player.getName());
		player.sendPacket(msg);
	}
}