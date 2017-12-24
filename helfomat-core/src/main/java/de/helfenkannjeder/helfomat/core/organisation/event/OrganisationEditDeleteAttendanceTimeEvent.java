package de.helfenkannjeder.helfomat.core.organisation.event;

import de.helfenkannjeder.helfomat.core.organisation.AttendanceTime;
import de.helfenkannjeder.helfomat.core.organisation.Organisation;
import de.helfenkannjeder.helfomat.core.organisation.OrganisationId;

/**
 * @author Valentin Zickner
 */
public class OrganisationEditDeleteAttendanceTimeEvent extends OrganisationEditEvent {
    private AttendanceTime attendanceTime;

    public OrganisationEditDeleteAttendanceTimeEvent(OrganisationId organisationId, AttendanceTime attendanceTime) {
        super(organisationId);
        this.attendanceTime = attendanceTime;
    }

    @Override
    public Organisation.Builder applyOnOrganisationBuilder(Organisation.Builder organisation) {
        return organisation.removeAttendanceTime(attendanceTime);
    }
}
