package de.webalf.slotbot.assembler;

import de.webalf.slotbot.model.EventType;
import de.webalf.slotbot.model.Guild;
import de.webalf.slotbot.model.dtos.EventTypeDto;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

import java.util.List;
import java.util.stream.StreamSupport;

/**
 * @author Alf
 * @since 08.04.2021
 */
@UtilityClass
public final class EventTypeAssembler {
	public static EventType fromDto(@NonNull EventTypeDto dto, Guild guild) {
		return EventType.builder()
				.name(dto.getName())
				.color(dto.getColor())
				.guild(guild)
				.build();
	}

	public static EventTypeDto toDto(EventType eventType) {
		final Guild guild = eventType.getGuild();
		final String guildId = guild != null ? Long.toString(guild.getId()) : null;
		return EventTypeDto.builder()
				.name(eventType.getName())
				.color(eventType.getColor())
				.guild(guildId)
				.build();
	}

	public static List<EventTypeDto> toDtoList(Iterable<? extends EventType> eventTypes) {
		return StreamSupport.stream(eventTypes.spliterator(), false)
				.map(EventTypeAssembler::toDto)
				.toList();
	}
}
