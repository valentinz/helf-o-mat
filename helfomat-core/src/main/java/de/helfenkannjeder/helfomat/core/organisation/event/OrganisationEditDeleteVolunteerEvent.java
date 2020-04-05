package de.helfenkannjeder.helfomat.core.organisation.event;

import de.helfenkannjeder.helfomat.core.organisation.Organisation;
import de.helfenkannjeder.helfomat.core.organisation.OrganisationId;
import de.helfenkannjeder.helfomat.core.organisation.OrganizationEventVisitor;
import de.helfenkannjeder.helfomat.core.organisation.Volunteer;

/**
 * @author Valentin Zickner
 */
@SuppressWarnings({"WeakerAccess", "CanBeFinal"})
public class OrganisationEditDeleteVolunteerEvent extends OrganisationEditEvent {
    private Volunteer volunteer;

    protected OrganisationEditDeleteVolunteerEvent() {
    }

    public OrganisationEditDeleteVolunteerEvent(OrganisationId organisationId, Volunteer volunteer) {
        super(organisationId);
        this.volunteer = volunteer;
    }

    public Volunteer getVolunteer() {
        return volunteer;
    }

    @Override
    public Organisation.Builder applyOnOrganisationBuilder(Organisation.Builder organisation) {
        return organisation.removeVolunteer(volunteer);
    }

    @Override
    public <T> T visit(OrganizationEventVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
