package com.example.shop.repository;

import com.example.shop.entity.Cart;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface CartRepo extends JpaRepository<Cart, Long> {
    Optional<Cart> findByRestaurantTable_tableId(Long tableId);

    @EntityGraph(attributePaths = {
            "cartItems",
            "cartItems.menuItem",
            "restaurantTable"
    })



    @Query("""
    select c
    from Cart c
    where c.status <> 'CLOSED'
      and c.isHidden = false
    order by c.updatedAt desc
""")
    Page<Cart> findCashierCarts(Pageable pageable);




List<Cart>findAllByIdIn(List<Long>ids);


}
