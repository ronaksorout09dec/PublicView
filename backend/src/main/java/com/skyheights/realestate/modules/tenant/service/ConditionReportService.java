package com.skyheights.realestate.modules.tenant.service;

import com.skyheights.realestate.common.service.S3Service;
import com.skyheights.realestate.exception.ResourceNotFoundException;
import com.skyheights.realestate.modules.organization.entity.AppUser;
import com.skyheights.realestate.modules.organization.repository.AppUserRepository;
import com.skyheights.realestate.modules.organization.repository.OrganizationRepository;
import com.skyheights.realestate.modules.tenant.dto.ConditionReportCreateRequest;
import com.skyheights.realestate.modules.tenant.dto.ConditionReportResponse;
import com.skyheights.realestate.modules.tenant.entity.*;
import com.skyheights.realestate.modules.tenant.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConditionReportService {

    private final UnitConditionReportRepository reportRepository;
    private final ConditionReportItemRepository itemRepository;
    private final ConditionPhotoRepository photoRepository;
    private final LeaseAgreementRepository leaseRepository;
    private final TenantProfileRepository tenantRepository;
    private final ChecklistTemplateRepository templateRepository;
    private final OrganizationRepository organizationRepository;
    private final AppUserRepository appUserRepository;
    private final S3Service s3Service;

    @Transactional
    public ConditionReportResponse createReport(Long orgId, Long inspectorUserId, ConditionReportCreateRequest request) {
        var org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

        var lease = leaseRepository.findByIdAndOrganizationIdAndIsDeletedFalse(request.getLeaseId(), orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Lease not found"));

        var tenant = lease.getTenant();
        var unit = lease.getUnit();

        ChecklistTemplate template = null;
        if (request.getTemplateId() != null) {
            template = templateRepository.findByIdAndOrganizationIdAndIsDeletedFalse(request.getTemplateId(), orgId)
                    .orElseThrow(() -> new ResourceNotFoundException("Template not found"));
        }

        AppUser inspector = appUserRepository.findByIdAndIsDeletedFalse(inspectorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Inspector user not found"));

        UnitConditionReport report = UnitConditionReport.builder()
                .organization(org)
                .lease(lease)
                .unit(unit)
                .tenant(tenant)
                .type(request.getType())
                .template(template)
                .inspectedBy(inspector)
                .inspectedAt(Instant.now())
                .overallCondition(request.getOverallCondition() != null ? request.getOverallCondition() : "GOOD")
                .notes(request.getNotes())
                .status("COMPLETED")
                .build();

        report = reportRepository.save(report);

        // Create items
        List<ConditionReportItem> savedItems = new ArrayList<>();
        if (request.getItems() != null) {
            for (var itemReq : request.getItems()) {
                ConditionReportItem item = new ConditionReportItem();
                item.setReport(report);
                item.setArea(itemReq.getArea());
                item.setItemName(itemReq.getItemName());
                item.setCondition(itemReq.getCondition());
                item.setDescription(itemReq.getDescription());
                item.setEstimatedRepairCost(itemReq.getEstimatedRepairCost() != null ? itemReq.getEstimatedRepairCost() : BigDecimal.ZERO);
                item = itemRepository.save(item);

                // Create photo entries if s3 keys provided
                if (itemReq.getPhotoS3Keys() != null) {
                    for (int i = 0; i < itemReq.getPhotoS3Keys().size(); i++) {
                        String s3Key = itemReq.getPhotoS3Keys().get(i);
                        String caption = itemReq.getPhotoCaptions() != null && i < itemReq.getPhotoCaptions().size()
                                ? itemReq.getPhotoCaptions().get(i) : null;

                        ConditionPhoto photo = ConditionPhoto.builder()
                                .reportItem(item)
                                .report(report)
                                .s3Key(s3Key)
                                .caption(caption)
                                .takenAt(Instant.now())
                                .build();
                        photoRepository.save(photo);
                    }
                }

                savedItems.add(item);
            }
        }

        report.setItems(savedItems);
        log.info("Created condition report {} type {} for lease {} org {}", report.getId(), report.getType(), lease.getId(), orgId);
        return toResponse(report);
    }

    @Transactional
    public ConditionReportResponse uploadPhotos(Long orgId, Long reportId, Long itemId, List<MultipartFile> files, List<String> captions) {
        UnitConditionReport report = reportRepository.findByIdAndOrganizationIdAndIsDeletedFalse(reportId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));

        ConditionReportItem item = null;
        if (itemId != null) {
            item = itemRepository.findById(itemId)
                    .orElseThrow(() -> new ResourceNotFoundException("Report item not found"));
            if (!item.getReport().getId().equals(reportId)) {
                throw new RuntimeException("Item does not belong to report");
            }
        }

        List<ConditionPhoto> uploaded = new ArrayList<>();
        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            if (file.isEmpty()) continue;
            try {
                String key = s3Service.generateKey(orgId, "condition-reports/" + reportId, file.getOriginalFilename());
                String s3Key = s3Service.uploadFile(key, file.getInputStream(), file.getSize(), file.getContentType());

                ConditionPhoto photo = ConditionPhoto.builder()
                        .reportItem(item)
                        .report(report)
                        .s3Key(s3Key)
                        .caption(captions != null && i < captions.size() ? captions.get(i) : null)
                        .takenAt(Instant.now())
                        .build();
                photo = photoRepository.save(photo);
                uploaded.add(photo);
                log.info("Uploaded condition photo {} for report {} org {}", s3Key, reportId, orgId);
            } catch (Exception e) {
                throw new RuntimeException("Failed to upload photo: " + e.getMessage());
            }
        }

        return toResponse(report);
    }

    @Transactional(readOnly = true)
    public Page<ConditionReportResponse> searchReports(Long orgId, Long leaseId, Long unitId, Long tenantId, com.skyheights.realestate.modules.tenant.enums.ReportType type, Pageable pageable) {
        Page<UnitConditionReport> page = reportRepository.search(orgId, leaseId, unitId, tenantId, type, pageable);
        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ConditionReportResponse getReport(Long orgId, Long id) {
        UnitConditionReport report = reportRepository.findByIdAndOrganizationIdAndIsDeletedFalse(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Condition report not found"));
        return toResponse(report);
    }

    @Transactional
    public ConditionReportResponse updateReportStatus(Long orgId, Long id, String status) {
        UnitConditionReport report = reportRepository.findByIdAndOrganizationIdAndIsDeletedFalse(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));
        report.setStatus(status);
        report = reportRepository.save(report);
        return toResponse(report);
    }

    @Transactional(readOnly = true)
    public ConditionReportResponse toResponse(UnitConditionReport report) {
        // Fetch items
        List<ConditionReportItem> items = itemRepository.findByReportId(report.getId());
        List<ConditionReportResponse.ConditionReportItemResponse> itemResponses = items.stream().map(item -> {
            List<ConditionPhoto> photos = photoRepository.findByReportItemId(item.getId());
            List<ConditionReportResponse.ConditionPhotoResponse> photoResponses = photos.stream().map(p -> {
                String presigned = null;
                try {
                    presigned = s3Service.generatePresignedUrl(p.getS3Key(), Duration.ofMinutes(30));
                } catch (Exception e) {
                    log.warn("Failed presigned for photo {}", p.getId());
                }
                return ConditionReportResponse.ConditionPhotoResponse.builder()
                        .id(p.getId())
                        .uuid(p.getUuid())
                        .s3Key(p.getS3Key())
                        .presignedUrl(presigned)
                        .caption(p.getCaption())
                        .takenAt(p.getTakenAt())
                        .build();
            }).collect(Collectors.toList());

            return ConditionReportResponse.ConditionReportItemResponse.builder()
                    .id(item.getId())
                    .area(item.getArea())
                    .itemName(item.getItemName())
                    .condition(item.getCondition())
                    .description(item.getDescription())
                    .estimatedRepairCost(item.getEstimatedRepairCost())
                    .photos(photoResponses)
                    .build();
        }).collect(Collectors.toList());

        BigDecimal totalRepair = itemResponses.stream()
                .map(i -> i.getEstimatedRepairCost() != null ? i.getEstimatedRepairCost() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String pdfPresigned = null;
        try {
            if (report.getPdfS3Key() != null) {
                pdfPresigned = s3Service.generatePresignedUrl(report.getPdfS3Key(), Duration.ofMinutes(30));
            }
        } catch (Exception e) {
            log.warn("Failed presigned for report pdf {}", report.getId());
        }

        return ConditionReportResponse.builder()
                .id(report.getId())
                .uuid(report.getUuid())
                .orgId(report.getOrganization() != null ? report.getOrganization().getId() : null)
                .leaseId(report.getLease() != null ? report.getLease().getId() : null)
                .leaseNumber(report.getLease() != null ? report.getLease().getLeaseNumber() : null)
                .unitId(report.getUnit() != null ? report.getUnit().getId() : null)
                .unitNumber(report.getUnit() != null ? report.getUnit().getUnitNumber() : null)
                .tenantId(report.getTenant() != null ? report.getTenant().getId() : null)
                .tenantName(report.getTenant() != null && report.getTenant().getUser() != null ? report.getTenant().getUser().getFullName() : null)
                .type(report.getType())
                .templateId(report.getTemplate() != null ? report.getTemplate().getId() : null)
                .templateName(report.getTemplate() != null ? report.getTemplate().getName() : null)
                .inspectedByUserId(report.getInspectedBy() != null ? report.getInspectedBy().getId() : null)
                .inspectedByName(report.getInspectedBy() != null ? report.getInspectedBy().getFullName() : null)
                .inspectedAt(report.getInspectedAt())
                .overallCondition(report.getOverallCondition())
                .notes(report.getNotes())
                .status(report.getStatus())
                .pdfS3Key(report.getPdfS3Key())
                .pdfPresignedUrl(pdfPresigned)
                .items(itemResponses)
                .totalEstimatedRepairCost(totalRepair)
                .createdAt(report.getCreatedAt())
                .build();
    }
}
