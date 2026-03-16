package net.runelite.client.plugins.aiassistant;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertNotNull;

@RunWith(MockitoJUnitRunner.class)
public class AiAssistantPluginTest
{
	@Test
	public void testPluginInstantiation()
	{
		AiAssistantPlugin plugin = new AiAssistantPlugin();
		assertNotNull("Plugin should be instantiable", plugin);
	}
}