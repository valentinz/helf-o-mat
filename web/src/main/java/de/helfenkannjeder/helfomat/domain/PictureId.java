package de.helfenkannjeder.helfomat.domain;

import com.google.common.base.Preconditions;

import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * @author Valentin Zickner
 */
public class PictureId {

    private static Pattern VALID_UUID = Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$");

    private String value;

    public PictureId() {
        this(UUID.randomUUID().toString());
    }

    public PictureId(String value) {
        Preconditions.checkArgument(VALID_UUID.matcher(value).matches(), "Invalid PictureId provided");
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PictureId pictureId = (PictureId) o;
        return Objects.equals(value, pictureId.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "PictureId{" +
            "value='" + value + '\'' +
            '}';
    }
}
