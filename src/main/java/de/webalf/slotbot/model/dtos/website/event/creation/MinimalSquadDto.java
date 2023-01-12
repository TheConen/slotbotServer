package de.webalf.slotbot.model.dtos.website.event.creation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

import static de.webalf.slotbot.util.MaxLength.TEXT;

/**
 * @author Alf
 * @since 25.07.2022
 */
@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class MinimalSquadDto {
	@NotBlank
	@Size(max = TEXT)
	private final String name;

	private List<MinimalSlotDto> slotList;

	private final String reservedFor;
}
