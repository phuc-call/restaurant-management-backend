package com.example.shop.service;

import com.example.shop.entity.Address;
import com.example.shop.payloads.AddressDTO;
import com.example.shop.repository.AddressRepo;

import java.util.List;

public interface AddressService {
    AddressDTO getAddressById(Long addressId);
    List<AddressDTO> getAllAddress();
    AddressDTO createAddress(AddressDTO addressDTO);
    AddressDTO updateAddress(Long addressId, Address address);
    void deleteAddress(Long addressId);
}
