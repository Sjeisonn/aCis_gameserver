package net.sf.l2j.custom.premium;

/**
 * @author SweeTs
 */
public enum PremiumType
{
	NONE(0),
	BRONZE(1),
	SILVER(2),
	GOLD(3);
	
	private final int premiumId;
	
	private PremiumType(final int id)
	{
		premiumId = id;
	}
	
	public int getId()
	{
		return premiumId;
	}
}