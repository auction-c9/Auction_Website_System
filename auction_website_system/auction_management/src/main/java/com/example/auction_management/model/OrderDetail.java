package com.example.auction_management.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity(name = "order_detail")
public class OrderDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_detail_id", columnDefinition = "INT")
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "order_id", referencedColumnName = "order_id", nullable = false)
    @NotNull(message = "Đơn hàng không được để trống")
    private Order order;

    @ManyToOne
    @JoinColumn(name = "product_id", referencedColumnName = "product_id", nullable = false)
    @NotNull(message = "Sản phẩm không được để trống")
    private Product product;

    @Min(value = 1, message = "Số lượng phải ít nhất là 1")
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Min(value = 0, message = "Giá sản phẩm không thể âm")
    @Column(name = "price", precision = 10, scale = 2, nullable = false)
    private BigDecimal price;
}
