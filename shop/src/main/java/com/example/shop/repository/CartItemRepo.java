package com.example.shop.repository;

import com.example.shop.entity.Cart;
import com.example.shop.entity.CartItem;
import com.example.shop.entity.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepo extends JpaRepository<CartItem,Long> {
    @Query("""
            SELECT ci FROM CartItem ci
            WHERE ci.cart.id = :cartId AND ci.menuItem.id = :menuItemId
            """)
    Optional<CartItem> findByCartIdAndMenuItemId(
            @Param("cartId") Long cartId,
            @Param("menuItemId") Long menuItemId
    );
    List<CartItem> findByCartId(Long cartId);


}
