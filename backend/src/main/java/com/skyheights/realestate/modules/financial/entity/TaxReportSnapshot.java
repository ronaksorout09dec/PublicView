package com.skyheights.realestate.modules.financial.entity;

import com.skyheights.realestate.common.entity.BaseEntity;
import com.skyheights.realestate.modules.organization.entity.Organization;
import com.skyheights.realestate.modules.organization.entity.AppUser;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "tax_report_snapshots", uniqueConstraints = {
    @UniqueConstraint(name = "uk_org_fy", columnNames = {"org_id","financial_year"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TaxReportSnapshot extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    @Column(name = "financial_year", nullable = false, length = 20)
    private String financialYear;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "total_income", precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal totalIncome = BigDecimal.ZERO;

    @Column(name = "total_expense", precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal totalExpense = BigDecimal.ZERO;

    @Column(name = "total_tds", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalTds = BigDecimal.ZERO;

    @Column(name = "total_gst", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalGst = BigDecimal.ZERO;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "report_json", columnDefinition = "jsonb")
    private String reportJson;

    @Column(name = "report_pdf_s3_key", length = 500)
    private String reportPdfS3Key;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generated_by")
    private AppUser generatedBy;
}
