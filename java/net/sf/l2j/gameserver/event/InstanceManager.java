package net.sf.l2j.gameserver.event;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Kara`
 *
 */
public class InstanceManager
{
	private final List<Integer> _instances = new ArrayList<>();
	
	public int createInstance(int instanceId)
	{
		if (!_instances.contains(instanceId))
		{
			_instances.add(instanceId);
			return instanceId;
		}
		
		return 0;
	}
	
	public boolean instanceExists(int instanceId)
	{
		return _instances.contains(instanceId);
	}
	
	public int getInstance(int instanceId)
	{
		return _instances.get(instanceId);
	}
	
	public void removeInstance(int instanceId)
	{
		_instances.remove(instanceId);
	}
	
	public static InstanceManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		static InstanceManager _instance = new InstanceManager();
	}
}
