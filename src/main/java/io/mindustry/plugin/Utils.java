package mindustry.plugin;

import mindustry.maps.Map;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.entity.message.embed.EmbedBuilder;

import java.awt.*;
import java.util.List;

import static mindustry.Vars.maps;

public class Utils {
    static int nukeDistance = 25;
    static int messageBufferSize = 10; // number of messages sent at once to discord
    public static int chatMessageMaxSize = 200;
    public static Boolean antiNukeEnabled = true;;
    public static String welcomeMessage = "[sky]Welcome to mindustry.io! Consider joining our discord here: http://discord.mindustry.io";
    public static class Pals {
        public static Color scarlet = (Color.getHSBColor(5, 85, 95));
    }

    static double DistanceBetween(double x1, double y1, double x2, double y2) {
        return Math.sqrt((y2 - y1) * (y2 - y1) + (x2 - x1) * (x2 - x1));
    }

    public static String escapeBackticks(String string){
        return string.replaceAll("`", "");
    }

    public static String escapeAt(String string) { return string.replaceAll("@", ""); }

    public static String constructMessage(List<String> array) {
        StringBuilder result = new StringBuilder();
        for(String string : array){
            result.append(string).append("\n");
        }
        return result.toString();
    }

    public static String stringArrayToString(List<String> array) {
        StringBuilder result= new StringBuilder();
        for(String string : array){
            result.append(string);
        }
        return result.toString();
    }

    public static Map getMapBySelector(String query) {
        Map found = null;
        try {
            // try by number
            found = maps.customMaps().get(Integer.parseInt(query));
        } catch (Exception e) {
            // try by name
            for (Map m : maps.customMaps()) {
                if (m.name().equals(query)) {
                    found = m;
                    break;
                }
            }
        }
        return found;
    }

    public static void LogAction(String title, String message, MessageAuthor user, String victim){
        EmbedBuilder embed = new EmbedBuilder()
                .setDescription(message)
                .setTitle("An action was executed: " + title);
        if(user!=null){
            embed.setAuthor(user);
        }
        if(victim!=null){
            embed.addInlineField("On user: ", victim);
        }

        //TODO: make it send to bot_log channel

    }
}
