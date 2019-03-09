package net.sf.l2j.custom.premium;

import net.sf.l2j.gameserver.templates.StatsSet;

/**
 * @author SweeTs
 */
public class PremiumLevel
{
	private final int id;
	private final PremiumType type;
	private final double exp;
	private final double sp;
	private final double adena;
	private final double items;
	private final int enchant;
	private final int nameColor;
	private final int titleColor;
	
	public PremiumLevel(StatsSet set)
	{
		id = set.getInteger("id");
		type = set.getEnum("type", PremiumType.class);
		exp = set.getDouble("exp", 1.);
		sp = set.getDouble("sp", 1.);
		adena = set.getDouble("adena", 1.);
		items = set.getDouble("items", 1.);
		enchant = set.getInteger("enchant", 0);
		nameColor = Integer.decode("0x" + set.getString("nameColor", "FFFFFF"));
		titleColor = Integer.decode("0x" + set.getString("titleColor", "FFFF77"));
	}
	
	/**
	 * @return the premium id
	 */
	public int getId()
	{
		return id;
	}
	
	/**
	 * @return the premium type
	 */
	public PremiumType getType()
	{
		return type;
	}
	
	/**
	 * @return additional exp value
	 */
	public double getExp()
	{
		return exp;
	}
	
	/**
	 * @return additional sp value
	 */
	public double getSp()
	{
		return sp;
	}
	
	/**
	 * @return additional adena value
	 */
	public double getAdena()
	{
		return adena;
	}
	
	/**
	 * @return additional items value
	 */
	public double getItems()
	{
		return items;
	}
	
	/**
	 * @return additional enchant value
	 */
	public int getEnchant()
	{
		return enchant;
	}
	
	/**
	 * @return VIP name color
	 */
	public int getNameColor()
	{
		return nameColor;
	}
	
	/**
	 * @return VIP title color
	 */
	public int getTitleColor()
	{
		return titleColor;
	}
}