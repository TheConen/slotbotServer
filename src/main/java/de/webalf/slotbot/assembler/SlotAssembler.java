package de.webalf.slotbot.assembler;

import de.webalf.slotbot.model.Slot;
import de.webalf.slotbot.model.dtos.SlotDto;
import de.webalf.slotbot.model.dtos.referenceless.SlotReferencelessDto;
import lombok.experimental.UtilityClass;

import java.util.Collections;
import java.util.List;
import java.util.stream.StreamSupport;

/**
 * @author Alf
 * @since 23.06.2020
 */
@UtilityClass
public final class SlotAssembler {
	public static Slot fromDto(SlotDto slotDto) {
		if (slotDto == null) {
			return null;
		}

		return Slot.builder()
				.id(slotDto.getId())
				.name(slotDto.getName().trim())
				.number(slotDto.getNumber())
				.squad(SquadAssembler.fromDto(slotDto.getSquad()))
				.reservedFor(GuildAssembler.fromDto(slotDto.getReservedFor()))
				.user(UserAssembler.fromDto(slotDto.getUser()))
				.replacementText(slotDto.getReplacementText())
				.build();
	}

	static List<Slot> fromDtoList(Iterable<? extends SlotDto> slotList) {
		if (slotList == null) {
			return Collections.emptyList();
		}

		return StreamSupport.stream(slotList.spliterator(), false)
				.map(SlotAssembler::fromDto)
				.toList();
	}

	/**
	 * To be used if the focus relies on the slot
	 */
	private static SlotDto toDto(Slot slot) {
		return SlotDto.builder()
				.id(slot.getId())
				.name(slot.getName())
				.number(slot.getNumber())
				.squad(SquadAssembler.toDto(slot.getSquad()))
				.reservedFor(GuildAssembler.toDto(slot.getReservedFor()))
				.user(UserAssembler.toDto(slot.getUser()))
				.replacementText(slot.getReplacementText())
				.build();
	}

	/**
	 * To be used if the focus relies on the slot
	 */
	public static List<SlotDto> toDtoList(List<Slot> slotList) {
		return slotList.stream().map(SlotAssembler::toDto).toList();
	}

	/**
	 * To be used if the focus relies on the event
	 */
	private static SlotReferencelessDto toReferencelessDto(Slot slot) {
		return SlotReferencelessDto.builder()
				.id(slot.getId())
				.name(slot.getName())
				.number(slot.getNumber())
				.reservedFor(GuildAssembler.toDto(slot.getReservedFor()))
				.user(UserAssembler.toDto(slot.getUser()))
				.replacementText(slot.getReplacementText())
				.build();
	}

	/**
	 * To be used if the focus relies on the event
	 */
	static List<SlotReferencelessDto> toReferencelessDtoList(Iterable<? extends Slot> slotList) {
		return StreamSupport.stream(slotList.spliterator(), false)
				.map(SlotAssembler::toReferencelessDto)
				.toList();
	}
}
