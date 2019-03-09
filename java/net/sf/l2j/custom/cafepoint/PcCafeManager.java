package net.sf.l2j.custom.cafepoint;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.concurrent.TimeUnit;

import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.commons.logging.CLogger;
import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.network.serverpackets.ExPCCafePointInfo;

/**
 * @author SweeTs
 */
public class PcCafeManager
{
	public static final CLogger LOGGER = new CLogger(PcCafeManager.class.getName());
	
	private static final String CLEAR_MEMO = "DELETE FROM character_memo WHERE var='cafe_points_today'";
	
	private static final String ENABLE_TIMER_BONUS = "enable_timer_bonus";
	private static final String TIMER_POINTS = "timer_points";
	private static final String DOUBLE_POINTS_PROBABILITY = "timer_double_points_probability";
	private static final String ENABLE_DAILY_BONUS = "enable_daily_bonus";
	private static final String DAILY_BONUS_POINTS = "daily_bonus_points";
	private static final String DAILY_POINTS = "cafe_points_today";
	private static final String ENABLE_FARMING_BONUS = "enable_farming_bonus";
	private static final String FARMING_BONUS_POINTS = "farming_bonus_points";
	private static final String FARMING_BONUS_LEVEL_DIFF = "farming_bonus_level_diff";
	private static final String ENABLE_PVP_BONUS = "enable_pvp_bonus";
	private static final String PVP_BONUS_POINTS = "pvp_bonus_points";
	
	protected PcCafeManager()
	{
		if (PcCafeData.getInstance().getCafeBool(ENABLE_TIMER_BONUS, false))
			ThreadPool.scheduleAtFixedRate(() -> rewardPoint(), TimeUnit.MINUTES.toMillis(1), TimeUnit.MINUTES.toMillis(1));
	}
	
	private static void rewardPoint()
	{
		int pointsToAdd = PcCafeData.getInstance().getCafeInt(TIMER_POINTS, 10);
		World.getInstance().getPlayers().stream().forEach(player -> player.increasePcCafePoints(pointsToAdd, (Rnd.get(100) < PcCafeData.getInstance().getCafeInt(DOUBLE_POINTS_PROBABILITY, 10))));
	}
	
	public void onPlayerLogin(Player player)
	{
		if (PcCafeData.getInstance().getCafeBool(ENABLE_DAILY_BONUS, false))
		{
			if (!player.getMemos().getBool(DAILY_POINTS, false))
			{
				player.getMemos().set(DAILY_POINTS, true);
				player.increasePcCafePoints(PcCafeData.getInstance().getCafeInt(DAILY_BONUS_POINTS, 1000));
			}
		}
		
		player.sendPacket(new ExPCCafePointInfo(player.getPcCafePoints(), 0, PcCafeConsumeType.NORMAL));
	}
	
	public void onAttackableKill(Player player)
	{
		if (PcCafeData.getInstance().getCafeBool(ENABLE_FARMING_BONUS, false))
		{
			final Creature target = (Creature) player.getTarget();
			final int levelDiff = target.getLevel() - player.getLevel();
			final int maxLevelDiff = PcCafeData.getInstance().getCafeInt(FARMING_BONUS_LEVEL_DIFF, 11);
			
			if ((levelDiff > -maxLevelDiff) && (levelDiff < maxLevelDiff))
				player.increasePcCafePoints(PcCafeData.getInstance().getCafeInt(FARMING_BONUS_POINTS, 50));
		}
	}
	
	public void onReset()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement(CLEAR_MEMO))
		{
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.error("Failed to reset daily login pc points.", e);
		}
		
		World.getInstance().getPlayers().stream().forEach(player -> player.getMemos().remove(DAILY_POINTS));
	}
	
	public void onPlayerPvPKill(Player player)
	{
		if (PcCafeData.getInstance().getCafeBool(ENABLE_PVP_BONUS, false))
			player.increasePcCafePoints(PcCafeData.getInstance().getCafeInt(PVP_BONUS_POINTS, 100));
	}
	
	public static final PcCafeManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final PcCafeManager INSTANCE = new PcCafeManager();
	}
}
