package com.example.shop.repository;

import com.example.shop.entity.CartItem;

import com.example.shop.payloads.CartItemDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepo extends JpaRepository<CartItem, Long> {
    @Query("""
            SELECT ci FROM CartItem ci
            WHERE ci.cart.id = :cartId AND ci.menuItem.id = :menuItemId
            """)
    Optional<CartItem> findByCartIdAndMenuItemId(

            @Param("cartId") Long cartId,
            @Param("menuItemId") Long menuItemId
    );

    List<CartItem> findByCartId(Long cartId);

    @Query("""
            SELECT new com.example.shop.payloads.CartItemDTO(
            ci.id,
            ci.cart.id,
                                          ci.menuItem.id,
                                          ci.menuItem.name,
                                          ci.quantity,
                                          ci.unitPrice,
                                          ci.discount,
                                          (ci.quantity * ci.unitPrice - ci.discount)
                                          )FROM CartItem ci
                WHERE ci.cart.id = :cartId
            """)
    List<CartItemDTO> findCartItemDTO(@Param("cartId") Long cartId);
    @Modifying
    @Query("delete from CartItem ci where ci.cart.id = :cartId")
    void deleteByCartId(@Param("cartId") Long cartId);


}
