package mindustry.plugin;

import arc.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.entities.type.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.world.*;
import org.javacord.api.*;
import org.javacord.api.entity.channel.*;
import org.javacord.api.entity.message.*;
import org.javacord.api.entity.message.embed.*;
import org.javacord.api.entity.permission.*;
import org.json.simple.*;
import org.json.simple.parser.JSONParser;

import java.awt.*;
import java.io.IOException;
import java.text.*;
import java.util.*;

import static mindustry.content.Bullets.arc;
import static mindustry.Vars.*;

public class IoPlugin extends Plugin {
    public static DiscordApi api = null;
    private final Long cooldownTime = 300L;
    private final String fileNotFoundErrorMessage = "File not found: config\\mods\\settings.json";
    private JSONObject alldata;
    public static JSONObject data; //token, channel_id, role_id
    private HashMap<Long, String> cooldowns = new HashMap<Long, String>(); //uuid

    //register event handlers and create variables in the constructor
    public IoPlugin() throws InterruptedException {
        try {
            String pureJson = Core.settings.getDataDirectory().child("mods/settings.json").readString();
            alldata = (JSONObject) new JSONParser().parse(pureJson);
            if (!alldata.containsKey("in-game")){
                Log.err("[ERR!] discordplugin: settings.json has an invalid format!\n");
                //this.makeSettingsFile("settings.json");
                return;
            } else {
                data = (JSONObject) alldata.get("in-game");
            }
        } catch (Exception e) {
            if (e.getMessage().contains(fileNotFoundErrorMessage)){
                Log.err("[ERR!] discordplugin: settings.json file is missing.\nBot can't start.");
                //this.makeSettingsFile("settings.json");
                return;
            } else {
                Log.err("[ERR!] discordplugin: Init Error");
                e.printStackTrace();
                return;
            }
        }
        try {
            api = new DiscordApiBuilder().setToken((String) alldata.get("token")).login().join();
        }catch (Exception e){
            if (e.getMessage().contains("READY packet")){
                Log.err("\n[ERR!] discordplugin: invalid token.\n");
            } else {
                e.printStackTrace();
            }
        }
        BotThread bt = new BotThread(api, Thread.currentThread(), (JSONObject) alldata.get("discord"));
        bt.setDaemon(false);
        bt.start();

        // live chat
        if (data.containsKey("live_chat_channel_id")) {
            TextChannel tc = getTextChannel((String) data.get("live_chat_channel_id"));
            if (tc != null) {
                HashMap<String, String> messageBuffer = new HashMap<>();
                Events.on(EventType.PlayerChatEvent.class, event -> {
                    messageBuffer.put(Utils.escapeBackticks(event.player.name), Utils.escapeBackticks(event.message));
                    if(messageBuffer.size() >= Utils.messageBufferSize) { // if message buffer size is below the expected size
                        EmbedBuilder eb = new EmbedBuilder().setTitle(new SimpleDateFormat("yyyy_MM_dd").format(Calendar.getInstance().getTime()));
                        for (Map.Entry<String, String> entry : messageBuffer.entrySet()) {
                            String username = entry.getKey();
                            String message = entry.getValue();
                            eb.addField(Utils.escapeAt(username), Utils.escapeAt(message));
                        }
                        tc.sendMessage(eb);
                        messageBuffer.clear();
                    }
                });
            }
        }

        // anti nuke

        Events.on(EventType.BuildSelectEvent.class, event -> {
            if (Utils.antiNukeEnabled) {
                try {
                    Tile nukeTile = event.builder.buildRequest().tile();
                    if (!event.breaking && event.builder.buildRequest().block == Blocks.thoriumReactor || event.builder.buildRequest().block == Blocks.combustionGenerator || event.builder.buildRequest().block == Blocks.turbineGenerator || event.builder.buildRequest().block == Blocks.impactReactor && event.builder instanceof Player) {
                        Tile coreTile = ((Player) event.builder).getClosestCore().getTile();
                        if (coreTile == null) {
                            return;
                        }
                        double distance = Utils.DistanceBetween(coreTile.x, coreTile.y, nukeTile.x, nukeTile.y);
                        if (distance <= Utils.nukeDistance) {
                            Call.beginBreak(event.builder.getTeam(), event.tile.x, event.tile.y);
                            Call.onDeconstructFinish(event.tile, Blocks.thoriumReactor, ((Player) event.builder).id);
                            ((Player) event.builder).kill();
                            ((Player) event.builder).sendMessage("[scarlet]Too close to the core, please find a better spot.");
                        }
                    }
                } catch (Exception ignored) {}
            } else{
                Log.info("Caught a nuker, but not preventing since anti nuke is off.");
            }
        });

        Events.on(EventType.PlayerConnect.class, player ->{
            TempBan.update();//updates tempban list
            for(String i : TempBan.getBanArrayList()){//checks if connecting player is in tempban list
                if(i.equals(player.player.con.address)) Call.onKick(player.player.con, "Tempban time remaining: "+TempBan.getBanTime(player.player.con.address));//kicks player and says how long tempbanned
            }
            });

        if (data.containsKey("warnings_chat_channel_id")) {
            TextChannel tc = this.getTextChannel((String) data.get("warnings_chat_channel_id"));
            if (tc != null) {
                Events.on(EventType.WaveEvent.class, event -> {
                    EmbedBuilder eb = new EmbedBuilder().setTitle("Wave " + state.wave + " started.");
                    tc.sendMessage(eb);
                });
                Events.on(EventType.BuildSelectEvent.class, event -> {
                    if (!event.breaking && event.builder.buildRequest().block == Blocks.thoriumReactor || event.builder.buildRequest().block == Blocks.combustionGenerator || event.builder.buildRequest().block == Blocks.turbineGenerator || event.builder.buildRequest().block == Blocks.impactReactor && event.builder instanceof Player) {
                        Player builder = (Player) event.builder;
                        Block buildBlock = event.builder.buildRequest().block;
                        Tile buildTile = event.builder.buildRequest().tile();
                        EmbedBuilder eb = new EmbedBuilder().setTitle("Suspicious block was placed.");
                        eb.setDescription("Builder: " + Utils.escapeBackticks(builder.name) + " (#" + builder.id + ")");
                        eb.addField(buildBlock.name, "Location: (" + buildTile.x + ", " + buildTile.y + ")");
                        eb.setColor(Utils.Pals.scarlet);
                        tc.sendMessage(eb);
                    }
                });
                //TODO: make it register when power graphs get split apart by griefers
            }
        }

        // welcome message
        if(Utils.welcomeMessage!=null) {
            Events.on(EventType.PlayerJoin.class, event -> {
                event.player.sendMessage(Utils.welcomeMessage);
            });
        }

    }

    //register commands that run on the server
    @Override
    public void registerServerCommands(CommandHandler handler){

    }

    //register commands that player can invoke in-game
    @Override
    public void registerClientCommands(CommandHandler handler){
        if (api != null) {
            handler.<Player>register("d", "<text...>", "Sends a message to moderators. (Use when a griefer is online)", (args, player) -> {

                if (!data.containsKey("dchannel_id")) {
                    player.sendMessage("[scarlet]This command is disabled.");
                } else {
                    TextChannel tc = this.getTextChannel((String) data.get("dchannel_id"));
                    if (tc == null) {
                        player.sendMessage("[scarlet]This command is disabled.");
                        return;
                    }
                    tc.sendMessage(Utils.escapeBackticks(player.name + " *@mindustry* : `" + args[0] + "`"));
                    player.sendMessage("[scarlet]Successfully sent message to moderators.");
                }

            });

            handler.<Player>register("players", "Display all players and their ids", (args, player) -> {
                StringBuilder builder = new StringBuilder();
                builder.append("[orange]List of players: \n");
                for (Player p : Vars.playerGroup.all()) {
                    if(p.isAdmin) {
                        builder.append("[scarlet]<ADMIN> ");
                    } else{
                        builder.append("[lightgray] ");
                    }
                    builder.append(p.name).append("[accent] (#").append(p.id).append(")\n");
                }
                player.sendMessage(builder.toString());
            });

            handler.<Player>register("gr", "<player> <reason...>", "Report a griefer by id (use '/gr' to get a list of ids)", (args, player) -> {
                //https://github.com/Anuken/Mindustry/blob/master/core/src/mindustry/core/NetServer.java#L300-L351
                if (!(data.containsKey("channel_id") && data.containsKey("role_id"))) {
                    player.sendMessage("[scarlet]This command is disabled.");
                    return;
                }


                for (Long key : cooldowns.keySet()) {
                    if (key + cooldownTime < System.currentTimeMillis() / 1000L) {
                        cooldowns.remove(key);
                        continue;
                    } else if (player.uuid.equals(cooldowns.get(key))) {
                        player.sendMessage("[scarlet]This command is on a 5 minute cooldown!");
                        return;
                    }
                }

                if (args.length == 0) {
                    StringBuilder builder = new StringBuilder();
                    builder.append("[orange]List of players: \n");
                    for (Player p : Vars.playerGroup.all()) {
                        if(p.isAdmin) {
                            builder.append("[scarlet]<ADMIN> ");
                        } else{
                            builder.append("[lightgray] ");
                        }
                        builder.append(p.name).append("[accent] (#").append(p.id).append(")\n");
                    }
                    player.sendMessage(builder.toString());
                } else {
                    Player found = null;
                    if (args[0].length() > 1 && args[0].startsWith("#") && Strings.canParseInt(args[0].substring(1))) {
                        int id = Strings.parseInt(args[0].substring(1));
                        for (Player p: Vars.playerGroup.all()){
                            if (p.id == id){
                                found = p;
                                break;
                            }
                        }
                    } else {
                        for (Player p: Vars.playerGroup.all()){
                            if (p.name.equalsIgnoreCase(args[0])){
                                found = p;
                                break;
                            }
                        }
                    }
                    if (found != null) {
                        if (found.isAdmin) {
                            player.sendMessage("[scarlet]Did you really expect to be able to report an admin?");
                        } else if (found.getTeam() != player.getTeam()) {
                            player.sendMessage("[scarlet]Only players on your team can be reported.");
                        } else {
                            TextChannel tc = this.getTextChannel((String) data.get("channel_id"));
                            Role r = this.getRole((String) data.get("role_id"));
                            if (tc == null || r == null) {
                                player.sendMessage("[scarlet]This command is disabled.");
                                return;
                            }
                            //send message
                            if (args.length > 1) {
                                new MessageBuilder()
                                        .setEmbed(new EmbedBuilder()
                                                .setTitle("A griefer was reported on the survival server.")
                                                .setDescription(r.getMentionTag())
                                                .addField("Name", found.name)
                                                .addField("Reason", args[1])
                                                .setColor(Color.ORANGE)
                                                .setFooter("Reported by " + player.name))
                                        .send(tc);
                            } else {
                                new MessageBuilder()
                                        .setEmbed(new EmbedBuilder()
                                                .setTitle("A griefer was reported on the survival server.")
                                                .setDescription(r.getMentionTag())
                                                .addField("Name", found.name)
                                                .setColor(Color.ORANGE)
                                                .setFooter("Reported by " + player.name))
                                        .send(tc);
                            }
                            player.sendMessage(found.name + "[sky] was reported to discord.");
                            cooldowns.put(System.currentTimeMillis() / 1000L, player.uuid);
                        }
                    }
                }
            });
        }
    }


    public static TextChannel getTextChannel(String id){
        Optional<Channel> dc =  ((Optional<Channel>) api.getChannelById(id));
        if (!dc.isPresent()) {
            Log.err("[ERR!] discordplugin: channel not found!");
            return null;
        }
        Optional<TextChannel> dtc = dc.get().asTextChannel();
        if (!dtc.isPresent()){
            Log.err("[ERR!] discordplugin: textchannel not found!");
            return null;
        }
        return dtc.get();
    }

    public Role getRole(String id){
        Optional<Role> r1 = api.getRoleById(id);
        if (!r1.isPresent()) {
            Log.err("[ERR!] discordplugin: adminrole not found!");
            return null;
        }
        return r1.get();
    }

}