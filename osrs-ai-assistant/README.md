# OSRS AI Assistant Plugin for RuneLite

An intelligent AI assistant plugin for RuneLite that provides context-aware help by analyzing your current game state, including skills, inventory, equipment, location, and more.

## Features

- **In-game Chat Interface**: Clean, intuitive chat panel integrated into RuneLite's sidebar
- **Context-Aware AI**: Automatically includes your player data in every query for personalized assistance
- **Multiple AI Providers**: Support for both OpenAI (GPT-4o, GPT-4o-mini) and Anthropic (Claude)
- **Comprehensive Data Collection**:
  - All skill levels and experience
  - Current inventory and equipment
  - Bank contents (when open)
  - Player location and quest points
  - Combat stats (HP, Prayer, Special Attack, Run Energy)
- **Snapshot System**: Save and load complete player state snapshots
- **Auto-Snapshot**: Periodically save your progress automatically
- **Privacy-Focused**: All data stays local; only sent to AI when you request it

## Setup Instructions

### 1. Prerequisites

- RuneLite client installed
- Java 11 or higher
- An API key from either:
  - OpenAI (https://platform.openai.com/api-keys)
  - Anthropic (https://console.anthropic.com/)

### 2. Installation

#### Option A: Build from Source

```bash
# Clone this repository
git clone <your-repository-url>
cd osrs-ai-assistant

# Build the plugin
./gradlew shadowJar

# The plugin JAR will be in build/libs/
```

#### Option B: Use Pre-built JAR

1. Download the latest release JAR
2. Place it in your RuneLite plugins folder

### 3. Configuration

1. Open RuneLite
2. Click the Configuration (wrench) icon
3. Find "AI Assistant" in the plugin list
4. Configure the following settings:

**AI Provider Settings:**
- **AI Provider**: Choose between OpenAI or Anthropic
- **API Key**: Enter your API key (kept secure)
- **OpenAI Model**: Select gpt-4o-mini (cheaper) or gpt-4o (more capable)
- **Anthropic Model**: Select claude-sonnet-4 or claude-haiku

**Snapshot Settings:**
- **Auto-save Snapshots**: Enable/disable automatic snapshots
- **Snapshot Interval**: How often to save (in minutes)
- **Max Snapshots to Keep**: Limit the number of saved snapshots

### 4. Usage

1. Click the AI Assistant icon in the RuneLite sidebar
2. Type your question in the input field
3. Press Enter or click Send
4. The AI will respond with context-aware advice

**Example Questions:**
- "What should I train next to improve my combat level?"
- "Based on my inventory, what boss should I fight?"
- "What quests should I do with my current stats?"
- "How can I make money with my current skills?"
- "What gear should I upgrade next?"

## Technical Architecture

### File Structure

```
aiassistant/
├── AiAssistantPlugin.java       # Main plugin class
├── AiAssistantConfig.java       # Configuration interface
├── AiAssistantPanel.java        # UI panel with chat
├── data/
│   ├── PlayerDataCollector.java # Collects all game data
│   ├── PlayerSnapshot.java      # Data model
│   └── SnapshotManager.java     # Saves/loads snapshots
└── ai/
    ├── AiProvider.java          # Interface for AI providers
    ├── OpenAiProvider.java      # OpenAI implementation
    ├── AnthropicProvider.java   # Anthropic implementation
    └── PromptBuilder.java       # Builds context prompts
```

### Data Collection

The plugin collects the following data from the RuneLite client:

- **Player Info**: Name, combat level, location
- **Skills**: All 23 skills with levels and XP
- **Combat Stats**: Current/max HP, Prayer, Special Attack %, Run Energy
- **Equipment**: All equipped items by slot
- **Inventory**: All items with quantities
- **Bank**: All items (when bank is open)
- **Quest Progress**: Quest points

### AI Integration

**OpenAI API:**
- Endpoint: `https://api.openai.com/v1/chat/completions`
- Models: gpt-4o-mini (default), gpt-4o
- Authentication: Bearer token

**Anthropic API:**
- Endpoint: `https://api.anthropic.com/v1/messages`
- Models: claude-sonnet-4-20250514, claude-3-5-haiku-20241022
- Authentication: x-api-key header

### Snapshot System

Snapshots are saved to `~/.runelite/aiassistant/snapshots/` as JSON files:
- Format: `{PlayerName}_{YYYY-MM-DD_HH-mm-ss}.json`
- Contains complete player state at that moment
- Can be manually saved or auto-saved at intervals
- Old snapshots are automatically pruned

## Privacy & Security

- **API Keys**: Stored securely in RuneLite's config system
- **Local Data**: All snapshots stored locally on your machine
- **No Telemetry**: No data sent anywhere except when you request AI assistance
- **User Control**: You control when data is sent to AI providers

## Cost Estimates

**OpenAI (gpt-4o-mini):**
- Input: $0.150 per 1M tokens (~$0.0002 per query)
- Output: $0.600 per 1M tokens (~$0.0001 per response)
- Estimated: **$0.50 - $2.00 per month** for regular use

**Anthropic (Claude Sonnet):**
- Input: $3.00 per 1M tokens (~$0.004 per query)
- Output: $15.00 per 1M tokens (~$0.002 per response)
- Estimated: **$5.00 - $20.00 per month** for regular use

*Estimates based on ~1000 tokens per query context + response*

## Troubleshooting

**"Please configure your API key in the plugin settings"**
- Ensure you've entered a valid API key in the configuration

**"AI provider not initialized"**
- Check that your API key is correct
- Verify you've selected the right provider (OpenAI/Anthropic)

**"Error: 401 Unauthorized"**
- Your API key is invalid or expired
- Generate a new key from your provider's dashboard

**"Cannot save snapshot - player not logged in"**
- You must be logged into OSRS to save snapshots

**Plugin not appearing in sidebar**
- Restart RuneLite
- Check that the plugin is enabled in the plugin list

## Development

### Building

```bash
./gradlew build
```

### Testing

```bash
./gradlew test
```

### Running in Development

```bash
./gradlew shadowJar
# Then load the JAR in RuneLite's plugin panel
```

## Dependencies

- **RuneLite API**: Client, UI components, events
- **Lombok**: Boilerplate reduction
- **OkHttp**: HTTP client for API calls
- **Gson**: JSON serialization

## Contributing

Contributions are welcome! Please:

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Submit a pull request

## License

This project is licensed under the BSD 2-Clause License - see the LICENSE file for details.

## Acknowledgments

- RuneLite team for the excellent plugin API
- OpenAI and Anthropic for their AI APIs
- OSRS community for inspiration and feedback

## Support

For issues, questions, or suggestions:
- Open an issue on GitHub
- Join the discussion on Discord

## Disclaimer

This plugin is not affiliated with, endorsed by, or connected to Jagex Ltd. Use at your own risk. Ensure your use complies with Jagex's Third-Party Client Guidelines and Terms of Service.

## Version History

### v1.0.0 (Initial Release)
- In-game chat interface
- OpenAI and Anthropic support
- Comprehensive player data collection
- Snapshot system with auto-save
- Configurable settings

---

**Happy scaping! 🏹🗡️✨**