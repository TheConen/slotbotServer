package de.webalf.slotbot.service.bot.command.event;

import de.webalf.slotbot.model.annotations.Command;
import de.webalf.slotbot.service.bot.EventBotService;
import de.webalf.slotbot.service.bot.command.DiscordCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Message;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static de.webalf.slotbot.util.PermissionHelper.Authorization.EVENT_MANAGE;
import static de.webalf.slotbot.util.StringUtils.onlyNumbers;
import static de.webalf.slotbot.util.bot.MessageUtils.deleteMessagesInstant;
import static de.webalf.slotbot.util.bot.MessageUtils.replyAndDelete;

/**
 * @author Alf
 * @since 12.01.2021
 */
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Slf4j
@Command(names = {"renameSlot", "editSlot", "eventRenameSlot"},
		description = "Ermöglicht es einen Slot umzubenennen.",
		usage = "<Slotnummer> \"<Slotname>\"",
		argCount = {2},
		authorization = EVENT_MANAGE)
public class RenameSlot implements DiscordCommand {
	private final EventBotService eventBotService;

	@Override
	public void execute(Message message, List<String> args) {
		log.trace("Command: renameslot");

		final String slotNumber = args.get(0);
		if (!onlyNumbers(slotNumber)) {
			replyAndDelete(message, "Die Slotnummer muss eine Zahl sein.");
			return;
		}

		eventBotService.renameSlot(message.getChannel().getIdLong(), Integer.parseInt(slotNumber), args.get(1));
		deleteMessagesInstant(message);
	}
}
