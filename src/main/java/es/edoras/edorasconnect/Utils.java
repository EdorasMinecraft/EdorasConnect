package es.edoras.edorasconnect;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public class Utils {
    // Name - UUID
    private static Map<String, String> cachedPlayers = new HashMap<>();
    private static Map<String, String> cachedBedrockPlayers = new HashMap<>();

    // OBTENCIÓN DE NOMBRES

    // this.getUUID(String playername) y this.getXUID(String gamertag) fusionados
    public static String getName(String id) throws Exception {
        String playername;
        try {
            // Obtener UUID de jugador de Java
            playername = Utils.getMojangName(id);
        } catch (Exception e) {
            // Si no existe jugador en Java, comprobar para Bedrock (Xbox Live)
            playername = Utils.getGamertag(id);
        }
        return playername;
    }

    public static String getMojangName(String uuid) throws Exception {
        if(!cachedPlayers.containsValue(uuid)){
            cachedPlayers.put(Utils.requestName(uuid), uuid);
        }

        String name = null;
        for(String o : cachedPlayers.keySet()){
            if(cachedPlayers.get(o).equals(uuid)){
                name = o;
            }
        }
        return name;
    }

    public static String getGamertag(String xuid) throws Exception {
        if(!cachedBedrockPlayers.containsValue(xuid)){
            cachedBedrockPlayers.put(Utils.requestGamertag(xuid), xuid);
        }

        String name = null;
        for(String o : cachedBedrockPlayers.keySet()){
            if(cachedBedrockPlayers.get(o).equals(xuid)){
                name = o;
            }
        }
        return name;
    }

    // OBTENCIÓN DE UUIDs

    // this.getUUID(String playername) y this.getXUID(String gamertag) fusionados
    public static String getUniqueId(String playername) throws IOException {
        String playeruuid;
        try {
            // Obtener UUID de jugador de Java
            playeruuid = Utils.getUUID(playername);
        } catch (Exception e) {
            // Si no existe jugador en Java, comprobar para Bedrock (Xbox Live)
            playeruuid = Utils.getXUID(playername);
        }
        return playeruuid;
    }

    // Obtener UUID de jugadores Java
    public static String getUUID(String playername) throws Exception {
        if(!cachedPlayers.containsKey(playername)){
            cachedPlayers.put(playername, Utils.requestUUID(playername));
        }
        return cachedPlayers.get(playername);
    }

    // Obtener XUID de jugadores Bedrock
    public static String getXUID(String gamertag) throws IOException {
        if(!cachedBedrockPlayers.containsKey(gamertag)){
            cachedBedrockPlayers.put(gamertag, Utils.requestXUID(gamertag));
        }
        return cachedBedrockPlayers.get(gamertag);
    }

    // Private functions

    private static String requestName(String uuid) throws Exception {
        // Obtención de nombre
        Scanner scanner = new Scanner(new URL("https://api.mojang.com/user/profiles/" + uuid + "/names").openConnection().getInputStream());
        JsonArray json = new Gson().fromJson(scanner.next(), JsonArray.class);
        scanner.close();
        return json.get(json.size() - 1).getAsJsonObject().get("name").getAsString();
    }

    private static String requestGamertag(String rawxuid) throws Exception {
        String hex = rawxuid.substring(19).replace("-", "");
        BigInteger xuid = new BigInteger(hex, 16);
        // Obtención de Gamertag
        URL gamertagapi = new URL("https://xapi.us/v2/gamertag/" + xuid);
        HttpURLConnection httpURLConnection = (HttpURLConnection) gamertagapi.openConnection();
        httpURLConnection.setRequestProperty("X-AUTH", ECConfig.XAPI_TOKEN.getString());
        Scanner scanner = new Scanner(httpURLConnection.getInputStream());
        // scanner.next() devuelve String gamertag
        return scanner.next();
    }

    private static String requestUUID(String name) throws Exception {
        // Obtención de UUID
        Scanner scanner = new Scanner(new URL("https://api.mojang.com/users/profiles/minecraft/" + name).openConnection().getInputStream());
        JsonObject json = new Gson().fromJson(scanner.next(), JsonObject.class);
        scanner.close();
        // Se añaden los guiones de las UUID
        StringBuilder stringBuilder = new StringBuilder(json.get("id").getAsString());
        stringBuilder.insert(8, "-").insert(13, "-").insert(18, "-").insert(23, "-");
        return stringBuilder.toString();
    }

    private static String requestXUID(String gamertag) throws IOException {
        URL gamertagapi = new URL("https://xapi.us/v2/xuid/" + gamertag);
        HttpURLConnection httpURLConnection = (HttpURLConnection) gamertagapi.openConnection();
        httpURLConnection.setRequestProperty("X-AUTH", ECConfig.XAPI_TOKEN.getString());
        Scanner scanner = new Scanner(httpURLConnection.getInputStream());
        String xuid = scanner.next();
        scanner.close();
        return new UUID(0, Long.parseLong(xuid)).toString();
    }
}
