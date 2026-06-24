package com.example.shop.service.impl;

import com.example.shop.entity.Category;
import com.example.shop.entity.Image;
import com.example.shop.entity.MenuItem;
import com.example.shop.exception.APIException;
import com.example.shop.payloads.MenuItemDTO;
import com.example.shop.payloads.MenuItemRequestDTO;
import com.example.shop.payloads.reponse.MenuItemResponse;
import com.example.shop.repository.CategoryRepo;
import com.example.shop.repository.ImageRepo;
import com.example.shop.repository.MenuItemRepo;
import com.example.shop.service.MenuItemService;
import com.example.shop.hellper.TextNormalizer;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class MenuItemServiceImpl implements MenuItemService {
    @Autowired
    MenuItemRepo menuItemRepo;
    @Autowired
    CategoryRepo categoryRepo;
    @Autowired
    ModelMapper modelMapper;
    @Autowired
    FileStorageService fileStorageService;
    @Autowired
    ImageRepo imageRepo;
    @Autowired
    FileSystemStorageService fileSystemStorageService;
    @Override
    public MenuItemResponse getAllMenu(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder, String keyword,
                                       Long categoryId, BigDecimal minPrice, BigDecimal maxPrice) {
        Sort sortByAndOrderBy = sortOrder.equalsIgnoreCase("asc") ?
                Sort.by(sortBy).ascending() : Sort.by(sortBy);
        Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndOrderBy);
        Page<MenuItem> pageMenu = menuItemRepo.filter(keyword, categoryId, minPrice, maxPrice, pageDetails);
        if (pageMenu.isEmpty()) {
            throw new APIException("Not found menu!");
        }
        //convert to DTO
        List<MenuItemDTO> ListToDo = pageMenu.getContent().stream().map(m -> modelMapper.map(m, MenuItemDTO.class)).toList();
        MenuItemResponse menuItemResponse = new MenuItemResponse();
        menuItemResponse.setContent(ListToDo);
        menuItemResponse.setPageNumber(pageMenu.getNumber());
        menuItemResponse.setPageSize(pageMenu.getSize());
        menuItemResponse.setTotalElement(pageMenu.getTotalElements());
        menuItemResponse.setTotalPage(pageMenu.getTotalPages());
        return menuItemResponse;
    }

    @Override
    public MenuItemDTO createMenuItem(MenuItemRequestDTO dto, List<MultipartFile> files) {
        Category category = categoryRepo.findById(dto.getCategoryId()).orElseThrow(
                () -> new APIException("Category not found!"));
        if (menuItemRepo.existsByNameIgnoreCase(dto.getName())) {
            throw new APIException("Menu item name already exist!!");
        }
        MenuItem item = new MenuItem();
        item.setName(TextNormalizer.normalizeName(dto.getName()));
        item.setDescription(TextNormalizer.normalizeDescriptionAndNotification(dto.getDescription()));
        item.setPrice(dto.getPrice());
        item.setCategory(category);
        menuItemRepo.save(item);
        if (files != null && !files.isEmpty()) {
            List<Image> images = new ArrayList<>();
            for (MultipartFile file : files) {
                String url = fileSystemStorageService.save(file);
                Image image = new Image();
                image.setImageUrl(url);
                image.setAltText(item.getName());
                image.setMenuItem(item);
                images.add(image);
            }
            imageRepo.saveAll(images);
            item.setImages(images);
        }
        return modelMapper.map(item, MenuItemDTO.class);
    }

    @Override
    public String deleteMenu(Long id) {
        MenuItem menuItemDB = menuItemRepo.findById(id).orElseThrow(() -> new APIException("Not found menu item!"));
        menuItemRepo.deleteById(id);
        return "Delete Success!";
    }

    @Override
    public MenuItemDTO updateMenu(Long id, MenuItemDTO dto, List<MultipartFile> files) {
        MenuItem menuItem = menuItemRepo.findById(id)
                .orElseThrow(() -> new APIException("Not found menu item!"));
        Category category = categoryRepo.findById(dto.getCategoryId())
                .orElseThrow(() -> new APIException("Category not found!"));
        menuItem.setCategory(category);
        menuItem.setPrice(dto.getPrice());
        menuItem.setName(dto.getName());
        menuItem.setDescription(dto.getDescription());
        // Upload new files
        if (files != null && !files.isEmpty()) {
            List<Image> newImages = new ArrayList<>();
            for (MultipartFile file : files) {
                String url = fileSystemStorageService.save(file);
                Image img = new Image();
                img.setMenuItem(menuItem);
                img.setImageUrl(url);
                img.setAltText(dto.getName());
                newImages.add(img);
            }
            imageRepo.saveAll(newImages);
            menuItem.setImages(newImages);
        }
        MenuItem saved = menuItemRepo.save(menuItem);
        return modelMapper.map(saved, MenuItemDTO.class);
    }

    @Override
    public MenuItemDTO getOneMenu(Long id) {
        Optional<MenuItem> menuItem = menuItemRepo.findById(id);
        if (menuItem.isPresent()) {
            MenuItem menuItem1 = menuItem.get();
            return modelMapper.map(menuItem1, MenuItemDTO.class);
        } else {
            throw new APIException("Menu item not exception!");
        }
    }

}
