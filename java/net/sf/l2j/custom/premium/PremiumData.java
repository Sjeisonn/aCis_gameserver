package net.sf.l2j.custom.premium;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.l2j.commons.data.xml.IXmlReader;

import net.sf.l2j.gameserver.templates.StatsSet;

import org.w3c.dom.Document;

/**
 * @author SweeTs
 */
public final class PremiumData implements IXmlReader
{
	private final Map<Integer, PremiumLevel> _premiumLevels = new ConcurrentHashMap<>();
	
	protected PremiumData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		parseFile("./data/xml/premium.xml");
		LOGGER.info(getClass().getSimpleName() + ": Loaded {} premium levels.", _premiumLevels.size());
	}
	
	@Override
	public void parseDocument(Document doc, Path path)
	{
		forEach(doc, "list", listNode -> forEach(listNode, "package", packageNode ->
		{
			final StatsSet set = parseAttributes(packageNode);
			forEach(packageNode, "stats", node -> set.putAll(parseAttributes(node)));
			_premiumLevels.put(set.getInteger("id"), new PremiumLevel(set));
		}));
	}
	
	public void reload()
	{
		_premiumLevels.clear();
		load();
	}
	
	/**
	 * @param level : The level to check.
	 * @return the {@link PremiumLevel} based on its level.
	 */
	public PremiumLevel getPremiumLevel(int level)
	{
		return _premiumLevels.get(level);
	}
	
	public static final PremiumData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static final class SingletonHolder
	{
		protected static final PremiumData INSTANCE = new PremiumData();
	}
}