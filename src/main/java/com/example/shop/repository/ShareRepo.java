package com.example.shop.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.Optional;
import java.util.Set;

@NoRepositoryBean
public interface ShareRepo<T,ID> extends JpaRepository<T,ID> {
    @Query("SELECT u FROM #{#entityName} u JOIN FETCH u.addresses a WHERE a.addressId=?1")
    Set<T> findByAddressId(Long addressId);
    Optional<T>findByEmail(String email);
}
