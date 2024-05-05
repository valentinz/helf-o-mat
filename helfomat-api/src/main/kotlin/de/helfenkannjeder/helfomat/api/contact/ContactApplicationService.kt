package de.helfenkannjeder.helfomat.api.contact

import de.helfenkannjeder.helfomat.api.CaptchaValidationFailedException
import de.helfenkannjeder.helfomat.api.CaptchaValidator
import de.helfenkannjeder.helfomat.api.EmailService
import de.helfenkannjeder.helfomat.api.randomString
import de.helfenkannjeder.helfomat.core.contact.ContactRequest
import de.helfenkannjeder.helfomat.core.contact.ContactRequestId
import de.helfenkannjeder.helfomat.core.contact.ContactRequestRepository
import de.helfenkannjeder.helfomat.core.contact.ContactRequestStatus
import de.helfenkannjeder.helfomat.core.organization.Organization
import de.helfenkannjeder.helfomat.core.organization.OrganizationRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ClassPathResource
import org.springframework.orm.jpa.JpaObjectRetrievalFailureException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

/**
 * @author Valentin Zickner
 */
@Service
@Transactional
open class ContactApplicationService(
    val organizationRepository: OrganizationRepository,
    val contactRequestRepository: ContactRequestRepository,
    val captchaValidator: CaptchaValidator,
    val emailService: EmailService,
    @Value("\${helfomat.contact-form.domain:https://www.helfenkannjeder.de/}") val domain: String,
    @Value("\${helfomat.contact-form.locale:de_DE}") val locale: String,
    @Value("\${helfomat.contact-form.force-to:}") val contactFormForceTo: String,
    @Value("\${helfomat.contact-form.support-email}") val supportEmail: String
) {

    open fun createContactRequest(contactRequestDto: CreateContactRequestDto): ContactRequestResult {
        if (!this.captchaValidator.validate(contactRequestDto.captcha)) {
            throw CaptchaValidationFailedException()
        }

        val organization = organizationRepository.findOne(contactRequestDto.organizationId.value) ?: throw ContactRequestInvalid()
        val contactRequest = contactRequestDto.toContactRequest(randomString(25), organization)
        sendConfirmationEmail(contactRequest, organization)
        contactRequest.markConfirmationAsSent()
        return contactRequestRepository.save(contactRequest).toContactRequestResult()
    }

    open fun resendContactRequest(resendContactRequestDto: ResendContactRequestDto): ContactRequestResult {
        if (!this.captchaValidator.validate(resendContactRequestDto.captcha)) {
            throw CaptchaValidationFailedException()
        }

        val contactRequest = contactRequestRepository.getOne(resendContactRequestDto.contactRequestId)
        val organization = organizationRepository.findOne(contactRequest.organizationId.value) ?: throw ContactRequestInvalid()

        if (contactRequest.numberOfConfirmationEmails > 3 || contactRequest.status != ContactRequestStatus.CONFIRMATION_REQUEST_SENT) {
            throw MaxContactRequestReached()
        }

        sendConfirmationEmail(contactRequest, organization)
        contactRequest.markConfirmationAsSent()
        return contactRequestRepository.save(contactRequest).toContactRequestResult()
    }

    open fun confirmContactRequest(confirmContactRequestDto: ConfirmContactRequestDto): ConfirmContactRequestResult {
        try {
            val contactRequest = this.contactRequestRepository.getOne(confirmContactRequestDto.contactRequestId)
            if (contactRequest.confirmationCode != confirmContactRequestDto.confirmationCode) {
                throw ContactRequestInvalid()
            }
            if (contactRequest.status != ContactRequestStatus.CONFIRMATION_REQUEST_SENT) {
                throw ContactRequestInvalid()
            }
            val organization = this.organizationRepository.findOne(contactRequest.organizationId.value) ?: throw ContactRequestInvalid()

            val attributes = mapOf(
                Pair("domain", domain),
                Pair("contactRequest", contactRequest),
                Pair("organization", organization)
            )
            val attachments = listOf(
                Triple("logo", ClassPathResource("templates/logo.jpg"), "image/jpeg")
            )
            var to = contactRequest.contactPerson.email
            if (contactFormForceTo != "") {
                to = contactFormForceTo
            }
            emailService.sendEmail(to, "contact-request-send-email", arrayOf(contactRequest.subject), attributes, attachments, toLocale(), contactRequest.email)

            contactRequest.status = ContactRequestStatus.EMAIL_CONFIRMED
            this.contactRequestRepository.save(contactRequest)
            return ConfirmContactRequestResult(
                contactRequest.organizationId,
                organization.urlName
            )
        } catch (e: JpaObjectRetrievalFailureException) {
            throw ContactRequestInvalid()
        }
    }

    open fun getById(contactRequestId: ContactRequestId): ConfirmContactRequestResult {
        val contactRequest = this.contactRequestRepository.getOne(contactRequestId)
        val organization = this.organizationRepository.findOne(contactRequest.organizationId.value) ?: throw ContactRequestInvalid()
        return ConfirmContactRequestResult(
            contactRequest.organizationId,
            organization.urlName
        )
    }

    open fun createGeneralContactRequest(generalContactRequestDto: CreateGeneralContactRequestDto) {
        if (!this.captchaValidator.validate(generalContactRequestDto.captcha)) {
            throw CaptchaValidationFailedException()
        }

        val attributes = mapOf(
            Pair("domain", domain),
            Pair("contactRequest", generalContactRequestDto)
        )

        emailService.sendEmail(
            supportEmail,
            "general-contact-request-send-email",
            arrayOf(generalContactRequestDto.subject),
            attributes,
            listOf(),
            toLocale(),
            generalContactRequestDto.email
        )

    }

    private fun toLocale(): Locale {
        val localeParts = locale.split("_")
        return when (localeParts.size) {
            2 -> Locale(localeParts[0], localeParts[1])
            else -> Locale.getDefault()
        }
    }

    private fun sendConfirmationEmail(contactRequest: ContactRequest, organization: Organization) {
        val attributes = mapOf(
            Pair("domain", domain),
            Pair("contactRequest", contactRequest),
            Pair("organization", organization)
        )
        val attachments = listOf(
            Triple("logo", ClassPathResource("templates/logo.jpg"), "image/jpeg")
        )
        emailService.sendEmail(contactRequest.email, "contact-request-confirmation-email", arrayOf(contactRequest.subject), attributes, attachments, toLocale(), null)
    }

}