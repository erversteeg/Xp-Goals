package com.ericversteeg;

import com.ericversteeg.bar.*;
import com.ericversteeg.config.AnchorType;
import com.ericversteeg.config.StackOrientation;
import com.ericversteeg.goal.Goal;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.SkillColor;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.ComponentConstants;
import net.runelite.client.ui.overlay.components.TextComponent;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

class XpGoalsOverlay extends Overlay {

	private int ICON_SIZE = 16;

	private final Client client;
	private final XpGoalsPlugin plugin;
	private final XpGoalsConfig config;
	private final SkillIconManager iconManager;

	Widget viewportWidget;

	private Font font;

	private Color outerBorderColor = new Color(57, 41, 13, 124);
	private Color innerBorderColor = new Color(147, 141, 130, 37);
	private Color pastProgressBgColor = new Color(30, 30, 30, 125);
	private Color barBackgroundColor = Color.decode("#1b1b1b");
	private Color barBorderColor = Color.decode("#0b0b0b");

	int panelTopPadding = 4;
	int panelBottomPadding = 4;
	int panelHPadding = 4;

	int iconRightPadding = 3;

	private int panelX;
	private int panelY;
	private int panelWidth;
	private int panelHeight;

	private int topSectionHeight;

	boolean textOutsideOverride = false;
	boolean hideTextOverride = false;

	private int anchorX;
	private int anchorY;

	private int tooltipWidth = 120;
	int tooltipHeight = 120;

	@Inject
	private XpGoalsOverlay(
			Client client,
			XpGoalsPlugin plugin,
			XpGoalsConfig config,
			SkillIconManager iconManager
	) {
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		this.iconManager = iconManager;

		try {
			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
			InputStream inRunescapeSmall = FontManager.class.getResourceAsStream("runescape_small.ttf");
			Font smallFont = Font.createFont(Font.TRUETYPE_FONT, inRunescapeSmall)
					.deriveFont(Font.PLAIN,  12);
			ge.registerFont(smallFont);
			font = smallFont;
		}
		catch (Exception e)
		{
			font = FontManager.getRunescapeSmallFont();
		}

		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.UNDER_WIDGETS);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		viewportWidget = getViewportWidget();

		List<Goal> goals = getTrackedGoals();

		net.runelite.api.Point mouse = client.getMouseCanvasPosition();
		int mouseX = mouse.getX();
		int mouseY = mouse.getY();

		boolean hideIcons = config.hideSkillIcons();
		if (hideIcons)
		{
			ICON_SIZE = 0;
		}
		else
		{
			ICON_SIZE = 16;
		}

		AnchorType anchorType = config.anchorType();

		BarTextType textType = config.barTextType();
		BarTextPosition textPosition = config.barTextPosition();
		BarTextSize textSize = config.barTextSize();

		int barWidth = Math.max(Math.min(config.barWidth(), 1000), 45);
		int barHeight = Math.max(config.barHeight(), 2);

		int span = config.stackSize();

		int minBarSpacing = 0;
		if ((textPosition == BarTextPosition.OUTSIDE || textOutsideOverride) && textType != BarTextType.NONE)
		{
			if (textSize == BarTextSize.SMALL)
			{
				minBarSpacing = 13;
			}
			else
			{
				minBarSpacing = 16;
			}
		}
		else if (barHeight < ICON_SIZE + 4)
		{
			minBarSpacing = (ICON_SIZE - barHeight) / 2 + 2;
		}

		if (config.pastProgressSpan() > 4)
		{
			tooltipWidth = 140;
			tooltipHeight = 140;
		}

		int barSpacing = config.barSpacing() + minBarSpacing;

		boolean hideLabel = config.hideLabel();

		if (!goals.isEmpty())
		{
			if (hideLabel)
			{
				if (textPosition == BarTextPosition.OUTSIDE)
				{
					if (textSize == BarTextSize.SMALL)
					{
						topSectionHeight = 9;
					}
					else
					{
						topSectionHeight = 12;
					}
				}
				else
				{
					topSectionHeight = 0;
				}
			}
			else
			{
				topSectionHeight = 18;
				if ((textPosition == BarTextPosition.OUTSIDE || textOutsideOverride)
						&& textType != BarTextType.NONE && !hideTextOverride)
				{
					if (textSize == BarTextSize.SMALL)
					{
						topSectionHeight += 13;
					}
					else
					{
						topSectionHeight += 16;
					}
				}
			}

			int extraBottomPadding = 0;
			if (barHeight < ICON_SIZE)
			{
				extraBottomPadding = (ICON_SIZE - barHeight) / 2;
			}

			if (config.stackOrientation() == StackOrientation.VERTICAL)
			{
				panelWidth = (barWidth + ICON_SIZE + iconRightPadding + barSpacing) *
						((goals.size() - 1) / span + 1) + panelHPadding * 2 - barSpacing + 5;
			}
			else
			{
				panelWidth = (barWidth + ICON_SIZE + iconRightPadding) * Math.min(goals.size(), span) + barSpacing *
						Math.min(Math.max(span - 1, 1), goals.size()) + panelHPadding * 2;
			}

			if (config.stackOrientation() == StackOrientation.VERTICAL)
			{
				panelHeight =  panelTopPadding + topSectionHeight + barHeight * Math.min(goals.size(), span) + barSpacing *
						Math.min(Math.max(span - 1, 1), goals.size()) + panelBottomPadding + extraBottomPadding;
			}
			else
			{
				panelHeight = panelTopPadding + topSectionHeight + (barHeight + barSpacing) *
						((goals.size() - 1) / span + 1) + panelBottomPadding - barSpacing;
			}

			anchorX = config.anchorX();
			if (anchorType == AnchorType.TOP_RIGHT || anchorType == AnchorType.BOTTOM_RIGHT)
			{
				anchorX = viewportWidget.getCanvasLocation().getX() + viewportWidget.getWidth() + 28 - anchorX - panelWidth;
			}

			anchorY = config.anchorY();
			if (anchorType == AnchorType.BOTTOM_LEFT || anchorType == AnchorType.BOTTOM_RIGHT)
			{
				anchorY = viewportWidget.getCanvasLocation().getY() + viewportWidget.getHeight() + 41 - anchorY - panelHeight;
			}

			panelX = anchorX - panelHPadding;
			panelY = anchorY - panelTopPadding;

			if (config.showPanel())
			{
				if (config.isPanelCorrectionXNegative())
				{
					panelX += config.panelCorrectionX() * -1;
				}
				else
				{
					panelX += config.panelCorrectionX();
				}

				if (config.isPanelCorrectionYNegative())
				{
					panelY += config.panelCorrectionY() * -1;
				}
				else
				{
					panelY += config.panelCorrectionY();
				}

				if (config.isPanelWidthExtensionNegative())
				{
					panelWidth += config.panelWidthExtension() * -1;
				}
				else
				{
					panelWidth += config.panelWidthExtension();
				}

				if (config.isPanelHeightExtensionNegative())
				{
					panelHeight += config.panelHeightExtension() * -1;
				}
				else
				{
					panelHeight += config.panelHeightExtension();
				}

				renderPanel(graphics, panelX, panelY, panelWidth, panelHeight);
			}

			if (!hideLabel)
			{
				renderLabel(graphics);
			}
		}

		Goal tooltipGoal = null;

		int offsetX = panelHPadding;
		int offsetY = topSectionHeight;
		for (int i = 0; i < goals.size(); i++)
		{
			Goal goal = goals.get(i);
			if (goal.enabled)
			{
				Rectangle2D rectangle;
				if (barHeight < ICON_SIZE)
				{
					rectangle = new Rectangle2D.Float(
							panelX + offsetX,
							anchorY + offsetY - (ICON_SIZE - barHeight) / 2f,
							barWidth + ICON_SIZE + iconRightPadding + barSpacing,
							ICON_SIZE
					);
				}
				else
				{
					rectangle = new Rectangle2D.Float(
							panelX + offsetX,
							anchorY + offsetY,
							barWidth + ICON_SIZE + iconRightPadding + barSpacing,
							barHeight
					);
				}

				if (rectangle.contains(mouseX, mouseY))
				{
					tooltipGoal = goal;
				}

				float progress = getXpProgress(
						goal.progressXp, goal.goalXp);

				renderSkillIcon(graphics, offsetX, offsetY, goal);

				renderXpBar(
						graphics,
						barWidth,
						offsetX,
						offsetY,
						progress,
						goal
				);

				renderBarText(graphics, offsetX, offsetY, goal, progress, tooltipGoal == goal);

				// vertical stack
				if (config.stackOrientation() == StackOrientation.VERTICAL)
				{
					if ((i + 1) % span == 0)
					{
						offsetX += barWidth + ICON_SIZE + iconRightPadding + barSpacing;
						offsetY = topSectionHeight;
					}
					else
					{
						offsetY += barHeight + barSpacing;
					}
				}
				else
				{
					if ((i + 1) % span == 0)
					{
						offsetX = panelHPadding;
						offsetY += barHeight + barSpacing;
					}
					else
					{
						offsetX += barWidth + ICON_SIZE + iconRightPadding + barSpacing;
					}
				}
			}
		}

		if (tooltipGoal != null && config.showTooltip())
		{
			int x = mouseX + 15;
			int y = mouseY + 15;

			if (anchorType == AnchorType.TOP_RIGHT || anchorType == AnchorType.BOTTOM_RIGHT)
			{
				x -= tooltipWidth + 30;
			}

			if (anchorType == AnchorType.BOTTOM_LEFT || anchorType == AnchorType.BOTTOM_RIGHT)
			{
				y -= tooltipHeight + 30;
			}

			renderTooltip(graphics, x, y, tooltipGoal);
		}

		return null;
	}

	private void renderPanel(Graphics2D graphics, int x, int y, int width, int height)
	{
		graphics.setColor(ComponentConstants.STANDARD_BACKGROUND_COLOR);
		graphics.fillRect(x, y, width, height);

		graphics.setColor(outerBorderColor);
		graphics.drawRect(x, y, width, height);

		graphics.setColor(outerBorderColor);
		graphics.drawRect(x - 1, y - 1, width + 2, height + 2);

		graphics.setColor(innerBorderColor);
		graphics.drawRect(x + 1, y + 1, width - 2, height - 2);
	}

	private void renderLabel(Graphics2D graphics)
	{
		String label = config.labelText();
		FontMetrics fontMetrics = graphics.getFontMetrics();

		TextComponent textComponent = new TextComponent();
		textComponent.setFont(FontManager.getRunescapeFont());
		textComponent.setText(label);
		textComponent.setColor(Color.GREEN);
		textComponent.setPosition(new Point(
				panelWidth / 2 - fontMetrics.stringWidth(label) / 2 + panelX,
				anchorY + panelTopPadding + fontMetrics.getHeight() - 2)
		);
		textComponent.render(graphics);
	}

	private void renderSkillIcon(Graphics2D graphics2D, int offsetX, int offsetY, Goal goal)
	{
		if (ICON_SIZE == 0) return;

		Skill skill = getSkillForId(goal.skillId);
		if (skill == null) return;

		BufferedImage icon = iconManager.getSkillImage(skill);
		icon = ImageUtil.resizeImage(icon, ICON_SIZE, ICON_SIZE, true);

		int x = anchorX + offsetX;

		int barHeight = Math.max(config.barHeight(), 2);
		int y = anchorY + offsetY - (ICON_SIZE - barHeight) / 2;

		graphics2D.drawImage(icon, x, y, null);
	}

	private void renderXpBar(Graphics2D graphics, int barWidth, int offsetX,
							 int offsetY, float progress, Goal goal)
	{
		Color progressColor;

		Skill skill = plugin.skillForSkillId(goal.skillId);
		if (skill != null)
		{
			progressColor = SkillColor.find(skill).getColor();
		}
		else
		{
			progressColor = Color.YELLOW;
		}

		Color backColor = barBackgroundColor;
		Color frontColor = progressColor;
		Color overfillColor = config.overfillColor();

		float relPercent = progress;

		if (progress > 1 && config.enableOverfill())
		{
			backColor = progressColor;
			frontColor = overfillColor;
			relPercent = progress - 1;

			if (relPercent > 1)
			{
				relPercent = 1;
			}
		}
		else if (progress > 1)
		{
			relPercent = 1;
		}

		int h = Math.max(config.barHeight(), 2);

		int x = anchorX + offsetX + ICON_SIZE + iconRightPadding;
		if (config.hideSkillIcons())
		{
			x -= iconRightPadding;
		}

		int y = anchorY + offsetY;

		graphics.setColor(backColor);
		graphics.fillRect(x, y, barWidth, h);

		graphics.setColor(frontColor);
		graphics.fillRect(x, y, (int) (relPercent * barWidth), h);

		graphics.setColor(barBorderColor);
		graphics.drawRect(x, y, barWidth, h);
	}

	private void renderBarText(Graphics2D graphics, int offsetX, int offsetY, Goal goal,
							   float progress, boolean isMouseover)
	{
		FontMetrics fontMetrics;
		TextComponent textComponent = new TextComponent();

		BarTextSize textSize = config.barTextSize();
		BarTextPosition position = config.barTextPosition();
		BarTextAlignment textAlignment = config.barTextAlignment();

		if (textSize == BarTextSize.SMALL)
		{
			graphics.setFont(font);
		}
		else
		{
			graphics.setFont(FontManager.getRunescapeSmallFont());
		}

		fontMetrics = graphics.getFontMetrics();

		int barH = Math.max(config.barHeight(), 2);

		textOutsideOverride = false;

		if (position == BarTextPosition.INSIDE)
		{
			if (barH < fontMetrics.getHeight())
			{
				graphics.setFont(font);
				fontMetrics = graphics.getFontMetrics();

				if (barH < fontMetrics.getHeight())
				{
					textOutsideOverride = true;
					if (textSize == BarTextSize.LARGE)
					{
						graphics.setFont(FontManager.getRunescapeSmallFont());
						fontMetrics = graphics.getFontMetrics();
					}
				}
			}
		}

		int h = fontMetrics.getHeight();

		String preciseFormatStr = "%.3f%%";

		Color textColor = Color.WHITE;
		String text = "";

		BarTextType textType = config.barTextType();
		if (isMouseover)
		{
			textType = config.mouseoverBarTextType();
		}

		switch (textType)
		{
			case FRACTION:
				text = NumberFormat.getInstance(Locale.ENGLISH).format(Math.max(goal.progressXp, 0)) +
						" / " + NumberFormat.getInstance(Locale.ENGLISH).format(goal.goalXp);
				break;
			case PERCENTAGE:
				text = (int) (progress * 100) + "%";
				break;
			case PRECISE_PERCENTAGE:
				text = String.format(preciseFormatStr, progress * 100);
				break;
			case GAINED:
				text = NumberFormat.getInstance(Locale.ENGLISH).format(Math.max(goal.progressXp, 0));
				break;
			case REMAINING:
				text = NumberFormat.getInstance(Locale.ENGLISH).format(goal.goalXp - Math.max(goal.progressXp, 0));
				break;
		}

		DoneTextType doneTextType = config.doneTextType();
		if (progress >= 1 && textType != BarTextType.NONE && doneTextType != DoneTextType.NONE)
		{
			text = doneTextType.getText();
		}

		if (config.includeResetType())
		{
			String label;
			switch (goal.resetType)
			{
				case Goal.resetHourly:
					label = "H";
					break;
				case Goal.resetDaily:
					label = "D";
					break;
				case Goal.resetWeekly:
					label = "W";
					break;
				case Goal.resetMonthly:
					label = "M";
					break;
				case Goal.resetYearly:
					label = "Y";
					break;
				case Goal.resetNone:
					label = "N";
					break;
				default:
					label = "";
			}
			text += " (" + label + ")";
		}

		if (position == BarTextPosition.INSIDE)
		{
			if (textAlignment == BarTextAlignment.LEADING)
			{
				text = "  " + text;
			}
			else if (textAlignment == BarTextAlignment.TRAILING)
			{
				text = text + "  ";
			}
		}

		int barW = Math.max(Math.min(config.barWidth(), 1000), 45);
		int w = fontMetrics.stringWidth(text);

		hideTextOverride = false;
		if (w > barW)
		{
			graphics.setFont(font);
			fontMetrics = graphics.getFontMetrics();

			w = fontMetrics.stringWidth(text);

			if (w > barW)
			{
				hideTextOverride = true;
				text = "";
			}
		}

		int x = 0;
		int y;

		int iconPadding = iconRightPadding;
		if (config.hideSkillIcons())
		{
			iconPadding = 0;
		}

		switch (textAlignment)
		{
			case LEADING:
				x = anchorX + offsetX + ICON_SIZE + iconPadding + 1;
				break;
			case CENTER:
				x = anchorX + offsetX + ICON_SIZE + iconPadding + (barW - w) / 2;
				break;
			case TRAILING:
				x = anchorX + offsetX + ICON_SIZE + iconPadding + barW - w;
		}

		if (position == BarTextPosition.OUTSIDE || textOutsideOverride)
		{
			y = anchorY + offsetY - 1;
		}
		else
		{
			y = anchorY + offsetY + (barH - h) / 2 + h;
		}

		textComponent.setText(text);
		textComponent.setPosition(new Point(x, y));
		textComponent.setColor(textColor);
		textComponent.render(graphics);
	}

	private void renderTooltip(Graphics2D graphics, int x, int y, Goal goal)
	{
		int w = tooltipWidth;
		int h = tooltipHeight;

		int border = 2;

		int span = Math.min(config.pastProgressSpan(), 5);
		span = Math.max(span, 1);

		if (span > 4)
		{
			w += 20;
			h += 20;
		}

		w -= (w - border * 2) % span;
		h -= (h - border * 2) % span;

		renderPanel(graphics, x, y, w, h);

		int xx = x + border;
		int yy = y + border;
		int ww = w - border * 2;
		int hh = h - border * 2;

		int cW = ww / span;
		int rH = hh / span;

		List<Float> pastProgress = goal.pastProgress;
		if (pastProgress == null || pastProgress.isEmpty())
		{
			String text = "No resets yet";

			TextComponent textComponent = new TextComponent();
			textComponent.setFont(FontManager.getRunescapeSmallFont());
			textComponent.setText(text);
			textComponent.setColor(Color.WHITE);

			graphics.setFont(FontManager.getRunescapeSmallFont());
			FontMetrics fontMetrics = graphics.getFontMetrics();

			textComponent.setPosition(new Point(
					x + (w - fontMetrics.stringWidth(text)) / 2,
					y + (h - fontMetrics.getHeight()) / 2 + fontMetrics.getHeight()
			));

			textComponent.render(graphics);

			return;
		}

		for (int r = 0; r < span; r++)
		{
			for (int c = 0; c < span; c++)
			{
				if (r * span + c < pastProgress.size())
				{
					float progress = pastProgress.get(r * span + c);

					renderPastProgressItem(
							graphics,
							xx + cW * c,
							yy + rH * r,
							cW,
							rH,
							goal,
							progress
					);
				}
			}
		}
	}

	private void renderPastProgressItem(Graphics2D graphics, int x, int y, int w, int h, Goal goal, float progress)
	{
		Color color = SkillColor.find(plugin.skillForSkillId(goal.skillId)).getColor();
		graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 125));

		//System.out.println("x = " + x + ", y = " + y + ", w = " + w + ", h = " + h + "yy = " + y + (h - (int) (h * progress)) + ", hh = " + (int) (h * progress));

		float percentToFill = Math.min(progress, 1);

		graphics.fillRect(x, y + (h - (int) (h * percentToFill)), w, (int) (h * percentToFill));

		graphics.setFont(FontManager.getRunescapeSmallFont());
		FontMetrics fontMetrics = graphics.getFontMetrics();

		String text = (int) (Math.min(progress, 9.99f) * 100) + "%";

		TextComponent textComponent = new TextComponent();
		textComponent.setFont(FontManager.getRunescapeSmallFont());
		textComponent.setText(text);
		textComponent.setPosition(new Point(x + (w - fontMetrics.stringWidth(text)) / 2, y + (h - fontMetrics.getHeight()) / 2 + 13));
		if (progress >= 1)
		{
			textComponent.setColor(Color.GREEN);
		}
		else
		{
			textComponent.setColor(Color.WHITE);
		}
		textComponent.render(graphics);
	}

	private Widget getViewportWidget()
	{
		Widget widget;

		widget = client.getWidget(ComponentID.RESIZABLE_VIEWPORT_INTERFACE_CONTAINER);
		if (widget != null) return widget;

		widget = client.getWidget(ComponentID.RESIZABLE_VIEWPORT_BOTTOM_LINE_INTERFACE_CONTAINER);
		if (widget != null) return widget;

		widget = client.getWidget(ComponentID.FIXED_VIEWPORT_INTERFACE_CONTAINER);
		if (widget != null) return widget;

		return client.getWidget(ComponentID.BANK_INVENTORY_ITEM_CONTAINER);
	}

	float getXpProgress(int progressXp, int goalXp)
	{
		if (goalXp == 0)
		{
			if (progressXp - goalXp <= 0)
			{
				return 1f;
			}
			else
			{
				return 2f;
			}
		}

		return Math.max(progressXp, 0) / (float) goalXp;
	}

	List<Goal> getTrackedGoals()
	{
		List<Goal> tracked = new LinkedList<>();

		for (Goal goal: plugin.goalData.goals)
		{
			if (goal.track)
			{
				tracked.add(goal);
			}
		}
		switch (config.sortType())
		{
			case PERCENTAGE:
				tracked = sortByPercent(tracked);
				break;
			case PERCENTAGE_FLIP:
				List<Goal> sortedPercent = sortByPercent(tracked);
				Collections.reverse(sortedPercent);
				tracked = sortedPercent;
				break;
			case XP_GAINED:
				tracked = sortByXpGained(tracked);
				break;
			case XP_GAINED_FLIP:
				List<Goal> sortedXpGained = sortByXpGained(tracked);
				Collections.reverse(sortedXpGained);
				tracked = sortedXpGained;
				break;
			case XP_REMAINING:
				tracked = sortByXpLeft(tracked);
				break;
			case XP_REMAINING_FLIP:
				List<Goal> sortedXpLeft = sortByXpLeft(tracked);
				Collections.reverse(sortedXpLeft);
				tracked = sortedXpLeft;
				break;
		}

		int maxGoals = config.maxGoals();
		if (maxGoals == 0)
		{
			return tracked;
		}
		else
		{
			int eIndex = Math.min(maxGoals, tracked.size());
			return tracked.subList(0, eIndex);
		}
	}

	private List<Goal> sortByPercent(List<Goal> goals)
	{
		return goals.stream().sorted((obj, other) -> {
			int progress = (int) (getXpProgress(
					obj.progressXp,
					obj.goalXp
			) * 100);

			if (progress >= 100 && progress < 200) progress = -2;
			else if (progress >= 200) progress = -1;

			int otherProgress = (int) (getXpProgress(
					other.progressXp,
					other.goalXp
			) * 100);

			if (otherProgress >= 100 && otherProgress < 200) otherProgress = -2;
			else if (otherProgress >= 200) otherProgress = -1;

			return otherProgress - progress;
		}).collect(Collectors.toList());
	}

	private List<Goal> sortByXpGained(List<Goal> goals)
	{
		return goals.stream().sorted((obj, other) -> {
			int xpGained = obj.progressXp;
			int otherXpGained = other.progressXp;

			return otherXpGained - xpGained;
		}).collect(Collectors.toList());
	}

	private List<Goal> sortByXpLeft(List<Goal> goals)
	{
		return goals.stream().sorted((obj, other) -> {
			int xpLeft = obj.goalXp - obj.progressXp;
			int otherXpLeft = other.goalXp - other.progressXp;

			return otherXpLeft - xpLeft;
		}).collect(Collectors.toList());
	}

	Skill getSkillForId(int skillId)
	{
		if (skillId == Skill.ATTACK.ordinal()) return Skill.ATTACK;
		else if (skillId == Skill.STRENGTH.ordinal()) return Skill.STRENGTH;
		else if (skillId == Skill.DEFENCE.ordinal()) return Skill.DEFENCE;
		else if (skillId == Skill.RANGED.ordinal()) return Skill.RANGED;
		else if (skillId == Skill.PRAYER.ordinal() )return Skill.PRAYER;
		else if (skillId == Skill.MAGIC.ordinal()) return Skill.MAGIC;
		else if (skillId == Skill.RUNECRAFT.ordinal()) return Skill.RUNECRAFT;
		else if (skillId == Skill.CONSTRUCTION.ordinal()) return Skill.CONSTRUCTION;
		else if (skillId == Skill.HITPOINTS.ordinal()) return Skill.HITPOINTS;
		else if (skillId == Skill.AGILITY.ordinal()) return Skill.AGILITY;
		else if (skillId == Skill.HERBLORE.ordinal()) return Skill.HERBLORE;
		else if (skillId == Skill.THIEVING.ordinal()) return Skill.THIEVING;
		else if (skillId == Skill.CRAFTING.ordinal()) return Skill.CRAFTING;
		else if (skillId == Skill.FLETCHING.ordinal()) return Skill.FLETCHING;
		else if (skillId == Skill.SLAYER.ordinal()) return Skill.SLAYER;
		else if (skillId == Skill.HUNTER.ordinal()) return Skill.HUNTER;
		else if (skillId == Skill.MINING.ordinal()) return Skill.MINING;
		else if (skillId == Skill.SMITHING.ordinal()) return Skill.SMITHING;
		else if (skillId == Skill.FISHING.ordinal()) return Skill.FISHING;
		else if (skillId == Skill.COOKING.ordinal()) return Skill.COOKING;
		else if (skillId == Skill.FIREMAKING.ordinal()) return Skill.FIREMAKING;
		else if (skillId == Skill.WOODCUTTING.ordinal()) return Skill.WOODCUTTING;
		else if (skillId == Skill.FARMING.ordinal()) return Skill.FARMING;
		else return null;
	}
}
