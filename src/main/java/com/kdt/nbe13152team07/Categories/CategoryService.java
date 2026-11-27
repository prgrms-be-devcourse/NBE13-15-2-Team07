package com.kdt.nbe13152team07.Categories;

import com.kdt.nbe13152team07.global.config.CacheNames;
import com.kdt.nbe13152team07.global.error.BusinessException;
import com.kdt.nbe13152team07.global.error.ErrorCode;
import com.kdt.nbe13152team07.room.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class CategoryService {
    private final CategoriesRepository categoriesRepository;
    private final RoomRepository roomRepository;
    @Autowired
    public CategoryService(CategoriesRepository categoriesRepository, RoomRepository roomRepository){
    this.categoriesRepository=categoriesRepository;
        this.roomRepository = roomRepository;
}
    @CacheEvict(cacheNames=CacheNames.CATEGORIES,allEntries = true)
    @Transactional
    public CategoryResponse create(CategoryCURequest request){
        Categories category=new Categories(request.code(),request.name(),request.excludable());
        categoriesRepository.save(category);
        return toResponse(category);
    }

    @Transactional
    @CacheEvict(cacheNames= CacheNames.CATEGORIES,allEntries = true)
    public CategoryResponse  update(Long categoryId,CategoryCURequest request) {
        Categories category = categoriesRepository.findById(categoryId).orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
        category.setName(request.name());
        category.setCode(request.code());
        category.setExcludable(request.excludable());
        categoriesRepository.save(category);
        return toResponse(category);
    }

    @Transactional(readOnly = true)
    public CategoryResponse findOne(Long Id) {
        Categories categories = categoriesRepository.findById(Id).orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
        return toResponse(categories);
    }

    @Transactional(readOnly=true)
    public List<CategoryResponse> findByCode(String code){
        List<CategoryResponse>list=new ArrayList<>();
        for(Categories category: categoriesRepository.findAll()){
            if(category.getCode().contains(code)){
                list.add(toResponse(category));
            }
        }
        return list;
    }

    @Cacheable(cacheNames = CacheNames.CATEGORIES)
    @Transactional(readOnly = true)
    public CategoryListResponse findAll() {
        List<CategoryResponse> list = new ArrayList<>();
        for (Categories categories : categoriesRepository.findAllByOrderByIdAsc()) {
            list.add(toResponse(categories));
        }
        return new CategoryListResponse(list);
    }


    @CacheEvict(cacheNames=CacheNames.CATEGORIES,allEntries = true)
    @Transactional
    public void delete(Long categoryId) {
       Categories category = categoriesRepository.findById(categoryId).orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
        if (roomRepository.existsByExcludedCategoryId(categoryId)) {
            throw new BusinessException(ErrorCode.CategoryInUseException);
        }
        categoriesRepository.delete(category);
    }

    private CategoryResponse toResponse(Categories category){
        return new CategoryResponse(category.getId(),category.getCode(),category.getName(),category.getExcludable());
    }


}
