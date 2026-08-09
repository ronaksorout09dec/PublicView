package com.skyheights.realestate.modules.financial.repository;

import com.skyheights.realestate.modules.financial.entity.SecurityDepositLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SecurityDepositLedgerRepository extends JpaRepository<SecurityDepositLedger, Long> {

    List<SecurityDepositLedger> findByDepositIdOrderByCreatedAtDesc(Long depositId);

    List<SecurityDepositLedger> findByDepositIdAndIsDeletedFalse(Long depositId);
}
