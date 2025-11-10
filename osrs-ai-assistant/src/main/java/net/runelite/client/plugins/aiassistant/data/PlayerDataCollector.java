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
	 * This is a simplified implementation - a full version would use region names.
	 */
	private String getLocationName(WorldPoint point)
	{
		int x = point.getX();
		int y = point.getY();
		int plane = point.getPlane();

		// Very basic location detection - could be enhanced with proper region mapping
		if (x >= 3200 && x <= 3230 && y >= 3200 && y <= 3230)
		{
			return "Lumbridge";
		}
		else if (x >= 3080 && x <= 3120 && y >= 3440 && y <= 3480)
		{
			return "Edgeville";
		}
		else if (x >= 3000 && x <= 3100 && y >= 3370 && y <= 3400)
		{
			return "Varrock";
		}
		else if (x >= 2940 && x <= 2980 && y >= 3360 && y <= 3400)
		{
			return "Falador";
		}
		else if (x >= 3230 && x <= 3260 && y >= 3410 && y <= 3450)
		{
			return "Grand Exchange";
		}

		return String.format("Coordinates(%d, %d, %d)", x, y, plane);
	}
}
