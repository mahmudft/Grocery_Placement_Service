package com.example.store.repository;

import com.example.store.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, String> {

    @Modifying
    @Query("update Product p set p.stock = p.stock - :qty where p.sku = :sku and p.stock >= :qty")
    int decrementStock(@Param("sku") String sku, @Param("qty") int qty);
}
