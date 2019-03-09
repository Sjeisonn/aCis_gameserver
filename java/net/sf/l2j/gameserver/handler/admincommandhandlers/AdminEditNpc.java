package net.sf.l2j.gameserver.handler.admincommandhandlers;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;

import net.sf.l2j.commons.lang.StringUtil;

import net.sf.l2j.Config;
import net.sf.l2j.custom.IconsTable;
import net.sf.l2j.gameserver.data.ItemTable;
import net.sf.l2j.gameserver.data.manager.BuyListManager;
import net.sf.l2j.gameserver.data.xml.NpcData;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.instance.Merchant;
import net.sf.l2j.gameserver.model.actor.instance.Monster;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate.SkillType;
import net.sf.l2j.gameserver.model.buylist.NpcBuyList;
import net.sf.l2j.gameserver.model.buylist.Product;
import net.sf.l2j.gameserver.model.item.DropCategory;
import net.sf.l2j.gameserver.model.item.DropData;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.scripting.EventType;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.templates.skills.L2SkillType;

public class AdminEditNpc implements IAdminCommandHandler
{
	private static final int PAGE_LIMIT = 20;
	
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_show_droplist",
		"admin_show_minion",
		"admin_show_scripts",
		"admin_show_shop",
		"admin_show_shoplist",
		"admin_show_skilllist"
	};
	
	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		final StringTokenizer st = new StringTokenizer(command, " ");
		st.nextToken();
		
		if (command.startsWith("admin_show_minion"))
		{
			// You need to target a Monster.
			final WorldObject target = activeChar.getTarget();
			if (!(target instanceof Monster))
			{
				activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
				return false;
			}
			
			final Monster monster = (Monster) target;
			
			// Load static Htm.
			final NpcHtmlMessage html = new NpcHtmlMessage(0);
			html.setFile("data/html/admin/minion.htm");
			html.replace("%target%", target.getName());
			
			final StringBuilder sb = new StringBuilder();
			
			// Monster is a minion, deliver boss state.
			final Monster master = monster.getMaster();
			if (master != null)
			{
				html.replace("%type%", "minion");
				StringUtil.append(sb, "<tr><td>", master.getNpcId(), "</td><td>", master.getName(), " (", ((master.isDead()) ? "Dead" : "Alive"), ")</td></tr>");
			}
			// Monster is a master, find back minions informations.
			else if (monster.hasMinions())
			{
				html.replace("%type%", "master");
				
				for (Entry<Monster, Boolean> data : monster.getMinionList().getMinions().entrySet())
					StringUtil.append(sb, "<tr><td>", data.getKey().getNpcId(), "</td><td>", data.getKey().toString(), " (", ((data.getValue()) ? "Alive" : "Dead"), ")</td></tr>");
			}
			// Monster isn't anything.
			else
				html.replace("%type%", "regular monster");
			
			html.replace("%minion%", sb.toString());
			activeChar.sendPacket(html);
		}
		else if (command.startsWith("admin_show_shoplist"))
		{
			try
			{
				showShopList(activeChar, Integer.parseInt(st.nextToken()));
			}
			catch (Exception e)
			{
				activeChar.sendMessage("Usage: //show_shoplist <list_id>");
			}
		}
		else if (command.startsWith("admin_show_shop"))
		{
			try
			{
				showShop(activeChar, Integer.parseInt(st.nextToken()));
			}
			catch (Exception e)
			{
				activeChar.sendMessage("Usage: //show_shop <npc_id>");
			}
		}
		else if (command.startsWith("admin_show_droplist"))
		{
			try
			{
				int npcId = Integer.parseInt(st.nextToken());
				int page = (st.hasMoreTokens()) ? Integer.parseInt(st.nextToken()) : 1;
				
				showNpcDropList(activeChar, npcId, page);
			}
			catch (Exception e)
			{
				activeChar.sendMessage("Usage: //show_droplist <npc_id> [<page>]");
			}
		}
		else if (command.startsWith("admin_show_skilllist"))
		{
			try
			{
				showNpcSkillList(activeChar, Integer.parseInt(st.nextToken()));
			}
			catch (Exception e)
			{
				activeChar.sendMessage("Usage: //show_skilllist <npc_id>");
			}
		}
		else if (command.startsWith("admin_show_scripts"))
		{
			try
			{
				showScriptsList(activeChar, Integer.parseInt(st.nextToken()));
			}
			catch (Exception e)
			{
				activeChar.sendMessage("Usage: //show_scripts <npc_id>");
			}
		}
		
		return true;
	}
	
	private static void showShopList(Player activeChar, int listId)
	{
		final NpcBuyList buyList = BuyListManager.getInstance().getBuyList(listId);
		if (buyList == null)
		{
			activeChar.sendMessage("BuyList template is unknown for id: " + listId + ".");
			return;
		}
		
		final StringBuilder sb = new StringBuilder(500);
		StringUtil.append(sb, "<html><body><center><font color=\"LEVEL\">", NpcData.getInstance().getTemplate(buyList.getNpcId()).getName(), " (", buyList.getNpcId(), ") buylist id: ", buyList.getListId(), "</font></center><br><table width=\"100%\"><tr><td width=200>Item</td><td width=80>Price</td></tr>");
		
		for (Product product : buyList.getProducts())
			StringUtil.append(sb, "<tr><td>", product.getItem().getName(), "</td><td>", product.getPrice(), "</td></tr>");
		
		sb.append("</table></body></html>");
		
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setHtml(sb.toString());
		activeChar.sendPacket(html);
	}
	
	private static void showShop(Player activeChar, int npcId)
	{
		final List<NpcBuyList> buyLists = BuyListManager.getInstance().getBuyListsByNpcId(npcId);
		if (buyLists.isEmpty())
		{
			activeChar.sendMessage("No buyLists found for id: " + npcId + ".");
			return;
		}
		
		final StringBuilder sb = new StringBuilder(500);
		StringUtil.append(sb, "<html><title>Merchant Shop Lists</title><body>");
		
		if (activeChar.getTarget() instanceof Merchant)
		{
			Npc merchant = (Npc) activeChar.getTarget();
			int taxRate = merchant.getCastle().getTaxPercent();
			
			StringUtil.append(sb, "<center><font color=\"LEVEL\">", merchant.getName(), " (", npcId, ")</font></center><br>Tax rate: ", taxRate, "%");
		}
		
		StringUtil.append(sb, "<table width=\"100%\">");
		
		for (NpcBuyList buyList : buyLists)
			StringUtil.append(sb, "<tr><td><a action=\"bypass -h admin_show_shoplist ", buyList.getListId(), " 1\">Buylist id: ", buyList.getListId(), "</a></td></tr>");
		
		StringUtil.append(sb, "</table></body></html>");
		
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setHtml(sb.toString());
		activeChar.sendPacket(html);
	}
	
	private static void showNpcDropList(Player activeChar, int npcId, int page)
	{
		final NpcTemplate npcData = NpcData.getInstance().getTemplate(npcId);
		if (npcData == null)
		{
			activeChar.sendMessage("Npc template is unknown for id: " + npcId + ".");
			return;
		}
		
		final StringBuilder sb = new StringBuilder(2000);
		StringUtil.append(sb, "<html><title>Droplist</title><body><center><br>", npcData.getName(), " (", npcId, ")</center><br>");
		
		if (!npcData.getDropData().isEmpty())
		{
			int myPage = 1;
			int i = 0;
			int shown = 0;
			boolean hasMore = false;
			
			sb.append("<img src=\"L2UI.SquareGray\" width=\"277\" height=\"1\">");
			
			for (DropCategory cat : npcData.getDropData())
			{
				for (DropData drop : cat.getAllDrops())
				{
					sb.append("<table width=\"280\">");
					
					if (myPage != page)
					{
						i++;
						if (i == PAGE_LIMIT)
						{
							myPage++;
							i = 0;
						}
						continue;
					}
					
					if (shown == PAGE_LIMIT)
					{
						hasMore = true;
						break;
					}
					
					final Item item = ItemTable.getInstance().getTemplate(drop.getItemId());
					String icon = IconsTable.getIcon(item.getItemId());
					if (icon == null)
						icon = "icon.etc_question_mark_i00";
					
					final NumberFormat nf = NumberFormat.getIntegerInstance(Locale.ITALY);
					
					final double chance = drop.getChance() / 10000.;
					final double roundChance = Math.round(chance * 100.) / 100.;
					
					StringUtil.append(sb, "<tr><td height=\"40\" width=\"40\"><img src=\"" + icon + "\" width=\"32\" height=\"32\"></td><td width=\"240\"><font color=\"B09878\">", item.getName(), "</font>", cat.isSweep() ? " <font color=\"0099ff\">[Spoil]</font>" : "", "<br1><font color=\"A2A0A2\">[", item.getItemId() == 57 ? nf.format(drop.getMinDrop() * Config.RATE_DROP_ADENA) : nf.format(drop.getMinDrop() * Config.RATE_DROP_ITEMS), " - ", item.getItemId() == 57 ? nf.format(drop.getMaxDrop() * Config.RATE_DROP_ADENA) : nf.format(drop.getMaxDrop() * Config.RATE_DROP_ITEMS), "] ", roundChance, "%</font></td></tr></table><img src=\"L2UI.SquareGray\" width=\"277\" height=\"1\">");
					shown++;
				}
			}
			
			sb.append("<table width=\"100%\"><tr>");
			
			if (page > 1)
			{
				StringUtil.append(sb, "<td width=\"120\"><button action=\"bypass -h admin_show_droplist ", npcId, " ", page - 1, "\" width=\"50\" height=\"25\" back=\"swamp.btn.left_down\" fore=\"swamp.btn.left\"></td>");
				if (!hasMore)
					StringUtil.append(sb, "<td width=\"100\">Page ", page, "</td><td width=\"70\"></td></tr>");
			}
			
			if (hasMore)
			{
				if (page <= 1)
					sb.append("<td width=\"120\"></td>");
				
				StringUtil.append(sb, "<td width=\"100\">Page ", page, "</td><td width=\"70\"><button action=\"bypass -h admin_show_droplist ", npcId, " ", page + 1, "\" width=\"50\" height=\"25\" back=\"swamp.btn.right_down\" fore=\"swamp.btn.right\"></td></tr>");
			}
			sb.append("</table>");
		}
		else
			sb.append("This NPC has no drops.");
		
		sb.append("</body></html>");
		
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setHtml(sb.toString());
		activeChar.sendPacket(html);
	}
	
	private static void showNpcSkillList(Player activeChar, int npcId)
	{
		final NpcTemplate npcData = NpcData.getInstance().getTemplate(npcId);
		if (npcData == null)
		{
			activeChar.sendMessage("Npc template is unknown for id: " + npcId + ".");
			return;
		}
		
		final StringBuilder sb = new StringBuilder(500);
		StringUtil.append(sb, "<html><body><center><font color=\"LEVEL\">", npcData.getName(), " (", npcId, ") skills</font></center><br>");
		
		if (!npcData.getSkills().isEmpty())
		{
			SkillType type = null; // Used to see if we moved of type.
			
			// For any type of SkillType
			for (Map.Entry<SkillType, List<L2Skill>> entry : npcData.getSkills().entrySet())
			{
				if (type != entry.getKey())
				{
					type = entry.getKey();
					StringUtil.append(sb, "<br><font color=\"LEVEL\">", type.name(), "</font><br1>");
				}
				
				for (L2Skill skill : entry.getValue())
					StringUtil.append(sb, ((skill.getSkillType() == L2SkillType.NOTDONE) ? ("<font color=\"777777\">" + skill.getName() + "</font>") : skill.getName()), " [", skill.getId(), "-", skill.getLevel(), "]<br1>");
			}
		}
		else
			sb.append("This NPC doesn't hold any skill.");
		
		sb.append("</body></html>");
		
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setHtml(sb.toString());
		activeChar.sendPacket(html);
	}
	
	private static void showScriptsList(Player activeChar, int npcId)
	{
		final NpcTemplate npcData = NpcData.getInstance().getTemplate(npcId);
		if (npcData == null)
		{
			activeChar.sendMessage("Npc template is unknown for id: " + npcId + ".");
			return;
		}
		
		final StringBuilder sb = new StringBuilder(500);
		StringUtil.append(sb, "<html><body><center><font color=\"LEVEL\">", npcData.getName(), " (", npcId, ")</font></center><br>");
		
		if (!npcData.getEventQuests().isEmpty())
		{
			EventType type = null; // Used to see if we moved of type.
			
			// For any type of EventType
			for (Map.Entry<EventType, List<Quest>> entry : npcData.getEventQuests().entrySet())
			{
				if (type != entry.getKey())
				{
					type = entry.getKey();
					StringUtil.append(sb, "<br><font color=\"LEVEL\">", type.name(), "</font><br1>");
				}
				
				for (Quest quest : entry.getValue())
					StringUtil.append(sb, quest.getName(), "<br1>");
			}
		}
		else
			sb.append("This NPC isn't affected by scripts.");
		
		sb.append("</body></html>");
		
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setHtml(sb.toString());
		activeChar.sendPacket(html);
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}