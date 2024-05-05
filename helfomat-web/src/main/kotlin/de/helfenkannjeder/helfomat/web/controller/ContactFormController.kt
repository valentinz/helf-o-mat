package de.helfenkannjeder.helfomat.web.controller

import de.helfenkannjeder.helfomat.api.contact.ContactApplicationService
import de.helfenkannjeder.helfomat.api.contact.CreateGeneralContactRequestDto
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api"], produces = [MediaType.APPLICATION_JSON_VALUE])
class ContactFormController(
    private val contactApplicationService: ContactApplicationService
) {

    @PostMapping("/contact-form")
    fun createGeneralContactRequest(@RequestBody generalContactRequestDto: CreateGeneralContactRequestDto) = contactApplicationService.createGeneralContactRequest(generalContactRequestDto)

}