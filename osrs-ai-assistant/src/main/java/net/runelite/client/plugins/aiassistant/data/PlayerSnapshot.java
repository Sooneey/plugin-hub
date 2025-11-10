package net.runelite.client.plugins.aiassistant.data;

import lombok.Builder;
import lombok.Data;
import java.util.Map;
import java.util.List;

/**
 * Represents a complete snapshot of player state at a point in time.
 */
@Data
@Builder
public class PlayerSnapshot
{
	// Player basic info
	private String playerName;
	private int combatLevel;
	private long timestamp;

	// Location
	private int worldX;
	private int worldY;
	private int plane;
	private String locationName;

	// Skills (skill name -> level)
	private Map<String, Integer> skillLevels;

	// Skills (skill name -> experience)
	private Map<String, Long> skillExperience;

	// Combat stats
	private int currentHealth;
	private int maxHealth;
	private int currentPrayer;
	private int maxPrayer;
	private int specialAttackPercent;
	private int runEnergy;

	// Quests
	private int questPoints;

	// Inventory items (item name -> quantity)
	private Map<String, Integer> inventory;

	// Equipment items (slot name -> item name)
	private Map<String, String> equipment;

	// Bank items (item name -> quantity)
	private Map<String, Integer> bank;

	// Additional context
	private List<String> activeQuests;
	private String currentAnimation;
	private boolean inCombat;
	private boolean isMoving;
}
