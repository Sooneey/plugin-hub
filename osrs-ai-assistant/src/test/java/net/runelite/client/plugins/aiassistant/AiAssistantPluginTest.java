package net.runelite.client.plugins.aiassistant;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class AiAssistantPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(AiAssistantPlugin.class);
		RuneLite.main(args);
	}
}