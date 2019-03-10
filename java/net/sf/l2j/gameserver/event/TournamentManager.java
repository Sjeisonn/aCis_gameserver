package net.sf.l2j.gameserver.event;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import net.sf.l2j.Config;

/**
 * @author Kara` (best)
 *
 */
public class TournamentManager
{
	private final Logger _log = Logger.getLogger(TournamentManager.class.getName());
	
	private final Map<Integer, Match> _matches = new ConcurrentHashMap<>();
	
	public TournamentManager()
	{
		if (!Config.ENABLED)
		{
			_log.warning(getClass().getSimpleName() + ": Disabled.");
			return;
		}
		
		if (Config.MATCHES == null || Config.MATCHES.length <= 0)
		{
			_log.warning(getClass().getSimpleName() + ": Empty matches config.");
			return;
		}
		
		for (String match : Config.MATCHES)
		{
			int matchSize = _matches.size() + 1;
			_matches.put(matchSize, new Match(matchSize, Integer.parseInt(match)));
		}
		
		_log.warning(getClass().getSimpleName() + " " + _matches.size() + " matches initialized.");
	}
	
	public Collection<Match> getMatches()
	{
		return _matches.values();
	}
	
	public void replaceMe(final Match match)
	{
		_matches.replace(match.getId(), new Match(match.getId(), match.getPartySize()));
	}
	
	public Match getMatch(int id)
	{
		return _matches.get(id);
	}
	
	public static TournamentManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		static TournamentManager _instance = new TournamentManager();
	}
}
