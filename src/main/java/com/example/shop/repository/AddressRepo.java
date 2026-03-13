package com.example.shop.repository;

import com.example.shop.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AddressRepo extends JpaRepository<Address, Long> {
    Address findByCityAndCountryAndStreetAndDistrictAndNoteAndWard(
            String city,
            String country,
            String street,
            String district,
            String note,
            String ward
    );
}
