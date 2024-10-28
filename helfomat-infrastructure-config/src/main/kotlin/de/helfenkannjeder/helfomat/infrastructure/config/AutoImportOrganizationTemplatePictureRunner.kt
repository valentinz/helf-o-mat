package de.helfenkannjeder.helfomat.infrastructure.config

import de.helfenkannjeder.helfomat.api.AuthenticationUtil
import de.helfenkannjeder.helfomat.api.Roles
import de.helfenkannjeder.helfomat.api.picture.PictureApplicationService
import de.helfenkannjeder.helfomat.core.ProfileRegistry
import de.helfenkannjeder.helfomat.core.picture.PictureId
import de.helfenkannjeder.helfomat.core.picture.PictureRepository
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Profile
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.stereotype.Component


@Component
@Profile("!${ProfileRegistry.TEST} & !${ProfileRegistry.IMPORT}")
open class AutoImportOrganizationTemplatePictureRunner(
    val pictureApplicationService: PictureApplicationService,
    val pictureRepository: PictureRepository
) : CommandLineRunner {

    private val uuidRegex = Regex("^(\\b[0-9a-f]{8}\\b-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-\\b[0-9a-f]{12}\\b)")

    override fun run(vararg args: String?) {
        val loader = PathMatchingResourcePatternResolver()
        val importPictures = loader.getResources("classpath*:/organization-template-pictures/*.png")

        AuthenticationUtil.configureAuthentication(Roles.ADMIN)
        for (importPicture in importPictures) {
            val filename = importPicture.filename ?: continue
            val pictureId = toPictureId(filename) ?: continue
            if (!pictureRepository.existsById(pictureId)) {
                pictureApplicationService.savePicture(pictureId, importPicture.inputStream, importPicture.contentLength())
                pictureRepository.save(pictureRepository.getReferenceById(pictureId).apply { public = true })
            }
        }
        AuthenticationUtil.clearAuthentication()
    }

    private fun toPictureId(filename: String): PictureId? {
        val match = uuidRegex.find(filename) ?: return null
        return PictureId(match.value)
    }

}