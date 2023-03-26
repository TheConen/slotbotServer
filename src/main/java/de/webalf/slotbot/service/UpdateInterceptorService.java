package de.webalf.slotbot.service;

import de.webalf.slotbot.model.*;
import de.webalf.slotbot.service.bot.EventUpdateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.collection.spi.PersistentList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * @author Alf
 * @since 29.12.2020
 */
@Service
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Slf4j
public class UpdateInterceptorService {
	private final EventUpdateService eventUpdateService;

	/**
	 * Informs the discord bot about a deletion in an event
	 *
	 * @param entity that may be an event related object
	 */
	public void onDelete(Object entity) {
		update(getEvent(entity));
	}

	public void update(Object entity, Object[] currentState, Object[] previousState, String[] propertyNames) {
		update(getEvent(entity, currentState, previousState, propertyNames));
	}

	private void update(Event event) {
		if (event == null || !event.isAssigned()) {
			return;
		}

		eventUpdateService.update(event);
	}

	/**
	 * Returns the associated event if the entity is a {@link Event}, {@link Squad} or {@link Slot}
	 * The reserve is removed only in conjunction with another "slot-creating" action. Therefore, no onDelete needs to be made in this case
	 *
	 * @param entity to get the event for
	 * @return associated event or null
	 */
	private Event getEvent(Object entity) {
		if (entity instanceof final Slot slot) {
			if (!slot.isInReserve()) {
				return slot.getSquad().getEvent();
			}
		}
		return null;
	}

	private Event getEvent(Object entity, Object[] currentState, Object[] previousState, String[] propertyNames) {
		if (entity instanceof final Event event) {
			eventUpdate(previousState, propertyNames, event);
			return event;
		} else if (entity instanceof final Squad squad) {
			if (!squad.isReserve()) {
				return squad.getEvent();
			}
		} else if (entity instanceof final Slot slot) {
			final Event event = slot.getSquad().getEvent();
			slotUpdate(currentState, previousState, propertyNames, slot, event);
			return event;
		}
		return null;
	}

	private void eventUpdate(Object[] previousState, String[] propertyNames, Event event) {
		boolean calendarRebuildTodo = true;
		for (int i = 0; i < propertyNames.length; i++) {
			if (calendarRebuildTodo) {
				if (propertyNames[i].equals(Event_.NAME)) {
					final String oldName = (String) previousState[i];
					final String newName = event.getName();
					if (!oldName.equals(newName)) {
						eventUpdateService.rebuildCalendar(event.getId());
						calendarRebuildTodo = false;
					}
				} else if (propertyNames[i].equals(Event_.HIDDEN)) {
					final boolean oldHiddenState = (boolean) previousState[i];
					final boolean newHiddenState = event.isHidden();
					if (oldHiddenState != newHiddenState) {
						eventUpdateService.rebuildCalendar(event.getId());
						calendarRebuildTodo = false;
					}
				}
			}

			if (propertyNames[i].equals(Event_.DATE_TIME)) {
				final LocalDateTime oldEventDateTime = (LocalDateTime) previousState[i];
				final LocalDateTime newEventDateTime = event.getDateTime();
				if (!oldEventDateTime.isEqual(newEventDateTime)) {
					eventUpdateService.updateEventNotifications(event.getId());
					eventUpdateService.rebuildCalendar(event.getId());
					break;
				}
			}
		}
	}

	private void slotUpdate(Object[] currentState, Object[] previousState, String[] propertyNames, Slot slot, Event event) {
		if (!slot.isInReserve()) {
			for (int i = 0; i < propertyNames.length; i++) {
				if (propertyNames[i].equals(Slot_.USER)) {
					eventUpdateService.informAboutSlotChange(event, slot, (User) currentState[i], (User) previousState[i]);
					break;
				}
			}
		}
	}

	public void onCollectionUpdate(Object collection) {
		if (collection instanceof final PersistentList<?> persistentList) {
			if (persistentList.isEmpty()) return;
			final Object el = persistentList.get(0);

			if (el instanceof final Squad squad) {
				if (!squad.isReserve()) {
					update(squad.getEvent());
				}
			} else if (el instanceof final EventField eventField) {
				update(eventField.getEvent());
			}
		}
	}
}
