package net.sf.l2j.custom.premium;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import net.sf.l2j.commons.logging.CLogger;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.network.clientpackets.Say2;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;

/**
 * @author SweeTs
 */
public class PremiumManager
{
	private static final CLogger LOGGER = new CLogger(PremiumManager.class.getName());
	
	private static final String UPDATE = "UPDATE accounts SET premium=?,premium_level=? WHERE login=?";
	private static final String SELECT = "SELECT premium,premium_level FROM accounts WHERE login=?";
	
	public static final void setPremium(final Player player, final long days, final PremiumType premiumLevel)
	{
		if (days < 0)
			return;
		
		final boolean canExtend = player.getPremiumEndTime() > 0 && player.getPremiumType() == premiumLevel;
		final long premiumEndTime = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(days);
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement(UPDATE))
		{
			ps.setLong(1, canExtend ? (player.getPremiumEndTime() + TimeUnit.DAYS.toMillis(days)) : premiumEndTime);
			ps.setInt(2, premiumLevel.getId());
			ps.setString(3, player.getAccountName());
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.warn("Could not update premium status for account: {}", player.getAccountName(), e);
		}
		
		if (canExtend)
			player.sendPacket(new CreatureSay(0, Say2.HERO_VOICE, "System", "Your premium period has been extended by " + days + " days."));
		else
			player.sendPacket(new CreatureSay(0, Say2.HERO_VOICE, "System", "Dear player, your account has been upgraded, congratulations."));
		
		player.setPremiumLevel(premiumLevel.getId());
		player.setPremiumEndTime(premiumEndTime);
		
		PremiumTaskManager.getInstance().add(player, premiumEndTime);
		PremiumTaskManager.getInstance().addLevel(player, premiumLevel.getId());
	}
	
	public static final void removePremium(final Player player)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement(UPDATE))
		{
			ps.setLong(1, 0);
			ps.setInt(1, 0);
			ps.setString(2, player.getAccountName());
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.warn("Could not update premium status for account: {}", player.getAccountName(), e);
		}
		
		player.setPremiumLevel(0);
		player.sendPacket(new CreatureSay(0, Say2.HERO_VOICE, "System", "Your premium period have been expired."));
	}
	
	public static final void onPlayerLogin(final Player activeChar)
	{
		final SimpleDateFormat df = new SimpleDateFormat("dd MMMM, HH:mm zz");
		if (PremiumTaskManager.getInstance().getPremiumCharacters().containsKey(activeChar))
		{
			final int premiumLevel = PremiumTaskManager.getInstance().getCharactersLevel().get(activeChar);
			final long endTime = PremiumTaskManager.getInstance().getPremiumCharacters().get(activeChar);
			final Date date = new Date(endTime);
			
			activeChar.sendPacket(new CreatureSay(0, Say2.HERO_VOICE, "System", "Your premium period ends: " + df.format(date) + "."));
			activeChar.setPremiumLevel(premiumLevel);
			activeChar.setPremiumEndTime(endTime);
		}
		else
		{
			try (Connection con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement ps = con.prepareStatement(SELECT))
			{
				ps.setString(1, activeChar.getAccountName());
				
				try (ResultSet rset = ps.executeQuery())
				{
					if (rset.next())
					{
						final long endTime = rset.getLong("premium");
						final int premiumLevel = rset.getInt("premium_level");
						
						if (endTime > 0)
						{
							if (endTime > System.currentTimeMillis())
							{
								activeChar.setPremiumLevel(premiumLevel);
								activeChar.setPremiumEndTime(endTime);
								
								final Date date = new Date(endTime);
								activeChar.sendPacket(new CreatureSay(0, Say2.HERO_VOICE, "System", "Your premium period ends: " + df.format(date) + "."));
								
								PremiumTaskManager.getInstance().add(activeChar, endTime);
								PremiumTaskManager.getInstance().addLevel(activeChar, premiumLevel);
							}
							else
								removePremium(activeChar);
						}
					}
				}
			}
			catch (Exception e)
			{
				LOGGER.warn("Could not check premium status for account: " + activeChar.getAccountName() + e.getMessage(), e);
			}
		}
	}
}