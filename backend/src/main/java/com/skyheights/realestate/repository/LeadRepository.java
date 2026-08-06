package com.skyheights.realestate.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.skyheights.realestate.entity.Lead;

import java.util.List;
import java.util.Optional;

@Repository
public interface LeadRepository extends JpaRepository<Lead, Long> {
    Optional<Lead> findByPhone(String phone);
    List<Lead> findByCustomerNameContainingIgnoreCase(String name);
    List<Lead> findByLocationContainingIgnoreCase(String location);
    List<Lead> findByPropertyType(String propertyType);
}
