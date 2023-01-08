package de.webalf.slotbot.service.bot.command.event;

import de.webalf.slotbot.configuration.properties.DiscordProperties;
import de.webalf.slotbot.model.Event;
import de.webalf.slotbot.model.annotations.bot.SlashCommand;
import de.webalf.slotbot.service.bot.EventBotService;
import de.webalf.slotbot.service.bot.command.DiscordSlashCommand;
import de.webalf.slotbot.util.EventUtils;
import de.webalf.slotbot.util.bot.ChannelUtils;
import de.webalf.slotbot.util.bot.DiscordLocaleHelper;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.springframework.beans.factory.annotation.Autowired;

import static de.webalf.slotbot.util.bot.InteractionUtils.failedInteraction;
import static de.webalf.slotbot.util.bot.MessageUtils.sendMessage;

/**
 * @author Alf
 * @since 07.07.2021
 */
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Slf4j
@SlashCommand(name = "bot.slash.event.archive",
		description = "bot.slash.event.archive.description",
		authorization = Permission.MANAGE_CHANNEL)
public class ArchiveEvent implements DiscordSlashCommand {
	private final EventBotService eventBotService;
	private final DiscordProperties discordProperties;

	@Override
	public void execute(@NonNull SlashCommandInteractionEvent interactionEvent, @NonNull DiscordLocaleHelper locale) {
		log.trace("Slash command: unslot");

		final MessageChannelUnion channel = interactionEvent.getChannel();
		final Event event = eventBotService.findByChannelOrThrow(channel.getIdLong());
		//noinspection DataFlowIssue Guild only command
		long guildId = interactionEvent.getGuild().getIdLong();
		eventBotService.archiveEvent(event.getId(), guildId);

		final TextChannel archiveChannel = ChannelUtils.getChannel(discordProperties.getArchive(guildId), interactionEvent.getGuild(), "archive");
		if (archiveChannel == null) {
			failedInteraction(interactionEvent, locale.t("bot.slash.event.archive.response.configError"));
			return;
		}

		sendMessage(archiveChannel, EventUtils.buildArchiveMessage(event), ignored -> channel.delete().queue());
	}
}
