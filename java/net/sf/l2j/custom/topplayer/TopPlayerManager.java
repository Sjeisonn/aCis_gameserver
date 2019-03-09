package net.sf.l2j.custom.topplayer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.TimeUnit;

import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.commons.logging.CLogger;

import net.sf.l2j.L2DatabaseFactory;

/**
 * @author SweeTs
 */
public final class TopPlayerManager
{
	private static final CLogger LOGGER = new CLogger(TopPlayerManager.class.getName());
	
	private static final String SELECT = "SELECT char_name, pvpkills, pkkills, SUM(pvpkills+pkkills) as TOTAL FROM characters WHERE accesslevel = 0 GROUP BY char_name, pvpkills, pkkills ORDER BY TOTAL DESC, char_name LIMIT 1";	
	private static final long REFRESH_INTERVAL = TimeUnit.HOURS.toMillis(24);
	
	private final TopPlayer topPlayer = new TopPlayer();
	
	protected TopPlayerManager()
	{
		ThreadPool.scheduleAtFixedRate(this::run, 1000, REFRESH_INTERVAL);
		LOGGER.info(getClass().getSimpleName() + ": initialized.");
	}
	
	private void run()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(SELECT);
			ResultSet rset = statement.executeQuery())
		{
			if (rset.next())
				topPlayer.setTop(rset.getString("char_name"), rset.getInt("pvpkills"), rset.getInt("pkkills"));
		}
		catch (Exception e)
		{
			LOGGER.warn("Could not restore: {}", e);
		}
	}
	
	public TopPlayer getTopPlayer()
	{
		return topPlayer;
	}
	
	public class TopPlayer
	{
		private String playerName;
		private int pvpCount;
		private int pkCount;
		
		public void setTop(final String name, final int pvp, final int pk)
		{
			playerName = name;
			pvpCount = pvp;
			pkCount = pk;
		}
		
		public String getName()
		{
			return playerName;
		}
		
		public int getPvp()
		{
			return pvpCount;
		}
		
		public int getPk()
		{
			return pkCount;
		}
	}
	
	public static final TopPlayerManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final TopPlayerManager INSTANCE = new TopPlayerManager();
	}
}