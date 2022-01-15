package es.edoras.edorasconnect.minecraft.commands;

import es.edoras.edorasconnect.ECConfig;
import es.edoras.edorasconnect.ECMessages;
import es.edoras.edorasconnect.EdorasConnect;
import es.edoras.edorasconnect.Utils;
import es.edoras.edorasconnect.discord.DiscordStatics;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class DiscordCommands extends Command implements TabExecutor {
    private final EdorasConnect minecraft;
    private final JDA discord;
    private final Connection mysql;

    public DiscordCommands(EdorasConnect minecraft, JDA discord, Connection mysql) {
        super("discord", "edorasconnect.link", "edorasconnect");
        this.minecraft = minecraft;
        this.discord = discord;
        this.mysql = mysql;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (sender instanceof ProxiedPlayer) {
            ProxiedPlayer player = (ProxiedPlayer) sender;
            String uuid = player.getUniqueId().toString();
            switch (args.length) {
                case 1:
                    if (args[0].equalsIgnoreCase("unlink")) {
                        try {
                            if (this.isDiscordLinked(uuid)) {
                                if (!DiscordStatics.pendingUnlinks.contains(uuid)) {
                                    // Añadir a la lista para confirmar desvinculación
                                    DiscordStatics.pendingUnlinks.add(uuid);
                                    // Eliminar tras un breve instante
                                    minecraft.getProxy().getScheduler().schedule(minecraft, () -> DiscordStatics.pendingUnlinks.remove(uuid), ECConfig.UNLINK_EXPIRATION.getInteger(), TimeUnit.SECONDS);
                                    player.sendMessage(TextComponent.fromLegacyText(ECMessages.MINECRAFT_UNLINK_ASK_CONFIRMATION.getMinecraftString()));
                                } else {
                                    // Confirmar desvinculación
                                    this.unlink(uuid);
                                    DiscordStatics.pendingUnlinks.remove(uuid);
                                    player.sendMessage(TextComponent.fromLegacyText(ECMessages.MINECRAFT_UNLINKED_SUCCESSFULLY.getMinecraftString()));
                                }
                            } else {
                                player.sendMessage(TextComponent.fromLegacyText(ECMessages.MINECRAFT_NO_LINKED_ACCOUTS_FOUND.getMinecraftString()));
                            }
                        } catch (SQLException e) {
                            player.sendMessage(TextComponent.fromLegacyText(ECMessages.MINECRAFT_GENERIC_ERROR_WITH_CODE.getMinecraftString().replace("{code}", "Database connection error")));
                            e.printStackTrace();
                        }
                    } else {
                        player.sendMessage(TextComponent.fromLegacyText(ECMessages.MINECRAFT_COMMAND_CONSTRUCTOR_NOT_FOUND.getMinecraftString()));
                    }
                    break;
                case 2:
                    if (args[0].equalsIgnoreCase("link")) {
                        // Obtener código de verificación introducido (Snowflake)
                        String snowflake = args[1];
                        // Verificar que el código introducido existe en el proceso de vinculación
                        if (DiscordStatics.pendingLinks.containsKey(snowflake)) {
                            // Comprobar que el usuario de Minecraft asociado al proceso inicial coincide con el emisor del comando
                            if (DiscordStatics.pendingLinks.get(snowflake).equals(uuid)) {
                                // Vincular
                                try {
                                    this.link(snowflake, uuid);
                                    DiscordStatics.pendingLinks.remove(snowflake);
                                    discord.retrieveUserById(snowflake).queue(user -> player.sendMessage(TextComponent.fromLegacyText(ECMessages.MINECRAFT_ACCOUNT_LINKED.getMinecraftString().replace("{player}", player.getName()).replace("{discriminator}", user.getDiscriminator()).replace("{name}", user.getName()))));
                                    // Mensaje global de vinculación exitosa
                                    for(ProxiedPlayer p : minecraft.getProxy().getPlayers()){
                                        if(player.getUniqueId() != p.getUniqueId()) {
                                            TextComponent linkSuccessful = new TextComponent(TextComponent.fromLegacyText(ECMessages.MINECRAFT_LINK_SUCCESSFUL_GLOBAL_MESSAGE.getMinecraftString().replace("{player}", player.getName())));
                                            linkSuccessful.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(TextComponent.fromLegacyText(ECMessages.MINECRAFT_LINK_SUCCESSFUL_GLOBAL_MESSAGE_HOVER.getMinecraftString()))));
                                            linkSuccessful.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, ECConfig.DISCORD_GUILD_URL.getString()));
                                            p.sendMessage(linkSuccessful);
                                        }
                                    }
                                } catch (SQLException e) {
                                    player.sendMessage(TextComponent.fromLegacyText(ECMessages.MINECRAFT_GENERIC_ERROR_WITH_CODE.getMinecraftString().replace("{code}", "Database connection error")));
                                    e.printStackTrace();
                                }
                            } else {
                                player.sendMessage(TextComponent.fromLegacyText(ECMessages.MINECRAFT_WRONG_ACCOUNT.getMinecraftString()));
                            }
                        } else {
                            player.sendMessage(TextComponent.fromLegacyText(ECMessages.MINECRAFT_NO_LINK_EXPECTED.getMinecraftString()));
                        }
                    } else if (args[0].equalsIgnoreCase("check") && sender.hasPermission("edorasconnect.link.check")) {
                        // Comprobar cuentas vinculadas a un usuario de Minecraft
                        try {
                            String playername = args[1];
                            // Obtener de la base de datos las cuentas vinculadas a un usuario de Minecraft
                            List<String> snowflakes = this.getLinkedDiscords(playername);
                            if (!snowflakes.isEmpty()) {
                                List<String> discords = new ArrayList<>();
                                // Agrupar los datos de cada cuenta en una nueva lista
                                for (int i = 0; i < snowflakes.size(); i++) {
                                    // Declarar i para usarla posteriormente
                                    int finalI = i;
                                    String snowflake = snowflakes.get(i);
                                    // Buscar miembro con el Snowflake obtenido
                                    discord.retrieveUserById(snowflake).queue(user -> {
                                        // Recopilar nombre y discriminador
                                        String name = user.getName();
                                        String discriminator = user.getDiscriminator();
                                        // Añadir datos a la lista agrupadora
                                        discords.add(name + "#" + discriminator);
                                        // Detectar última cuenta vinculada
                                        if (finalI == snowflakes.size() - 1) {
                                            // Enviar mensaje al sender con las cuentas de Discord asociadas a la cuenta de Minecraft indicada
                                            player.sendMessage(TextComponent.fromLegacyText(ECMessages.MINECRAFT_LINKED_ACCOUNTS.getMinecraftString().replace("{player}", playername).replace("{discords}", String.join(", ", discords))));
                                        }
                                    });
                                }
                            } else {
                                // Informar de que no hay ninguna cuenta de Discord vinculada al usuario de Minecraft indicado
                                player.sendMessage(TextComponent.fromLegacyText(ECMessages.MINECRAFT_NO_LINKED_ACCOUNTS.getMinecraftString().replace("{player}", playername)));
                            }
                        } catch (IOException e) {
                            // Error al contactar con los servidores
                            player.sendMessage(TextComponent.fromLegacyText(ECMessages.MINECRAFT_MOJANG_SERVERS_DOWN.getMinecraftString()));
                            // Comprobar si es Rate Limit e informar
                            if (e.getMessage().contains("HTTP response code: 429")) {
                                player.sendMessage(TextComponent.fromLegacyText(ECMessages.MINECRAFT_GENERIC_ERROR_WITH_CODE.getMinecraftString().replace("{code}", "429 Limit Reached")));
                                minecraft.getLogger().severe("You have reached the request limit of the Mojang api! Please retry later!");
                            } else {
                                player.sendMessage(TextComponent.fromLegacyText(ECMessages.MINECRAFT_GENERIC_ERROR_WITH_CODE.getMinecraftString().replace("{code}", "Unknown")));
                                e.printStackTrace();
                            }
                        } catch (Exception e) {
                            // Jugador no encontrado
                            player.sendMessage(TextComponent.fromLegacyText(ECMessages.MINECRAFT_PLAYER_NOT_FOUND.getMinecraftString()));
                        }
                    } else {
                        player.sendMessage(TextComponent.fromLegacyText(ECMessages.MINECRAFT_COMMAND_CONSTRUCTOR_NOT_FOUND.getMinecraftString()));
                    }
                    break;
                default:
                    player.sendMessage(TextComponent.fromLegacyText(ECMessages.MINECRAFT_COMMAND_CONSTRUCTOR_NOT_FOUND.getMinecraftString()));
                    break;
            }
        } else {
            sender.sendMessage(TextComponent.fromLegacyText(ECMessages.MINECRAFT_CONSOLE_NOT_ALLOWED.getMinecraftString()));
        }
    }

    private void link(String snowflake, String uuid) throws SQLException {
        // Añadir registro a la base de datos
        PreparedStatement statement = mysql.prepareStatement("INSERT INTO edorasconnect_discord (discord, minecraft) VALUES (?, ?);");
        statement.setString(1, snowflake);
        statement.setString(2, uuid);
        statement.executeUpdate();
        // Closing database resources
        statement.close();
        // Añadir rol al usuario
        Guild guild = discord.getGuildById(ECConfig.DISCORD_GUILD.getString());
        Role role = Objects.requireNonNull(guild, "Guild must not be null").getRoleById(ECConfig.DISCORD_MEMBER_ROLE.getString());
        discord.retrieveUserById(snowflake).queue(user -> guild.retrieveMember(user).queue(member -> guild.addRoleToMember(member, Objects.requireNonNull(role, "Role must not be null")).queue()));
    }

    private void unlink(String uuid) throws SQLException {
        // Eliminar rol de todas las cuentas vinculadas
        PreparedStatement statement = mysql.prepareStatement("SELECT discord FROM edorasconnect_discord WHERE minecraft = ?;");
        statement.setString(1, uuid);
        if (statement.execute()) {
            ResultSet resultSet = statement.getResultSet();
            while (resultSet.next()) {
                // Obtener snowflake de la base de datos
                String snowflake = resultSet.getString("discord");
                Guild guild = discord.getGuildById(ECConfig.DISCORD_GUILD.getString());
                Role role = Objects.requireNonNull(guild, "Guild must not be null").getRoleById(ECConfig.DISCORD_MEMBER_ROLE.getString());
                // Encontrar al usuario por Snowflake y eliminar rol
                discord.retrieveUserById(snowflake).queue(user -> guild.retrieveMember(user).queue(member -> guild.removeRoleFromMember(member, Objects.requireNonNull(role, "Role must not be null")).queue()));
            }
            // Closing database resources
            resultSet.close();
        }
        // Closing database resources
        statement.close();

        // Eliminar registros
        PreparedStatement deleteStatement = mysql.prepareStatement("DELETE FROM edorasconnect_discord WHERE minecraft = ?;");
        deleteStatement.setString(1, uuid);
        deleteStatement.executeUpdate();
        // Closing database resources
        deleteStatement.close();
    }

    private boolean isDiscordLinked(@NotNull String uuid) throws SQLException {
        PreparedStatement statement = mysql.prepareStatement("SELECT NULL FROM edorasconnect_discord WHERE minecraft = ?;");
        statement.setString(1, uuid);
        statement.execute();
        ResultSet result = statement.getResultSet();
        boolean linked = result.first();
        statement.close();
        return linked;
    }

    private List<String> getLinkedDiscords(@NotNull String playername) throws Exception {
        List<String> snowflakes = new ArrayList<>();
        String playeruuid = Utils.getUniqueId(playername);
        // Consultar Snowflake en la base de datos con la ID obtenida
        PreparedStatement getSnowflakeQuery = mysql.prepareStatement("SELECT discord FROM edorasconnect_discord WHERE minecraft = ?;");
        getSnowflakeQuery.setString(1, playeruuid);
        getSnowflakeQuery.execute();
        ResultSet resultSet = getSnowflakeQuery.getResultSet();
        while (resultSet.next()) {
            snowflakes.add(resultSet.getString("discord"));
        }
        // Closing database resources
        resultSet.close();
        getSnowflakeQuery.close();
        return snowflakes;
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        List<String> tabComplete = new ArrayList<>();
        if (args.length == 1) {
            if ("link".startsWith(args[0].toLowerCase())) {
                tabComplete.add("link");
            }
            if ("unlink".startsWith(args[0].toLowerCase())) {
                tabComplete.add("unlink");
            }
            if ("check".startsWith(args[0].toLowerCase()) && sender.hasPermission("edorasconnect.link.check")) {
                tabComplete.add("check");
            }
        } else if (args[0].equalsIgnoreCase("check") && sender.hasPermission("edorasconnect.link.check") && args.length == 2) {
            for (ProxiedPlayer player : minecraft.getProxy().getPlayers()) {
                if (player.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                    tabComplete.add(player.getName());
                }
            }
        }
        return tabComplete;
    }
}