package de.helfenkannjeder.helfomat.infrastructure.download

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "helfomat.download")
data class DownloadConfigurationProperties(
    val api: String = "https://helfenkannjeder.de/api",
    val filterOrganizationTypes: List<String> = listOf()
)