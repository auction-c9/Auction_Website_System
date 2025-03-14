package com.example.auction_management.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity(name = "image")
public class Image {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", columnDefinition = "INT")
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "product_id", referencedColumnName = "product_id", nullable = true)
    @JsonBackReference
    private Product product;

    @NotBlank(message = "Image URL must not be empty")
    @Column(name = "image_url", columnDefinition = "TEXT", nullable = false)
    private String imageUrl;

    @OneToOne(mappedBy = "avatar")
    @JsonBackReference
    private Customer customer;
}
