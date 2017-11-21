package de.helfenkannjeder.helfomat.core.picture;

import java.io.IOException;
import java.nio.file.Path;

/**
 * @author Valentin Zickner
 */
public interface PictureRepository {

    PictureId savePicture(String url, String folder, PictureId pictureId) throws DownloadFailedException;

    PictureId savePicture(byte[] bytes, String folder, PictureId pictureId) throws DownloadFailedException;

    Path getPicture(PictureId pictureId) throws IOException;

    Path getPicture(PictureId pictureId, String size);

}
