package net.sf.l2j.gameserver.event;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Future;

import net.sf.l2j.commons.concurrent.ThreadPool;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.xml.MapRegionData.TeleportType;
import net.sf.l2j.gameserver.model.actor.Summon;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.group.Party;

/**
 * @author Kara`
 *
 */
public class Match extends AbstractMatch implements Runnable 
{
	private final Map<Integer, Player> _players = new HashMap<>();
	
	private int _size;
	private int _id;
	private int _instanceId;
	private int _phase;
	private int _partiesSize;
	private int _winnerTeam;
	private int _team1Score;
	private int _team2Score;
	
	public Match(int id, int size)
	{
		_id = id;
		_size = size;
	}
	
	private boolean _isRunning;
	private Future<?> _task;
	
	public void schedule(int seconds)
	{
		if (_task !=null)
		{
			if (!_task.isDone() || !_task.isCancelled())
			{
				_task.cancel(true);
			}
			_task = null;
		}
		
		_task = ThreadPool.schedule(this, seconds * 1000);
	}
	
	@Override
	public void onKill(final Player victim)
	{
		if (_phase > 0)
		{
			if (victim.getTeam() == 1)
			{
				_team2Score ++;
				
				if (_team2Score >= _size)
				{
					_winnerTeam = 2;
					onFinish();
				}
			}
			else if (victim.getTeam() == 2)
			{
				_team1Score ++;
				
				if (_team1Score >= _size)
				{
					_winnerTeam = 1;
					onFinish();
				}
			}
		}
	}
	
	@Override
	public void onDisconnect(final Player player)
	{
		_players.remove(player.getObjectId());
		
		if (_phase > 0)
		{
			switch(player.getTeam())
			{
				case 1:
					_team2Score++;
					break;
				case 2:
					_team1Score++;
					break;
			}
			
			if (_team2Score == _size)
			{
				_winnerTeam = 1;
				onFinish();
			}
			else if (_team2Score == _size)
			{
				_winnerTeam = 2;
				onFinish();
			}
			
			for (Player p : getPlayers())
			{
				if (p.getTeam() == player.getTeam())
				{
					p.sendMessage(player.getName() + " left the match.");
				}
			}
		}
		else
		{
			if (player.getParty() !=null)
			{
				for (Player member : player.getParty().getMembers())
				{
					member.sendMessage(player.getName() + " left.");
					member.setMatch(null);
					member.setTeam(0);
					member.broadcastUserInfo();
					
					_players.remove(member.getObjectId());
				}
				
				_partiesSize --;
			}
		}
	}
	
	@Override
	public void onRegister(final Player player)
	{
		if (player.getMatch() !=null)
		{
			player.sendMessage("You're already registered.");
			return;
		}
		
		final Party party = player.getParty();
		
		if (party == null)
		{
			player.sendMessage("You don't have a party.");
			return;
		}
		
		if (party.getLeaderObjectId() != player.getObjectId())
		{
			player.sendMessage("Only party leader may register party in this match.");
			return;
		}
		
		if (party.getMembersCount() != getPartySize())
		{
			player.sendMessage("You need " + getPartySize() + " party members in order to participate in this match.");
			return;
		}
		
		if (player.getMatch().getPhase() != 0)
		{
			player.sendMessage("There is already a match in progress.");
			return;
		}
		
		for (final Player member : party.getMembers())
		{
			if (member.getMatch() != null)
			{
				player.sendMessage(member.getName() + " is already in a match.");
				return;
			}
		}
		
		for (final Player member : party.getMembers())
		{
			_players.put(member.getObjectId(), member);
			member.setTeam(_partiesSize == 1 ? 2 : 1);
			member.setMatch(this);
			member.sendMessage(member.getObjectId() == player.getObjectId() ? "You successfuly registered your party in this match." : "Your leader registered party in a match.");
		}
		
		_partiesSize ++;
		
		if (_partiesSize == 2)
		{
			TournamentManager.getInstance().replaceMe(this);
			setNextPhase();
		}
	}
	
	@Override
	public void onUnregister(final Player player)
	{
		if (player.getMatch() == null)
		{
			player.sendMessage("You're not registered in any match.");
			return;
		}
		
		if (_phase > 0)
		{
			player.sendMessage("Cannot unregister now, match is already running.");
			return;
		}
		
		if (!player.isInParty())
		{
			return;
		}
		
		if (player.getParty().getLeaderObjectId() != player.getObjectId())
		{
			player.sendMessage("Only party leader may unregister party in tournament.");
			return;
		}
		
		for (final Player member : player.getParty().getMembers())
		{
			if (member.getObjectId() == player.getObjectId())
			{
				member.sendMessage("You unregistered your party from this match.");
			}
			else
			{
				member.sendMessage(player.getName() + " unregistered party from match.");
			}
			
			_players.remove(member.getObjectId());
			
			member.setTeam(0);
			member.setMatch(null);
			
			member.broadcastUserInfo();
		}
		
		_partiesSize --;
	}
	
	@Override
	public int getPartySize()
	{
		return _size;
	}
	
	@Override
	public Collection<Player> getPlayers()
	{
		return _players.values();
	}
	
	@Override
	public String getName()
	{
		return "" + _size + " vs " + _size;
	}
	
	@Override
	public void onFinish()
	{
		schedule(5);
		
		switch(_winnerTeam)
		{
			case 1:
			case 2:
				for (final Player p : getPlayers())
				{
					p.sendMessage(p.getTeam() != _winnerTeam ? "Unfortunately your team lost this match." : "Congratulations, your team won this match");
					
					if (p.getTeam() == _winnerTeam)
					{
						for (Entry<Integer, Integer> set : Config.REWARDS.entrySet())
						{
							p.addItem("", set.getKey(), set.getValue(), p, true);
						}
					}
					reviveAndHeal(p);
				}
				break;
			default:
				for (final Player p : getPlayers())
				{
					p.sendMessage("Unfortunately match ended in a tie. Nobody won.");
					reviveAndHeal(p);
				}
				break;
		}
	}
	
	@Override
	public void run()
	{
		if (_winnerTeam == 0)
		{
			setNextPhase();	
		}
		else
		{
			for (final Player p : getPlayers())
			{
				p.setMatch(null);
				p.setTeam(0);
				p.broadcastUserInfo();
				p.sendMessage("You will be teleport back in town now.");
				p.setInstanceId(0);
				p.teleportTo(TeleportType.TOWN);
				reviveAndHeal(p);
			}
			
			_players.clear();
		}
	}
	
	private static void reviveAndHeal(Player player)
	{
		if (player.isDead())
		{
			player.doRevive();
		}
		
		player.setCurrentCp(player.getMaxCp());
		player.setCurrentHpMp(player.getMaxHp(), player.getMaxMp());
		
		final Summon pet = player.getPet();
		
		if (pet !=null)
		{
			if (pet.isDead())
			{
				pet.doRevive();
			}
			
			pet.setCurrentCp(pet.getMaxCp());
			pet.setCurrentHpMp(pet.getMaxHp(), pet.getMaxMp());
		}
	}

	public boolean isRunning()
	{
		return _isRunning;
	}
	
	private void setNextPhase()
	{
		_phase ++;
		
		switch(_phase)
		{
			case 1:
				schedule(10);
				
				for (final Player p : getPlayers())
				{
					p.sendMessage("You will be teleport in area in 10 seconds.");
				}
				break;
			case 2:
				schedule(30);
				
				for (final Player p : getPlayers())
				{
					p.sendMessage("Battle start in 30 seconds. Prepare yourself.");
					
					if (p.getTeam() == 1)
					{
						p.teleportTo(Config.TEAM_1_LOC, 0); //TODO config
					}
					else
					{
						p.teleportTo(Config.TEAM_2_LOC, 0);  //TODO config
					}
					
					reviveAndHeal(p);
				}
				break;
			case 3:
				_isRunning = true;
				_instanceId = 0;
				
				do
				{
					_instanceId ++;
					_instanceId = InstanceManager.getInstance().createInstance(_instanceId);
				}
				while(InstanceManager.getInstance().instanceExists(_instanceId));
				
				for (final Player p : getPlayers())
				{
					p.setInstanceId(_instanceId);
					p.sendMessage("Match has started, fight your opponent!");
					reviveAndHeal(p);
				}
				break;
			case 4:
				_isRunning = false;
				
				for (final Player p : getPlayers())
				{
					p.sendMessage("Match ended in a tie. You will be teleport back in town.");
					reviveAndHeal(p);
				}
				
				_winnerTeam = -1;
				schedule(10);
				break;
		}
	}
	
	public int getPhase()
	{
		return _phase;
	}

	@Override
	public int getId()
	{
		return _id;
	}
}