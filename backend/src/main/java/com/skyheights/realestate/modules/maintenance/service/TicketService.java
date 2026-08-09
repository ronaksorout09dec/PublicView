package com.skyheights.realestate.modules.maintenance.service;

import com.skyheights.realestate.common.service.S3Service;
import com.skyheights.realestate.exception.ResourceNotFoundException;
import com.skyheights.realestate.modules.maintenance.dto.TicketCreateRequest;
import com.skyheights.realestate.modules.maintenance.dto.TicketResponse;
import com.skyheights.realestate.modules.maintenance.entity.MaintenanceTicket;
import com.skyheights.realestate.modules.maintenance.entity.TicketMedia;
import com.skyheights.realestate.modules.maintenance.enums.TicketPriority;
import com.skyheights.realestate.modules.maintenance.enums.TicketStatus;
import com.skyheights.realestate.modules.maintenance.repository.MaintenanceTicketRepository;
import com.skyheights.realestate.modules.maintenance.repository.TicketMediaRepository;
import com.skyheights.realestate.modules.maintenance.repository.VendorBidRepository;
import com.skyheights.realestate.modules.maintenance.repository.VendorProfileRepository;
import com.skyheights.realestate.modules.organization.entity.AppUser;
import com.skyheights.realestate.modules.organization.repository.AppUserRepository;
import com.skyheights.realestate.modules.organization.repository.OrganizationRepository;
import com.skyheights.realestate.modules.portfolio.entity.Property;
import com.skyheights.realestate.modules.portfolio.entity.Unit;
import com.skyheights.realestate.modules.portfolio.repository.PropertyRepository;
import com.skyheights.realestate.modules.portfolio.repository.UnitRepository;
import com.skyheights.realestate.modules.tenant.entity.TenantProfile;
import com.skyheights.realestate.modules.tenant.repository.TenantProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketService {

    private final MaintenanceTicketRepository ticketRepository;
    private final TicketMediaRepository mediaRepository;
    private final VendorBidRepository bidRepository;
    private final VendorProfileRepository vendorRepository;
    private final OrganizationRepository organizationRepository;
    private final PropertyRepository propertyRepository;
    private final UnitRepository unitRepository;
    private final TenantProfileRepository tenantRepository;
    private final AppUserRepository appUserRepository;
    private final S3Service s3Service;

    @Transactional
    public TicketResponse createTicket(Long orgId, Long raisedByUserId, TicketCreateRequest request) {
        var org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

        Property property = propertyRepository.findByIdAndOrganizationIdAndIsDeletedFalse(request.getPropertyId(), orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));

        Unit unit = null;
        if (request.getUnitId() != null) {
            unit = unitRepository.findByIdAndOrganizationIdAndIsDeletedFalse(request.getUnitId(), orgId)
                    .orElseThrow(() -> new ResourceNotFoundException("Unit not found"));
            if (!unit.getProperty().getId().equals(property.getId())) {
                throw new RuntimeException("Unit does not belong to property");
            }
        }

        TenantProfile tenant = null;
        if (request.getTenantId() != null) {
            tenant = tenantRepository.findByIdAndOrganizationIdAndIsDeletedFalse(request.getTenantId(), orgId)
                    .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));
        }

        AppUser raisedBy = appUserRepository.findByIdAndIsDeletedFalse(raisedByUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Raised by user not found"));

        // SLA: based on priority
        Instant slaDue = calculateSlaDue(request.getPriority() != null ? request.getPriority() : TicketPriority.MEDIUM);

        MaintenanceTicket ticket = MaintenanceTicket.builder()
                .organization(org)
                .property(property)
                .unit(unit)
                .tenant(tenant)
                .raisedBy(raisedBy)
                .category(request.getCategory())
                .priority(request.getPriority() != null ? request.getPriority() : TicketPriority.MEDIUM)
                .title(request.getTitle())
                .description(request.getDescription())
                .status(TicketStatus.OPEN)
                .slaDueAt(slaDue)
                .build();

        ticket = ticketRepository.save(ticket);

        // Handle pre-uploaded S3 keys
        if (request.getMediaS3Keys() != null) {
            for (int i = 0; i < request.getMediaS3Keys().size(); i++) {
                String s3Key = request.getMediaS3Keys().get(i);
                String mediaType = request.getMediaTypes() != null && i < request.getMediaTypes().size() ? request.getMediaTypes().get(i) : "IMAGE";
                TicketMedia media = new TicketMedia();
                media.setTicket(ticket);
                media.setS3Key(s3Key);
                media.setMediaType(mediaType);
                media.setUploadedBy(raisedBy);
                mediaRepository.save(media);
            }
        }

        log.info("Created ticket {} category {} priority {} org {}", ticket.getId(), ticket.getCategory(), ticket.getPriority(), orgId);
        return toResponse(ticket);
    }

    @Transactional(readOnly = true)
    public Page<TicketResponse> searchTickets(Long orgId, Long propertyId, Long unitId, Long tenantId,
                                              TicketStatus status, TicketPriority priority, String category,
                                              Long assignedVendorId, String search, Pageable pageable) {
        Page<MaintenanceTicket> page = ticketRepository.search(orgId, propertyId, unitId, tenantId, status, priority, category, assignedVendorId, search, pageable);
        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public TicketResponse getTicket(Long orgId, Long id) {
        MaintenanceTicket ticket = ticketRepository.findByIdAndOrganizationIdAndIsDeletedFalse(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));
        return toResponse(ticket);
    }

    @Transactional
    public TicketResponse updateTicketStatus(Long orgId, Long id, TicketStatus newStatus) {
        MaintenanceTicket ticket = ticketRepository.findByIdAndOrganizationIdAndIsDeletedFalse(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));

        validateStatusTransition(ticket.getStatus(), newStatus);

        TicketStatus old = ticket.getStatus();
        ticket.setStatus(newStatus);

        if (newStatus == TicketStatus.COMPLETED || newStatus == TicketStatus.CLOSED) {
            ticket.setCompletedAt(Instant.now());
        }

        ticket = ticketRepository.save(ticket);
        log.info("Ticket {} status {} -> {} org {}", id, old, newStatus, orgId);
        return toResponse(ticket);
    }

    @Transactional
    public TicketResponse broadcastTicket(Long orgId, Long id) {
        MaintenanceTicket ticket = ticketRepository.findByIdAndOrganizationIdAndIsDeletedFalse(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));

        if (ticket.getStatus() != TicketStatus.OPEN) {
            throw new RuntimeException("Only OPEN tickets can be broadcasted");
        }

        ticket.setStatus(TicketStatus.BROADCASTED);
        ticket = ticketRepository.save(ticket);

        // Find matching vendors by specialization
        var matchingVendors = vendorRepository.findByOrganizationIdAndSpecializationAndIsDeletedFalse(orgId,
                com.skyheights.realestate.modules.maintenance.enums.VendorSpecialization.valueOf(ticket.getCategory()));

        log.info("Broadcasted ticket {} to {} vendors of specialization {} org {}", id, matchingVendors.size(), ticket.getCategory(), orgId);

        // In Phase 5, would create NotificationLog for each vendor via Communication domain
        // For now, just update status to BIDDING to allow bids
        ticket.setStatus(TicketStatus.BIDDING);
        ticket = ticketRepository.save(ticket);

        return toResponse(ticket);
    }

    @Transactional
    public TicketResponse addMedia(Long orgId, Long ticketId, List<MultipartFile> files, List<String> mediaTypes, List<String> captions, Long uploaderUserId) {
        MaintenanceTicket ticket = ticketRepository.findByIdAndOrganizationIdAndIsDeletedFalse(ticketId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));

        AppUser uploader = appUserRepository.findByIdAndIsDeletedFalse(uploaderUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Uploader not found"));

        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            if (file.isEmpty()) continue;
            try {
                String key = s3Service.generateKey(orgId, "maintenance/tickets/" + ticketId, file.getOriginalFilename());
                String s3Key = s3Service.uploadFile(key, file.getInputStream(), file.getSize(), file.getContentType());

                String type = mediaTypes != null && i < mediaTypes.size() ? mediaTypes.get(i) : detectMediaType(file.getContentType());
                String caption = captions != null && i < captions.size() ? captions.get(i) : null;

                TicketMedia media = new TicketMedia();
                media.setTicket(ticket);
                media.setS3Key(s3Key);
                media.setMediaType(type);
                media.setFileSize(file.getSize());
                media.setUploadedBy(uploader);
                media.setCaption(caption);
                mediaRepository.save(media);
                log.info("Uploaded media {} for ticket {} org {}", s3Key, ticketId, orgId);
            } catch (Exception e) {
                throw new RuntimeException("Failed to upload media: " + e.getMessage());
            }
        }

        return toResponse(ticket);
    }

    @Transactional
    public TicketResponse rateTicket(Long orgId, Long ticketId, int rating, String feedback) {
        if (rating < 1 || rating > 5) throw new RuntimeException("Rating must be 1-5");

        MaintenanceTicket ticket = ticketRepository.findByIdAndOrganizationIdAndIsDeletedFalse(ticketId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));

        if (ticket.getStatus() != TicketStatus.COMPLETED && ticket.getStatus() != TicketStatus.CLOSED) {
            throw new RuntimeException("Can only rate COMPLETED/CLOSED tickets");
        }

        ticket.setRatingByTenant(rating);
        ticket.setFeedback(feedback);
        ticket = ticketRepository.save(ticket);

        // Update vendor rating
        if (ticket.getAssignedVendor() != null) {
            var vendor = ticket.getAssignedVendor();
            // Simple avg: (oldRating*totalJobs + newRating) / (totalJobs+1)
            // For demo, recalculate naive
            int totalJobs = vendor.getTotalJobsCompleted() != null ? vendor.getTotalJobsCompleted() : 0;
            double oldRating = vendor.getRating() != null ? vendor.getRating().doubleValue() : 0;
            double newRating = (oldRating * totalJobs + rating) / (totalJobs + 1);
            vendor.setRating(java.math.BigDecimal.valueOf(newRating).setScale(2, java.math.RoundingMode.HALF_UP));
            vendor.setTotalJobsCompleted(totalJobs + 1);
            vendorRepository.save(vendor);
        }

        log.info("Ticket {} rated {} stars org {}", ticketId, rating, orgId);
        return toResponse(ticket);
    }

    private Instant calculateSlaDue(TicketPriority priority) {
        // SLA: URGENT 4h, HIGH 24h, MEDIUM 72h, LOW 168h (7 days)
        long hours = switch (priority) {
            case URGENT -> 4;
            case HIGH -> 24;
            case MEDIUM -> 72;
            case LOW -> 168;
        };
        return Instant.now().plus(Duration.ofHours(hours));
    }

    private void validateStatusTransition(TicketStatus current, TicketStatus target) {
        if (current == target) return;
        switch (current) {
            case OPEN:
                if (target != TicketStatus.BROADCASTED && target != TicketStatus.BIDDING && target != TicketStatus.CANCELLED) {
                    throw new RuntimeException("OPEN can only go to BROADCASTED/BIDDING/CANCELLED");
                }
                break;
            case BROADCASTED:
                if (target != TicketStatus.BIDDING && target != TicketStatus.CANCELLED) {
                    throw new RuntimeException("BROADCASTED can only go to BIDDING/CANCELLED");
                }
                break;
            case BIDDING:
                if (target != TicketStatus.ASSIGNED && target != TicketStatus.CANCELLED) {
                    throw new RuntimeException("BIDDING can only go to ASSIGNED/CANCELLED");
                }
                break;
            case ASSIGNED:
                if (target != TicketStatus.IN_PROGRESS && target != TicketStatus.CANCELLED) {
                    throw new RuntimeException("ASSIGNED can only go to IN_PROGRESS/CANCELLED");
                }
                break;
            case IN_PROGRESS:
                if (target != TicketStatus.COMPLETED && target != TicketStatus.PENDING_PARTS && target != TicketStatus.CANCELLED) {
                    throw new RuntimeException("IN_PROGRESS can only go to COMPLETED/PENDING_PARTS/CANCELLED");
                }
                break;
            case PENDING_PARTS:
                if (target != TicketStatus.IN_PROGRESS && target != TicketStatus.CANCELLED) {
                    throw new RuntimeException("PENDING_PARTS can only go to IN_PROGRESS/CANCELLED");
                }
                break;
            case COMPLETED:
                if (target != TicketStatus.CLOSED) {
                    throw new RuntimeException("COMPLETED can only go to CLOSED");
                }
                break;
            case CANCELLED:
            case CLOSED:
                throw new RuntimeException(current + " is terminal");
        }
    }

    private String detectMediaType(String contentType) {
        if (contentType == null) return "IMAGE";
        if (contentType.startsWith("image/")) return "IMAGE";
        if (contentType.startsWith("video/")) return "VIDEO";
        return "DOCUMENT";
    }

    private TicketResponse toResponse(MaintenanceTicket t) {
        List<TicketMedia> medias = mediaRepository.findByTicketId(t.getId());
        List<TicketResponse.MediaResponse> mediaResponses = medias.stream().map(m -> {
            String presigned = null;
            try {
                presigned = s3Service.generatePresignedUrl(m.getS3Key(), Duration.ofMinutes(30));
            } catch (Exception ignored) {}
            return TicketResponse.MediaResponse.builder()
                    .id(m.getId()).uuid(m.getUuid()).s3Key(m.getS3Key()).presignedUrl(presigned)
                    .mediaType(m.getMediaType()).fileSize(m.getFileSize()).caption(m.getCaption()).createdAt(m.getCreatedAt())
                    .build();
        }).collect(Collectors.toList());

        boolean slaBreached = t.getSlaDueAt() != null && Instant.now().isAfter(t.getSlaDueAt()) &&
                (t.getStatus() != TicketStatus.COMPLETED && t.getStatus() != TicketStatus.CLOSED && t.getStatus() != TicketStatus.CANCELLED);

        long bidsCount = bidRepository.countByTicketIdAndIsDeletedFalse(t.getId());

        return TicketResponse.builder()
                .id(t.getId()).uuid(t.getUuid())
                .orgId(t.getOrganization() != null ? t.getOrganization().getId() : null)
                .propertyId(t.getProperty() != null ? t.getProperty().getId() : null)
                .propertyName(t.getProperty() != null ? t.getProperty().getName() : null)
                .unitId(t.getUnit() != null ? t.getUnit().getId() : null)
                .unitNumber(t.getUnit() != null ? t.getUnit().getUnitNumber() : null)
                .tenantId(t.getTenant() != null ? t.getTenant().getId() : null)
                .tenantName(t.getTenant() != null && t.getTenant().getUser() != null ? t.getTenant().getUser().getFullName() : null)
                .raisedByUserId(t.getRaisedBy() != null ? t.getRaisedBy().getId() : null)
                .raisedByName(t.getRaisedBy() != null ? t.getRaisedBy().getFullName() : null)
                .category(t.getCategory()).priority(t.getPriority()).title(t.getTitle()).description(t.getDescription())
                .status(t.getStatus())
                .assignedVendorId(t.getAssignedVendor() != null ? t.getAssignedVendor().getId() : null)
                .assignedVendorName(t.getAssignedVendor() != null ? t.getAssignedVendor().getCompanyName() : null)
                .assignedBidId(t.getAssignedBidId())
                .estimatedCost(t.getEstimatedCost()).actualCost(t.getActualCost())
                .scheduledAt(t.getScheduledAt()).completedAt(t.getCompletedAt())
                .completionNotes(t.getCompletionNotes()).ratingByTenant(t.getRatingByTenant()).feedback(t.getFeedback())
                .slaDueAt(t.getSlaDueAt()).slaBreached(slaBreached)
                .media(mediaResponses).bidsCount(bidsCount)
                .createdAt(t.getCreatedAt()).updatedAt(t.getUpdatedAt())
                .build();
    }
}
