package com.ericversteeg;

import com.ericversteeg.pattern.Pattern;
import com.google.gson.Gson;
import com.google.inject.Provides;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.StatChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.RuneScapeProfileChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoField;
import java.time.temporal.IsoFields;
import java.util.Arrays;
import java.util.Date;

@PluginDescriptor(
	name = "Xp Goals",
	description = "Track xp goals."
)

public class XpGoalsPlugin extends Plugin
{
	@Inject
	private XpGoalsOverlay overlay;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private Client client;

	@Inject
	private XpGoalsConfig config;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ItemManager itemManager;

	@Inject
	private ConfigManager configManager;

	@Inject
	private Gson gson;

	GoalData goalData;

	private String profileKey = "";

	LocalDateTime lastDateTime = null;

	@Override
	protected void startUp() throws Exception
	{
		overlayManager.add(overlay);
	}

	@Override
	protected void shutDown() throws Exception
	{
		overlayManager.remove(overlay);
	}

	@Subscribe
	public void onRuneScapeProfileChanged(RuneScapeProfileChanged e)
	{
		profileKey = configManager.getRSProfileKey();
		if (profileKey != null)
		{
			goalData = getSavedData();

			lastDateTime = LocalDateTime.ofEpochSecond(
					goalData.lastReset, 0,
					ZoneId.systemDefault()
						.getRules()
						.getOffset(
							LocalDate.now()
								.atStartOfDay()
						)
			);

			checkResets();
		}
	}

	@Provides
	XpGoalsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(XpGoalsConfig.class);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged config)
	{
		configSyncGoals();
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		checkResets();
	}

	@Subscribe
	public void onStatChanged(StatChanged statChanged)
	{
		Skill skill = statChanged.getSkill();
		Goal goal = goalForSkillId(skill.ordinal());

		if (goal != null && goal.track)
		{
			int xp = statChanged.getXp();
			if (goal.startXp < 0) { goal.startXp = xp; }
			goal.currentXp = xp;
		}
	}

	private void checkResets()
	{
		if (lastDateTime == null) return;

		LocalDateTime dateTIme = LocalDateTime.now();

		if (dateTIme.get(ChronoField.HOUR_OF_DAY) != lastDateTime.get(ChronoField.HOUR_OF_DAY)
				|| (dateTIme.get(ChronoField.DAY_OF_MONTH) != lastDateTime.get(ChronoField.DAY_OF_MONTH))
				|| (dateTIme.get(ChronoField.MONTH_OF_YEAR) != lastDateTime.get(ChronoField.MONTH_OF_YEAR))
				|| (dateTIme.get(ChronoField.YEAR) != lastDateTime.get(ChronoField.YEAR)))
		{
			resetGoals(Goal.resetHourly);
			configSyncGoals();
		}
		if (dateTIme.get(ChronoField.DAY_OF_MONTH) != lastDateTime.get(ChronoField.DAY_OF_MONTH)
				|| (dateTIme.get(ChronoField.MONTH_OF_YEAR) != lastDateTime.get(ChronoField.MONTH_OF_YEAR))
				|| (dateTIme.get(ChronoField.YEAR) != lastDateTime.get(ChronoField.YEAR)))
		{
			resetGoals(Goal.resetDaily);
		}
		if (dateTIme.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR) != lastDateTime.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
				|| (dateTIme.get(ChronoField.YEAR) != lastDateTime.get(ChronoField.YEAR)))
		{
			resetGoals(Goal.resetWeekly);
		}
		if (dateTIme.get(ChronoField.MONTH_OF_YEAR) != lastDateTime.get(ChronoField.MONTH_OF_YEAR)
				|| (dateTIme.get(ChronoField.YEAR) != lastDateTime.get(ChronoField.YEAR)))
		{
			resetGoals(Goal.resetMonthly);
		}
		if (dateTIme.get(ChronoField.YEAR) != lastDateTime.get(ChronoField.YEAR))
		{
			resetGoals(Goal.resetYearly);
		}

		// check save data
		if (dateTIme.get(ChronoField.MINUTE_OF_HOUR) != lastDateTime.get(ChronoField.MINUTE_OF_HOUR))
		{
			writeSavedData();
		}

		lastDateTime = dateTIme;
	}

	private void resetGoals(int resetType)
	{
		for (Goal goal: goalData.goals)
		{
			if (goal.resetType == resetType)
			{
				goal.reset();
			}
		}

		goalData.lastReset = Instant.now().toEpochMilli() / 1000;
	}

	GoalData getSavedData()
	{
		String profile = configManager.getRSProfileKey();
		String json = configManager.getConfiguration(XpGoalsConfig.GROUP, profile, "xp_goals_data");

		GoalData savedData = gson.fromJson(json, GoalData.class);

		if (savedData == null)
		{
			return new GoalData();
		}
		return savedData;
	}

	void writeSavedData()
	{
		String profile = configManager.getRSProfileKey();

		String json = gson.toJson(goalData);
		configManager.setConfiguration(XpGoalsConfig.GROUP, profile, "xp_goals_data", json);
	}

	void configSyncGoals()
	{
		if (goalData.goals.isEmpty())
		{
			goalData.goals = Arrays.asList(
					new Goal(Skill.ATTACK.ordinal()),
					new Goal(Skill.STRENGTH.ordinal()),
					new Goal(Skill.DEFENCE.ordinal()),
					new Goal(Skill.RANGED.ordinal()),
					new Goal(Skill.PRAYER.ordinal()),
					new Goal(Skill.MAGIC.ordinal()),
					new Goal(Skill.RUNECRAFT.ordinal()),
					new Goal(Skill.CONSTRUCTION.ordinal()),
					new Goal(Skill.HITPOINTS.ordinal()),
					new Goal(Skill.AGILITY.ordinal()),
					new Goal(Skill.HERBLORE.ordinal()),
					new Goal(Skill.THIEVING.ordinal()),
					new Goal(Skill.CRAFTING.ordinal()),
					new Goal(Skill.FLETCHING.ordinal()),
					new Goal(Skill.SLAYER.ordinal()),
					new Goal(Skill.HUNTER.ordinal()),
					new Goal(Skill.MINING.ordinal()),
					new Goal(Skill.SMITHING.ordinal()),
					new Goal(Skill.FISHING.ordinal()),
					new Goal(Skill.COOKING.ordinal()),
					new Goal(Skill.FIREMAKING.ordinal()),
					new Goal(Skill.WOODCUTTING.ordinal()),
					new Goal(Skill.FARMING.ordinal())
			);
		}

		for (Goal goal: goalData.goals)
		{
			goal.resetType = Goal.resetDaily;

			if (goal.skillId == Skill.MINING.ordinal())
			{
				goal.enabled = config.enableMiningSkill();
				goal.track = false;

				String [] patterns = config.miningPattens().split("\n");

				for (String patternStr: patterns)
				{
					if (patternStr.trim().isEmpty())
					{
						continue;
					}

					String pattern = "";
					int goalXp = config.miningXpGoal();

					if (patternStr.contains("="))
					{
						String [] patternParts = patternStr.split("=");
						pattern = patternParts[0].trim();

						goalXp = Integer.parseInt(patternParts[1].trim());
					}
					else
					{
						pattern = patternStr;
					}

					if (Pattern.parse(pattern, new Date()).matches())
					{
						goal.track = true;
						goal.goalXp = goalXp;

						break;
					}
				}
			}
			else if (goal.skillId == Skill.RUNECRAFT.ordinal())
			{
				goal.enabled = config.enableRunecraftingSkill();
				goal.track = false;

				String [] patterns = config.runecraftingPattens().split("\n");

				for (String patternStr: patterns)
				{
					if (patternStr.trim().isEmpty())
					{
						continue;
					}

					String pattern = "";
					int goalXp = config.runecraftingXpGoal();

					if (patternStr.contains("="))
					{
						String [] patternParts = patternStr.split("=");
						pattern = patternParts[0].trim();

						goalXp = Integer.parseInt(patternParts[1].trim());
					}
					else
					{
						pattern = patternStr;
					}

					if (Pattern.parse(pattern, new Date()).matches())
					{
						goal.track = true;
						goal.goalXp = goalXp;

						break;
					}
				}
			}
		}
	}

	Goal goalForSkillId(int skillId)
	{
		for (Goal goal: goalData.goals)
		{
			if (goal.skillId == skillId)
			{
				return goal;
			}
		}
		return null;
	}
}
