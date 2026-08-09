package com.skyheights.realestate.modules.financial.repository;

import com.skyheights.realestate.modules.financial.entity.InvoiceLineItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoiceLineItemRepository extends JpaRepository<InvoiceLineItem, Long> {

    List<InvoiceLineItem> findByInvoiceId(Long invoiceId);

    void deleteByInvoiceId(Long invoiceId);
}
