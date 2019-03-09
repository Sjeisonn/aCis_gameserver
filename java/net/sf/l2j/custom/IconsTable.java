package net.sf.l2j.custom;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import net.sf.l2j.commons.data.xml.IXmlReader;
import net.sf.l2j.commons.logging.CLogger;

import net.sf.l2j.gameserver.templates.StatsSet;

import org.w3c.dom.Document;

/**
 * @author SweeTs
 */
public final class IconsTable implements IXmlReader
{
	private static final Map<Integer, String> itemIcons = new HashMap<>();
	private static final CLogger LOGGER = new CLogger(IconsTable.class.getName());
	
	protected IconsTable()
	{
		load();
	}
	
	@Override
	public void load()
	{
		final long start = System.currentTimeMillis();
		parseFile("./data/xml/icons.xml");
		LOGGER.info(getClass().getSimpleName() + ": Succesfully loaded {} icons, in {} milliseconds.", itemIcons.size(), System.currentTimeMillis() - start);
	}
	
	@Override
	public void parseDocument(Document doc, Path path)
	{
		forEach(doc, "list", listNode ->
		{
			forEach(listNode, "icon", accessNode ->
			{
				final StatsSet set = parseAttributes(accessNode);
				itemIcons.put(set.getInteger("id"), set.getString("value"));
			});
		});
	}
	
	/**
	 * @param id the requested itemId
	 * @return the String value of the Icon of the given itemId.
	 */
	public static String getIcon(final int id)
	{
		final String icon = itemIcons.get(id);
		if (icon == null)
		{
			LOGGER.warn("IconsTable: Invalid item icon request: {}, or it doesn't exist, ignoring ...", id);
			return "icon.noimage";
		}
		return icon;
	}
	
	public static final IconsTable getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final IconsTable _instance = new IconsTable();
	}
}