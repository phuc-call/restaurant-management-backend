package com.example.shop.repository;

import com.example.shop.entity.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepo extends ShareRepo<User, Long> {
    @Query("""
       SELECT u FROM User u
       LEFT JOIN FETCH u.addresses
       WHERE u.email=:email
       """)
    Optional<User> findUserWithAddresses(String email);
    Optional<User> findByEmail(String email);

}
