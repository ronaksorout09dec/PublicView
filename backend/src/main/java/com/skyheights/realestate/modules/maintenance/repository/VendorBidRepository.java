package com.skyheights.realestate.modules.maintenance.repository;

import com.skyheights.realestate.modules.maintenance.entity.VendorBid;
import com.skyheights.realestate.modules.maintenance.enums.BidStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VendorBidRepository extends JpaRepository<VendorBid, Long> {

    Optional<VendorBid> findByIdAndOrganizationIdAndIsDeletedFalse(Long id, Long orgId);

    List<VendorBid> findByTicketIdAndIsDeletedFalse(Long ticketId);

    Page<VendorBid> findByTicketIdAndIsDeletedFalse(Long ticketId, Pageable pageable);

    List<VendorBid> findByVendorIdAndIsDeletedFalse(Long vendorId);

    List<VendorBid> findByTicketIdAndStatusAndIsDeletedFalse(Long ticketId, BidStatus status);

    boolean existsByTicketIdAndVendorIdAndIsDeletedFalse(Long ticketId, Long vendorId);

    long countByTicketIdAndIsDeletedFalse(Long ticketId);

    Optional<VendorBid> findByTicketIdAndVendorIdAndIsDeletedFalse(Long ticketId, Long vendorId);
}
