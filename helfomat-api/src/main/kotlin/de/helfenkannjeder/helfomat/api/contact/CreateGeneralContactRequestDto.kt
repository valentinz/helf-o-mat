package de.helfenkannjeder.helfomat.api.contact

data class CreateGeneralContactRequestDto(
    val captcha: String,
    val name: String,
    val email: String,
    val subject: String,
    val message: String,
    val location: String?,
    val address: String?
)