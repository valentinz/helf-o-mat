package de.helfenkannjeder.helfomat.api.organization

import de.helfenkannjeder.helfomat.api.Roles
import de.helfenkannjeder.helfomat.api.currentUsername
import de.helfenkannjeder.helfomat.api.isAuthenticated
import de.helfenkannjeder.helfomat.api.organization.event.OrganizationEventDto
import de.helfenkannjeder.helfomat.api.organization.event.OrganizationEventDtoAssembler
import de.helfenkannjeder.helfomat.api.organization.event.toOrganizationEventDtos
import de.helfenkannjeder.helfomat.core.approval.ApprovalRepository
import de.helfenkannjeder.helfomat.core.geopoint.BoundingBox
import de.helfenkannjeder.helfomat.core.geopoint.GeoPoint
import de.helfenkannjeder.helfomat.core.organization.*
import de.helfenkannjeder.helfomat.core.organization.event.OrganizationCreateEvent
import de.helfenkannjeder.helfomat.core.organization.event.OrganizationEditUrlNameEvent
import de.helfenkannjeder.helfomat.core.organization.event.OrganizationEvent
import de.helfenkannjeder.helfomat.core.organization.event.ProposedChangeOrganizationEvent
import de.helfenkannjeder.helfomat.core.question.QuestionRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.security.access.annotation.Secured
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/**
 * @author Valentin Zickner
 */
@Service
open class OrganizationApplicationService(
    private var organizationRepository: OrganizationRepository,
    private var questionRepository: QuestionRepository,
    private var applicationEventPublisher: ApplicationEventPublisher,
    private var answerOrganizationQuestionService: AnswerOrganizationQuestionService,
    private var approvalRepository: ApprovalRepository
) {

    open fun findOrganizationDetails(urlName: String): OrganizationDetailDto? {
        var organization = organizationRepository.findByUrlName(urlName)
        val questions = questionRepository.findQuestions()
        var isPreview = false
        if (isAuthenticated() && organization != null) {
            val findToApproveWithCreator = approvalRepository.findToApproveWithCreator(currentUsername())
            val builder = Organization.Builder(organization)
            val id = organization.id
            val allChanges = findToApproveWithCreator
                .map { it.requestedDomainEvent }
                .filter { it.organizationId == id }
                .flatMap { it.changes }
            isPreview = allChanges.isNotEmpty()
            allChanges
                .forEach { it.applyOnOrganizationBuilder(builder, false) }
            organization = builder.build()
        }
        return organization?.toOrganizationDetailDto(questions, isPreview)
    }

    open fun findGlobalOrganizations(): List<OrganizationDto> {
        return organizationRepository.findGlobalOrganizations().toOrganizationDtos()
    }

    open fun getGlobalOrganizationByType(organizationType: OrganizationType): OrganizationDetailDto {
        val questions = questionRepository.findQuestions()
        return organizationRepository.findGlobalOrganizations()
            .firstOrNull { it.organizationType == organizationType }
            ?.toOrganizationDetailDto(questions)
            ?: OrganizationDetailDto(
                id = OrganizationId().value,
                name = organizationType.internalName,
                organizationType = organizationType,
                urlName = organizationType.name.lowercase()
            )
    }

    open fun findGlobalOrganizationsWith(questionAnswerDtos: List<QuestionAnswerDto>): List<OrganizationDto> {
        return organizationRepository.findGlobalOrganizationsByQuestionAnswersSortByAnswerMatch(questionAnswerDtos.toQuestionAnswers())
            .toScoredOrganizationDtos()
    }

    open fun findOrganizationsWith(position: GeoPoint, distance: Double): List<OrganizationDto> {
        val organizations = organizationRepository.findOrganizationsByDistanceSortByDistance(position, distance)

        val organizationTypes = toOrganizationTypes(organizations)
        val resultOrganizations = mutableListOf<Organization>()
        resultOrganizations.addAll(organizations)
        for (globalOrganization in organizationRepository.findGlobalOrganizations()) {
            if (!organizationTypes.contains(globalOrganization.organizationType)) {
                resultOrganizations.add(globalOrganization)
            }
        }
        return resultOrganizations.toOrganizationDtos()
    }

    open fun findOrganizationsWith(questionAnswerDtos: List<QuestionAnswerDto>, position: GeoPoint, distance: Double): List<OrganizationDto> {
        val questionAnswers = questionAnswerDtos.toQuestionAnswers()
        val organizations = organizationRepository.findOrganizationsByQuestionAnswersAndDistanceSortByAnswerMatchAndDistance(
            questionAnswers,
            position,
            distance
        )
        val organizationTypes = toOrganizationTypes(organizations.map { it.organization })

        val resultOrganizations = mutableListOf<ScoredOrganization>()
        resultOrganizations.addAll(organizations)
        for (globalOrganization in organizationRepository.findGlobalOrganizationsByQuestionAnswersSortByAnswerMatch(questionAnswers)) {
            if (!organizationTypes.contains(globalOrganization.organization.organizationType)) {
                resultOrganizations.add(globalOrganization)
            }
        }
        return resultOrganizations
            .sortedByDescending { it.score }
            .toScoredOrganizationDtos()
    }

    open fun findClusteredGeoPoints(position: GeoPoint?, distance: Double, boundingBox: BoundingBox): List<GeoPoint> {
        return organizationRepository.findGeoPointsOfOrganizationsInsideBoundingBox(position, distance, boundingBox)
    }

    open fun findOrganizationTypes() = OrganizationType.values().toOrganizationTypeDtos().sortedBy { it.name }

    open fun compareOrganizations(compareOrganizationDto: CompareOrganizationDto): List<OrganizationEventDto> {
        val original = compareOrganizationDto.original?.toOrganization()
        val updated = compareOrganizationDto.updated.toOrganization()
        val updatedWithAnsweredQuestions = answerOrganizationQuestionService.answerQuestions(updated)
        val questions = questionRepository.findQuestions()
        return updatedWithAnsweredQuestions.compareTo(original).toOrganizationEventDtos(questions, original)
    }

    @PreAuthorize("isAuthenticated()")
    open fun submitOrganization(organizationSubmitEventDto: OrganizationSubmitEventDto) {
        val organizationEvents = OrganizationEventDtoAssembler.toOrganizationEvent(organizationSubmitEventDto.events)
        val organizationId = organizationSubmitEventDto.organizationId
        assertOrganizationSubmitValid(organizationId, organizationEvents)
        val proposedChangeOrganizationEvent = ProposedChangeOrganizationEvent(
            organizationId,
            currentUsername(),
            organizationSubmitEventDto.sources,
            organizationEvents
        )
        applicationEventPublisher.publishEvent(proposedChangeOrganizationEvent)
    }

    @PreAuthorize("isAuthenticated()")
    open fun validateWebsite(validateWebsiteDto: ValidateWebsiteDto): ValidateWebsiteResultDto {
        var websiteUrl = validateWebsiteDto.websiteUrl
        if (!websiteUrl.startsWith("http://") && !websiteUrl.startsWith("https://")) {
            websiteUrl = "http://" + websiteUrl
        }
        if (websiteUrl.startsWith("http://")) {
            val httpsUrl = websiteUrl.replace("http://", "https://")
            if (isUrlReachable(httpsUrl)) {
                return ValidateWebsiteResultDto(
                    httpsUrl,
                    true
                )
            }
        }

        if (isUrlReachable(websiteUrl)) {
            return ValidateWebsiteResultDto(
                websiteUrl,
                true
            )
        }

        return ValidateWebsiteResultDto(
            websiteUrl,
            false
        )
    }

    @Secured(Roles.ADMIN)
    open fun findSimilarOrganizations(searchSimilarOrganizationDto: SearchSimilarOrganizationDto): List<OrganizationDetailDto> {
        val address = searchSimilarOrganizationDto.address?.toAddress()
        val organizationType = searchSimilarOrganizationDto.organizationType
        val distance = searchSimilarOrganizationDto.distanceInMeters
        val organizations = organizationRepository.findOrganizationWithSameTypeInDistance(address, organizationType, distance)
        val questions = questionRepository.findQuestions()
        return organizations.toOrganizationDetailsDto(questions)
    }

    private fun assertOrganizationSubmitValid(organizationId: OrganizationId, events: List<OrganizationEvent>) {
        val databaseOrganization = organizationRepository.findOne(organizationId.value)
        val isCreate = events
            .mapNotNull {
                it.visit(object : NullableOrganizationEventVisitor<OrganizationCreateEvent> {
                    override fun visit(organizationCreateEvent: OrganizationCreateEvent): OrganizationCreateEvent? = organizationCreateEvent
                })
            }
            .any()
        val urlName = getUrlName(events)
        if (urlName != null) {
            val organizationWithSameName = organizationRepository.findByUrlName(urlName)
            if (organizationWithSameName != null) {
                throw OrganizationConflictException(organizationId, organizationWithSameName.id)
            }
        }

        if (databaseOrganization != null && isCreate) {
            throw OrganizationConflictException(organizationId, databaseOrganization.id)
        }
        if (databaseOrganization == null && !isCreate) {
            throw OrganizationNotFoundException(organizationId)
        }
    }

    private fun getUrlName(events: List<OrganizationEvent>): String? {
        return events
            .mapNotNull {
                it.visit(object : NullableOrganizationEventVisitor<String> {
                    override fun visit(organizationCreateEvent: OrganizationCreateEvent): String? = organizationCreateEvent.urlName
                    override fun visit(organizationEditUrlNameEvent: OrganizationEditUrlNameEvent): String? = organizationEditUrlNameEvent.urlName
                })
            }
            .lastOrNull()

    }

    private fun toOrganizationTypes(organizations: List<Organization>): List<OrganizationType> {
        return organizations
            .map { it.organizationType }
            .distinct()
    }

    private fun isUrlReachable(websiteUrl: String): Boolean {
        try {
            val client = HttpClient.newHttpClient()
            val request = HttpRequest.newBuilder()
                .uri(URI.create(websiteUrl))
                .build()
            val result = client.send(request, HttpResponse.BodyHandlers.ofString())
                .statusCode()
            return result in 200..399
        } catch (e: Exception) {
            return false
        }
    }

}