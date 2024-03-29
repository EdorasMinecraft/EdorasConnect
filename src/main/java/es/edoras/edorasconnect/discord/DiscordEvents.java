package es.edoras.edorasconnect.discord;

import com.zaxxer.hikari.HikariDataSource;
import es.edoras.edorasconnect.ECConfig;
import es.edoras.edorasconnect.ECMessages;
import es.edoras.edorasconnect.EdorasConnect;
import es.edoras.edorasconnect.Utils;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.voice.*;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.events.user.UserActivityEndEvent;
import net.dv8tion.jda.api.events.user.UserActivityStartEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class DiscordEvents extends ListenerAdapter {
    private final EdorasConnect plugin;
    private final HikariDataSource hikari;

    public DiscordEvents(EdorasConnect plugin, HikariDataSource hikari){
        this.plugin = plugin;
        this.hikari = hikari;
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        // Limpiar comandos globales
        event.getJDA().updateCommands().queue();

        // Limpiar y registrar comandos de la guild de Edoras
        // Vinculación
        Guild guild = event.getJDA().getGuildById(ECConfig.DISCORD_GUILD.getString());
        if(guild == null){
            this.plugin.getProxy().getLogger().severe("Discord guild is null! Is the id correct?");
            return;
        }
        guild.updateCommands().queue();
        guild.updateCommands().addCommands(
                Commands.slash("vincular", ECMessages.DISCORD_LINK_DESCRIPTION.getString()).addOption(OptionType.STRING, "nick", ECMessages.DISCORD_LINK_NICK_OPTION_DESCRIPTION.getString(), true),
                Commands.slash("desvincular", ECMessages.DISCORD_UNLINK_DESCRIPTION.getString())
        ).queue();

        // Comprobar que todos los Socios están vinculados
        plugin.checkMembersWithLinkedRole();
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if(event.getGuild() == null){
            return;
        }

        InteractionHook hook = event.getHook();
        hook.setEphemeral(true);
        event.deferReply(true).queue();
        switch(event.getName()){
            case "vincular":
                // Comprobar que el proceso se hace en el canal correcto
                if(this.correctChannel(event.getChannel().asTextChannel())){
                    // Try por posibles fallos en la base de datos
                    try {
                        // Comprobar que la cuenta no está vinculada
                        if (!this.isDiscordLinked(event.getUser().getId())) {
                            String playername = Objects.requireNonNull(event.getOption("nick"), "User must have introduced an argument for the nick option").getAsString();
                            // Comprobar que el usuario con dicho nick está conectado a Edoras
                            ProxiedPlayer player;
                            if ((player = plugin.getProxy().getPlayer(playername)) != null) {
                                // Comprobar que el usuario no ha superado el límite de cuentas vinculadas
                                if (!this.hasReachedLimit(player.getUniqueId().toString())) {
                                    // Comprobar que no hay ninguna vinculación pendiente
                                    if (!DiscordStatics.pendingLinks.containsKey(event.getUser().getId())) {
                                        // Todo correcto, vincular
                                        if (this.requestLink(player, event.getUser())) {
                                            hook.sendMessage(ECMessages.DISCORD_LINK_SUCCESSFUL.getString()).queue();
                                        } else {
                                            hook.sendMessage(ECMessages.DISCORD_LINK_ERROR.getString()).queue();
                                        }
                                    } else {
                                        hook.sendMessage(ECMessages.DISCORD_LINK_ALREADY_IN_PROCESS.getString()).queue();
                                    }
                                } else {
                                    hook.sendMessage(ECMessages.DISCORD_LINK_LIMIT_REACHED.getString()).queue();
                                }
                            } else {
                                hook.sendMessage(ECMessages.DISCORD_LINK_PLAYER_NOT_FOUND.getString()).queue();
                            }
                        } else {
                            hook.sendMessage(ECMessages.DISCORD_LINK_ALREADY_LINKED.getString()).queue();
                        }
                    } catch (SQLException e) {
                        hook.sendMessage(ECMessages.DISCORD_LINK_ERROR_WITH_CODE.getString().replace("{code}", "`Database connection error`")).queue();
                        e.printStackTrace();
                    }
                } else {
                    hook.sendMessage(ECMessages.DISCORD_LINK_WRONG_CHANNEL.getString().replace("{channel}", Objects.requireNonNull(event.getGuild().getTextChannelById(ECConfig.DISCORD_LINK_CHANNEL.getString()), "Link channel must not be null").getName())).addActionRow(
                            Button.link("https://discord.com/channels/" + ECConfig.DISCORD_GUILD.getString() + "/" + ECConfig.DISCORD_LINK_CHANNEL.getString(), "Ir al canal #" + Objects.requireNonNull(event.getGuild().getTextChannelById(ECConfig.DISCORD_LINK_CHANNEL.getString()), "Link channel must not be null").getName())
                    ).queue();
                }
                break;
            case "desvincular":
                // Comprobar que el proceso se hace en el canal correcto
                if(this.correctChannel(event.getChannel().asTextChannel())) {
                    // Try por posibles fallos en la base de datos
                    try {
                        // Comprobar que la cuenta está vinculada
                        if (this.isDiscordLinked(event.getUser().getId())) {
                            // Comprobar que se ha desvinculado correctamente
                            if (this.unlink(event.getGuild(), event.getMember())) {
                                hook.sendMessage(ECMessages.DISCORD_UNLINK_SUCCESSFUL.getString()).queue();
                            } else {
                                hook.sendMessage(ECMessages.DISCORD_UNLINK_ERROR.getString()).queue();
                            }
                        } else {
                            hook.sendMessage(ECMessages.DISCORD_UNLINK_PLAYER_NOT_FOUND.getString()).queue();
                        }
                    } catch (SQLException e) {
                        hook.sendMessage(ECMessages.DISCORD_UNLINK_ERROR_WITH_CODE.getString().replace("{code}", "Database connection error")).queue();
                        e.printStackTrace();
                    }
                }
                break;
            default:
                hook.sendMessage(ECMessages.DISCORD_GENERIC_ERROR_WITH_CODE.getString().replace("{code}", event.getName())).queue();
                throw new IllegalStateException("Unexpected value: " + event.getName());
        }
    }

    // Comprobar si un usuario al entrar en la Guild tiene la cuenta vinculada
    @Override
    public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent event){
        try {
            // Comprobar si el usuario que ha entrado tiene la cuenta vinculada
            if(this.isDiscordLinked(event.getMember().getId())){
                Guild guild = event.getGuild();
                guild.addRoleToMember(event.getMember(), Objects.requireNonNull(guild.getRoleById(ECConfig.DISCORD_MEMBER_ROLE.getString()), "Role could not be found (null)")).queue();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void sendMinecraftMessageToVoiceMembers(User user, AudioChannelUnion channel, ECMessages enableStatus, ECMessages disableStatus, boolean condition){
        // Si no hay canal asociado, paramos directamente
        if(channel == null) return;

        // Comprobar si se puede obtener el nombre de Minecraft (si está vinculado y los servidores de Mojang funcionan)
        String playername;

        try {
            if(this.isDiscordLinked(user.getId())){
                String playeruuid = this.getMinecraftUUIDfromDatabase(user.getId());
                playername = Utils.getName(playeruuid);
            } else {
                playername = user.getName() + "#" + user.getDiscriminator();
            }
        } catch (Exception e) {
            // En caso de algún error, usar nombre y discriminador de Discord
            playername = user.getName() + "#" + user.getDiscriminator();
        }

        // Enviar mensaje en Minecraft a todos los usuarios del canal de voz
        for(Member member : channel.asVoiceChannel().getMembers()){
            String snowflake = member.getUser().getId();
            // Enviar mensaje a los usuarios vinculados
            try {
                if(this.isDiscordLinked(snowflake)){
                    String playeruuid = this.getMinecraftUUIDfromDatabase(snowflake);
                    // Enviar mensaje si el usuario está conectado
                    ProxiedPlayer player;
                    if(playeruuid != null && (player = plugin.getProxy().getPlayer(UUID.fromString(playeruuid))) != null){
                        if(condition) {
                            player.sendMessage(TextComponent.fromLegacyText(enableStatus.getMinecraftString().replace("{user}", playername)));
                        } else {
                            player.sendMessage(TextComponent.fromLegacyText(disableStatus.getMinecraftString().replace("{user}", playername)));
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onGuildVoiceDeafen(@NotNull GuildVoiceDeafenEvent event){
        this.sendMinecraftMessageToVoiceMembers(event.getMember().getUser(), event.getVoiceState().getChannel(), ECMessages.MINECRAFT_DISCORD_USER_DEAFEN, ECMessages.MINECRAFT_DISCORD_USER_UNDEAFEN, event.isDeafened());
    }

    @Override
    public void onGuildVoiceMute(@NotNull GuildVoiceMuteEvent event){
        this.sendMinecraftMessageToVoiceMembers(event.getMember().getUser(), event.getVoiceState().getChannel(), ECMessages.MINECRAFT_DISCORD_USER_MUTED, ECMessages.MINECRAFT_DISCORD_USER_UNMUTED, event.isMuted());
    }

    @Override
    public void onGuildVoiceStream(@NotNull GuildVoiceStreamEvent event){
        this.sendMinecraftMessageToVoiceMembers(event.getMember().getUser(), event.getVoiceState().getChannel(), ECMessages.MINECRAFT_DISCORD_USER_STREAM_ON, ECMessages.MINECRAFT_DISCORD_USER_STREAM_OFF, event.isStream());
    }

    @Override
    public void onGuildVoiceVideo(@NotNull GuildVoiceVideoEvent event){
        this.sendMinecraftMessageToVoiceMembers(event.getMember().getUser(), event.getVoiceState().getChannel(), ECMessages.MINECRAFT_DISCORD_USER_VIDEO_ON, ECMessages.MINECRAFT_DISCORD_USER_VIDEO_OFF, event.isSendingVideo());
    }

    @Override
    public void onGuildVoiceUpdate(@NotNull GuildVoiceUpdateEvent event){
        // Move
        if(event.getChannelLeft() != null && event.getChannelJoined() != null){
            // Historial de conexiones
            TextChannel logchannel = event.getGuild().getTextChannelById(ECConfig.DISCORD_LOG_CHANNEL.getString());
            if(logchannel != null){
                logchannel.sendMessage(ECMessages.DISCORD_LOG_MEMBER_MOVED_VOICE_CHANNEL.getString().replace("{oldvoicechannel}", event.getChannelLeft().getName()).replace("{newvoicechannel}", event.getChannelJoined().getName()).replace("{user}", event.getMember().getUser().getName()).replace("{discriminator}", event.getMember().getUser().getDiscriminator())).queue();
            }

            // Enviar mensajes a los miembros de canal dentro del servidor (solo si esta vinculado)
            User user = event.getMember().getUser();

            // Enviar mensaje al canal de texto sobre la acción del usuario
            this.sendPublicLogMessage(event.getChannelJoined(), event.getGuild(), user, ECMessages.DISCORD_LOG_MEMBER_JOINED_VOICE_CHANNEL.getString());
            // Si el canal de voz queda vacio, ahorrar mensaje
            if(!event.getChannelLeft().getMembers().isEmpty()) {
                this.sendPublicLogMessage(event.getChannelLeft(), event.getGuild(), user, ECMessages.DISCORD_LOG_MEMBER_LEFT_VOICE_CHANNEL.getString());
            }

            // Enviar mensaje por Minecraft, a los usuarios del canal de voz, de la acción del usuario
            this.sendMessage(event.getChannelJoined().getMembers(), event.getChannelJoined().asVoiceChannel(), user, ECMessages.MINECRAFT_VOICECHANNEL_JOIN.getMinecraftString());
            this.sendMessage(event.getChannelLeft().getMembers(), event.getChannelLeft().asVoiceChannel(), user, ECMessages.MINECRAFT_VOICECHANNEL_LEAVE.getMinecraftString());
        } else if(event.getChannelLeft() == null && event.getChannelJoined() != null){
            // Join
            // Historial de conexiones
            TextChannel logchannel = event.getGuild().getTextChannelById(ECConfig.DISCORD_LOG_CHANNEL.getString());
            if(logchannel != null){
                logchannel.sendMessage(ECMessages.DISCORD_LOG_MEMBER_JOINED_VOICE_CHANNEL.getString().replace("{voicechannel}", event.getChannelJoined().getName()).replace("{user}", event.getMember().getUser().getName()).replace("{discriminator}", event.getMember().getUser().getDiscriminator())).queue();
            }

            // Enviar mensajes a los miembros de canal dentro del servidor (solo si esta vinculado)
            User user = event.getMember().getUser();

            // Enviar mensaje al canal de texto sobre la acción del usuario
            this.sendPublicLogMessage(event.getChannelJoined(), event.getGuild(), user, ECMessages.DISCORD_LOG_MEMBER_JOINED_VOICE_CHANNEL.getString());

            // Enviar mensaje por Minecraft, a los usuarios del canal de voz, de la acción del usuario
            this.sendMessage(event.getChannelJoined().getMembers(), event.getChannelJoined().asVoiceChannel(), user, ECMessages.MINECRAFT_VOICECHANNEL_JOIN.getMinecraftString());
        } else if (event.getChannelLeft() != null && event.getChannelJoined() == null){
            // Leave
            // Historial de conexiones
            TextChannel logchannel = event.getGuild().getTextChannelById(ECConfig.DISCORD_LOG_CHANNEL.getString());
            if(logchannel != null){
                logchannel.sendMessage(ECMessages.DISCORD_LOG_MEMBER_LEFT_VOICE_CHANNEL.getString().replace("{voicechannel}", event.getChannelLeft().getName()).replace("{user}", event.getMember().getUser().getName()).replace("{discriminator}", event.getMember().getUser().getDiscriminator())).queue();
            }

            // Enviar mensajes a los miembros de canal dentro del servidor (solo si esta vinculado)
            User user = event.getMember().getUser();

            // Enviar mensaje al canal de texto sobre la acción del usuario
            // Si el canal de voz queda vacio, ahorrar mensaje
            if(!event.getChannelLeft().getMembers().isEmpty()) {
                this.sendPublicLogMessage(event.getChannelLeft(), event.getGuild(), user, ECMessages.DISCORD_LOG_MEMBER_LEFT_VOICE_CHANNEL.getString());
            }

            // Enviar mensaje por Minecraft, a los usuarios del canal de voz, de la acción del usuario
            this.sendMessage(event.getChannelLeft().getMembers(), event.getChannelLeft().asVoiceChannel(), user, ECMessages.MINECRAFT_VOICECHANNEL_LEAVE.getMinecraftString());
        }
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event){
        // Ignorar bots y sistema
        if(event.getAuthor().isBot() || event.getAuthor().isSystem()){
            return;
        }

        // Si es un mensaje en el canal de vinculación y el usuario no tiene permiso de MESSAGE_MANAGE, eliminar
        if(event.getChannel().getId().equals(ECConfig.DISCORD_LINK_CHANNEL.getString()) && !event.getMember().hasPermission(Permission.MESSAGE_MANAGE)){
            event.getMessage().delete().queue();
        }

        // Si el mensaje es de un canal de voz, enviar mensaje del usuario por Minecraft
        User user = event.getAuthor();

        // Obtener canal de voz asociado al canal de texto
        VoiceChannel voiceChannel = event.getChannel().asVoiceChannel();
        // Comprobar si se puede obtener el nombre de Minecraft (si está vinculado y los servidores de Mojang funcionan)
        String playername;
        try {
            if(this.isDiscordLinked(user.getId())){
                String playeruuid = this.getMinecraftUUIDfromDatabase(user.getId());
                playername = Utils.getName(playeruuid);
            } else {
                playername = user.getName() + "#" + user.getDiscriminator();
            }
        } catch (Exception e) {
            // En caso de algún error, usar nombre y discriminador de Discord
            playername = user.getName() + "#" + user.getDiscriminator();
        }
        // Enviar mensaje en Minecraft a todos los usuarios del canal de voz
        for(Member member : voiceChannel.getMembers()){
            String snowflake = member.getUser().getId();
            // Enviar mensaje a los usuarios vinculados
            try {
                if(this.isDiscordLinked(snowflake)){
                    String playeruuid = this.getMinecraftUUIDfromDatabase(snowflake);
                    // Enviar mensaje si el usuario está conectado
                    ProxiedPlayer player;
                    if((player = plugin.getProxy().getPlayer(UUID.fromString(playeruuid))) != null){
                        Message message = event.getMessage();
                        if(!message.getContentRaw().isEmpty() && message.getAttachments().isEmpty()) {
                            player.sendMessage(TextComponent.fromLegacyText(ECMessages.MINECRAFT_DISCORD_USER_MESSAGE.getMinecraftString().replace("{message}", message.getContentRaw()).replace("{user}", playername)));
                            plugin.getLogger().info(ECMessages.MINECRAFT_DISCORD_USER_MESSAGE.getMinecraftString().replace("{message}", message.getContentRaw()).replace("{user}", playername));
                        } else if(message.getAttachments().size() >= 1){
                            player.sendMessage(TextComponent.fromLegacyText(ECMessages.MINECRAFT_DISCORD_USER_MESSAGE_WITH_FILE.getMinecraftString().replace("{message}", !message.getContentRaw().isEmpty() ? message.getContentRaw() : ECMessages.MINECRAFT_DISCORD_NO_MESSAGE.getMinecraftString()).replace("{file}", message.getAttachments().get(0).getFileName()).replace("{user}", playername)));
                            plugin.getLogger().info(ECMessages.MINECRAFT_DISCORD_USER_MESSAGE_WITH_FILE.getMinecraftString().replace("{message}", !message.getContentRaw().isEmpty() ? message.getContentRaw() : ECMessages.MINECRAFT_DISCORD_NO_MESSAGE.getMinecraftString()).replace("{file}", message.getAttachments().get(0).getFileName()).replace("{user}", playername));
                        } else {
                            player.sendMessage(TextComponent.fromLegacyText(ECMessages.MINECRAFT_DISCORD_USER_UNKNOWN_MESSAGE.getMinecraftString().replace("{user}", playername)));
                            plugin.getLogger().info(ECMessages.MINECRAFT_DISCORD_USER_UNKNOWN_MESSAGE.getMinecraftString().replace("{user}", playername));
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendPublicLogMessage(AudioChannelUnion channel, Guild guild, User useraction, String message){
        // Manda los cambios de estado de un chat de voz (gente que se une, que se va, etc) a su chat de texto
        plugin.getLogger().info(message.replace("{voicechannel}", channel.getName()).replace("{user}", useraction.getName()).replace("{discriminator}", useraction.getDiscriminator()));
        channel.asGuildMessageChannel().sendMessage(message.replace("{voicechannel}", channel.getName()).replace("{user}", useraction.getName()).replace("{discriminator}", useraction.getDiscriminator())).queue();
    }

    // Enviar mensajes a los miembros del canal de que un usuario se ha unido/ido
    private void sendMessage(List<Member> memberList, VoiceChannel voiceChannel, User user, String message){
        // Enviar mensaje a los usuarios del canal de voz conectados a Minecraft
        // MEMBER DE MEMBERLIST -> TODOS LOS USUARIOS DEL CANAL
        // USER -> USER QUE ENTRA O SALE
        for(Member member : memberList){
            // Si es un bot o el sistema, ignorar y saltar
            if(member.getUser().isBot() || member.getUser().isSystem()){
                continue;
            }

            try {
                // Obtener Snowflake del usuario a enviar el mensaje
                String snowflake = member.getUser().getId();
                // Obtener UUID asociada al Snowflake obtenido
                String playeruuid = this.getMinecraftUUIDfromDatabase(snowflake);
                // Obtener UUID del usuario que entra o sale
                String useruuid = this.getMinecraftUUIDfromDatabase(user.getId());
                // Declarar jugador que realiza la acción y comprobar si está vinculado o no para mostrar un mensaje u otro
                String playername;
                if(this.isDiscordLinked(user.getId())){
                    playername = Utils.getName(useruuid);
                } else {
                    playername = user.getName() + "#" + user.getDiscriminator();
                }
                ProxiedPlayer player;
                // Si el jugador está conectado, enviar mensaje
                // Soporta UUID y XUID
                if (playeruuid != null && (player = plugin.getProxy().getPlayer(UUID.fromString(playeruuid))) != null) {
                    player.sendMessage(TextComponent.fromLegacyText(message.replace("{voicechannel}", voiceChannel.getName()).replace("{user}", playername)));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private String getMinecraftUUIDfromDatabase(String snowflake) throws SQLException {
        Connection connection = hikari.getConnection();
        PreparedStatement preparedStatement = connection.prepareStatement("SELECT minecraft FROM edorasconnect_discord WHERE discord = ?;", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
        preparedStatement.setString(1, snowflake);
        ResultSet resultSet = preparedStatement.executeQuery();
        if(resultSet.first()){
            String uuid = resultSet.getString("minecraft");
            // Closing database resources
            preparedStatement.close();
            resultSet.close();
            connection.close();
            return uuid;
        } else {
            // Closing database resources
            preparedStatement.close();
            resultSet.close();
            connection.close();
            return null;
        }
    }

    private boolean correctChannel(TextChannel textChannel){
        return textChannel.getId().equals(ECConfig.DISCORD_LINK_CHANNEL.getString());
    }

    private boolean isDiscordLinked(String snowflake) throws SQLException {
        Connection connection = hikari.getConnection();
        PreparedStatement statement = connection.prepareStatement("SELECT NULL FROM edorasconnect_discord WHERE discord = ?;", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
        statement.setString(1, snowflake);
        statement.execute();
        ResultSet result = statement.getResultSet();
        boolean linked = result.first();
        // Closing database resources and return if the account is linked
        statement.close();
        result.close();
        connection.close();
        return linked;
    }

    private boolean hasReachedLimit(String uuid) throws SQLException {
        Connection connection = hikari.getConnection();
        PreparedStatement statement = connection.prepareStatement("SELECT NULL FROM edorasconnect_discord WHERE minecraft = ?;", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
        statement.setString(1, uuid);
        statement.execute();
        ResultSet result = statement.getResultSet();
        result.last();
        boolean limitReached = result.getRow() >= ECConfig.DISCORD_MAX_ACCOUNTS_LINKED.getInteger();
        // Closing database resources
        statement.close();
        result.close();
        connection.close();
        return limitReached;
    }

    private boolean requestLink(ProxiedPlayer player, User user){
        String snowflake = user.getId();
        // Petición de verificación / DISCORD / MINECRAFT
        DiscordStatics.pendingLinks.put(snowflake, player.getUniqueId().toString());
        // Caducar verificación a los 5 minutos
        plugin.getProxy().getScheduler().schedule(plugin, () -> DiscordStatics.pendingLinks.remove(snowflake), ECConfig.LINK_EXPIRATION.getInteger(), TimeUnit.SECONDS);
        // Avisar de verificación
        TextComponent linkMessage = new TextComponent(TextComponent.fromLegacyText(ECMessages.MINECRAFT_LINK_VERIFICATION.getMinecraftString().replace("{name}", user.getName()).replace("{discriminator}", user.getDiscriminator()).replace("{snowflake}", user.getId())));
        linkMessage.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/discord link " + snowflake));
        linkMessage.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(TextComponent.fromLegacyText(ECMessages.MINECRAFT_LINK_VERIFICATION_HOVER.getMinecraftString()))));
        player.sendMessage(linkMessage);
        return true;
    }

    private boolean unlink(Guild guild, Member member){
        try {
            Connection connection = hikari.getConnection();
            PreparedStatement statement = connection.prepareStatement("DELETE FROM edorasconnect_discord WHERE discord = ?;", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            statement.setString(1, member.getId());
            statement.executeUpdate();
            guild.removeRoleFromMember(member, Objects.requireNonNull(guild.getRoleById(ECConfig.DISCORD_MEMBER_ROLE.getString()), "Role could not be found (null)")).queue();
            statement.close();
            connection.close();
            return true;
        } catch (SQLException | NullPointerException e) {
            e.printStackTrace();
            return false;
        }
    }
}
