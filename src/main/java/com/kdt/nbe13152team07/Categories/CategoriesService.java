package com.kdt.nbe13152team07.Categories;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class CategoriesService {
    private final CategoriesRepository categoriesRepository;

    @Autowired
    public CategoriesService(CategoriesRepository categoriesRepository){
    this.categoriesRepository=categoriesRepository;
}
    @CacheEvict(cacheNames="categoryList",allEntries = true)
    @Transactional
    public CategoryResponse Create(CategoryCURequest request){
        Categories category=new Categories(request.code(),request.name(),request.excludable());
        categoriesRepository.save(category);
        return toResponse(category);
    }

    @Transactional
    @CacheEvict(cacheNames="categoryList",allEntries = true)
    public CategoryResponse  update(Long categoryId,CategoryCURequest request) {
        Categories category = categoriesRepository.findById(categoryId).orElseThrow(() -> new CATEGORY_NOT_FOUND());
        category.setName(request.name());
        category.setCode(request.code());
        category.setExcludable(request.excludable());
        categoriesRepository.save(category);
        return toResponse(category);
    }

    @Transactional(readOnly = true)
    public CategoryResponse findOne(Long Id) {
        Categories categories = categoriesRepository.findById(Id).orElseThrow(() -> new CATEGORY_NOT_FOUND());
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

    @Cacheable(cacheNames = "categoryList")
    @Transactional(readOnly = true)
    public CategoryListResponse findAll() {
        List<CategoryResponse> list = new ArrayList<>();
        for (Categories categories : categoriesRepository.findAllByOrderByIdAsc()) {
            list.add(toResponse(categories));
        }
        return new CategoryListResponse(list);
    }


    @CacheEvict(cacheNames="categoryList",allEntries = true)
    @Transactional
    public void delete(Long categoryId) {
       Categories category = categoriesRepository.findById(categoryId).orElseThrow(() -> new CATEGORY_NOT_FOUND());
        /*
        room_excluded_category구현되면 구현될 예정(여기에 category가 포함되어있으면 오류 생성)
         */
        categoriesRepository.delete(category);
    }

    private CategoryResponse toResponse(Categories category){
        return new CategoryResponse(category.getId(),category.getCode(),category.getName(),category.getExcludable());
    }


}
