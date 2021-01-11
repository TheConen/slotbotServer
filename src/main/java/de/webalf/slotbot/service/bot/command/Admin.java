package de.webalf.slotbot.service.bot.command;

import de.webalf.slotbot.model.annotations.Command;
import de.webalf.slotbot.util.bot.MessageUtils;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;

import java.util.List;

import static de.webalf.slotbot.util.PermissionHelper.Authorization.ADMINISTRATIVE;

/**
 * @author Alf
 * @since 02.01.2021
 */
@Slf4j
@Command(name = "admin",
		description = "Admin Funktionalitäten!",
		argCount = {1},
		authorization = ADMINISTRATIVE,
		dmAllowed = true)
public class Admin implements DiscordCommand {
	@Override
	public void execute(Message message, List<String> args) {
		log.trace("Command: admin");

		switch (args.get(0)) {
			case "ping":
				MessageUtils.replyAndDelete(message, "Pong.");
				return;
			case "channeltest":
				final MessageChannel channel = message.getChannel();
				log.info("Channel ID: {}", channel.getId());
				log.info("Channel Name: {}", channel.getName());
				break;
			case "usertest":
				final User author = message.getAuthor();
				log.info("Author ID: {}", author.getId());
				log.info("Author Tag: {}", author.getAsTag());
				log.info("Author Name: {}", author.getName());
				break;
			case "clearChannel":
				if (message.getAuthor().getIdLong() == 185067296623034368.) {
					TextChannel textChannel = (TextChannel) message.getChannel();
					textChannel.deleteMessages(textChannel.getHistory().retrievePast(100).complete()).queue();
				}
				return;
		}
		MessageUtils.deleteMessagesInstant(message);
	}
}
