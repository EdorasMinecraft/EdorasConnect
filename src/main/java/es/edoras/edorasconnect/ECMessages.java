package es.edoras.edorasconnect;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.config.Configuration;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum ECMessages {
    // DISCORD
    DISCORD_LINK_DESCRIPTION("discord.link-description", "Vincula esta cuenta de Discord a tu cuenta de Minecraft"),
    DISCORD_LINK_NICK_OPTION_DESCRIPTION("discord.link-nick-option-description", "Usuario de Minecraft a vincular a esta cuenta de Discord"),
    DISCORD_LINK_SUCCESSFUL("discord.link-successful", "¡Perfecto! Te hemos enviado un mensaje dentro del servidor de Minecraft para que puedas verificar tu cuenta. Revísalo rápido, ya que el código caducará en breve."),
    DISCORD_LINK_ERROR("discord.link-error", "Ha ocurrido un error al vincular tu cuenta de Discord. Vuelve a intentarlo más tarde. Si el problema persiste, ponte en contacto con el staff."),
    DISCORD_LINK_ERROR_WITH_CODE("discord.link-error-with-code", "Ha ocurrido un error al vincular esta cuenta de Discord. Por favor, reporta este fallo con el código de error {code}."),
    DISCORD_LINK_ALREADY_IN_PROCESS("discord.link-already-in-process", "Ya tienes una petición de vinculación pendiente. Espera hasta cinco minutos para poder pedir una nueva."),
    DISCORD_LINK_LIMIT_REACHED("discord.link-limit-reached", "¡Vaya! Esta cuenta de Minecraft se encuentra vinculada a demasiadas cuentas de Discord. Desvincula alguna cuenta ejecutando el comando `/desvincular` desde ella, o desvincula todas ejecutando ese mismo comando en Minecraft."),
    DISCORD_LINK_PLAYER_NOT_FOUND("discord.link-player-not-found", "No hemos encontrado a ningún usuario con ese nombre que se encuentre conectado en Edoras. ¿Lo has escrito bien?"),
    DISCORD_LINK_ALREADY_LINKED("discord.link-already-linked", "¡Vaya! Parece que la cuenta de Discord que estás utilizando ya se encuentra vinculada a una cuenta de Minecraft."),
    DISCORD_LINK_WRONG_CHANNEL("discord.link-wrong-channel", ""),
    DISCORD_UNLINK_DESCRIPTION("discord.unlink-description", "Desvincula esta cuenta de Discord de una cuenta de Minecraft"),
    DISCORD_UNLINK_SUCCESSFUL("discord.unlink-successful", "Has desvinculado correctamente esta cuenta de Discord."),
    DISCORD_UNLINK_PLAYER_NOT_FOUND("discord.unlink-player-not-found", "Esta cuenta de Discord no se encuentra vinculada a ningún usuario de Minecraft."),
    DISCORD_UNLINK_ERROR("discord.unlink-error", "Ha ocurrido un error al desvincular esta cuenta de Discord. Vuelve a intentarlo más tarde. Si el problema persiste, ponte el contacto con el staff."),
    DISCORD_UNLINK_ERROR_WITH_CODE("discord.unlink-error-with-code", "Ha ocurrido un error al desvincular esta cuenta de Discord. Por favor, reporta este fallo con el código de error `{code}`."),
    DISCORD_GENERIC_ERROR_WITH_CODE("discord.generic-error-with-code", "Ha ocurrido un error inesperado al procesar esta solicitud. Por favor, reporta este error al staff con el código de error `{code}`"),
    DISCORD_LOG_MEMBER_JOINED_VOICE_CHANNEL("discord.voicechannel-join", "{user}#{discriminator} se ha unido al canal de voz `{voicechannel}`"),
    DISCORD_LOG_MEMBER_LEFT_VOICE_CHANNEL("discord.voicechannel-leave", "{user}#{discriminator} ha salido del canal de voz `{voicechannel}`"),
    DISCORD_LOG_MEMBER_MOVED_VOICE_CHANNEL("discord.voicechannel-move", "{user}#{discriminator} se ha movido del canal de voz `{oldvoicechannel}` a `{newvoicechannel}`"),

    // DISCORD
    MINECRAFT_PREFIX("minecraft.prefix", "EdorasConnect: "),
    MINECRAFT_GENERIC_ERROR_WITH_CODE("minecraft.generic-error-with-code", "Ha ocurrido un error inesperado al procesar esta solicitud. Por favor, reporta este error al staff con el código de error {code}"),
    MINECRAFT_COMMAND_CONSTRUCTOR_NOT_FOUND("minecraft.command-constructor-not-found", "No se ha encontrado ningún comando disponible bajo los parámetros introducidos."),
    MINECRAFT_NO_LINKED_ACCOUTS_FOUND("minecraft.unlink.no-linked-accounts-found", "No se ha encontrado ninguna cuenta de Discord vinculada a tu usuario de Minecraft."),
    MINECRAFT_LINK_VERIFICATION("minecraft.link.link-verification", "Haz click aquí para vincular esta cuenta de Minecraft con la cuenta de Discord {name}#{discriminator}. Si no puedes clicar este mensaje, introduce el comando /discord link {snowflake}"),
    MINECRAFT_LINK_VERIFICATION_HOVER("minecraft.link.link-verification-hover", "Haz click aquí para finalizar el proceso de vinculación con Discord"),
    MINECRAFT_UNLINK_ASK_CONFIRMATION("minecraft.unlink.ask-confirmation", "Estás a punto de desvincular todas las cuentas de Discord asociadas a esta cuenta. Si deseas desvincular una en concreto, puedes hacerlo desde ella a través del canal #vincular de nuestro servidor de Discord. Si estás seguro de que quieres desvincular todas tus cuentas, vuelve a ejecutar este comando."),
    MINECRAFT_UNLINKED_SUCCESSFULLY("minecraft.unlink.unlinked-successfully", "Se han desvinculado todas las cuentas de Discord asociadas a tu usuario de Minecraft."),
    MINECRAFT_ACCOUNT_LINKED("minecraft.link.account-linked", "Se ha vinculado correctamente la cuenta de Discord {name}#{discriminator} a la cuenta de Minecraft {player}"),
    MINECRAFT_WRONG_ACCOUNT("minecraft.link.wrong-account", "Esta cuenta de Discord está siendo vinculada a una cuenta de Minecraft distinta. ¿Te has equivocado de cuenta?"),
    MINECRAFT_NO_LINK_EXPECTED("minecraft.link.no-link-expected", "La cuenta de Discord indicada no tiene ninguna petición de vinculación pendiente."),
    MINECRAFT_CONSOLE_NOT_ALLOWED("minecraft.console-not-allowed", "Solo un jugador de Minecraft puede ejecutar este comando."),
    MINECRAFT_TASKS_ACCOUNT_NOT_LINKED("minecraft.tasks.account-not-linked", "{name}#{discriminator} no tiene la cuenta vinculada correctamente. Quitando rol de vinculado..."),
    MINECRAFT_LINKED_ACCOUNTS("minecraft.link.check.linked-accounts", "El usuario {player} tiene las siguientes cuentas de Discord vinculadas: {discords}"),
    MINECRAFT_NO_LINKED_ACCOUNTS("minecraft.link.check.no-linked-accounts", "{prefix} &#00eabaEl usuario {player} no tiene ninguna cuenta de Discord vinculada."),
    MINECRAFT_PLAYER_NOT_FOUND("minecraft.player-not-found", "{prefix} &#00eabaNo se ha encontrado ninguna cuenta de Minecraft con el nombre indicado."),
    MINECRAFT_MOJANG_SERVERS_DOWN("minecraft.mojang-servers-down", "{prefix} &#00eabaLos servidores de Mojang parecen no responder. Por favor, intenta vincular tu cuenta más tarde. Si este problema persiste, ponte en contacto con el Staff de Edoras."),
    MINECRAFT_VOICECHANNEL_JOIN("minecraft.discord.voicechannel-join", "{prefix} &#00eaba{user}#{discriminator} se ha unido al canal de voz {voicechannel}"),
    MINECRAFT_VOICECHANNEL_LEAVE("minecraft.discord.voicechannel-leave", "{prefix} &#00eaba{user}#{discriminator} ha salido del canal de voz {voicechannel}"),
    MINECRAFT_DISCORD_USER_MESSAGE("minecraft.discord.user-message", "{prefix} &#00eaba{user}&f: {message}"),
    MINECRAFT_DISCORD_USER_MESSAGE_WITH_FILE("minecraft.discord.user-message-with-file", "{prefix} &#00eaba{user}&f ha adjuntado el archivo {file}. &f{message}"),
    MINECRAFT_DISCORD_NO_MESSAGE("minecraft.discord.no-message", "(Mensaje vacio)"),
    MINECRAFT_LINK_SUCCESSFUL_GLOBAL_MESSAGE("minecraft.link.link-successful-global-message", "{prefix} &#00eaba{player} ha vinculado su cuenta de Discord. ¿A qué esperas para hacer tú también? https://discord.edoras.es/"),
    MINECRAFT_LINK_SUCCESSFUL_GLOBAL_MESSAGE_HOVER("minecraft.link.link-successful-global-message-hover", "{prefix} &#00eabaHaz click aquí para acceder al Discord oficial del servidor")
    ;

    private static Configuration config;
    private Object path;
    private Object def;

    ECMessages(String path, String def){
        this.path = path;
        this.def = def;
    }

    public static void setMessages(Configuration config){
        ECMessages.config = config;
    }

    public String getString(){
        return config.getString((String) this.path, (String) this.def);
    }

    public String getMinecraftString(){
        if(this.path.equals("minecraft.prefix")){
            return "Easter Egg!";
        }

        return this.parseMessage(config.getString((String) this.path, (String) this.def).replace("{prefix}", this.parseMessage(MINECRAFT_PREFIX.getString())));
    }

    // 1.16+ - RGB format
    private String parseMessage(String string){
        Pattern pattern = Pattern.compile("&#([0-9a-fA-F]){6}");
        Matcher matcher = pattern.matcher(string);
        StringBuffer sb = new StringBuffer();
        while(matcher.find()){
            String hex = matcher.group();
            matcher.appendReplacement(sb, ChatColor.of(hex.substring(1)).toString());
        }
        matcher.appendTail(sb);
        return ChatColor.translateAlternateColorCodes('&', sb.toString());
    }
}
