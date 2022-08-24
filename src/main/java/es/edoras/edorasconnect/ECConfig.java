package es.edoras.edorasconnect;

import net.md_5.bungee.config.Configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum ECConfig {
    // DATABASE
    DATABASE_HOST("database.host", "127.0.0.1"),
    DATABASE_PORT("database.port", 3306),
    DATABASE_USER("database.user", "minecraft"),
    DATABASE_PASSWORD("database.password", "minecraft"),
    DATABASE_DATABASE("database.database", "minecraft"),

    XAPI_TOKEN("xapi-token", ""),

    DISCORD_GUILD_URL("discord-guild-url", "https://discord.edoras.es/"),

    // TELEGRAM
    TELEGRAM_TOKEN("telegram-token", ""),

    // DISCORD
    DISCORD_TOKEN("discord-token", ""),
    DISCORD_GUILD("discord-guild", "442668326888669195"),
    DISCORD_MEMBER_ROLE("member-role", "442668326888669195"),
    DISCORD_MAX_ACCOUNTS_LINKED("link.max-accounts-linked", 3),
    LINK_EXPIRATION("link.link-expiration", 300),
    UNLINK_EXPIRATION("link.unlink-expiration", 120),
    DISCORD_LINK_CHANNEL("link-channel", "695992710699679794"),
    DISCORD_LOG_CHANNEL("log-channel", "708762845428449351"),

    DISCORD_EXCLUDED_CHANNELS_FROM_PURGE("channels-excluded-from-purge", new ArrayList<>())
    ;

    private static Configuration config;
    private Object path;
    private Object def;

    ECConfig(String path, String def){
        this.path = path;
        this.def = def;
    }

    ECConfig(String path, int def){
        this.path = path;
        this.def = def;
    }

    ECConfig(String path, List<String> def){
        this.path = path;
        this.def = def;
    }

    public static void setConfig(Configuration config){
        ECConfig.config = config;
    }

    public String getString(){
        return config.getString((String) this.path, (String) this.def);
    }

    public int getInteger(){
        return config.getInt((String) this.path, (int) this.def);
    }

    public List<?> getList() {
        List<String> strings = new ArrayList<>();
        List<?> configList = config.getList((String) this.path, (List<?>) def);

        for(Object o : configList){
            strings.add(String.valueOf(o));
        }

        // No, un return config.getList no funciona.
        return strings;
    }
}
