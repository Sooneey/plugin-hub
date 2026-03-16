package net.runelite.client.plugins.aiassistant;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class AiAssistantPluginTest
{
	@Test
	public void testPluginInstantiation()
	{
		AiAssistantPlugin plugin = new AiAssistantPlugin();
		assertNotNull("Plugin should be instantiable", plugin);
	}
}