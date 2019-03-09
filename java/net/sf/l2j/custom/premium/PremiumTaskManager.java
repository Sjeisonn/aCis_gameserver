package net.sf.l2j.custom.premium;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import net.sf.l2j.commons.concurrent.ThreadPool;

import net.sf.l2j.gameserver.model.actor.instance.Player;

/**
 * @author SweeTs
 */
public final class PremiumTaskManager implements Runnable
{
	private final Map<Player, Long> _players = new ConcurrentHashMap<>();
	private final Map<Player, Integer> _playersLevel = new ConcurrentHashMap<>();
	
	private static final long INTERVAL = TimeUnit.MINUTES.toMillis(1);
	
	protected PremiumTaskManager()
	{
		ThreadPool.scheduleAtFixedRate(this, INTERVAL, INTERVAL);
	}
	
	public final void addLevel(Player player, int level)
	{
		_playersLevel.put(player, level);
	}
	
	public final void add(Player player, long time)
	{
		_players.put(player, time);
	}
	
	public final void remove(Player player)
	{
		_players.remove(player);
		_playersLevel.remove(player);
	}
	
	public Map<Player, Long> getPremiumCharacters()
	{
		return _players;
	}
	
	public Map<Player, Integer> getCharactersLevel()
	{
		return _playersLevel;
	}
	
	@Override
	public final void run()
	{
		if (_players.isEmpty())
			return;
		
		final long currentTime = System.currentTimeMillis();
		for (Map.Entry<Player, Long> entry : _players.entrySet())
		{
			final Player player = entry.getKey();
			final long timeLeft = entry.getValue();
			
			if (currentTime > timeLeft)
			{
				if (player.isOnline())
					PremiumManager.removePremium(player);
				
				remove(player);
			}
		}
	}
	
	public static final PremiumTaskManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final PremiumTaskManager _instance = new PremiumTaskManager();
	}
}