package com.example.auction_management.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "invoices")
public class Invoices {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "invoice_id", columnDefinition = "INT")
    private Integer invoiceId;

    @NotNull(message = "Đơn hàng không được để trống")
    @OneToOne
    @JoinColumn(name = "order_id", columnDefinition = "INT UNIQUE")
    private Order order;

    @CreationTimestamp
    @Column(name = "issue_date", columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime issueDate;

    @DecimalMin(value = "0.01", message = "Tổng tiền phải lớn hơn 0")
    @Column(name = "total_amount", columnDefinition = "DECIMAL(10,2) NOT NULL")
    private BigDecimal totalAmount;
}

