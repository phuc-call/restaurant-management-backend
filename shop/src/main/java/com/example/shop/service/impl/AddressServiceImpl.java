package com.example.shop.service.impl;

import com.example.shop.entity.Address;
import com.example.shop.exception.APIException;
import com.example.shop.exception.ResourceNotFoundException;
import com.example.shop.payloads.AddressDTO;
import com.example.shop.repository.AddressRepo;
import com.example.shop.service.AddressService;
import com.example.shop.hellper.TextNormalizer;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class AddressServiceImpl implements AddressService {
    @Autowired
    private AddressRepo addressRepo;
    @Autowired
    private ModelMapper modelMapper;

    public AddressDTO getAddressById(Long addressId) {
        Optional<Address> getAddressById = addressRepo.findById(addressId);
        if (getAddressById.isPresent()) {
            Address address = getAddressById.get();
            return modelMapper.map(address, AddressDTO.class);
        } else {
            throw new ResourceNotFoundException("Address", "addressId", addressId);
        }
    }

    @Override
    public AddressDTO createAddress(AddressDTO addressDTO) {
        String city = TextNormalizer.normalizeName(addressDTO.getCity());
        String tress = addressDTO.getStress();
        String district = addressDTO.getDistrict();
        String country = TextNormalizer.normalizeName(addressDTO.getCountry());
        String note = TextNormalizer.normalizeDescriptionAndNotification(addressDTO.getNote());
        String ward = addressDTO.getWard();
        Address addressDB = addressRepo
                .findByCityAndCountryAndStreetAndDistrictAndNoteAndWard(
                        city, tress, district, country, note, ward
                );
        if (addressDB != null) {
            throw new APIException("Address exist!");
        }
        Address addressSave = modelMapper.map(addressDTO, Address.class);
        Address address = addressRepo.save(addressSave);
        return modelMapper.map(address, AddressDTO.class);

    }

    @Override
    public AddressDTO updateAddress(Long addressId, Address address) {
        Address addressDB = addressRepo.findById(addressId).orElseThrow(() ->
                new ResourceNotFoundException("Address", "addressId", addressId));
        address.setCity(TextNormalizer.normalizeName(addressDB.getCity()));
        address.setDistrict(TextNormalizer.normalizeDescriptionAndNotification(addressDB.getDistrict()));
        address.setNote(TextNormalizer.normalizeDescriptionAndNotification(addressDB.getNote()));
        address.setCountry(TextNormalizer.normalizeName(addressDB.getCountry()));
        address.setWard(TextNormalizer.normalizeDescriptionAndNotification(addressDB.getWard()));
        address.setStreet(addressDB.getStreet());
        Address saveAddress = addressRepo.save(address);
        return modelMapper.map(saveAddress, AddressDTO.class);
    }

    @Override
    public void deleteAddress(Long addressId) {
        Address addressDB = addressRepo.findById(addressId).orElseThrow(() ->
                new APIException("No address fount to delete, please try again!"));
        addressRepo.deleteById(addressId);
    }
    @Override
    public List<AddressDTO> getAllAddress(){
        Set<Address> addresses=new HashSet<>(addressRepo.findAll());
        return addresses.stream().map(address -> modelMapper.map(address,AddressDTO.class)).toList();
    }
}
