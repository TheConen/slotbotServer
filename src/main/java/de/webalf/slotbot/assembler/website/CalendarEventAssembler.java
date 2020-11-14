package de.webalf.slotbot.assembler.website;

import de.webalf.slotbot.controller.website.EventWebController;
import de.webalf.slotbot.model.Event;
import de.webalf.slotbot.model.dtos.website.CalendarEventDto;
import de.webalf.slotbot.util.DiscordMarkdown;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * @author Alf
 * @since 24.10.2020
 */
@Component
public final class CalendarEventAssembler {
	private static CalendarEventDto toDto(Event event) {
		return CalendarEventDto.builder()
				.title(event.getName())
				.start(event.getDateTime())
				.description(DiscordMarkdown.toHtml(event.getDescription()))
				.url(linkTo(methodOn(EventWebController.class).getEventDetailsHtml(event.getId())).toUri().toString())
				.build();
	}

	public static List<CalendarEventDto> toDtoList(Iterable<? extends Event> content) {
		return StreamSupport.stream(content.spliterator(), false)
				.map(CalendarEventAssembler::toDto)
				.collect(Collectors.toList());
	}
}
