package net.sf.l2j.gameserver.data.manager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.commons.logging.CLogger;
import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.data.SpawnTable;
import net.sf.l2j.gameserver.data.xml.NpcData;
import net.sf.l2j.gameserver.model.L2Spawn;
import net.sf.l2j.gameserver.model.actor.instance.RaidBoss;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.templates.StatsSet;

public class RaidBossManager
{
	protected static final CLogger LOGGER = new CLogger(RaidBossManager.class.getName());
	
	private static final String LOAD_RAIDBOSSES = "SELECT * from raidboss_spawnlist ORDER BY boss_id";
	private static final String INSERT_RAIDBOSS = "INSERT INTO raidboss_spawnlist (boss_id,loc_x,loc_y,loc_z,heading,respawn_time,currentHp,currentMp) values(?,?,?,?,?,?,?,?)";
	private static final String UPDATE_RAIDBOSS = "UPDATE raidboss_spawnlist SET respawn_time = ?, currentHP = ?, currentMP = ? WHERE boss_id = ?";
	private static final String DELETE_RAIDBOSS = "DELETE FROM raidboss_spawnlist WHERE boss_id=?";
	
	protected final Map<Integer, RaidBoss> _bosses = new HashMap<>();
	protected final Map<Integer, L2Spawn> _spawns = new HashMap<>();
	protected final Map<Integer, StatsSet> _storedInfo = new HashMap<>();
	protected final Map<Integer, ScheduledFuture<?>> _schedules = new HashMap<>();
	
	public static enum StatusEnum
	{
		ALIVE,
		DEAD,
		UNDEFINED
	}
	
	public RaidBossManager()
	{
		load();
	}
	
	private void load()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement(LOAD_RAIDBOSSES);
			ResultSet rs = ps.executeQuery())
		{
			while (rs.next())
			{
				final NpcTemplate template = getValidTemplate(rs.getInt("boss_id"));
				if (template == null)
				{
					LOGGER.warn("Couldn't load raidboss #{}.", rs.getInt("boss_id"));
					continue;
				}
				
				// Generate a L2Spawn.
				final L2Spawn spawnDat = new L2Spawn(template);
				spawnDat.setLoc(rs.getInt("loc_x"), rs.getInt("loc_y"), rs.getInt("loc_z"), rs.getInt("heading"));
				spawnDat.setRespawnMinDelay(rs.getInt("spawn_time"));
				spawnDat.setRespawnMaxDelay(rs.getInt("random_time"));
				
				addNewSpawn(spawnDat, rs.getLong("respawn_time"), rs.getDouble("currentHP"), rs.getDouble("currentMP"), false);
			}
		}
		catch (Exception e)
		{
			LOGGER.error("Error restoring raid bosses.", e);
		}
		LOGGER.info("Loaded {} raid bosses ({} under schedule).", _bosses.size(), _schedules.size());
	}
	
	public void onDeath(RaidBoss boss)
	{
		final StatsSet info = _storedInfo.get(boss.getNpcId());
		if (info == null)
			return;
		
		boss.setRaidStatus(StatusEnum.DEAD);
		
		// getRespawnMinDelay() is used as fixed timer, while getRespawnMaxDelay() is used as random timer.
		final int respawnDelay = boss.getSpawn().getRespawnMinDelay() + Rnd.get(-boss.getSpawn().getRespawnMaxDelay(), boss.getSpawn().getRespawnMaxDelay());
		final long respawnTime = Calendar.getInstance().getTimeInMillis() + (respawnDelay * 3600000);
		
		// Refresh the StatsSet.
		info.set("currentHP", boss.getMaxHp());
		info.set("currentMP", boss.getMaxMp());
		info.set("respawnTime", respawnTime);
		
		// Cancel task is existing.
		final ScheduledFuture<?> respawnTask = _schedules.get(boss.getNpcId());
		if (respawnTask != null)
			respawnTask.cancel(false);
		
		// Register the task.
		_schedules.put(boss.getNpcId(), ThreadPool.schedule(new spawnSchedule(boss.getNpcId()), respawnDelay * 3600000));
		
		// Refresh the database for this particular boss entry.
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement(UPDATE_RAIDBOSS))
		{
			ps.setLong(1, info.getLong("respawnTime"));
			ps.setDouble(2, info.getDouble("currentHP"));
			ps.setDouble(3, info.getDouble("currentMP"));
			ps.setInt(4, boss.getNpcId());
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.error("Couldn't update raid boss #{}.", e, boss.getNpcId());
		}
		
		LOGGER.info("Raid boss: {} - {} ({}h).", boss.getName(), new SimpleDateFormat("dd-MM-yyyy HH:mm").format(respawnTime), respawnDelay);
	}
	
	public void addNewSpawn(L2Spawn spawnDat, long respawnTime, double currentHP, double currentMP, boolean storeInDb)
	{
		if (spawnDat == null)
			return;
		
		final int bossId = spawnDat.getNpcId();
		if (_spawns.containsKey(bossId))
			return;
		
		final long time = Calendar.getInstance().getTimeInMillis();
		
		// Add the spawn.
		SpawnTable.getInstance().addNewSpawn(spawnDat, false);
		
		// Boss is alive, spawn him.
		if (respawnTime == 0L || time > respawnTime)
		{
			final RaidBoss raidboss = (RaidBoss) spawnDat.doSpawn(false);
			
			currentHP = (currentHP == 0) ? raidboss.getMaxHp() : currentHP;
			currentMP = (currentMP == 0) ? raidboss.getMaxMp() : currentMP;
			
			// Set HP, MP and status.
			raidboss.setCurrentHp(currentHP);
			raidboss.setCurrentMp(currentMP);
			raidboss.setRaidStatus(StatusEnum.ALIVE);
			
			// Store the instance.
			_bosses.put(bossId, raidboss);
			
			// We generate the StatsSet.
			final StatsSet info = new StatsSet();
			info.set("currentHP", currentHP);
			info.set("currentMP", currentMP);
			info.set("respawnTime", 0L);
			
			// Store the StatsSet.
			_storedInfo.put(bossId, info);
		}
		// Boss isn't alive, we generate a scheduled task using its respawn time.
		else
		{
			long spawnTime = respawnTime - Calendar.getInstance().getTimeInMillis();
			_schedules.put(bossId, ThreadPool.schedule(new spawnSchedule(bossId), spawnTime));
		}
		
		// Add the spawn.
		_spawns.put(bossId, spawnDat);
		
		// Case of admincommand, the regular load doesn't save it.
		if (storeInDb)
		{
			try (Connection con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement ps = con.prepareStatement(INSERT_RAIDBOSS))
			{
				ps.setInt(1, spawnDat.getNpcId());
				ps.setInt(2, spawnDat.getLocX());
				ps.setInt(3, spawnDat.getLocY());
				ps.setInt(4, spawnDat.getLocZ());
				ps.setInt(5, spawnDat.getHeading());
				ps.setLong(6, respawnTime);
				ps.setDouble(7, currentHP);
				ps.setDouble(8, currentMP);
				ps.executeUpdate();
			}
			catch (Exception e)
			{
				LOGGER.error("Couldn't store raid boss #{}.", e, bossId);
			}
		}
	}
	
	public void deleteSpawn(L2Spawn spawnDat, boolean updateDb)
	{
		if (spawnDat == null)
			return;
		
		final int bossId = spawnDat.getNpcId();
		if (!_spawns.containsKey(bossId))
			return;
		
		// Remove the spawn.
		SpawnTable.getInstance().deleteSpawn(spawnDat, false);
		
		// Clean the different Maps.
		_spawns.remove(bossId);
		_bosses.remove(bossId);
		_storedInfo.remove(bossId);
		
		// Remove the task entry, and cancel the running task.
		final ScheduledFuture<?> respawnTask = _schedules.remove(bossId);
		if (respawnTask != null)
			respawnTask.cancel(false);
		
		// Delete from database.
		if (updateDb)
		{
			try (Connection con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement ps = con.prepareStatement(DELETE_RAIDBOSS))
			{
				ps.setInt(1, bossId);
				ps.executeUpdate();
			}
			catch (Exception e)
			{
				LOGGER.error("Couldn't remove raid boss #{}.", e, bossId);
			}
		}
	}
	
	private void saveRaidBosses()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement ps = con.prepareStatement(UPDATE_RAIDBOSS))
		{
			for (Map.Entry<Integer, StatsSet> entry : _storedInfo.entrySet())
			{
				final RaidBoss boss = _bosses.get(entry.getKey());
				if (boss == null)
					continue;
				
				final StatsSet info = entry.getValue();
				if (info == null)
					continue;
				
				if (boss.getRaidStatus() == StatusEnum.ALIVE)
				{
					ps.setLong(1, 0L);
					ps.setDouble(2, boss.getCurrentHp());
					ps.setDouble(3, boss.getCurrentMp());
				}
				else
				{
					ps.setLong(1, info.getLong("respawnTime"));
					ps.setDouble(2, info.getDouble("currentHP"));
					ps.setDouble(3, info.getDouble("currentMP"));
				}
				ps.setInt(4, entry.getKey());
				ps.addBatch();
			}
			ps.executeBatch();
		}
		catch (Exception e)
		{
			LOGGER.error("Couldn't save raid bosses.", e);
		}
	}
	
	public StatusEnum getRaidBossStatusId(int bossId)
	{
		final RaidBoss raidboss = _bosses.get(bossId);
		if (raidboss != null)
			return raidboss.getRaidStatus();
		
		if (_schedules.containsKey(bossId))
			return StatusEnum.DEAD;
		
		return StatusEnum.UNDEFINED;
	}
	
	public NpcTemplate getValidTemplate(int bossId)
	{
		final NpcTemplate template = NpcData.getInstance().getTemplate(bossId);
		if (template == null || !template.isType("RaidBoss"))
			return null;
		
		return template;
	}
	
	public boolean isDefined(int bossId)
	{
		return _spawns.containsKey(bossId);
	}
	
	public Map<Integer, RaidBoss> getBosses()
	{
		return _bosses;
	}
	
	public Map<Integer, L2Spawn> getSpawns()
	{
		return _spawns;
	}
	
	public void reloadBosses()
	{
		load();
	}
	
	/**
	 * Saves all raidboss status and then clears all info from memory, including all schedules.
	 */
	public void cleanUp()
	{
		saveRaidBosses();
		
		_bosses.clear();
		
		if (!_schedules.isEmpty())
		{
			for (ScheduledFuture<?> f : _schedules.values())
				f.cancel(false);
			
			_schedules.clear();
		}
		
		_storedInfo.clear();
		_spawns.clear();
	}
	
	private class spawnSchedule implements Runnable
	{
		private final int bossId;
		
		public spawnSchedule(int npcId)
		{
			bossId = npcId;
		}
		
		@Override
		public void run()
		{
			final RaidBoss raidboss = (RaidBoss) _spawns.get(bossId).doSpawn(false);
			raidboss.setRaidStatus(StatusEnum.ALIVE);
			
			// Retrieve boss StatsSet.
			final StatsSet info = _storedInfo.get(bossId);
			if (info != null)
			{
				info.set("currentHP", raidboss.getMaxHp());
				info.set("currentMP", raidboss.getMaxMp());
				info.set("respawnTime", 0L);
			}
			
			LOGGER.info("{} raid boss has spawned.", raidboss.getName());
			
			_bosses.put(bossId, raidboss);
			
			_schedules.remove(bossId);
		}
	}
	
	public static RaidBossManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final RaidBossManager INSTANCE = new RaidBossManager();
	}
}