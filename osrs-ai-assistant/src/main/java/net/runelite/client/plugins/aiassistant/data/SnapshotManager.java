package net.runelite.client.plugins.aiassistant.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Manages saving and loading player snapshots to/from disk.
 */
@Slf4j
@Singleton
public class SnapshotManager
{
	private static final String SNAPSHOT_DIR = "aiassistant/snapshots";
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");

	private final Gson gson;
	private final File snapshotDirectory;

	@Inject
	public SnapshotManager()
	{
		this.gson = new GsonBuilder().setPrettyPrinting().create();
		this.snapshotDirectory = new File(RuneLite.RUNELITE_DIR, SNAPSHOT_DIR);

		// Create snapshots directory if it doesn't exist
		if (!snapshotDirectory.exists())
		{
			if (snapshotDirectory.mkdirs())
			{
				log.info("Created snapshot directory: {}", snapshotDirectory.getAbsolutePath());
			}
			else
			{
				log.error("Failed to create snapshot directory: {}", snapshotDirectory.getAbsolutePath());
			}
		}
	}

	/**
	 * Saves a player snapshot to disk.
	 *
	 * @param snapshot The snapshot to save
	 * @return true if saved successfully, false otherwise
	 */
	public boolean saveSnapshot(PlayerSnapshot snapshot)
	{
		if (snapshot == null)
		{
			log.warn("Cannot save null snapshot");
			return false;
		}

		try
		{
			String timestamp = DATE_FORMAT.format(new Date(snapshot.getTimestamp()));
			String playerName = snapshot.getPlayerName().replaceAll("[^a-zA-Z0-9_-]", "");
			String filename = String.format("%s_%s.json", playerName, timestamp);
			File file = new File(snapshotDirectory, filename);

			try (FileWriter writer = new FileWriter(file))
			{
				gson.toJson(snapshot, writer);
				log.info("Saved snapshot to: {}", file.getAbsolutePath());
				return true;
			}
		}
		catch (IOException e)
		{
			log.error("Failed to save snapshot", e);
			return false;
		}
	}

	/**
	 * Loads the most recent snapshot for a player.
	 *
	 * @param playerName The player name
	 * @return The most recent snapshot, or null if none found
	 */
	public PlayerSnapshot loadLatestSnapshot(String playerName)
	{
		File[] snapshots = getSnapshotsForPlayer(playerName);

		if (snapshots.length == 0)
		{
			log.info("No snapshots found for player: {}", playerName);
			return null;
		}

		// Snapshots are sorted by modification time, so the first one is the latest
		File latestSnapshot = snapshots[0];

		try (FileReader reader = new FileReader(latestSnapshot))
		{
			PlayerSnapshot snapshot = gson.fromJson(reader, PlayerSnapshot.class);
			log.info("Loaded snapshot from: {}", latestSnapshot.getAbsolutePath());
			return snapshot;
		}
		catch (IOException e)
		{
			log.error("Failed to load snapshot", e);
			return null;
		}
	}

	/**
	 * Loads all snapshots for a player.
	 *
	 * @param playerName The player name
	 * @return List of all snapshots for the player
	 */
	public List<PlayerSnapshot> loadAllSnapshots(String playerName)
	{
		List<PlayerSnapshot> snapshots = new ArrayList<>();
		File[] snapshotFiles = getSnapshotsForPlayer(playerName);

		for (File file : snapshotFiles)
		{
			try (FileReader reader = new FileReader(file))
			{
				PlayerSnapshot snapshot = gson.fromJson(reader, PlayerSnapshot.class);
				snapshots.add(snapshot);
			}
			catch (IOException e)
			{
				log.error("Failed to load snapshot from: {}", file.getAbsolutePath(), e);
			}
		}

		log.info("Loaded {} snapshots for player: {}", snapshots.size(), playerName);
		return snapshots;
	}

	/**
	 * Deletes old snapshots, keeping only the most recent N snapshots.
	 *
	 * @param playerName The player name
	 * @param keepCount Number of recent snapshots to keep
	 */
	public void pruneSnapshots(String playerName, int keepCount)
	{
		File[] snapshots = getSnapshotsForPlayer(playerName);

		if (snapshots.length <= keepCount)
		{
			return; // Nothing to prune
		}

		// Delete older snapshots
		for (int i = keepCount; i < snapshots.length; i++)
		{
			if (snapshots[i].delete())
			{
				log.info("Deleted old snapshot: {}", snapshots[i].getName());
			}
			else
			{
				log.warn("Failed to delete snapshot: {}", snapshots[i].getName());
			}
		}
	}

	/**
	 * Gets all snapshot files for a player, sorted by modification time (newest first).
	 *
	 * @param playerName The player name
	 * @return Array of snapshot files
	 */
	private File[] getSnapshotsForPlayer(String playerName)
	{
		String sanitizedName = playerName.replaceAll("[^a-zA-Z0-9_-]", "");
		File[] files = snapshotDirectory.listFiles((dir, name) ->
			name.startsWith(sanitizedName + "_") && name.endsWith(".json"));

		if (files == null)
		{
			return new File[0];
		}

		// Sort by last modified time (newest first)
		Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
		return files;
	}

	/**
	 * Gets the snapshot directory path.
	 *
	 * @return The snapshot directory
	 */
	public File getSnapshotDirectory()
	{
		return snapshotDirectory;
	}
}
