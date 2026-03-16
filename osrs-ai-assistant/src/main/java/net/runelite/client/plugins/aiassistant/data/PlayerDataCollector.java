package net.runelite.client.plugins.aiassistant.data;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

/**
 * Collects comprehensive player data from the game client.
 */
@Slf4j
@Singleton
public class PlayerDataCollector
{
	@Inject
	private Client client;

	/**
	 * Collects all available player data and creates a snapshot.
	 *
	 * @return PlayerSnapshot containing all current player data
	 */
	public PlayerSnapshot collectSnapshot()
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			log.warn("Cannot collect snapshot - player not logged in");
			return null;
		}

		Player localPlayer = client.getLocalPlayer();
		if (localPlayer == null)
		{
			log.warn("Cannot collect snapshot - local player is null");
			return null;
		}

		PlayerSnapshot.PlayerSnapshotBuilder builder = PlayerSnapshot.builder();

		// Basic player info
		builder.playerName(client.getLocalPlayer().getName());
		builder.combatLevel(client.getLocalPlayer().getCombatLevel());
		builder.timestamp(System.currentTimeMillis());

		// Location
		WorldPoint location = localPlayer.getWorldLocation();
		if (location != null)
		{
			builder.worldX(location.getX());
			builder.worldY(location.getY());
			builder.plane(location.getPlane());
			builder.locationName(getLocationName(location));
		}

		// Skills
		Map<String, Integer> skillLevels = new HashMap<>();
		Map<String, Long> skillExperience = new HashMap<>();
		for (Skill skill : Skill.values())
		{
			String skillName = skill.getName();
			skillLevels.put(skillName, client.getRealSkillLevel(skill));
			skillExperience.put(skillName, client.getSkillExperience(skill));
		}
		builder.skillLevels(skillLevels);
		builder.skillExperience(skillExperience);

		// Combat stats
		builder.currentHealth(client.getBoostedSkillLevel(Skill.HITPOINTS));
		builder.maxHealth(client.getRealSkillLevel(Skill.HITPOINTS));
		builder.currentPrayer(client.getBoostedSkillLevel(Skill.PRAYER));
		builder.maxPrayer(client.getRealSkillLevel(Skill.PRAYER));
		builder.specialAttackPercent(client.getVarpValue(VarPlayer.SPECIAL_ATTACK_PERCENT) / 10);
		builder.runEnergy(client.getEnergy() / 100);

		// Quest points
		builder.questPoints(client.getVarpValue(VarPlayer.QUEST_POINTS));

		// Inventory
		builder.inventory(collectInventory());

		// Equipment
		builder.equipment(collectEquipment());

		// Bank (only if bank is open)
		builder.bank(collectBank());

		// Additional context
		builder.inCombat(localPlayer.getInteracting() != null);
		builder.isMoving(localPlayer.getIdlePoseAnimation() != localPlayer.getPoseAnimation());

		return builder.build();
	}

	/**
	 * Collects inventory items.
	 */
	private Map<String, Integer> collectInventory()
	{
		Map<String, Integer> inventory = new HashMap<>();
		ItemContainer inventoryContainer = client.getItemContainer(InventoryID.INVENTORY);

		if (inventoryContainer != null)
		{
			Item[] items = inventoryContainer.getItems();
			for (Item item : items)
			{
				if (item.getId() > 0)
				{
					String itemName = client.getItemDefinition(item.getId()).getName();
					inventory.merge(itemName, item.getQuantity(), Integer::sum);
				}
			}
		}

		return inventory;
	}

	/**
	 * Collects equipped items.
	 */
	private Map<String, String> collectEquipment()
	{
		Map<String, String> equipment = new HashMap<>();
		ItemContainer equipmentContainer = client.getItemContainer(InventoryID.EQUIPMENT);

		if (equipmentContainer != null)
		{
			Item[] items = equipmentContainer.getItems();
			for (int i = 0; i < items.length; i++)
			{
				Item item = items[i];
				if (item.getId() > 0)
				{
					String itemName = client.getItemDefinition(item.getId()).getName();
					String slotName = getEquipmentSlotName(i);
					equipment.put(slotName, itemName);
				}
			}
		}

		return equipment;
	}

	/**
	 * Collects bank items (only when bank is open).
	 */
	private Map<String, Integer> collectBank()
	{
		Map<String, Integer> bank = new HashMap<>();
		ItemContainer bankContainer = client.getItemContainer(InventoryID.BANK);

		if (bankContainer != null)
		{
			Item[] items = bankContainer.getItems();
			for (Item item : items)
			{
				if (item.getId() > 0)
				{
					String itemName = client.getItemDefinition(item.getId()).getName();
					bank.merge(itemName, item.getQuantity(), Integer::sum);
				}
			}
		}

		return bank;
	}

	/**
	 * Gets the equipment slot name from the slot index.
	 */
	private String getEquipmentSlotName(int index)
	{
		switch (index)
		{
			case 0: return "Head";
			case 1: return "Cape";
			case 2: return "Amulet";
			case 3: return "Weapon";
			case 4: return "Body";
			case 5: return "Shield";
			case 7: return "Legs";
			case 9: return "Gloves";
			case 10: return "Boots";
			case 12: return "Ring";
			case 13: return "Ammo";
			default: return "Unknown";
		}
	}

	/**
	 * Gets a human-readable location name from coordinates.
	 */
	private String getLocationName(WorldPoint point)
	{
		int x = point.getX();
		int y = point.getY();
		int plane = point.getPlane();

		// Major cities
		if (x >= 3200 && x <= 3230 && y >= 3200 && y <= 3235)
		{
			return "Lumbridge";
		}
		else if (x >= 3075 && x <= 3120 && y >= 3440 && y <= 3500)
		{
			return "Edgeville";
		}
		else if (x >= 3200 && x <= 3290 && y >= 3370 && y <= 3430)
		{
			return "Varrock";
		}
		else if (x >= 2940 && x <= 2985 && y >= 3310 && y <= 3400)
		{
			return "Falador";
		}
		else if (x >= 3000 && x <= 3100 && y >= 3200 && y <= 3260)
		{
			return "Draynor Village";
		}
		else if (x >= 2600 && x <= 2690 && y >= 3270 && y <= 3320)
		{
			return "Ardougne";
		}
		else if (x >= 2440 && x <= 2490 && y >= 3400 && y <= 3450)
		{
			return "Yanille";
		}
		else if (x >= 2500 && x <= 2560 && y >= 2850 && y <= 2900)
		{
			return "Shilo Village";
		}
		else if (x >= 2800 && x <= 2880 && y >= 2700 && y <= 2800)
		{
			return "Karamja";
		}
		else if (x >= 1600 && x <= 1680 && y >= 3800 && y <= 3900)
		{
			return "Zeah (Great Kourend)";
		}

		// Grand Exchange
		else if (x >= 3140 && x <= 3190 && y >= 3460 && y <= 3510)
		{
			return "Grand Exchange";
		}

		// Popular banks
		else if (x >= 3090 && x <= 3100 && y >= 3240 && y <= 3250)
		{
			return "Draynor Bank";
		}
		else if (x >= 2943 && x <= 2950 && y >= 3365 && y <= 3372)
		{
			return "Falador Bank";
		}

		// Training areas
		else if (x >= 3240 && x <= 3280 && y >= 3140 && y <= 3180)
		{
			return "Al Kharid";
		}
		else if (x >= 2640 && x <= 2690 && y >= 3670 && y <= 3720)
		{
			return "Seers' Village";
		}
		else if (x >= 2700 && x <= 2730 && y >= 3700 && y <= 3730)
		{
			return "Camelot";
		}

		// Wilderness
		else if (y >= 3520 && y <= 4000)
		{
			int wildyLevel = (y - 3520) / 8;
			return String.format("Wilderness (Level %d)", wildyLevel);
		}

		// Popular dungeons and areas
		else if (x >= 2438 && x <= 2462 && y >= 5126 && y <= 5166)
		{
			return "Motherlode Mine";
		}
		else if (x >= 3140 && x <= 3260 && y >= 9850 && y <= 9920)
		{
			return "Mining Guild";
		}
		else if (x >= 3000 && x <= 3050 && y >= 9700 && y <= 9800)
		{
			return "Lumbridge Swamp Caves";
		}
		else if (x >= 2100 && x <= 2200 && y >= 5200 && y <= 5350)
		{
			return "Wintertodt Arena";
		}

		// Slayer areas
		else if (x >= 2396 && x <= 2432 && y >= 3050 && y <= 3090)
		{
			return "Brimhaven Dungeon";
		}
		else if (x >= 2425 && x <= 2475 && y >= 9795 && y <= 9855)
		{
			return "Catacombs of Kourend";
		}

		// Bosses
		else if (x >= 2998 && x <= 3006 && y >= 3376 && y <= 3384)
		{
			return "King Black Dragon Lair";
		}
		else if (x >= 3038 && x <= 3072 && y >= 9564 && y <= 9600)
		{
			return "God Wars Dungeon";
		}

		// Minigames
		else if (x >= 3550 && x <= 3580 && y >= 3280 && y <= 3310)
		{
			return "Barrows";
		}
		else if (x >= 2435 && x <= 2475 && y >= 3080 && y <= 3110)
		{
			return "Fight Caves";
		}

		return String.format("Coordinates(%d, %d, %d)", x, y, plane);
	}
}
