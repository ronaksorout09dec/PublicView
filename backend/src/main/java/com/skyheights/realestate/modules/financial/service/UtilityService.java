package com.skyheights.realestate.modules.financial.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skyheights.realestate.common.service.S3Service;
import com.skyheights.realestate.exception.ResourceNotFoundException;
import com.skyheights.realestate.modules.financial.dto.*;
import com.skyheights.realestate.modules.financial.entity.*;
import com.skyheights.realestate.modules.financial.repository.*;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UtilityService {

    private final UtilityTypeRepository utilityTypeRepository;
    private final UtilityMeterRepository utilityMeterRepository;
    private final UtilityReadingRepository utilityReadingRepository;
    private final UtilityBillRepository utilityBillRepository;
    private final UtilityBillSplitRepository utilityBillSplitRepository;
    private final OrganizationRepository organizationRepository;
    private final PropertyRepository propertyRepository;
    private final UnitRepository unitRepository;
    private final TenantProfileRepository tenantRepository;
    private final S3Service s3Service;
    private final ObjectMapper objectMapper;

    // ===== Utility Types =====
    @Transactional
    public UtilityTypeResponse createUtilityType(Long orgId, UtilityTypeCreateRequest request) {
        var org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

        if (utilityTypeRepository.findByOrganizationIdAndNameAndIsDeletedFalse(orgId, request.getName()).isPresent()) {
            throw new RuntimeException("Utility type already exists: " + request.getName());
        }

        UtilityType type = UtilityType.builder()
                .organization(org)
                .name(request.getName().toUpperCase())
                .unitLabel(request.getUnitLabel())
                .defaultRate(request.getDefaultRate())
                .build();

        type = utilityTypeRepository.save(type);
        log.info("Created utility type {} org {}", type.getName(), orgId);
        return toTypeResponse(type);
    }

    @Transactional(readOnly = true)
    public List<UtilityTypeResponse> getUtilityTypes(Long orgId) {
        return utilityTypeRepository.findByOrganizationIdAndIsDeletedFalse(orgId).stream()
                .map(this::toTypeResponse)
                .collect(Collectors.toList());
    }

    // ===== Meters =====
    @Transactional
    public UtilityMeterResponse createMeter(Long orgId, UtilityMeterCreateRequest request) {
        var org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

        Property property = propertyRepository.findByIdAndOrganizationIdAndIsDeletedFalse(request.getPropertyId(), orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));

        Unit unit = null;
        if (request.getUnitId() != null) {
            unit = unitRepository.findByIdAndOrganizationIdAndIsDeletedFalse(request.getUnitId(), orgId)
                    .orElseThrow(() -> new ResourceNotFoundException("Unit not found"));
        }

        UtilityType utilityType = utilityTypeRepository.findByIdAndOrganizationIdAndIsDeletedFalse(request.getUtilityTypeId(), orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Utility type not found"));

        if (utilityMeterRepository.findByMeterNumber(request.getMeterNumber()).isPresent()) {
            throw new RuntimeException("Meter number already exists: " + request.getMeterNumber());
        }

        UtilityMeter meter = UtilityMeter.builder()
                .organization(org)
                .property(property)
                .unit(unit)
                .utilityType(utilityType)
                .meterNumber(request.getMeterNumber())
                .isShared(request.getIsShared() != null ? request.getIsShared() : false)
                .location(request.getLocation())
                .totalUnitsSharing(request.getTotalUnitsSharing() != null ? request.getTotalUnitsSharing() : 1)
                .ratioConfig(request.getRatioConfig())
                .status("ACTIVE")
                .build();

        meter = utilityMeterRepository.save(meter);
        log.info("Created utility meter {} org {}", meter.getMeterNumber(), orgId);
        return toMeterResponse(meter);
    }

    @Transactional(readOnly = true)
    public List<UtilityMeterResponse> getMetersByProperty(Long orgId, Long propertyId) {
        propertyRepository.findByIdAndOrganizationIdAndIsDeletedFalse(propertyId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));
        return utilityMeterRepository.findByPropertyIdAndIsDeletedFalse(propertyId).stream()
                .filter(m -> !Boolean.TRUE.equals(m.getIsDeleted()))
                .map(this::toMeterResponse)
                .collect(Collectors.toList());
    }

    // ===== Readings =====
    @Transactional
    public UtilityReadingResponse createReading(Long orgId, UtilityReadingCreateRequest request) {
        UtilityMeter meter = utilityMeterRepository.findByIdAndOrganizationIdAndIsDeletedFalse(request.getMeterId(), orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Meter not found"));

        // Validate current > previous
        BigDecimal prev = request.getPreviousReading() != null ? request.getPreviousReading() : BigDecimal.ZERO;
        if (request.getCurrentReading().compareTo(prev) < 0) {
            throw new RuntimeException("Current reading cannot be less than previous");
        }

        // Check previous reading from last reading if not provided
        if (request.getPreviousReading() == null) {
            var last = utilityReadingRepository.findTopByMeterIdOrderByReadingDateDesc(meter.getId());
            if (last.isPresent()) {
                prev = last.get().getCurrentReading();
                if (request.getCurrentReading().compareTo(prev) < 0) {
                    throw new RuntimeException("Current reading less than last recorded: " + prev);
                }
            }
        }

        BigDecimal amount = request.getCurrentReading().subtract(prev).multiply(request.getRatePerUnit()).setScale(2, RoundingMode.HALF_UP);

        UtilityReading reading = UtilityReading.builder()
                .meter(meter)
                .readingDate(request.getReadingDate())
                .previousReading(prev)
                .currentReading(request.getCurrentReading())
                .ratePerUnit(request.getRatePerUnit())
                .amount(amount)
                .photoS3Key(request.getPhotoS3Key())
                .source("MANUAL")
                .build();

        reading = utilityReadingRepository.save(reading);
        log.info("Created reading for meter {} org {} amount {}", meter.getMeterNumber(), orgId, amount);
        return toReadingResponse(reading);
    }

    @Transactional(readOnly = true)
    public Page<UtilityReadingResponse> getReadings(Long meterId, Pageable pageable) {
        return utilityReadingRepository.findByMeterId(meterId, pageable).map(this::toReadingResponse);
    }

    // ===== Bills + Splits =====
    @Transactional
    public UtilityBillResponse createBill(Long orgId, UtilityBillCreateRequest request) {
        var org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

        Property property = propertyRepository.findByIdAndOrganizationIdAndIsDeletedFalse(request.getPropertyId(), orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));

        UtilityType utilityType = utilityTypeRepository.findByIdAndOrganizationIdAndIsDeletedFalse(request.getUtilityTypeId(), orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Utility type not found"));

        UtilityMeter meter = null;
        if (request.getMeterId() != null) {
            meter = utilityMeterRepository.findByIdAndOrganizationIdAndIsDeletedFalse(request.getMeterId(), orgId)
                    .orElseThrow(() -> new ResourceNotFoundException("Meter not found"));
        }

        // Check duplicate bill for same property+type+month
        var existing = utilityBillRepository.findByPropertyIdAndUtilityTypeIdAndBillingMonthAndIsDeletedFalse(
                property.getId(), utilityType.getId(), request.getBillingMonth());
        if (existing.isPresent()) {
            throw new RuntimeException("Utility bill already exists for property+type+month");
        }

        UtilityBill bill = UtilityBill.builder()
                .organization(org)
                .property(property)
                .utilityType(utilityType)
                .meter(meter)
                .billingMonth(request.getBillingMonth().withDayOfMonth(1))
                .totalAmount(request.getTotalAmount())
                .totalUnitsConsumed(request.getTotalUnitsConsumed())
                .dueDate(request.getDueDate())
                .providerName(request.getProviderName())
                .billDocumentS3Key(request.getBillDocumentS3Key())
                .status("PENDING")
                .build();

        bill = utilityBillRepository.save(bill);
        log.info("Created utility bill {} org {} amount {}", bill.getId(), orgId, bill.getTotalAmount());

        // Auto-split logic if meter is shared or master meter
        if (meter == null || Boolean.TRUE.equals(meter.getIsShared()) || meter.getUnit() == null) {
            // Master/building meter: split among occupied units in property
            try {
                autoSplitBill(bill);
            } catch (Exception e) {
                log.warn("Auto-split failed for bill {}: {}", bill.getId(), e.getMessage());
            }
        }

        return toBillResponse(bill);
    }

    @Transactional
    public UtilityBillResponse autoSplitBill(Long orgId, Long billId) {
        UtilityBill bill = utilityBillRepository.findByIdAndOrganizationIdAndIsDeletedFalse(billId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found"));
        return autoSplitBill(bill);
    }

    private UtilityBillResponse autoSplitBill(UtilityBill bill) {
        // Fetch occupied units in property
        var occupiedUnits = unitRepository.findByOrganizationIdAndStatusAndPropertyIdAndIsDeletedFalse(
                bill.getOrganization().getId(),
                com.skyheights.realestate.modules.portfolio.enums.UnitStatus.OCCUPIED,
                bill.getProperty().getId());

        if (occupiedUnits.isEmpty()) {
            log.warn("No occupied units in property {} to split bill {}", bill.getProperty().getId(), bill.getId());
            return toBillResponse(bill);
        }

        Map<String, Double> ratios = parseRatios(bill.getMeter());

        // Calculate splits
        BigDecimal totalRatio = BigDecimal.ZERO;
        Map<Long, BigDecimal> unitRatios = new HashMap<>();

        for (var unit : occupiedUnits) {
            BigDecimal ratio;
            if (ratios != null && !ratios.isEmpty()) {
                Double r = ratios.get(unit.getUnitNumber());
                ratio = r != null ? new BigDecimal(r.toString()) : BigDecimal.ZERO;
            } else {
                // Equal split
                ratio = BigDecimal.ONE.divide(new BigDecimal(occupiedUnits.size()), 6, RoundingMode.HALF_UP);
            }
            unitRatios.put(unit.getId(), ratio);
            totalRatio = totalRatio.add(ratio);
        }

        // Normalize ratios if RATIO type and sum !=1, or if equal split we already divided
        // For equal split, totalRatio should be 1 (since 1/n * n =1)
        // If sum !=1, normalize
        if (totalRatio.compareTo(BigDecimal.ZERO) > 0 && totalRatio.compareTo(BigDecimal.ONE) != 0) {
            // For RATIO case where ratios sum to 1 expected, but if not, normalize
            // To keep simple, if equal split we already have 1, otherwise normalize
            if (ratios == null || ratios.isEmpty()) {
                // equal already normalized
            } else {
                // Normalize
                for (var entry : unitRatios.entrySet()) {
                    BigDecimal normalized = entry.getValue().divide(totalRatio, 6, RoundingMode.HALF_UP);
                    unitRatios.put(entry.getKey(), normalized);
                }
                totalRatio = BigDecimal.ONE;
            }
        }

        // Create splits
        for (var unit : occupiedUnits) {
            BigDecimal ratio = unitRatios.get(unit.getId());
            if (ratio.compareTo(BigDecimal.ZERO) <= 0) continue;

            BigDecimal amountShare = bill.getTotalAmount().multiply(ratio).setScale(2, RoundingMode.HALF_UP);
            BigDecimal unitsAllocated = null;
            if (bill.getTotalUnitsConsumed() != null) {
                unitsAllocated = bill.getTotalUnitsConsumed().multiply(ratio).setScale(2, RoundingMode.HALF_UP);
            }

            // Find tenant for unit
            var tenantOpt = tenantRepository.findByUnitIdAndIsDeletedFalse(unit.getId()).stream()
                    .filter(t -> t.getStatus() == com.skyheights.realestate.modules.tenant.enums.TenantStatus.ACTIVE)
                    .findFirst();

            Long tenantId = tenantOpt.map(TenantProfile::getId).orElse(null);
            if (tenantId == null) {
                log.warn("No active tenant for unit {} during bill split", unit.getUnitNumber());
                continue;
            }

            UtilityBillSplit split = UtilityBillSplit.builder()
                    .utilityBill(bill)
                    .tenantId(tenantId)
                    .unit(unit)
                    .shareRatio(ratio)
                    .unitsAllocated(unitsAllocated)
                    .amountShare(amountShare)
                    .calculationNotes("Auto-split ratio " + ratio + " from " + (ratios != null ? "RATIO config" : "EQUAL"))
                    .build();

            utilityBillSplitRepository.save(split);
        }

        bill.setStatus("SPLIT");
        bill = utilityBillRepository.save(bill);

        log.info("Auto-split bill {} into {} splits org {}", bill.getId(), occupiedUnits.size(), bill.getOrganization().getId());
        return toBillResponse(bill);
    }

    private Map<String, Double> parseRatios(UtilityMeter meter) {
        if (meter == null || meter.getRatioConfig() == null) return null;
        try {
            Map<String, Object> config = objectMapper.readValue(meter.getRatioConfig(), new TypeReference<Map<String, Object>>() {});
            String type = (String) config.get("type");
            if (type == null || type.equalsIgnoreCase("EQUAL")) {
                return null; // equal
            }
            if (type.equalsIgnoreCase("RATIO") && config.containsKey("ratios")) {
                Map<String, Double> ratios = (Map<String, Double>) config.get("ratios");
                return ratios;
            }
            // SUBMETER: handled via separate readings, not here
            return null;
        } catch (Exception e) {
            log.warn("Failed to parse ratio_config for meter {}: {}", meter.getId(), e.getMessage());
            return null;
        }
    }

    @Transactional(readOnly = true)
    public Page<UtilityBillResponse> searchBills(Long orgId, Long propertyId, Long utilityTypeId, String status, Pageable pageable) {
        return utilityBillRepository.search(orgId, propertyId, utilityTypeId, status, pageable).map(this::toBillResponse);
    }

    @Transactional(readOnly = true)
    public UtilityBillResponse getBill(Long orgId, Long id) {
        UtilityBill bill = utilityBillRepository.findByIdAndOrganizationIdAndIsDeletedFalse(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found"));
        return toBillResponse(bill);
    }

    // ===== Mappers =====
    private UtilityTypeResponse toTypeResponse(UtilityType t) {
        return UtilityTypeResponse.builder()
                .id(t.getId()).uuid(t.getUuid()).orgId(t.getOrganization() != null ? t.getOrganization().getId() : null)
                .name(t.getName()).unitLabel(t.getUnitLabel()).defaultRate(t.getDefaultRate()).createdAt(t.getCreatedAt())
                .build();
    }

    private UtilityMeterResponse toMeterResponse(UtilityMeter m) {
        return UtilityMeterResponse.builder()
                .id(m.getId()).uuid(m.getUuid()).orgId(m.getOrganization() != null ? m.getOrganization().getId() : null)
                .propertyId(m.getProperty() != null ? m.getProperty().getId() : null)
                .propertyName(m.getProperty() != null ? m.getProperty().getName() : null)
                .unitId(m.getUnit() != null ? m.getUnit().getId() : null)
                .unitNumber(m.getUnit() != null ? m.getUnit().getUnitNumber() : null)
                .utilityTypeId(m.getUtilityType() != null ? m.getUtilityType().getId() : null)
                .utilityTypeName(m.getUtilityType() != null ? m.getUtilityType().getName() : null)
                .meterNumber(m.getMeterNumber()).isShared(m.getIsShared()).location(m.getLocation())
                .totalUnitsSharing(m.getTotalUnitsSharing()).ratioConfig(m.getRatioConfig())
                .status(m.getStatus()).createdAt(m.getCreatedAt())
                .build();
    }

    private UtilityReadingResponse toReadingResponse(UtilityReading r) {
        String presigned = null;
        try {
            if (r.getPhotoS3Key() != null) presigned = s3Service.generatePresignedUrl(r.getPhotoS3Key(), Duration.ofMinutes(15));
        } catch (Exception ignored) {}

        // Calculate units consumed if generated column not available via JPA
        var units = r.getCurrentReading() != null && r.getPreviousReading() != null ?
                r.getCurrentReading().subtract(r.getPreviousReading()) : null;

        return UtilityReadingResponse.builder()
                .id(r.getId()).uuid(r.getUuid()).meterId(r.getMeter() != null ? r.getMeter().getId() : null)
                .meterNumber(r.getMeter() != null ? r.getMeter().getMeterNumber() : null)
                .readingDate(r.getReadingDate()).previousReading(r.getPreviousReading())
                .currentReading(r.getCurrentReading()).unitsConsumed(units)
                .ratePerUnit(r.getRatePerUnit()).amount(r.getAmount())
                .recordedByUserId(r.getRecordedBy() != null ? r.getRecordedBy().getId() : null)
                .recordedByName(r.getRecordedBy() != null ? r.getRecordedBy().getFullName() : null)
                .photoS3Key(r.getPhotoS3Key()).photoPresignedUrl(presigned)
                .source(r.getSource()).createdAt(r.getCreatedAt())
                .build();
    }

    private UtilityBillResponse toBillResponse(UtilityBill b) {
        List<UtilityBillResponse.SplitResponse> splits = utilityBillSplitRepository.findByUtilityBillId(b.getId()).stream()
                .map(s -> UtilityBillResponse.SplitResponse.builder()
                        .id(s.getId()).uuid(s.getUuid())
                        .tenantId(s.getTenantId()).tenantName(null) // could lookup
                        .unitId(s.getUnit() != null ? s.getUnit().getId() : null)
                        .unitNumber(s.getUnit() != null ? s.getUnit().getUnitNumber() : null)
                        .shareRatio(s.getShareRatio()).unitsAllocated(s.getUnitsAllocated())
                        .amountShare(s.getAmountShare())
                        .invoiceId(s.getInvoice() != null ? s.getInvoice().getId() : null)
                        .invoiceNumber(s.getInvoice() != null ? s.getInvoice().getInvoiceNumber() : null)
                        .calculationNotes(s.getCalculationNotes())
                        .build())
                .collect(Collectors.toList());

        BigDecimal totalSplit = splits.stream().map(s -> s.getAmountShare()).reduce(BigDecimal.ZERO, BigDecimal::add);

        String presigned = null;
        try {
            if (b.getBillDocumentS3Key() != null) presigned = s3Service.generatePresignedUrl(b.getBillDocumentS3Key(), Duration.ofMinutes(30));
        } catch (Exception ignored) {}

        return UtilityBillResponse.builder()
                .id(b.getId()).uuid(b.getUuid()).orgId(b.getOrganization() != null ? b.getOrganization().getId() : null)
                .propertyId(b.getProperty() != null ? b.getProperty().getId() : null)
                .propertyName(b.getProperty() != null ? b.getProperty().getName() : null)
                .utilityTypeId(b.getUtilityType() != null ? b.getUtilityType().getId() : null)
                .utilityTypeName(b.getUtilityType() != null ? b.getUtilityType().getName() : null)
                .meterId(b.getMeter() != null ? b.getMeter().getId() : null)
                .meterNumber(b.getMeter() != null ? b.getMeter().getMeterNumber() : null)
                .billingMonth(b.getBillingMonth()).totalAmount(b.getTotalAmount())
                .totalUnitsConsumed(b.getTotalUnitsConsumed()).dueDate(b.getDueDate())
                .providerName(b.getProviderName()).billDocumentS3Key(b.getBillDocumentS3Key())
                .billPresignedUrl(presigned).status(b.getStatus()).createdAt(b.getCreatedAt())
                .splits(splits).splitsCount(splits.size()).totalSplitAmount(totalSplit)
                .build();
    }
}
