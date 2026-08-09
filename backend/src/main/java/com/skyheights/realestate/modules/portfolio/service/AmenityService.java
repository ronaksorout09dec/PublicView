package com.skyheights.realestate.modules.portfolio.service;

import com.skyheights.realestate.exception.ResourceNotFoundException;
import com.skyheights.realestate.modules.organization.entity.Organization;
import com.skyheights.realestate.modules.organization.repository.OrganizationRepository;
import com.skyheights.realestate.modules.portfolio.dto.AmenityCreateRequest;
import com.skyheights.realestate.modules.portfolio.dto.AmenityResponse;
import com.skyheights.realestate.modules.portfolio.entity.Amenity;
import com.skyheights.realestate.modules.portfolio.enums.AmenityCategory;
import com.skyheights.realestate.modules.portfolio.repository.AmenityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AmenityService {

    private final AmenityRepository amenityRepository;
    private final OrganizationRepository organizationRepository;

    @Transactional
    public AmenityResponse createAmenity(Long orgId, AmenityCreateRequest request) {
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

        if (amenityRepository.existsByOrganizationIdAndNameAndIsDeletedFalse(orgId, request.getName())) {
            throw new RuntimeException("Amenity already exists: " + request.getName());
        }

        Amenity amenity = Amenity.builder()
                .organization(org)
                .name(request.getName())
                .category(request.getCategory() != null ? request.getCategory() : AmenityCategory.COMMON)
                .icon(request.getIcon())
                .description(request.getDescription())
                .build();

        amenity = amenityRepository.save(amenity);
        log.info("Created amenity {} for org {}", amenity.getName(), orgId);
        return toResponse(amenity);
    }

    @Transactional(readOnly = true)
    public List<AmenityResponse> getAmenities(Long orgId) {
        return amenityRepository.findByOrganizationIdAndIsDeletedFalse(orgId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AmenityResponse getAmenity(Long orgId, Long id) {
        Amenity amenity = amenityRepository.findByIdAndOrganizationIdAndIsDeletedFalse(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Amenity not found"));
        return toResponse(amenity);
    }

    @Transactional
    public AmenityResponse updateAmenity(Long orgId, Long id, AmenityCreateRequest request) {
        Amenity amenity = amenityRepository.findByIdAndOrganizationIdAndIsDeletedFalse(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Amenity not found"));

        if (!amenity.getName().equalsIgnoreCase(request.getName()) &&
                amenityRepository.existsByOrganizationIdAndNameAndIdNotAndIsDeletedFalse(orgId, request.getName(), id)) {
            throw new RuntimeException("Amenity name already exists: " + request.getName());
        }

        amenity.setName(request.getName());
        amenity.setCategory(request.getCategory());
        amenity.setIcon(request.getIcon());
        amenity.setDescription(request.getDescription());

        amenity = amenityRepository.save(amenity);
        return toResponse(amenity);
    }

    @Transactional
    public void deleteAmenity(Long orgId, Long id) {
        Amenity amenity = amenityRepository.findByIdAndOrganizationIdAndIsDeletedFalse(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Amenity not found"));
        amenity.setIsDeleted(true);
        amenityRepository.save(amenity);
        log.info("Soft deleted amenity {} org {}", id, orgId);
    }

    private AmenityResponse toResponse(Amenity a) {
        return AmenityResponse.builder()
                .id(a.getId())
                .uuid(a.getUuid())
                .name(a.getName())
                .category(a.getCategory() != null ? a.getCategory().name() : null)
                .icon(a.getIcon())
                .description(a.getDescription())
                .build();
    }
}
