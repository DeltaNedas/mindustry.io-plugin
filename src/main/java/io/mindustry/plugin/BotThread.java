package mindustry.plugin;

import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.permission.Role;
import org.json.simple.JSONObject;

import mindustry.plugin.discordcommands.DiscordCommands;

public class BotThread extends Thread {
    public DiscordApi api;
    private Thread mt;
    private JSONObject data;
    public DiscordCommands commandHandler = new DiscordCommands();

    public BotThread(DiscordApi api, Thread mt, JSONObject data) {
        this.api = api; //new DiscordApiBuilder().setToken(data.get(0)).login().join();
        this.mt = mt;
        this.data = data;

        // register commands
        this.api.addMessageCreateListener(commandHandler);
        new ComCommands().registerCommands(commandHandler);
        new ServerCommands(data).registerCommands(commandHandler);
        new MessageCreatedListeners(data).registerListeners(commandHandler);
    }

    public void run(){
        while (this.mt.isAlive()){
            try {
                Thread.sleep(1000);
            } catch (Exception e) {

            }
        }
        if (data.containsKey("serverdown_role_id")){
            Role r = new UtilMethods().getRole(api, (String) data.get("serverdown_role_id"));
            TextChannel tc = new UtilMethods().getTextChannel(api, (String) data.get("serverdown_channel_id"));
            if (r == null || tc ==  null) {
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {}
            } else {
                if (data.containsKey("serverdown_name")){
                    String serverNaam = (String) data.get("serverdown_name");
                    new MessageBuilder()
                            .append(String.format("%s\nServer %s is down",r.getMentionTag(),((serverNaam != "") ? ("**"+serverNaam+"**") : "")))
                            .send(tc);
                } else {
                    new MessageBuilder()
                            .append(String.format("%s\nServer is down.", r.getMentionTag()))
                            .send(tc);
                }
            }
        }
        api.disconnect();
    }
}
