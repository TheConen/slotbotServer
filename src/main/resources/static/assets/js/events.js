$(function () {
    "use strict";
    const $spinner = $('#spinner');

    const calendarEl = document.getElementById('calendar');
    const calendar = new FullCalendar.Calendar(calendarEl, {
        initialView: 'dayGridMonth',
        locale: 'de',
        eventSources: [
            {
                url: getEventsUrl,
                color: 'blue'
            }
        ],
        eventDidMount: (arg) => {
            const description = arg.event.extendedProps.description;
            if (description) {
                $(arg.el).tooltip({title: description, html: true, container: 'body', boundary: 'viewport'});
            }
        },
        eventClick: () => {
            $spinner.show();
        }
    });

    // Allow event manage roles to click on the calendar to create event
    if (eventManageRoles.includes(authentication.authorities.filter(authority => authority.authority.startsWith('ROLE_'))[0].authority)) {
        calendar.on('dateClick', function (info) {
            window.location.href = `${createEventUrl}?date=${info.dateStr}`;
        });
    }
    calendar.render();
});