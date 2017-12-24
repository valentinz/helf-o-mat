package de.helfenkannjeder.helfomat.core.organisation.event;

import de.helfenkannjeder.helfomat.core.organisation.Organisation;
import de.helfenkannjeder.helfomat.core.organisation.OrganisationId;

/**
 * @author Valentin Zickner
 */
public class OrganisationEditDescriptionEvent extends OrganisationEditEvent {
    private String description;

    public OrganisationEditDescriptionEvent(OrganisationId organisationId, String description) {
        super(organisationId);
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public Organisation.Builder applyOnOrganisationBuilder(Organisation.Builder organisation) {
        return organisation.setDescription(description);
    }
}
