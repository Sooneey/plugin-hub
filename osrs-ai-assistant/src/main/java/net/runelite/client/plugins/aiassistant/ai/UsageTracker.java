package net.runelite.client.plugins.aiassistant.ai;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Tracks API usage for rate limiting and statistics.
 */
@Slf4j
public class UsageTracker
{
	private final List<ApiCall> recentCalls;
	private long totalTokensUsed;
	private int totalCalls;

	public UsageTracker()
	{
		this.recentCalls = new ArrayList<>();
		this.totalTokensUsed = 0;
		this.totalCalls = 0;
	}

	/**
	 * Records an API call.
	 *
	 * @param tokensUsed Estimated tokens used (if known)
	 */
	public synchronized void recordCall(int tokensUsed)
	{
		recentCalls.add(new ApiCall(Instant.now(), tokensUsed));
		totalTokensUsed += tokensUsed;
		totalCalls++;

		// Clean up old calls (older than 1 hour)
		cleanOldCalls();
	}

	/**
	 * Checks if rate limit is exceeded.
	 *
	 * @param maxCallsPerMinute Maximum calls per minute
	 * @return true if rate limit is exceeded
	 */
	public synchronized boolean isRateLimitExceeded(int maxCallsPerMinute)
	{
		cleanOldCalls();
		Instant oneMinuteAgo = Instant.now().minus(1, ChronoUnit.MINUTES);

		long callsInLastMinute = recentCalls.stream()
			.filter(call -> call.getTimestamp().isAfter(oneMinuteAgo))
			.count();

		return callsInLastMinute >= maxCallsPerMinute;
	}

	/**
	 * Gets the number of calls in the last minute.
	 */
	public synchronized int getCallsInLastMinute()
	{
		cleanOldCalls();
		Instant oneMinuteAgo = Instant.now().minus(1, ChronoUnit.MINUTES);

		return (int) recentCalls.stream()
			.filter(call -> call.getTimestamp().isAfter(oneMinuteAgo))
			.count();
	}

	/**
	 * Gets the number of calls in the last hour.
	 */
	public synchronized int getCallsInLastHour()
	{
		cleanOldCalls();
		return recentCalls.size();
	}

	/**
	 * Gets total tokens used.
	 */
	public synchronized long getTotalTokensUsed()
	{
		return totalTokensUsed;
	}

	/**
	 * Gets total number of calls.
	 */
	public synchronized int getTotalCalls()
	{
		return totalCalls;
	}

	/**
	 * Gets usage statistics.
	 */
	public synchronized UsageStats getStats()
	{
		cleanOldCalls();
		return new UsageStats(
			totalCalls,
			totalTokensUsed,
			getCallsInLastMinute(),
			getCallsInLastHour()
		);
	}

	/**
	 * Resets all statistics.
	 */
	public synchronized void reset()
	{
		recentCalls.clear();
		totalTokensUsed = 0;
		totalCalls = 0;
		log.debug("Usage statistics reset");
	}

	/**
	 * Removes calls older than 1 hour.
	 */
	private void cleanOldCalls()
	{
		Instant oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS);
		recentCalls.removeIf(call -> call.getTimestamp().isBefore(oneHourAgo));
	}

	/**
	 * Represents a single API call.
	 */
	@Data
	private static class ApiCall
	{
		private final Instant timestamp;
		private final int tokensUsed;
	}

	/**
	 * Usage statistics.
	 */
	@Data
	public static class UsageStats
	{
		private final int totalCalls;
		private final long totalTokens;
		private final int callsLastMinute;
		private final int callsLastHour;

		@Override
		public String toString()
		{
			return String.format(
				"Total calls: %d | Total tokens: %,d | Last minute: %d | Last hour: %d",
				totalCalls, totalTokens, callsLastMinute, callsLastHour
			);
		}
	}
}
