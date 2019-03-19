package net.sf.l2j.gameserver.handler.admincommandhandlers;

import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.instance.Player;

public class AdminAIO implements IAdminCommandHandler
{	
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_aio",
		"admin_removeaio"
	};
	
	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		StringTokenizer st = new StringTokenizer(command);
		st.nextToken();
		
		if (command.startsWith(ADMIN_COMMANDS[0]))
		{
			if (!st.hasMoreTokens())
			{
				activeChar.sendMessage("Usage of command: //aio <name> <days>");
			}
			else
			{
				String next = st.nextToken();
				
				Player targetPlayer = World.getInstance().getPlayer(next);
				
				if (targetPlayer == null)
				{
					activeChar.sendMessage("Target with name " + next + " is not found online.");
				}
				else 
				{
					try
					{
						long dur = TimeUnit.DAYS.toMillis(Integer.parseInt(st.nextToken()));
						
						if (targetPlayer.isAio())
						{
							activeChar.sendMessage("Target player is already AIO.");
						}
						else
						{
							targetPlayer.setAioTime(dur + System.currentTimeMillis());
							targetPlayer.setAio();
							
							activeChar.sendMessage("You gave AIO to " + targetPlayer.getName() + " for " + TimeUnit.MILLISECONDS.toDays(dur) + " days.");
						}
					}
					catch(Exception e)
					{
						activeChar.sendMessage("Usage of command: //aio <name> <days>");
						return false;
					}
				}
				
			}
		}
		else
		{
			if (!st.hasMoreTokens())
			{
				activeChar.sendMessage("Usage of command: //aio <name> <days>");
			}
			else
			{
				String next = st.nextToken();
				
				Player targetPlayer = World.getInstance().getPlayer(next);
				
				if (targetPlayer == null)
				{
					activeChar.sendMessage("Target with name " + next + " is not found online.");
				}
				else
				{
					targetPlayer.getAppearance().setNameColor(255, 255, 255);
					targetPlayer.getAppearance().setTitleColor(255, 255, 255);
					
					targetPlayer.broadcastUserInfo();
					targetPlayer.broadcastTitleInfo();
					
					for (int i : Config.AIO_SKILLS.keySet())
					{
						targetPlayer.removeSkill(i, false);
					}
					
					targetPlayer.sendSkillList();
					
					activeChar.sendMessage("You removed AIO from " + targetPlayer.getName());
				}
				
			}
		}
		
		return true;
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}