package net.runelite.client.plugins.aiassistant.ai;

import net.runelite.client.plugins.aiassistant.data.PlayerSnapshot;

import java.util.Map;

/**
 * Builds context-rich prompts from player data.
 */
public class PromptBuilder
{
	/**
	 * Builds a comprehensive context string from a player snapshot.
	 *
	 * @param snapshot The player snapshot
	 * @return Formatted context string
	 */
	public static String buildContext(PlayerSnapshot snapshot)
	{
		if (snapshot == null)
		{
			return "No player data available.";
		}

		StringBuilder context = new StringBuilder();

		// Basic player info
		context.append("=== PLAYER INFORMATION ===\n");
		context.append(String.format("Name: %s\n", snapshot.getPlayerName()));
		context.append(String.format("Combat Level: %d\n", snapshot.getCombatLevel()));
		context.append(String.format("Location: %s\n", snapshot.getLocationName()));
		context.append(String.format("Quest Points: %d\n", snapshot.getQuestPoints()));
		context.append("\n");

		// Combat stats
		context.append("=== COMBAT STATUS ===\n");
		context.append(String.format("Health: %d/%d\n", snapshot.getCurrentHealth(), snapshot.getMaxHealth()));
		context.append(String.format("Prayer: %d/%d\n", snapshot.getCurrentPrayer(), snapshot.getMaxPrayer()));
		context.append(String.format("Special Attack: %d%%\n", snapshot.getSpecialAttackPercent()));
		context.append(String.format("Run Energy: %d%%\n", snapshot.getRunEnergy()));
		context.append(String.format("In Combat: %s\n", snapshot.isInCombat() ? "Yes" : "No"));
		context.append("\n");

		// Skills
		context.append("=== SKILLS ===\n");
		if (snapshot.getSkillLevels() != null)
		{
			for (Map.Entry<String, Integer> entry : snapshot.getSkillLevels().entrySet())
			{
				String skillName = entry.getKey();
				Integer level = entry.getValue();
				Long xp = snapshot.getSkillExperience() != null ?
					snapshot.getSkillExperience().get(skillName) : null;

				if (xp != null)
				{
					context.append(String.format("%s: Level %d (XP: %,d)\n", skillName, level, xp));
				}
				else
				{
					context.append(String.format("%s: Level %d\n", skillName, level));
				}
			}
		}
		context.append("\n");

		// Equipment
		context.append("=== EQUIPMENT ===\n");
		if (snapshot.getEquipment() != null && !snapshot.getEquipment().isEmpty())
		{
			for (Map.Entry<String, String> entry : snapshot.getEquipment().entrySet())
			{
				context.append(String.format("%s: %s\n", entry.getKey(), entry.getValue()));
			}
		}
		else
		{
			context.append("No equipment worn\n");
		}
		context.append("\n");

		// Inventory
		context.append("=== INVENTORY ===\n");
		if (snapshot.getInventory() != null && !snapshot.getInventory().isEmpty())
		{
			for (Map.Entry<String, Integer> entry : snapshot.getInventory().entrySet())
			{
				String itemName = entry.getKey();
				Integer quantity = entry.getValue();
				if (quantity > 1)
				{
					context.append(String.format("%s x%d\n", itemName, quantity));
				}
				else
				{
					context.append(String.format("%s\n", itemName));
				}
			}
		}
		else
		{
			context.append("Inventory is empty\n");
		}
		context.append("\n");

		// Bank (if available)
		if (snapshot.getBank() != null && !snapshot.getBank().isEmpty())
		{
			context.append("=== BANK (Summary) ===\n");
			context.append(String.format("Total unique items: %d\n", snapshot.getBank().size()));

			// Show notable items (high value or important items)
			int itemsShown = 0;
			for (Map.Entry<String, Integer> entry : snapshot.getBank().entrySet())
			{
				if (itemsShown >= 10) break; // Limit to 10 items to keep context manageable

				String itemName = entry.getKey();
				Integer quantity = entry.getValue();
				if (quantity > 1)
				{
					context.append(String.format("%s x%d\n", itemName, quantity));
				}
				else
				{
					context.append(String.format("%s\n", itemName));
				}
				itemsShown++;
			}

			if (snapshot.getBank().size() > 10)
			{
				context.append(String.format("... and %d more items\n", snapshot.getBank().size() - 10));
			}
			context.append("\n");
		}

		return context.toString();
	}

	/**
	 * Builds a brief summary context from a player snapshot.
	 * Useful when full context is too large.
	 *
	 * @param snapshot The player snapshot
	 * @return Brief formatted context string
	 */
	public static String buildBriefContext(PlayerSnapshot snapshot)
	{
		if (snapshot == null)
		{
			return "No player data available.";
		}

		StringBuilder context = new StringBuilder();
		context.append(String.format("Player: %s (Combat Level %d)\n",
			snapshot.getPlayerName(), snapshot.getCombatLevel()));
		context.append(String.format("Location: %s\n", snapshot.getLocationName()));
		context.append(String.format("Quest Points: %d\n", snapshot.getQuestPoints()));

		// Key combat stats
		if (snapshot.getSkillLevels() != null)
		{
			context.append(String.format("Attack: %d, Strength: %d, Defence: %d, Ranged: %d, Magic: %d\n",
				snapshot.getSkillLevels().getOrDefault("Attack", 1),
				snapshot.getSkillLevels().getOrDefault("Strength", 1),
				snapshot.getSkillLevels().getOrDefault("Defence", 1),
				snapshot.getSkillLevels().getOrDefault("Ranged", 1),
				snapshot.getSkillLevels().getOrDefault("Magic", 1)));
		}

		return context.toString();
	}
}
