package com.example.auction_management.model;

import com.fasterxml.jackson.annotation.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "products")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "productId")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id", columnDefinition = "INT")
    private Integer productId;

    @NotBlank(message = "Tên sản phẩm không được để trống")
    @Column(name = "name", columnDefinition = "VARCHAR(255) NOT NULL")
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", columnDefinition = "INT")
    private Category category;

    @Lob
    @Column(name = "description", columnDefinition = "TEXT")
    @Size(max = 300, message = "Mô tả không được vượt quá 300 ký tự")
    private String description;

    @DecimalMin(value = "0.00", inclusive = false, message = "Giá khởi điểm phải lớn hơn 0")
    @Column(name = "base_price", columnDefinition = "DECIMAL(10,2) NOT NULL")
    private BigDecimal basePrice;

    @Column(name = "image", columnDefinition = "LONGTEXT")
    private String image;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<Image> images = new ArrayList<>();

    @Column(name = "is_deleted", columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean isDeleted;

    // Quan hệ với Account (người đăng tin)
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "account_id", columnDefinition = "INT")
    private Account account;

    // Nếu ProductService đang gọi getIsDeleted() thì đảm bảo có getter này:
    public Boolean getIsDeleted() {
        return isDeleted;
    }
}
