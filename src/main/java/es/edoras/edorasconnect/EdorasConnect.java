package es.edoras.edorasconnect;

import es.edoras.edorasconnect.discord.DiscordEvents;
import es.edoras.edorasconnect.minecraft.commands.DiscordCommands;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class EdorasConnect extends Plugin {
    public Connection mysql;
    public JDA discord;

    @Override
    public void onEnable() {
        // Iniciar configuración
        ECConfig.setConfig(this.getConfig("config.yml"));
        ECMessages.setMessages(this.getConfig("messages.yml"));

        try {
            // Iniciar conexión con la base de datos (MySQL)
            this.openConnection();

            // Iniciar bot de Discord
            // GatewayIntent para acceder a información más sensible
            this.discord = JDABuilder.createDefault(ECConfig.DISCORD_TOKEN.getString(),
                    GatewayIntent.GUILD_MEMBERS,
                    GatewayIntent.GUILD_VOICE_STATES,
                    GatewayIntent.GUILD_EMOJIS_AND_STICKERS,
                    GatewayIntent.GUILD_MESSAGES)
                    .addEventListeners(new DiscordEvents(this, mysql))
                    .setActivity(Activity.watching("Edoras"))
                    .build();
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }

        // Iniciar bot de Telegram

        // Minecraft
        // Registrar comandos
        this.getProxy().getPluginManager().registerCommand(this, new DiscordCommands(this, discord, mysql));
    }

    @Override
    public void onDisable() {
        discord.shutdownNow();
    }

    private Configuration getConfig(@NotNull String filename){
        // Crea la carpeta de configuración en caso de que no exista
        if (!this.getDataFolder().exists()) {
            this.getDataFolder().mkdir();
        }
        File file = new File(this.getDataFolder(), filename);
        // Crea el archivo en caso de que no exista
        try {
            if (!file.exists()) {
                InputStream in = this.getResourceAsStream(filename);
                Files.copy(in, file.toPath());
            }
            return ConfigurationProvider.getProvider(YamlConfiguration.class).load(file);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public void openConnection() throws SQLException, ClassNotFoundException {
        if (mysql != null && !mysql.isClosed()) {
            return;
        }

        synchronized(this){
            if (mysql != null && !mysql.isClosed()) {
                return;
            }
            mysql = DriverManager.getConnection("jdbc:mysql://" + ECConfig.DATABASE_HOST.getString() + ":" + ECConfig.DATABASE_PORT.getInteger() + "/" + ECConfig.DATABASE_DATABASE.getString(), ECConfig.DATABASE_USER.getString(), ECConfig.DATABASE_PASSWORD.getString());
        }
    }

    public void checkMembersWithLinkedRole(){
        Guild guild = Objects.requireNonNull(discord.getGuildById(ECConfig.DISCORD_GUILD.getString()), "Guild must not be null");
        // Comprobar que todos los Socios están vinculados
        Role linkedRole = Objects.requireNonNull(guild.getRoleById(ECConfig.DISCORD_MEMBER_ROLE.getString()), "Role must not be null");
        List<String> linkedAccounts = new ArrayList<>();
        try {
            // Obtener cuentas vinculadas de la base de datos
            ResultSet linkedAccountsQuery = mysql.createStatement().executeQuery("SELECT discord FROM edorasconnect_discord;");
            while(linkedAccountsQuery.next()){
                // Añadir resultados a la lista inicial para compararla después
                linkedAccounts.add(linkedAccountsQuery.getString("discord"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Comprobar que los Socios del servidor están vinculados usando la lista anterior
        guild.findMembersWithRoles(linkedRole).onSuccess(members -> {
            for(Member member : members){
                // Si no está vinculado, eliminar Socio
                if(!linkedAccounts.contains(member.getUser().getId())){
                    this.getProxy().getLogger().info(ECMessages.MINECRAFT_TASKS_ACCOUNT_NOT_LINKED.getString().replace("{discriminator}", member.getUser().getDiscriminator()).replace("{name}", member.getUser().getName()));
                    guild.removeRoleFromMember(member, linkedRole).queue();
                }
            }
        });

        // Eliminar Socio a los no vinculados y añadirlo a los vinculados
        guild.loadMembers().onSuccess(members -> {
            for(Member member : members){
                if(member.getUser().getId().equals(discord.getSelfUser().getId())) { continue; } // Skip self bot
                if(!linkedAccounts.contains(member.getUser().getId()) && member.getRoles().contains(linkedRole)){
                    this.getProxy().getLogger().info(ECMessages.MINECRAFT_TASKS_ACCOUNT_NOT_LINKED.getString().replace("{discriminator}", member.getUser().getDiscriminator()).replace("{name}", member.getUser().getName()));
                    guild.removeRoleFromMember(member, linkedRole).queue();
                } else if(linkedAccounts.contains(member.getUser().getId()) && !member.getRoles().contains(linkedRole)){
                    this.getProxy().getLogger().info(ECMessages.MINECRAFT_TASKS_ACCOUNT_LINKED.getString().replace("{discriminator}", member.getUser().getDiscriminator()).replace("{name}", member.getUser().getName()));
                    guild.addRoleToMember(member, linkedRole).queue();
                }
            }
        });
    }
}
