package net.sf.l2j.custom.cafepoint;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import net.sf.l2j.commons.data.xml.IXmlReader;

import net.sf.l2j.gameserver.templates.StatsSet;

import org.w3c.dom.Document;

/**
 * @author SweeTs
 */
public final class PcCafeData implements IXmlReader
{
	private final Map<String, String> _cafeData = new HashMap<>();
	
	protected PcCafeData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		parseFile("./data/xml/pcCafe.xml");
		LOGGER.info("PcCafeData: Loaded {} variables.", _cafeData.size());
	}
	
	@Override
	public void parseDocument(Document doc, Path path)
	{
		forEach(doc, "list", listNode ->
		{
			forEach(listNode, "variable", accessNode ->
			{
				final StatsSet set = parseAttributes(accessNode);
				_cafeData.put(set.getString("name"), set.getString("value"));
			});
		});
	}
	
	public void reload()
	{
		_cafeData.clear();
		load();
	}
	
	public boolean getCafeBool(final String key, final boolean defaultValue)
	{
		final Object val = _cafeData.get(key);
		
		if (val instanceof Boolean)
			return (Boolean) val;
		if (val instanceof String)
			return Boolean.parseBoolean((String) val);
		if (val instanceof Number)
			return ((Number) val).intValue() != 0;
		
		return defaultValue;
	}
	
	public int getCafeInt(final String key, final int defaultValue)
	{
		final Object val = _cafeData.get(key);
		
		if (val instanceof Number)
			return ((Number) val).intValue();
		if (val instanceof String)
			return Integer.parseInt((String) val);
		if (val instanceof Boolean)
			return (Boolean) val ? 1 : 0;
		
		return defaultValue;
	}
	
	public static PcCafeData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final PcCafeData INSTANCE = new PcCafeData();
	}
}