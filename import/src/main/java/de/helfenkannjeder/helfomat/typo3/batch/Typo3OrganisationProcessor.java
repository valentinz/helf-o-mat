package de.helfenkannjeder.helfomat.typo3.batch;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import de.helfenkannjeder.helfomat.domain.AddressBuilder;
import de.helfenkannjeder.helfomat.domain.GeoPoint;
import de.helfenkannjeder.helfomat.domain.Group;
import de.helfenkannjeder.helfomat.domain.Organisation;
import de.helfenkannjeder.helfomat.domain.OrganisationBuilder;
import de.helfenkannjeder.helfomat.domain.OrganisationType;
import de.helfenkannjeder.helfomat.typo3.domain.TOrganisation;
import de.helfenkannjeder.helfomat.typo3.domain.TOrganisationType;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import static de.helfenkannjeder.helfomat.domain.OrganisationType.Aktivbuero;
import static de.helfenkannjeder.helfomat.domain.OrganisationType.Helfenkannjeder;
import static de.helfenkannjeder.helfomat.domain.OrganisationType.Sonstige;

/**
 * @author Valentin Zickner
 */
@Component
@JobScope
public class Typo3OrganisationProcessor implements ItemProcessor<TOrganisation, Organisation> {

    @Override
    public Organisation process(TOrganisation tOrganisation) throws Exception {
        if (organisationIsNoCandidateToImport(tOrganisation)) {
            return null;
        }

        return new OrganisationBuilder()
                .setId(UUID.randomUUID().toString())
                .setName(tOrganisation.getName())
                .setType(tOrganisation.getOrganisationtype().getName())
                .setDescription(tOrganisation.getDescription())
                .setLogo(tOrganisation.getLogo())
                .setWebsite(tOrganisation.getWebsite())
                .setMapPin(tOrganisation.getOrganisationtype().getPicture())
                .setPictures(extractPictures(tOrganisation.getPictures()))
                .setAddresses(
                        tOrganisation.getAddresses().stream().map(tAddress -> new AddressBuilder()
                                .setWebsite(tAddress.getWebsite())
                                .setTelephone(tAddress.getTelephone())
                                .setStreet(tAddress.getStreet())
                                .setAddressAppendix(tAddress.getAddressappendix())
                                .setCity(tAddress.getCity())
                                .setZipcode(tAddress.getZipcode())
                                .setLocation(new GeoPoint(tAddress.getLatitude(), tAddress.getLongitude()))
                                .build()).collect(Collectors.toList())
                )
                .setGroups(
                        tOrganisation.getGroups().stream().map(tGroup -> {
                            Group group = new Group();
                            group.setName(tGroup.getName());
                            group.setDescription(tGroup.getDescription());
                            return group;
                        }).collect(Collectors.toList())
                )
                .build();
    }

    private boolean organisationIsNoCandidateToImport(TOrganisation tOrganisation) {
        // We don't want to import organisations without a specified type
        if (tOrganisation.getOrganisationtype() == null) {
            return true;
        }

        List<OrganisationType> irrelevantOrganisationTypes = Arrays.asList(Aktivbuero, Sonstige, Helfenkannjeder);
        TOrganisationType organisationType = tOrganisation.getOrganisationtype();
        if(irrelevantOrganisationTypes.stream().map(OrganisationType::toString).anyMatch(item -> organisationType.getName().equals(item))) {
            return true;
        }

        return false;
    }

    private List<String> extractPictures(String picturesString) {
        if (picturesString == null) {
            return Collections.emptyList();
        }
        return Arrays.asList(picturesString.split(","));
    }

}
