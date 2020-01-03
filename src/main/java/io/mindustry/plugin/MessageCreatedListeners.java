package mindustry.plugin;

import mindustry.plugin.discordcommands.DiscordCommands;
import mindustry.plugin.discordcommands.MessageCreatedListener;
import org.json.simple.JSONObject;

public class MessageCreatedListeners {
    private final JSONObject data;

    public MessageCreatedListeners(JSONObject data){
        this.data = data;
    }
    public void registerListeners(DiscordCommands handler){

    }
}
