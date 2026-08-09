package com.skyheights.realestate.modules.portfolio.controller;

import com.skyheights.realestate.dto.ApiResponse;
import com.skyheights.realestate.modules.portfolio.dto.AmenityCreateRequest;
import com.skyheights.realestate.modules.portfolio.dto.AmenityResponse;
import com.skyheights.realestate.modules.portfolio.service.AmenityService;
import com.skyheights.realestate.security.CurrentUser;
import com.skyheights.realestate.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/amenities")
@RequiredArgsConstructor
public class AmenityController {

    private final AmenityService amenityService;

    @PostMapping
    @PreAuthorize("@permEval.hasPermission('AMENITY_MANAGE') or @permEval.hasHierarchy(50)")
    public ResponseEntity<ApiResponse<AmenityResponse>> createAmenity(@CurrentUser UserPrincipal currentUser,
                                                                      @Valid @RequestBody AmenityCreateRequest request) {
        AmenityResponse resp = amenityService.createAmenity(currentUser.getOrgId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(resp, "Amenity created"));
    }

    @GetMapping
    @PreAuthorize("@permEval.hasPermission('PROPERTY_READ')")
    public ResponseEntity<ApiResponse<List<AmenityResponse>>> getAmenities(@CurrentUser UserPrincipal currentUser) {
        List<AmenityResponse> list = amenityService.getAmenities(currentUser.getOrgId());
        return ResponseEntity.ok(ApiResponse.success(list, "Amenities fetched"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permEval.hasPermission('PROPERTY_READ')")
    public ResponseEntity<ApiResponse<AmenityResponse>> getAmenity(@CurrentUser UserPrincipal currentUser,
                                                                   @PathVariable Long id) {
        AmenityResponse resp = amenityService.getAmenity(currentUser.getOrgId(), id);
        return ResponseEntity.ok(ApiResponse.success(resp, "Amenity fetched"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@permEval.hasPermission('AMENITY_MANAGE')")
    public ResponseEntity<ApiResponse<AmenityResponse>> updateAmenity(@CurrentUser UserPrincipal currentUser,
                                                                      @PathVariable Long id,
                                                                      @Valid @RequestBody AmenityCreateRequest request) {
        AmenityResponse resp = amenityService.updateAmenity(currentUser.getOrgId(), id, request);
        return ResponseEntity.ok(ApiResponse.success(resp, "Amenity updated"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@permEval.hasPermission('AMENITY_MANAGE')")
    public ResponseEntity<ApiResponse<Void>> deleteAmenity(@CurrentUser UserPrincipal currentUser,
                                                           @PathVariable Long id) {
        amenityService.deleteAmenity(currentUser.getOrgId(), id);
        return ResponseEntity.ok(ApiResponse.success(null, "Amenity deleted"));
    }
}
