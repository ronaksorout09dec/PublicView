package com.skyheights.realestate.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import com.skyheights.realestate.dto.LeadRequest;
import com.skyheights.realestate.dto.LeadResponse;
import com.skyheights.realestate.entity.Lead;

import java.util.List;

@Mapper(componentModel = "spring")
public interface LeadMapper {

    Lead toEntity(LeadRequest request);

    LeadResponse toResponse(Lead lead);

    List<LeadResponse> toResponseList(List<Lead> leads);

    void updateEntityFromRequest(LeadRequest request, @MappingTarget Lead lead);
}
