package com.example.shop.service.impl;

import com.example.shop.entity.TableType;
import com.example.shop.exception.APIException;
import com.example.shop.payloads.TableTypeDTO;
import com.example.shop.repository.TableTypeRepo;
import com.example.shop.service.TypeTableService;
import com.example.shop.hellper.TextNormalizer;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TableTypeServiceImplService implements TypeTableService {
    @Autowired
    TableTypeRepo tableTypeRepo;
    @Autowired
    ModelMapper modelMapper;
    @Override
    public TableTypeDTO createTable(TableTypeDTO tableTypeDTO){
        if(tableTypeRepo.existsByNameIgnoreCase(tableTypeDTO.getName())){
            throw  new APIException("The table type name already exists!!");
        }
        TableType tableType=new TableType();
        tableType.setName(TextNormalizer.normalizeName(tableTypeDTO.getName()));
        tableType.setDescription(TextNormalizer.normalizeDescriptionAndNotification(tableTypeDTO.getDescription()));
        tableType.setSeatCount(tableTypeDTO.getSeatCount());
        tableType.setExtraFee(tableTypeDTO.getExtraFee());
        tableTypeRepo.save(tableType);
        return modelMapper.map(tableType,TableTypeDTO.class);
    }

    @Override
    public TableTypeDTO updateTableType(TableTypeDTO tableTypeDTO, Long id){
        TableType tableTypeDB=tableTypeRepo.findById(id).orElseThrow(()->
        new APIException("Table type not found!"));
        tableTypeDB.setDescription(TextNormalizer.normalizeDescriptionAndNotification(tableTypeDTO.getDescription()));
        tableTypeDB.setName(TextNormalizer.normalizeName(tableTypeDTO.getName()));
        tableTypeDB.setSeatCount(tableTypeDTO.getSeatCount());
        tableTypeDB.setExtraFee(tableTypeDTO.getExtraFee());
        tableTypeRepo.save(tableTypeDB);
        return modelMapper.map(tableTypeDB,TableTypeDTO.class);
    }
    @Override
    public String deleted(Long id){
        TableType tableType=tableTypeRepo.findById(id).orElseThrow(()->
                new APIException("Not found table type"));
        tableTypeRepo.deleteById(id);
        return "Delete "+tableType.getName()+"success!!";
    }
}
