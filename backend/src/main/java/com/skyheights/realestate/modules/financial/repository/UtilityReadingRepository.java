package com.skyheights.realestate.modules.financial.repository;

import com.skyheights.realestate.modules.financial.entity.UtilityReading;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface UtilityReadingRepository extends JpaRepository<UtilityReading, Long> {

    List<UtilityReading> findByMeterIdOrderByReadingDateDesc(Long meterId);

    Optional<UtilityReading> findTopByMeterIdOrderByReadingDateDesc(Long meterId);

    Page<UtilityReading> findByMeterId(Long meterId, Pageable pageable);

    @Query("SELECT r FROM UtilityReading r WHERE r.meter.id = :meterId AND r.readingDate BETWEEN :start AND :end ORDER BY r.readingDate DESC")
    List<UtilityReading> findByMeterIdAndReadingDateBetween(@Param("meterId") Long meterId,
                                                             @Param("start") LocalDate start,
                                                             @Param("end") LocalDate end);
}
