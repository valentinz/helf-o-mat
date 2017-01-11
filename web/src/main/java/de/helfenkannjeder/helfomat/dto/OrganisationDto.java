package de.helfenkannjeder.helfomat.dto;

import java.util.List;

/**
 * @author Valentin Zickner
 */
public class OrganisationDto {
    private String id;
    private String name;
    private String description;
    private String website;
    private float scoreNorm;
    private String mapPin;
    private List<AddressDto> addresses;
    private String logo;

    public OrganisationDto() {
    }

    public OrganisationDto(String id, String name, String description, String website, String mapPin, List<AddressDto> addresses, String logo) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.website = website;
        this.mapPin = mapPin;
        this.addresses = addresses;
        this.logo = logo;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getWebsite() {
        return website;
    }

    public float getScoreNorm() {
        return scoreNorm;
    }

    public String getMapPin() {
        return mapPin;
    }

    public List<AddressDto> getAddresses() {
        return addresses;
    }

    public String getLogo() {
        return logo;
    }

    public void setScoreNorm(float scoreNorm) {
        this.scoreNorm = scoreNorm;
    }
}
