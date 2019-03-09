package net.sf.l2j.gameserver.model.actor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.l2j.commons.concurrent.ThreadPool;

import net.sf.l2j.Config;
import net.sf.l2j.custom.cafepoint.PcCafeManager;
import net.sf.l2j.gameserver.model.AggroInfo;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.ai.CtrlIntention;
import net.sf.l2j.gameserver.model.actor.ai.type.AttackableAI;
import net.sf.l2j.gameserver.model.actor.ai.type.CreatureAI;
import net.sf.l2j.gameserver.model.actor.ai.type.SiegeGuardAI;
import net.sf.l2j.gameserver.model.actor.instance.Player;
import net.sf.l2j.gameserver.model.actor.status.AttackableStatus;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.scripting.EventType;
import net.sf.l2j.gameserver.scripting.Quest;

/**
 * This class manages all NPCs that can be attacked. It inherits from {@link Npc}.
 */
public class Attackable extends Npc
{
	private final Set<Creature> _attackByList = ConcurrentHashMap.newKeySet();
	
	private final Map<Creature, AggroInfo> _aggroList = new ConcurrentHashMap<>();
	
	private boolean _isReturningToSpawnPoint;
	private boolean _seeThroughSilentMove;
	
	public Attackable(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	public void initCharStatus()
	{
		setStatus(new AttackableStatus(this));
	}
	
	@Override
	public AttackableStatus getStatus()
	{
		return (AttackableStatus) super.getStatus();
	}
	
	@Override
	public CreatureAI getAI()
	{
		CreatureAI ai = _ai;
		if (ai == null)
		{
			synchronized (this)
			{
				if (_ai == null)
					_ai = new AttackableAI(this);
				
				return _ai;
			}
		}
		return ai;
	}
	
	/**
	 * Reduce the current HP of the L2Attackable.
	 * @param damage The HP decrease value
	 * @param attacker The Creature who attacks
	 */
	@Override
	public void reduceCurrentHp(double damage, Creature attacker, L2Skill skill)
	{
		reduceCurrentHp(damage, attacker, true, false, skill);
	}
	
	/**
	 * Reduce the current HP of the L2Attackable, update its _aggroList and launch the doDie Task if necessary.
	 * @param attacker The Creature who attacks
	 * @param awake The awake state (If True : stop sleeping)
	 */
	@Override
	public void reduceCurrentHp(double damage, Creature attacker, boolean awake, boolean isDOT, L2Skill skill)
	{
		// Add damage and hate to the attacker AggroInfo of the L2Attackable _aggroList
		addDamage(attacker, (int) damage, skill);
		
		// Reduce the current HP of the L2Attackable and launch the doDie Task if necessary
		super.reduceCurrentHp(damage, attacker, awake, isDOT, skill);
	}
	
	/**
	 * Kill the L2Attackable (the corpse disappeared after 7 seconds), distribute rewards (EXP, SP, Drops...) and notify Quest Engine.
	 * <ul>
	 * <li>Distribute Exp and SP rewards to Player (including Summon owner) that hit the L2Attackable and to their Party members</li>
	 * <li>Notify the Quest Engine of the L2Attackable death if necessary</li>
	 * <li>Kill the L2Npc (the corpse disappeared after 7 seconds)</li>
	 * </ul>
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T GIVE rewards to L2PetInstance</B></FONT>
	 * @param killer The Creature that has killed the L2Attackable
	 */
	@Override
	public boolean doDie(Creature killer)
	{
		// Kill the L2Npc (the corpse disappeared after 7 seconds)
		if (!super.doDie(killer))
			return false;
		
		// Increase the PC Cafe points
		PcCafeManager.getInstance().onAttackableKill(killer.getActingPlayer());
		
		final List<Quest> scripts = getTemplate().getEventQuests(EventType.ON_KILL);
		if (scripts != null)
			for (Quest quest : scripts)
				ThreadPool.schedule(() -> quest.notifyKill(this, killer), 3000);
			
		_attackByList.clear();
		
		return true;
	}
	
	@Override
	public void onSpawn()
	{
		super.onSpawn();
		
		// Clear all aggro char from list
		_aggroList.clear();
		
		setWalking();
		
		// check the region where this mob is, do not activate the AI if region is inactive.
		if (!isInActiveRegion())
		{
			if (hasAI())
				getAI().stopAITask();
		}
	}
	
	/**
	 * Check if the server allows Random Animation.<BR>
	 * <BR>
	 * This is located here because L2Monster and L2FriendlyMob both extend this class. The other non-pc instances extend either L2Npc or L2MonsterInstance.
	 */
	@Override
	public boolean hasRandomAnimation()
	{
		return Config.MAX_MONSTER_ANIMATION > 0 && !isRaidRelated();
	}
	
	@Override
	public boolean isMob()
	{
		return true; // This means we use MAX_MONSTER_ANIMATION instead of MAX_NPC_ANIMATION
	}
	
	public void addAttackerToAttackByList(Creature attacker)
	{
		if (attacker == null || attacker == this)
			return;
		
		_attackByList.add(attacker);
	}
	
	/**
	 * Add damage and hate to the attacker AggroInfo of the L2Attackable _aggroList.
	 * @param attacker The Creature that gave damages to this L2Attackable
	 * @param damage The number of damages given by the attacker Creature
	 * @param skill The skill used to make damage.
	 */
	public void addDamage(Creature attacker, int damage, L2Skill skill)
	{
		if (attacker == null || isDead())
			return;
		
		final List<Quest> scripts = getTemplate().getEventQuests(EventType.ON_ATTACK);
		if (scripts != null)
			for (Quest quest : scripts)
				quest.notifyAttack(this, attacker, damage, skill);
	}
	
	/**
	 * Add damage and hate to the attacker AggroInfo of the L2Attackable _aggroList.
	 * @param attacker The Creature that gave damages to this L2Attackable
	 * @param damage The number of damages given by the attacker Creature
	 * @param aggro The hate (=damage) given by the attacker Creature
	 */
	public void addDamageHate(Creature attacker, int damage, int aggro)
	{
		if (attacker == null)
			return;
		
		// Get or create the AggroInfo of the attacker.
		final AggroInfo ai = _aggroList.computeIfAbsent(attacker, AggroInfo::new);
		ai.addDamage(damage);
		ai.addHate(aggro);
		
		if (aggro == 0)
		{
			final Player targetPlayer = attacker.getActingPlayer();
			if (targetPlayer != null)
			{
				final List<Quest> scripts = getTemplate().getEventQuests(EventType.ON_AGGRO);
				if (scripts != null)
					for (Quest quest : scripts)
						quest.notifyAggro(this, targetPlayer, (attacker instanceof Summon));
			}
			else
			{
				aggro = 1;
				ai.addHate(1);
			}
		}
		else
		{
			// Set the intention to the L2Attackable to ACTIVE
			if (aggro > 0 && getAI().getDesire().getIntention() == CtrlIntention.IDLE)
				getAI().setIntention(CtrlIntention.ACTIVE);
		}
	}
	
	/**
	 * Reduce hate for the target. If the target is null, decrease the hate for the whole aggrolist.
	 * @param target The target to check.
	 * @param amount The amount to remove.
	 */
	public void reduceHate(Creature target, int amount)
	{
		if (getAI() instanceof SiegeGuardAI)
		{
			stopHating(target);
			setTarget(null);
			getAI().setIntention(CtrlIntention.IDLE);
			return;
		}
		
		if (target == null) // whole aggrolist
		{
			Creature mostHated = getMostHated();
			
			// If not most hated target is found, makes AI passive for a moment more
			if (mostHated == null)
			{
				((AttackableAI) getAI()).setGlobalAggro(-25);
				return;
			}
			
			for (AggroInfo ai : _aggroList.values())
				ai.addHate(-amount);
			
			amount = getHating(mostHated);
			
			if (amount <= 0)
			{
				((AttackableAI) getAI()).setGlobalAggro(-25);
				_aggroList.clear();
				getAI().setIntention(CtrlIntention.ACTIVE);
				setWalking();
			}
			return;
		}
		
		AggroInfo ai = _aggroList.get(target);
		if (ai == null)
			return;
		
		ai.addHate(-amount);
		
		if (ai.getHate() <= 0)
		{
			if (getMostHated() == null)
			{
				((AttackableAI) getAI()).setGlobalAggro(-25);
				_aggroList.clear();
				getAI().setIntention(CtrlIntention.ACTIVE);
				setWalking();
			}
		}
	}
	
	/**
	 * Clears _aggroList hate of the Creature without removing from the list.
	 * @param target The target to clean from that L2Attackable _aggroList.
	 */
	public void stopHating(Creature target)
	{
		if (target == null)
			return;
		
		AggroInfo ai = _aggroList.get(target);
		if (ai != null)
			ai.stopHate();
	}
	
	public void cleanAllHate()
	{
		for (AggroInfo ai : _aggroList.values())
			ai.stopHate();
	}
	
	/**
	 * @return the most hated Creature of the L2Attackable _aggroList.
	 */
	public Creature getMostHated()
	{
		if (_aggroList.isEmpty() || isAlikeDead())
			return null;
		
		Creature mostHated = null;
		int maxHate = 0;
		
		// Go through the aggroList of the L2Attackable
		for (AggroInfo ai : _aggroList.values())
		{
			if (ai.checkHate(this) > maxHate)
			{
				mostHated = ai.getAttacker();
				maxHate = ai.getHate();
			}
		}
		return mostHated;
	}
	
	/**
	 * @return the list of hated Creature. It also make checks, setting hate to 0 following conditions.
	 */
	public List<Creature> getHateList()
	{
		if (_aggroList.isEmpty() || isAlikeDead())
			return Collections.emptyList();
		
		final List<Creature> result = new ArrayList<>();
		for (AggroInfo ai : _aggroList.values())
		{
			ai.checkHate(this);
			result.add(ai.getAttacker());
		}
		return result;
	}
	
	/**
	 * @param target The Creature whose hate level must be returned
	 * @return the hate level of the L2Attackable against this Creature contained in _aggroList.
	 */
	public int getHating(final Creature target)
	{
		if (_aggroList.isEmpty() || target == null)
			return 0;
		
		final AggroInfo ai = _aggroList.get(target);
		if (ai == null)
			return 0;
		
		if (ai.getAttacker() instanceof Player && ((Player) ai.getAttacker()).getAppearance().getInvisible())
		{
			// Remove Object Should Use This Method and Can be Blocked While Interating
			_aggroList.remove(target);
			return 0;
		}
		
		if (!ai.getAttacker().isVisible())
		{
			_aggroList.remove(target);
			return 0;
		}
		
		if (ai.getAttacker().isAlikeDead())
		{
			ai.stopHate();
			return 0;
		}
		return ai.getHate();
	}
	
	public void useMagic(L2Skill skill)
	{
		if (skill == null || isAlikeDead())
			return;
		
		if (skill.isPassive())
			return;
		
		if (isCastingNow())
			return;
		
		if (isSkillDisabled(skill))
			return;
		
		if (getCurrentMp() < getStat().getMpConsume(skill) + getStat().getMpInitialConsume(skill))
			return;
		
		if (getCurrentHp() <= skill.getHpConsume())
			return;
		
		if (skill.isMagic())
		{
			if (isMuted())
				return;
		}
		else
		{
			if (isPhysicalMuted())
				return;
		}
		
		WorldObject target = skill.getFirstOfTargetList(this);
		if (target == null)
			return;
		
		getAI().setIntention(CtrlIntention.CAST, skill, target);
	}
	
	/**
	 * @return true if the {@link Attackable} successfully returned to spawn point. In case of minions, they are simply deleted.
	 */
	public boolean returnHome()
	{
		// Do nothing if the Attackable is already dead.
		if (isDead())
			return false;
		
		// Minions are simply squeezed if they lose activity.
		if (isMinion() && !isRaidRelated())
		{
			deleteMe();
			return true;
		}
		
		// For regular Attackable, we check if a spawn exists, and if we're far from it (using drift range).
		if (getSpawn() != null && !isInsideRadius(getSpawn().getLocX(), getSpawn().getLocY(), getDriftRange(), false))
		{
			cleanAllHate();
			
			setIsReturningToSpawnPoint(true);
			setWalking();
			getAI().setIntention(CtrlIntention.MOVE_TO, getSpawn().getLoc());
			return true;
		}
		return false;
	}
	
	public int getDriftRange()
	{
		return Config.MAX_DRIFT_RANGE;
	}
	
	public final Set<Creature> getAttackByList()
	{
		return _attackByList;
	}
	
	public final Map<Creature, AggroInfo> getAggroList()
	{
		return _aggroList;
	}
	
	public final boolean isReturningToSpawnPoint()
	{
		return _isReturningToSpawnPoint;
	}
	
	public final void setIsReturningToSpawnPoint(boolean value)
	{
		_isReturningToSpawnPoint = value;
	}
	
	public boolean canSeeThroughSilentMove()
	{
		return _seeThroughSilentMove;
	}
	
	public void seeThroughSilentMove(boolean val)
	{
		_seeThroughSilentMove = val;
	}
	
	/**
	 * @return the active weapon of this L2Attackable (= null).
	 */
	public ItemInstance getActiveWeapon()
	{
		return null;
	}
	
	/**
	 * @return leader of this minion or null.
	 */
	public Attackable getMaster()
	{
		return null;
	}
	
	public boolean isGuard()
	{
		return false;
	}
	
	@Override
	public void addKnownObject(WorldObject object)
	{
		if (object instanceof Player && getAI().getDesire().getIntention() == CtrlIntention.IDLE)
			getAI().setIntention(CtrlIntention.ACTIVE, null);
	}
	
	@Override
	public void removeKnownObject(WorldObject object)
	{
		super.removeKnownObject(object);
		
		// remove object from agro list
		if (object instanceof Creature)
			getAggroList().remove(object);
	}
}