package de.helfenkannjeder.helfomat.api.picture

import de.helfenkannjeder.helfomat.api.Roles
import de.helfenkannjeder.helfomat.core.picture.Picture
import de.helfenkannjeder.helfomat.core.picture.PictureId
import de.helfenkannjeder.helfomat.core.picture.PictureRepository
import org.apache.tika.Tika
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.security.access.annotation.Secured
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * @author Valentin Zickner
 */
@Service
@EnableConfigurationProperties(PictureConfiguration::class)
open class PictureApplicationService(
    val pictureRepository: PictureRepository,
    val pictureStorageService: PictureStorageService,
    val pictureConfiguration: PictureConfiguration,
    val resizeImageService: ResizeImageService
) {

    @PreAuthorize("@pictureAuthorizationService.canAccess(#pictureId)")
    open fun getPicture(pictureId: PictureId): PictureDto {
        if (!pictureStorageService.existPicture(pictureId)) {
            throw PictureNotFoundException(pictureId)
        }
        ensurePictureExists(pictureId)
        val pictureInformation = pictureRepository.getReferenceById(pictureId)
        val inputStream = pictureStorageService.getPicture(pictureId)
        return PictureDto(inputStream, pictureInformation.contentType)
    }

    @PreAuthorize("@pictureAuthorizationService.canAccess(#pictureId)")
    open fun getPicture(pictureId: PictureId, size: String): PictureDto {
        if (!pictureStorageService.existPicture(pictureId)) {
            throw PictureNotFoundException(pictureId)
        }
        ensurePictureExists(pictureId)
        val pictureInformation = pictureRepository.getReferenceById(pictureId)
        val inputStream = pictureStorageService.getPicture(pictureId, size)
        return PictureDto(inputStream, pictureInformation.contentType)
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional
    open fun savePicture(pictureId: PictureId, inputStream: InputStream, size: Long): PictureId {
        if (pictureRepository.existsById(pictureId)) {
            throw PictureAlreadyExistException(pictureId)
        }
        val tempFile = File.createTempFile("cache", null)
        Files.copy(inputStream, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        pictureStorageService.savePicture(pictureId, tempFile.inputStream(), size)
        val contentType = Tika().detect(tempFile)
        for (pictureSize in pictureConfiguration.pictureSizes) {
            val inputStreamToResize = tempFile.inputStream()
            val resizedImage = resizeImageService.resize(inputStreamToResize, pictureSize.width, pictureSize.height, contentType)
            pictureStorageService.savePicture(pictureId, resizedImage.first, resizedImage.second, pictureSize.name)
        }
        tempFile.delete()
        pictureRepository.save(Picture(pictureId, false, contentType))
        return pictureId
    }

    private fun ensurePictureExists(pictureId: PictureId) {
        if (!pictureRepository.existsById(pictureId)) {
            throw PictureNotFoundException(pictureId)
        }
    }

}