package com.ericversteeg;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class InventoryTotalPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(XpGoalsPlugin.class);
		RuneLite.main(args);
	}
}