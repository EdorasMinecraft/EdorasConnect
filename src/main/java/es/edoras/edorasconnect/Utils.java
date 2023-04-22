package es.edoras.edorasconnect;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
    // 2023: Método mejorable, fiarse de un catch es lo peor, pierdes el tiempo buscando errores
    public static String getUniqueId(String playername) throws Exception {
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
    public static String getXUID(String gamertag) throws Exception {
        if(!cachedBedrockPlayers.containsKey(gamertag)){
            cachedBedrockPlayers.put(gamertag, Utils.requestXUID(gamertag));
        }
        return cachedBedrockPlayers.get(gamertag);
    }

    // Private functions

    private static String requestName(String uuid) throws Exception {
        // Obtención de nombre
        URL mojangGetName = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid);
        BufferedReader in = new BufferedReader(new InputStreamReader(mojangGetName.openStream()));

        // JSON final
        StringBuilder jsonRaw = new StringBuilder();

        // Linea a linea hacemos un append al json final
        String buffer;
        while((buffer = in.readLine()) != null){
            jsonRaw.append(buffer);
        }
        in.close();

        // Aquí el JSON ya está entero, trabajamos con él
        JsonObject json = new Gson().fromJson(jsonRaw.toString(), JsonObject.class);

        // Nombre listo
        return json.get("name").getAsString();
    }

    private static String requestGamertag(String rawxuid) throws Exception {
        try {

            String hex = rawxuid.substring(19).replace("-", "");
            BigInteger xuid = new BigInteger(hex, 16);

            URL xboxGetGamertag = new URL("https://xbl.io/api/v2/account/" + xuid);

            HttpURLConnection httpURLConnection = (HttpURLConnection) xboxGetGamertag.openConnection();
            httpURLConnection.setRequestProperty("X-Authorization", ECConfig.XAPI_TOKEN.getString());

            BufferedReader in = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()));

            // JSON final
            StringBuilder jsonRaw = new StringBuilder();

            // Linea a linea hacemos un append al json final
            String buffer;
            while((buffer = in.readLine()) != null){
                jsonRaw.append(buffer);
            }
            in.close();

            // Aquí el JSON ya está entero, trabajamos con él
            JsonObject json = new Gson().fromJson(jsonRaw.toString(), JsonObject.class);

            String gamertagObtained = json.getAsJsonArray("profileUsers").get(0).getAsJsonObject().getAsJsonArray("settings").get(2).getAsJsonObject().get("value").getAsString();
            return gamertagObtained;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    private static String requestUUID(String name) throws Exception {
        // Obtención de UUID
        URL mojangGetUUID = new URL("https://api.mojang.com/users/profiles/minecraft/" + name);
        BufferedReader in = new BufferedReader(new InputStreamReader(mojangGetUUID.openStream()));

        // JSON final
        StringBuilder jsonRaw = new StringBuilder();

        // Linea a linea hacemos un append al json final
        String buffer;
        while((buffer = in.readLine()) != null){
            jsonRaw.append(buffer);
        }
        in.close();

        // Aquí el JSON ya está entero, trabajamos con él
        JsonObject json = new Gson().fromJson(jsonRaw.toString(), JsonObject.class);

        // Se añaden los guiones de las UUID
        StringBuilder stringBuilder = new StringBuilder(json.get("id").getAsString());
        stringBuilder.insert(8, "-").insert(13, "-").insert(18, "-").insert(23, "-");

        // UUID lista
        return stringBuilder.toString();
    }

    private static String requestXUID(String gamertag) throws Exception {
        URL xboxGetXUID = new URL("https://xbl.io/api/v2/search/" + gamertag.substring(1));

        HttpURLConnection httpURLConnection = (HttpURLConnection) xboxGetXUID.openConnection();
        httpURLConnection.setRequestProperty("X-Authorization", ECConfig.XAPI_TOKEN.getString());

        BufferedReader in = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()));

        // JSON final
        StringBuilder jsonRaw = new StringBuilder();

        // Linea a linea hacemos un append al json final
        String buffer;
        while((buffer = in.readLine()) != null){
            jsonRaw.append(buffer);
        }
        in.close();

        // Aquí el JSON ya está entero, trabajamos con él
        JsonObject json = new Gson().fromJson(jsonRaw.toString(), JsonObject.class);

        String gamertagObtained = json.getAsJsonArray("people").get(0).getAsJsonObject().get("gamertag").getAsString();
        if(gamertag.substring(1).equalsIgnoreCase(gamertagObtained)){
            String xuid = json.getAsJsonArray("people").get(0).getAsJsonObject().get("xuid").getAsString();
            return new UUID(0, Long.parseLong(xuid)).toString();
        } else {
            throw new Exception("Bedrock player not found. Obtained " + gamertagObtained + ", expected " + gamertag);
        }
    }
}