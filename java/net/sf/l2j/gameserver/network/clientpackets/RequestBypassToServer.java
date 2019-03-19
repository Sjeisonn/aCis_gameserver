package net.sf.l2j.gameserver.network.clientpackets;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import net.sf.l2j.commons.lang.StringUtil;

import net.sf.l2j.Config;
import net.sf.l2j.custom.IconsTable;
import net.sf.l2j.custom.pincode.PincodeTable;
import net.sf.l2j.custom.voicedhandlers.IVoicedCommandHandler;
import net.sf.l2j.custom.voicedhandlers.VoicedCommandHandler;
import net.sf.l2j.gameserver.communitybbs.CommunityBoard;
import net.sf.l2j.gameserver.data.BotManager;
import net.sf.l2j.gameserver.data.ItemTable;
import net.sf.l2j.gameserver.data.xml.AdminData;
import net.sf.l2j.gameserver.data.xml.NpcData;
import net.sf.l2j.gameserver.handler.AdminCommandHandler;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.instance.OlympiadManagerNpc;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.entity.Hero;
import net.sf.l2j.gameserver.model.item.DropCategory;
import net.sf.l2j.gameserver.model.item.DropData;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.model.olympiad.OlympiadManager;
import net.sf.l2j.gameserver.network.FloodProtectors;
import net.sf.l2j.gameserver.network.FloodProtectors.Action;
import net.sf.l2j.gameserver.network.L2GameClient.GameClientState;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.CharSelected;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.SSQInfo;
import net.sf.l2j.gameserver.scripting.QuestState;

public final class RequestBypassToServer extends L2GameClientPacket
{
	private static final Logger GMAUDIT_LOG = Logger.getLogger("gmaudit");
	
	private String _command;
	
	@Override
	protected void readImpl()
	{
		_command = readS();
	}
	
	@Override
	protected void runImpl()
	{
		if (_command.isEmpty())
			return;
		
		if (!FloodProtectors.performAction(getClient(), Action.SERVER_BYPASS))
			return;
		
		if (_command.startsWith("writepin"))
		{
			final StringTokenizer st = new StringTokenizer(_command, " ");
			st.nextToken();
			
			try
			{
				final int isNew = Integer.parseInt(st.nextToken());
				final int objId = Integer.parseInt(st.nextToken());
				final int charSlot = Integer.parseInt(st.nextToken());
				final String pincode = st.hasMoreTokens() ? st.nextToken() : "";
				try
				{
					Integer.parseInt(pincode);
					
					if (isNew == 1)
					{
						if (pincode.length() < 6)
							PincodeTable.sendPinCodeWindow(getClient(), isNew, objId, charSlot, "Pincode too short, minimum length: 6.");
						else if (pincode.length() > 8)
							PincodeTable.sendPinCodeWindow(getClient(), isNew, objId, charSlot, "Pincode too long, maximum length: 8.");
						else
							PincodeTable.getInstance().updatePincode(objId, pincode, charSlot, getClient());
					}
					else
					{
						if (pincode.equals(PincodeTable.getInstance().getPincode(objId)))
						{
							final Player cha = getClient().loadCharFromDisk(charSlot);
							if (cha == null)
								return;
							
							cha.setClient(getClient());
							getClient().setActiveChar(cha);
							cha.setOnlineStatus(true, true);
							
							sendPacket(SSQInfo.sendSky());
							
							getClient().setState(GameClientState.IN_GAME);
							
							sendPacket(new CharSelected(cha, getClient().getSessionId().playOkID1));
						}
						else
							PincodeTable.sendPinCodeWindow(getClient(), isNew, objId, charSlot, "Pincode incorrect, please try again.");
					}
				}
				catch (final NumberFormatException e)
				{
					PincodeTable.sendPinCodeWindow(getClient(), isNew, objId, charSlot, "Pincode must be numbers only.");
				}
			}
			catch (final NumberFormatException e)
			{
				LOGGER.warn("Failed writing pincode: {} ", e);
			}
			
			return;
		}
		
		final Player player = getClient().getActiveChar();
		if (player == null)
			return;
		
		if (_command.startsWith("admin_"))
		{
			String command = _command.split(" ")[0];
			
			final IAdminCommandHandler ach = AdminCommandHandler.getInstance().getHandler(command);
			if (ach == null)
			{
				if (player.isGM())
					player.sendMessage("The command " + command.substring(6) + " doesn't exist.");
				
				LOGGER.warn("No handler registered for admin command '{}'.", command);
				return;
			}
			
			if (!AdminData.getInstance().hasAccess(command, player.getAccessLevel()))
			{
				player.sendMessage("You don't have the access rights to use this command.");
				LOGGER.warn("{} tried to use admin command '{}' without proper Access Level.", player.getName(), command);
				return;
			}
			
			if (Config.GMAUDIT)
				GMAUDIT_LOG.info(player.getName() + " [" + player.getObjectId() + "] used '" + _command + "' command on: " + ((player.getTarget() != null) ? player.getTarget().getName() : "none"));
			
			ach.useAdminCommand(_command, player);
		}
		else if (_command.startsWith("report"))
		{
			BotManager.getInstance().AnalyseBypass(_command, player);
		}
		else if (_command.startsWith("player_help "))
		{
			final String path = _command.substring(12);
			if (path.indexOf("..") != -1)
				return;
			
			final StringTokenizer st = new StringTokenizer(path);
			final String[] cmd = st.nextToken().split("#");
			
			final NpcHtmlMessage html = new NpcHtmlMessage(0);
			html.setFile("data/html/help/" + cmd[0]);
			if (cmd.length > 1)
			{
				final int itemId = Integer.parseInt(cmd[1]);
				html.setItemId(itemId);
				
				if (itemId == 7064 && cmd[0].equalsIgnoreCase("lidias_diary/7064-16.htm"))
				{
					final QuestState qs = player.getQuestState("Q023_LidiasHeart");
					if (qs != null && qs.getInt("cond") == 5 && qs.getInt("diary") == 0)
						qs.set("diary", "1");
				}
			}
			html.disableValidation();
			player.sendPacket(html);
		}
		else if (_command.startsWith("npc_"))
		{
			if (!player.validateBypass(_command))
				return;
			
			int endOfId = _command.indexOf('_', 5);
			String id;
			if (endOfId > 0)
				id = _command.substring(4, endOfId);
			else
				id = _command.substring(4);
			
			try
			{
				final WorldObject object = World.getInstance().getObject(Integer.parseInt(id));
				
				if (object != null && object instanceof Npc && endOfId > 0 && ((Npc) object).canInteract(player))
					((Npc) object).onBypassFeedback(player, _command.substring(endOfId + 1));
				
				player.sendPacket(ActionFailed.STATIC_PACKET);
			}
			catch (NumberFormatException nfe)
			{
			}
		}
		// Navigate throught Manor windows
		else if (_command.startsWith("manor_menu_select?"))
		{
			WorldObject object = player.getTarget();
			if (object instanceof Npc)
				((Npc) object).onBypassFeedback(player, _command);
		}
		else if (_command.startsWith("bbs_") || _command.startsWith("_bbs") || _command.startsWith("_friend") || _command.startsWith("_mail") || _command.startsWith("_block"))
		{
			CommunityBoard.getInstance().handleCommands(getClient(), _command);
		}
		else if (_command.startsWith("Quest "))
		{
			if (!player.validateBypass(_command))
				return;
			
			String[] str = _command.substring(6).trim().split(" ", 2);
			if (str.length == 1)
				player.processQuestEvent(str[0], "");
			else
				player.processQuestEvent(str[0], str[1]);
		}
		else if (_command.startsWith("_match"))
		{
			String params = _command.substring(_command.indexOf("?") + 1);
			StringTokenizer st = new StringTokenizer(params, "&");
			int heroclass = Integer.parseInt(st.nextToken().split("=")[1]);
			int heropage = Integer.parseInt(st.nextToken().split("=")[1]);
			int heroid = Hero.getInstance().getHeroByClass(heroclass);
			if (heroid > 0)
				Hero.getInstance().showHeroFights(player, heroclass, heroid, heropage);
		}
		else if (_command.startsWith("_diary"))
		{
			String params = _command.substring(_command.indexOf("?") + 1);
			StringTokenizer st = new StringTokenizer(params, "&");
			int heroclass = Integer.parseInt(st.nextToken().split("=")[1]);
			int heropage = Integer.parseInt(st.nextToken().split("=")[1]);
			int heroid = Hero.getInstance().getHeroByClass(heroclass);
			if (heroid > 0)
				Hero.getInstance().showHeroDiary(player, heroclass, heroid, heropage);
		}
		else if (_command.startsWith("arenachange")) // change
		{
			final boolean isManager = player.getCurrentFolk() instanceof OlympiadManagerNpc;
			if (!isManager)
			{
				// Without npc, command can be used only in observer mode on arena
				if (!player.isInObserverMode() || player.isInOlympiadMode() || player.getOlympiadGameId() < 0)
					return;
			}
			
			if (OlympiadManager.getInstance().isRegisteredInComp(player))
			{
				player.sendPacket(SystemMessageId.WHILE_YOU_ARE_ON_THE_WAITING_LIST_YOU_ARE_NOT_ALLOWED_TO_WATCH_THE_GAME);
				return;
			}
			
			final int arenaId = Integer.parseInt(_command.substring(12).trim());
			player.enterOlympiadObserverMode(arenaId);
		}
		else if (_command.startsWith("voice_"))
		{
			String command = _command.split(" ")[0];
			
			final String substring = command.substring(6).toLowerCase();
			final IVoicedCommandHandler voicedCommand = VoicedCommandHandler.getInstance().getVoicedCommand(substring);
			if (voicedCommand != null)
				voicedCommand.useVoicedCommand(substring, player);
		}
		else if (_command.startsWith("dropinfo"))
		{
			final StringTokenizer st = new StringTokenizer(_command, " ");
			st.nextToken();
			
			try
			{
				int npcId = Integer.parseInt(st.nextToken());
				int page = (st.hasMoreTokens()) ? Integer.parseInt(st.nextToken()) : 1;
				
				showNpcDropList(player, npcId, page);
			}
			catch (Exception e)
			{
			}
		}
	}
	
	private static void showNpcDropList(final Player activeChar, final int npcId, final int page)
	{
		final int PAGE_LIMIT = 20;
		
		final NpcTemplate npcData = NpcData.getInstance().getTemplate(npcId);
		if (npcData == null)
		{
			activeChar.sendMessage("Npc template is unknown for id: " + npcId + ".");
			return;
		}
		
		final StringBuilder sb = new StringBuilder(2000);
		StringUtil.append(sb, "<html><title>Droplist</title><body><center><br>", npcData.getName(), "</center><br>");
		
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
					String icon = IconsTable.getInstance().getIcon(item.getItemId());
					if (icon == null)
						icon = "icon.etc_question_mark_i00";
					
					final NumberFormat nf = NumberFormat.getIntegerInstance(Locale.ITALY);
					
					final double chance = drop.getChance() / 10000.0;
					final double roundChance = Math.round(chance * 100.0) / 100.0;
					
					StringUtil.append(sb, "<tr><td height=\"40\" width=\"40\"><img src=\"" + icon + "\" width=\"32\" height=\"32\"></td><td width=\"240\"><font color=\"B09878\">", item.getName(), "</font>", cat.isSweep() ? " <font color=\"0099ff\">[Spoil]</font>" : "", "<br1><font color=\"A2A0A2\">[", item.getItemId() == 57 ? nf.format(drop.getMinDrop() * Config.RATE_DROP_ADENA) : nf.format(drop.getMinDrop() * Config.RATE_DROP_ITEMS), " - ", item.getItemId() == 57 ? nf.format(drop.getMaxDrop() * Config.RATE_DROP_ADENA) : nf.format(drop.getMaxDrop() * Config.RATE_DROP_ITEMS), "] ", roundChance, "%</font></td></tr></table><img src=L2UI.SquareGray width=\"277\" height=\"1\">");
					shown++;
				}
			}
			
			sb.append("<table width=\"100%\"><tr>");
			
			if (page > 1)
			{
				StringUtil.append(sb, "<td width=\"120\"><button action=\"bypass dropinfo ", npcId, " ", page - 1, "\" width=\"50\" height=\"25\" back=\"swamp.btn.left_down\" fore=\"swamp.btn.left\"></td>");
				if (!hasMore)
					StringUtil.append(sb, "<td width=\"100\">Page ", page, "</td><td width=\"70\"></td></tr>");
			}
			
			if (hasMore)
			{
				if (page <= 1)
					sb.append("<td width=\"120\"></td>");
				
				StringUtil.append(sb, "<td width=\"100\">Page ", page, "</td><td width=\"70\"><button action=\"bypass dropinfo ", npcId, " ", page + 1, "\" width=\"50\" height=\"25\" back=\"swamp.btn.right_down\" fore=\"swamp.btn.right\"></td></tr>");
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
}