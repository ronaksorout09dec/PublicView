package com.skyheights.realestate.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skyheights.realestate.dto.LeadRequest;
import com.skyheights.realestate.dto.LeadResponse;
import com.skyheights.realestate.entity.Lead;
import com.skyheights.realestate.exception.ResourceNotFoundException;
import com.skyheights.realestate.mapper.LeadMapper;
import com.skyheights.realestate.repository.LeadRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeadService {

    private final LeadRepository leadRepository;
    private final LeadMapper leadMapper;

    @Transactional
    public LeadResponse createLead(LeadRequest request) {
        log.info("Creating lead for customer: {}, phone: {}", request.getCustomerName(), request.getPhone());
        Lead lead = leadMapper.toEntity(request);
        Lead saved = leadRepository.save(lead);
        log.info("Lead created with ID: {}", saved.getId());
        return leadMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<LeadResponse> getAllLeads() {
        log.info("Fetching all leads");
        List<Lead> leads = leadRepository.findAll();
        return leadMapper.toResponseList(leads);
    }

    @Transactional(readOnly = true)
    public LeadResponse getLeadById(Long id) {
        log.info("Fetching lead by ID: {}", id);
        Lead lead = leadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found with id: " + id));
        return leadMapper.toResponse(lead);
    }

    @Transactional
    public void deleteLead(Long id) {
        log.info("Deleting lead with ID: {}", id);
        Lead lead = leadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found with id: " + id));
        leadRepository.delete(lead);
        log.info("Lead deleted: {}", id);
    }

    @Transactional
    public LeadResponse createLeadFromEntity(Lead lead) {
        log.info("Saving lead entity: {}", lead.getCustomerName());
        Lead saved = leadRepository.save(lead);
        return leadMapper.toResponse(saved);
    }
}
