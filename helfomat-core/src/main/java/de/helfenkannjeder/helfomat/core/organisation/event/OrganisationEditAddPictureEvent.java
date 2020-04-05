package de.helfenkannjeder.helfomat.core.organisation.event;

import de.helfenkannjeder.helfomat.core.organisation.Organisation;
import de.helfenkannjeder.helfomat.core.organisation.OrganisationId;
import de.helfenkannjeder.helfomat.core.organisation.OrganizationEventVisitor;
import de.helfenkannjeder.helfomat.core.picture.PictureId;

/**
 * @author Valentin Zickner
 */
@SuppressWarnings({"WeakerAccess", "CanBeFinal"})
public class OrganisationEditAddPictureEvent extends OrganisationEditEvent {
    private int index;
    private PictureId pictureId;

    protected OrganisationEditAddPictureEvent() {
    }

    public OrganisationEditAddPictureEvent(OrganisationId organisationId, int index, PictureId pictureId) {
        super(organisationId);
        this.index = index;
        this.pictureId = pictureId;
    }

    public int getIndex() {
        return index;
    }

    public PictureId getPictureId() {
        return pictureId;
    }

    @Override
    public Organisation.Builder applyOnOrganisationBuilder(Organisation.Builder organisation) {
        return organisation.addPicture(index, pictureId);
    }

    @Override
    public <T> T visit(OrganizationEventVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
