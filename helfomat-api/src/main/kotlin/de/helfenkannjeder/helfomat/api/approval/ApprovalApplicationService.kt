package de.helfenkannjeder.helfomat.api.approval

import de.helfenkannjeder.helfomat.api.Roles
import de.helfenkannjeder.helfomat.api.currentUsername
import de.helfenkannjeder.helfomat.api.organization.event.OrganizationEventDto
import de.helfenkannjeder.helfomat.api.organization.event.OrganizationEventDtoAssembler
import de.helfenkannjeder.helfomat.core.approval.ApprovalId
import de.helfenkannjeder.helfomat.core.approval.ApprovalRepository
import de.helfenkannjeder.helfomat.core.organization.Organization
import de.helfenkannjeder.helfomat.core.organization.OrganizationRepository
import de.helfenkannjeder.helfomat.core.organization.event.ConfirmedChangeOrganizationEvent
import de.helfenkannjeder.helfomat.core.organization.event.OrganizationEvent.Companion.toPictureIds
import de.helfenkannjeder.helfomat.core.organization.event.ProposedChangeOrganizationEvent
import de.helfenkannjeder.helfomat.core.picture.PictureRepository
import de.helfenkannjeder.helfomat.core.question.QuestionRepository
import de.helfenkannjeder.helfomat.core.user.UserRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.PageRequest
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Service


/**
 * @author Valentin Zickner
 */
@Service
@Secured(Roles.ADMIN, Roles.REVIEWER)
open class ApprovalApplicationService(
    private val approvalRepository: ApprovalRepository,
    private val organizationRepository: OrganizationRepository,
    private val questionRepository: QuestionRepository,
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val pictureRepository: PictureRepository,
    private val userRepository: UserRepository
) {

    open fun findItemsToApprove(): List<ApprovalOverviewDto> {
        return approvalRepository.findToApprove()
            .map {
                val organizationId = it.requestedDomainEvent.organizationId
                val organization: Organization? = organizationRepository.findOne(organizationId.value)
                it.toApprovalOverviewDto(organizationId, organization)
            }
    }

    open fun findApprovalHistory(): List<ApprovalOverviewDto> {
        return approvalRepository.findApprovalHistory(PageRequest.of(0, 20))
            .map {
                val organizationId = it.requestedDomainEvent.organizationId
                val organization: Organization? = organizationRepository.findOne(organizationId.value)
                it.toApprovalOverviewDto(organizationId, organization)
            }
    }

    open fun findApprovalItem(approvalId: ApprovalId): ApprovalDetailDto {
        val questions = this.questionRepository.findQuestions()
        val approval = approvalRepository.getReferenceById(approvalId)
        val author = userRepository.findByUsername(approval.requestedDomainEvent.author)
        val organizationId = approval.requestedDomainEvent.organizationId
        val organization: Organization? = organizationRepository.findOne(organizationId.value)
        return approval.toApprovalDetailDto(organization, questions, author, approval.approvedDomainEvent != null)
    }

    open fun confirmOrganizationChange(approvalId: ApprovalId, confirmedEvents: List<OrganizationEventDto>) {
        val approval = this.approvalRepository.getReferenceById(approvalId)
        if (approval.approvedDomainEvent != null) {
            return
        }
        val proposedChangeOrganizationEvent = approval.requestedDomainEvent
        val confirmedChangeOrganizationEvent = ConfirmedChangeOrganizationEvent(
            proposedChangeOrganizationEvent.organizationId,
            currentUsername(),
            proposedChangeOrganizationEvent.author,
            proposedChangeOrganizationEvent.sources,
            OrganizationEventDtoAssembler.toOrganizationEvent(confirmedEvents)
        )
        approval.approvedDomainEvent = confirmedChangeOrganizationEvent
        approval.isDeclined = confirmedEvents.isEmpty()
        approvalRepository.save(approval)
        if (confirmedEvents.isNotEmpty()) {
            publishNewPictures(proposedChangeOrganizationEvent)
            applicationEventPublisher.publishEvent(confirmedChangeOrganizationEvent)
        }
    }

    private fun publishNewPictures(proposedChangeOrganizationEvent: ProposedChangeOrganizationEvent) {
        this.pictureRepository.saveAll(
            pictureRepository.findAllById(toPictureIds(proposedChangeOrganizationEvent.changes))
                .map { it.apply { public = true } }
        )
    }

}