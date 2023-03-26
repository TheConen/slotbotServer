package de.webalf.slotbot.service;

import de.webalf.slotbot.assembler.EventAssembler;
import de.webalf.slotbot.assembler.api.EventApiAssembler;
import de.webalf.slotbot.exception.BusinessRuntimeException;
import de.webalf.slotbot.exception.ResourceNotFoundException;
import de.webalf.slotbot.model.*;
import de.webalf.slotbot.model.dtos.*;
import de.webalf.slotbot.model.dtos.api.EventRecipientApiDto;
import de.webalf.slotbot.model.dtos.website.event.edit.EventUpdateDto;
import de.webalf.slotbot.repository.EventRepository;
import de.webalf.slotbot.util.CollectionUtils;
import de.webalf.slotbot.util.DateUtils;
import de.webalf.slotbot.util.DtoUtils;
import de.webalf.slotbot.util.EventUtils;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;

import static de.webalf.slotbot.model.Guild.GUILD_PLACEHOLDER;
import static de.webalf.slotbot.util.permissions.BotPermissionHelper.hasEventManageRole;

/**
 * @author Alf
 * @since 27.07.2020
 */
@Service
@Transactional
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class EventService {
	private final EventRepository eventRepository;
	private final EventAssembler eventAssembler;
	private final SquadService squadService;
	private final SlotService slotService;
	private final UserService userService;
	private final EventTypeService eventTypeService;
	private final EventFieldService eventFieldService;
	private final EventDiscordInformationService eventDiscordInformationService;
	private final GuildService guildService;

	/**
	 * Creates a new event with values from the {@link EventDto}
	 * {@link Event#setOwnerGuild(Guild)} is set by current request uri ({@link GuildService#getOwnerGuild(AbstractEventDto)})
	 *
	 * @param eventDto new event
	 * @return saved new event
	 */
	public Event createEvent(@NonNull EventDto eventDto) {
		final Set<EventDiscordInformationDto> discordInformation = eventDto.getDiscordInformation();
		if (CollectionUtils.isNotEmpty(discordInformation) &&
				eventDiscordInformationService.existsByChannelInDtos(discordInformation)) {
			throw BusinessRuntimeException.builder().title("In mindestens einem der angegebenen Kanäle gibt es bereits ein Event.").build();
		}
		Event event = eventAssembler.fromDto(eventDto);
		final Guild ownerGuild = guildService.getOwnerGuild(eventDto);
		event.setEventType(eventTypeService.find(eventDto.getEventType(), ownerGuild));
		event.setOwnerGuild(ownerGuild);
		setReservedFor(event);

		event.validate();

		return eventRepository.save(event);
	}

	private void setReservedFor(@NonNull Event event) {
		event.getSquadList().forEach(squad -> {
			evaluateReservedFor(squad.getReservedFor(), squad::setReservedFor);
			squad.getSlotList().forEach(slot ->
					evaluateReservedFor(slot.getReservedFor(), slot::setReservedFor));
		});
	}

	private void evaluateReservedFor(Guild reservedFor, Consumer<Guild> consumer) {
		if (reservedFor != null) {
			if (reservedFor.getId() != GUILD_PLACEHOLDER) {
				consumer.accept(guildService.find(reservedFor.getId()));
			} else {
				consumer.accept(null);
			}
		}
	}

	/**
	 * Returns an optional for the event associated with the given channelId
	 *
	 * @param channel to find event for
	 * @return Event found by channel or empty optional
	 */
	public Optional<Event> findOptionalByChannel(long channel) {
		return eventDiscordInformationService.findEventByChannel(channel);
	}

	/**
	 * Returns the event associated with the given channelId
	 *
	 * @param channel to find event for
	 * @return Event from channel
	 * @throws ResourceNotFoundException if no event with this channelId could be found
	 */
	public Event findByChannel(long channel) {
		return findOptionalByChannel(channel).orElseThrow(ResourceNotFoundException::new);
	}

	/**
	 * Returns the event associated with the given eventId
	 *
	 * @param eventId to find event for
	 * @return Event found by id
	 * @throws ResourceNotFoundException if no event with this eventId could be found
	 */
	public Event findById(long eventId) {
		return eventRepository.findById(eventId).orElseThrow(ResourceNotFoundException::new);
	}

	/**
	 * In addition to {@link #findById(long)}, the API access rights for the event are checked
	 */
	public Event findByIdForApi(long eventId) {
		final Event event = findById(eventId);
		EventUtils.assertApiReadAccess(event);
		return event;
	}

	/**
	 * Returns all events owned by the given guild that take place in the specified period
	 *
	 * @return all events in given period
	 */
	public List<Event> findAllBetweenOfGuild(LocalDateTime start, LocalDateTime end, @NonNull Guild ownerGuild) {
		if (ownerGuild.getId() == GUILD_PLACEHOLDER) {
			return hasEventManageRole() ?
					eventRepository.findAllByDateTimeBetweenAndShareableTrueOrPlaceholderGuild(start, end) :
					eventRepository.findAllByDateTimeBetweenAndHiddenFalseAndShareableTrueOrPlaceholderGuild(start, end);
		}

		return hasEventManageRole() ?
				eventRepository.findAllByGuildAndDateTimeBetween(ownerGuild, start, end) :
				eventRepository.findAllByGuildAndDateTimeBetweenAndHiddenFalse(ownerGuild, start, end);
	}

	public List<Event> findAllPublicByGuild(Guild ownerGuild) {
		return eventRepository.findAllByGuildAndHiddenFalse(ownerGuild);
	}

	/**
	 * Returns all events that happened before now
	 *
	 * @return all events from the past
	 */
	public List<Event> findAllInPast() {
		return eventRepository.findAllByDateTimeIsBeforeAndOrderByDateTime(DateUtils.now());
	}

	/**
	 * Returns all events that are scheduled after now
	 *
	 * @return all events from the future
	 */
	public List<Event> findAllInFuture() {
		return eventRepository.findByDateTimeGreaterThan(DateUtils.now());
	}

	/**
	 * Returns all events of the given owner guild that are scheduled in the future and have no discord information
	 *
	 * @param guildId to find events for
	 * @return all events in the future that have no channel
	 */
	public List<Event> findAllNotAssignedInFuture(long guildId) {
		return eventRepository.findAllByDateTimeIsAfterAndNotScheduledAndOwnerGuildAndForGuildAndOrderByDateTime(DateUtils.now(), guildId);
	}

	/**
	 * Returns all events that the given guild is not owner of, that are scheduled in the future and have no discord information
	 *
	 * @param guildId to exclude as owner guild
	 * @return all events in the future that have no channel
	 */
	public List<Event> findAllForeignNotAssignedInFuture(long guildId) {
		return eventRepository.findAllByDateTimeIsAfterAndNotScheduledAndNotOwnerGuildAndForGuildAndOrderByDateTime(DateUtils.now(), guildId);
	}

	/**
	 * Returns all {@link User}s slotted in the event associated with the given channelId.
	 * {@link User#DEFAULT_USER_ID} is filtered out.
	 *
	 * @param channel to find event for
	 * @return participant list
	 */
	public List<User> findAllParticipants(long channel) {
		return eventRepository.findAllParticipants(channel);
	}

	/**
	 * Returns the owner guild of the event found by its id.
	 *
	 * @param eventId to find owner guild of
	 * @return owner guild
	 */
	public Guild getGuildByEventId(long eventId) {
		return eventRepository.findOwnerGuildById(eventId).orElseThrow(ResourceNotFoundException::new);
	}

	/**
	 * Updates the event found by id with values from the {@link EventDto}.
	 * <p>
	 * For updating discord information see {@link #updateDiscordInformation(AbstractEventDto)}
	 *
	 * @param eventId event to update
	 * @param dto     with values to update
	 * @return updated event
	 */
	public Event updateEvent(long eventId, @NonNull EventUpdateDto dto) {
		Event event = findById(eventId);

		DtoUtils.ifPresent(dto.getHidden(), event::setHidden);
		DtoUtils.ifPresent(dto.getShareable(), event::setShareable);
		DtoUtils.ifPresent(dto.getName(), event::setName);
		DtoUtils.ifPresent(dto.getDateTime(), event::setDateTime);
		DtoUtils.ifPresent(dto.getCreator(), event::setCreator);
		DtoUtils.ifPresentObject(dto.getEventType(), eventType -> event.setEventType(eventTypeService.find(dto.getEventType(), event.getOwnerGuild())));
		DtoUtils.ifPresentOrEmpty(dto.getDescription(), event::setDescription);
		DtoUtils.ifPresentOrEmpty(dto.getMissionType(), event::setMissionType);
		DtoUtils.ifPresentOrEmpty(dto.getMissionLength(), event::setMissionLength);
		DtoUtils.ifPresentOrEmpty(dto.getPictureUrl(), event::setPictureUrl);
		DtoUtils.ifPresent(dto.getReserveParticipating(), event::setReserveParticipating);

		DtoUtils.ifPresentObject(dto.getDetails(), details -> eventFieldService.updateEventDetails(details, event));
		DtoUtils.ifPresentObject(dto.getSquadList(), squadlist -> {
			squadService.updateSquadList(squadlist, event);
			event.removeReservedForDefaultGuild();
		});

		return event;
	}

	/**
	 * Updates the discord information of the event found by id with values from the {@link AbstractEventDto}.
	 *
	 * @param dto to get discord information from
	 * @return updated event
	 */
	public Event updateDiscordInformation(AbstractEventDto dto) {
		Event event = findById(dto.getId());

		final Set<EventDiscordInformationDto> dtoDiscordInformation = dto.getDiscordInformation();
		DtoUtils.ifPresent(dtoDiscordInformation, discordInformation -> eventDiscordInformationService.updateDiscordInformation(dtoDiscordInformation, event));

		return event;
	}

	/**
	 * Searches for the given event by id and archives it
	 *
	 * @param eventId event id
	 * @param guildId in which the event is being archived
	 */
	public void archiveEvent(long eventId, long guildId) {
		findById(eventId).archive(guildId);
	}

	/**
	 * Deletes the given event
	 */
	public void deleteEvent(Event event) {
		eventRepository.delete(event);
	}

	/**
	 * Enters the given user for the slot with given number in given event, if available.
	 *
	 * @param event      event
	 * @param slotNumber Slot to slot into
	 * @param userDto    person that should be slotted
	 * @return Event in which the person has been slotted
	 * @throws BusinessRuntimeException if the slot is already occupied
	 */
	public Event slot(@NonNull Event event, int slotNumber, UserDto userDto) {
		Slot slot = event.findSlot(slotNumber).orElseThrow(ResourceNotFoundException::new);
		User user = userService.find(userDto);

		slot.assertSlotIsPossible(user);
		event.unslotIfAlreadySlotted(user);
		eventRepository.saveAndFlush(event);
		slotService.slot(slot, user);
		return event;
	}

	/**
	 * Searches for the given channel the matching event and {@link #slot(Event, int, UserDto)}
	 */
	public Event slot(long channel, int slotNumber, UserDto userDto) {
		return slot(findByChannel(channel), slotNumber, userDto);
	}

	/**
	 * Removes the user, found by userDto, from its slot in given event.
	 *
	 * @param event   event
	 * @param userDto person that should be unslotted
	 * @return Event in which the person has been unslotted
	 */
	public Event unslot(@NonNull Event event, UserDto userDto) {
		User user = userService.find(userDto);
		Slot slot = event.findSlotOfUser(user).orElseThrow(ResourceNotFoundException::new);
		slotService.unslot(slot, user);
		return event;
	}

	/**
	 * Searches for the given channel the matching event and {@link #unslot(Event, UserDto)}
	 */
	public Event unslot(long channel, UserDto userDto) {
		return unslot(findByChannel(channel), userDto);
	}

	/**
	 * Removes the user, found by slotNumber, from its slot in the given event.
	 *
	 * @param event      event
	 * @param slotNumber slot that should be cleared
	 * @return Event in which the unslot has been performed
	 */
	public EventRecipientApiDto unslot(@NonNull Event event, int slotNumber) {
		Slot slot = event.findSlot(slotNumber).orElseThrow(ResourceNotFoundException::new);
		User userToUnslot = slot.getUser();
		slotService.unslot(slot, userToUnslot);
		return EventApiAssembler.toActionDto(event, userToUnslot);
	}

	/**
	 * Searches for the given channel the matching event and {@link #unslot(Event, int)}
	 */
	public EventRecipientApiDto unslot(long channel, int slotNumber) {
		return unslot(findByChannel(channel), slotNumber);
	}

	/**
	 * Searches for the given channel the matching event and enters the given user for a random empty slot, if available.
	 *
	 * @param channel event channel
	 * @param userDto person that should be slotted
	 * @return Event in which the person has been slotted
	 * @throws BusinessRuntimeException if no slot is available
	 */
	public Event randomSlot(long channel, UserDto userDto) {
		Event event = findByChannel(channel);
		User user = userService.find(userDto);
		Slot slot = event.randomSlot(user);
		slotService.slot(slot, user);
		return event;
	}

	/**
	 * Renames the squad by position in the given event.
	 *
	 * @param event         event
	 * @param squadPosition to edit name of
	 * @param squadName     new name
	 * @return event in which the slot has been renamed
	 */
	public Event renameSquad(@NonNull Event event, int squadPosition, String squadName) {
		event.findSquadByPosition(squadPosition).setName(squadName);
		return event;
	}

	/**
	 * Searches for the given channel the matching event and {@link #renameSquad(Event, int, String)}
	 */
	public Event renameSquad(long channel, int squadPosition, String squadName) {
		return renameSquad(findByChannel(channel), squadPosition, squadName);
	}

	/**
	 * Adds the given slot to the squad found by squadPosition in the given event.
	 *
	 * @param event         event
	 * @param squadPosition Counted, starting by 0
	 * @param slotDto       slot to add
	 * @return event in which the slot has been added
	 */
	public Event addSlot(@NonNull Event event, int squadPosition, SlotDto slotDto) {
		final Squad squad = event.findSquadByPosition(squadPosition);
		if (squad.isReserve()) {
			throw BusinessRuntimeException.builder().title("Der Reserve kann manuell kein Slot hinzugefügt werden.").build();
		}
		squad.addSlot(slotService.newSlot(slotDto));
		return event;
	}

	/**
	 * Searches for the given channel the matching event and {@link #addSlot(Event, int, SlotDto)}
	 */
	public Event addSlot(long channel, int squadPosition, SlotDto slotDto) {
		return addSlot(findByChannel(channel), squadPosition, slotDto);
	}

	/**
	 * Removes the slot by number in the given event.
	 *
	 * @param event      event
	 * @param slotNumber to delete
	 * @return event in which the slot has been deleted
	 */
	public Event deleteSlot(@NonNull Event event, int slotNumber) {
		Slot slot = event.findSlot(slotNumber).orElseThrow(ResourceNotFoundException::new);
		slotService.deleteSlot(slot);
		return event;
	}

	/**
	 * Searches for the given channel the matching event and {@link #deleteSlot(Event, int)}
	 */
	public Event deleteSlot(long channel, int slotNumber) {
		return deleteSlot(findByChannel(channel), slotNumber);
	}

	/**
	 * Renames the slot by number in the given event.
	 *
	 * @param event      event
	 * @param slotNumber to edit name of
	 * @param slotName   new name
	 * @return event in which the slot has been renamed
	 */
	public Event renameSlot(@NonNull Event event, int slotNumber, String slotName) {
		Slot slot = event.findSlot(slotNumber).orElseThrow(ResourceNotFoundException::new);
		slotService.renameSlot(slot, slotName);
		return event;
	}

	/**
	 * Searches for the given channel the matching event and {@link #renameSlot(Event, int, String)}
	 */
	public Event renameSlot(long channel, int slotNumber, String slotName) {
		return renameSlot(findByChannel(channel), slotNumber, slotName);
	}

	/**
	 * Blocks the slot by number and sets the replacement text in the given event.
	 *
	 * @param event           event
	 * @param slotNumber      to block
	 * @param replacementName text to be shown instead of user
	 * @return event in which the slot has been blocked
	 */
	public Event blockSlot(@NonNull Event event, int slotNumber, String replacementName) {
		Slot slot = event.findSlot(slotNumber).orElseThrow(ResourceNotFoundException::new);
		slotService.blockSlot(slot, replacementName);
		return event;
	}

	/**
	 * Searches for the given channel the matching event and {@link #blockSlot(Event, int, String)}
	 */
	public Event blockSlot(long channel, int slotNumber, String replacementName) {
		return blockSlot(findByChannel(channel), slotNumber, replacementName);
	}

	/**
	 * Returns the slots matching the two given users in the given event.
	 *
	 * @param event    event
	 * @param userDtos slotted users
	 * @return two slots
	 */
	public List<Slot> findSwapSlots(@NonNull Event event, List<UserDto> userDtos) {
		if (org.springframework.util.CollectionUtils.isEmpty(userDtos) || userDtos.size() != 2) {
			throw BusinessRuntimeException.builder().title("Zum tauschen müssen zwei Nutzer angegeben werden.").build();
		}

		ArrayList<Slot> slots = new ArrayList<>();
		userDtos.forEach(userDto -> slots.add(event.findSlotOfUser(userService.find(userDto)).orElseThrow(ResourceNotFoundException::new)));
		return slots;
	}

	/**
	 * Searches for the given channel the matching event and {@link #findSwapSlots(Event, List)}
	 */
	public List<Slot> findSwapSlots(long channel, List<UserDto> userDtos) {
		return findSwapSlots(findByChannel(channel), userDtos);
	}

	/**
	 * Returns the slots matching the given slotNumber and user in the given event.
	 *
	 * @param event      event
	 * @param slotNumber slot1 to find by slotNumber
	 * @param userDto    slot2 to find by user
	 * @return two Slots
	 */
	public List<Slot> findSwapSlots(@NonNull Event event, int slotNumber, UserDto userDto) {
		User user = userService.find(userDto);

		Slot slotFoundByNumber = event.findSlot(slotNumber).orElseThrow(ResourceNotFoundException::new);
		if (slotFoundByNumber.getUser() != null && slotFoundByNumber.getUser().isDefaultUser()) {
			throw BusinessRuntimeException.builder().title("Mit einem gesperrten Slot kann nicht getauscht werden.").build();
		}

		return Arrays.asList(
				event.findSlotOfUser(user).orElseThrow(ResourceNotFoundException::new),
				slotFoundByNumber
		);
	}

	/**
	 * Searches for the given channel the matching event and {@link #findSwapSlots(Event, int, UserDto)}
	 */
	public List<Slot> findSwapSlots(long channel, int slotNumber, UserDto userDto) {
		return findSwapSlots(findByChannel(channel), slotNumber, userDto);
	}
}
