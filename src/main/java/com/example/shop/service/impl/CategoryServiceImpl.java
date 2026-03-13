package com.example.shop.service.impl;

import com.example.shop.entity.Category;
import com.example.shop.entity.Image;
import com.example.shop.exception.APIException;
import com.example.shop.payloads.CategoryDTO;
import com.example.shop.payloads.reponse.CategoryResponse;
import com.example.shop.repository.CategoryRepo;
import com.example.shop.repository.ImageRepo;
import com.example.shop.service.CategoryService;
import com.example.shop.hellper.TextNormalizer;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class CategoryServiceImpl implements CategoryService {
    @Autowired
    private CategoryRepo categoryRepo;
    @Autowired
    private ImageRepo imageRepo;
    @Autowired
    private FileStorageService fileStorageService;
    @Autowired
    private ModelMapper modelMapper;

    @Override
    public CategoryDTO createCategory(CategoryDTO categoryDTO, List<MultipartFile> files) {
        Category category = new Category();
        String normalized = TextNormalizer.normalizeName(categoryDTO.getName());
        if (categoryRepo.existsByName(normalized)) {
            throw new APIException("Category name already exists");
        }        category.setName(TextNormalizer.normalizeName(categoryDTO.getName()));
        category.setDescription(TextNormalizer.normalizeDescriptionAndNotification(categoryDTO.getDescription()));
        category = categoryRepo.save(category);
        // Save image
        if (files != null && !files.isEmpty()) {
            List<Image> images = new ArrayList<>();
            for (MultipartFile file : files) {
                String url = fileStorageService.save(file);
                Image image = new Image();
                image.setImageUrl(url);
                image.setAltText(category.getName());
                image.setCategory(category);
                images.add(image);
            }
            imageRepo.saveAll(images);
            category.setImages(images); // if category have List<image>
        }
        return modelMapper.map(category, CategoryDTO.class);
    }

    @Override
    public CategoryDTO getCategoryById(Long categoryId) {
        Category category = categoryRepo.findById(categoryId).orElseThrow(() -> new APIException("Not found with category!"));
        return modelMapper.map(category, CategoryDTO.class);
    }

    @Override
    public String deleteCategory(Long categoryId) {
        Category category = categoryRepo.findById(categoryId).orElseThrow(() -> new APIException("category not exception!"));
        categoryRepo.deleteById(categoryId);
        return "Category deleted success!";
    }

    @Override
    public CategoryResponse getAllCategory(Integer pageNumber, Integer pageSize, String sortBy, String sortOder) {
        Sort sortByAndSortOrder = sortOder.equalsIgnoreCase("asc") ?
                Sort.by(sortBy).ascending() : Sort.by(sortBy ).descending();
        Pageable pageDetail = PageRequest.of(pageNumber, pageSize, sortByAndSortOrder);
        Page<Category> pageCategory = categoryRepo.findAll(pageDetail);
        List<Category> categories = pageCategory.getContent();
        if (pageCategory.isEmpty()) {
            throw new APIException("Not found menu!");
        }
        List<CategoryDTO> categoryDTOS = categories.stream().map(c -> {
            CategoryDTO categoryDTO = modelMapper.map(c, CategoryDTO.class);
            return categoryDTO;
        }).toList();
        CategoryResponse categoryResponse = new CategoryResponse();
        categoryResponse.setContent(categoryDTOS);
        categoryResponse.setPageNumber(pageCategory.getNumber());
        categoryResponse.setPageSize(pageCategory.getSize());
        categoryResponse.setTotalElement(pageCategory.getTotalElements());
        categoryResponse.setTotalElement(pageCategory.getTotalElements());
        return categoryResponse;
    }
}
