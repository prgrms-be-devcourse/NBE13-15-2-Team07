package com.kdt.nbe13152team07.payment.service;

import com.kdt.nbe13152team07.Categories.Categories;
import com.kdt.nbe13152team07.Categories.CategoriesRepository;
import com.kdt.nbe13152team07.global.error.BusinessException;
import com.kdt.nbe13152team07.global.error.ErrorCode;
import com.kdt.nbe13152team07.payment.entity.MerchantCategory;
import org.springframework.stereotype.Component;

@Component
public class CategoryClassifier {

    private final CategoriesRepository categoriesRepository;

    public CategoryClassifier(CategoriesRepository categoriesRepository) {
        this.categoriesRepository = categoriesRepository;
    }


    public Categories classify(String mcc) {
        //mcc -> 카테고리 코드
        String categoryCode = MerchantCategory.findCategoryCode(mcc);

        //카테고리 코드 -> Categories 엔티티
        Categories category = categoriesRepository.findByCode(categoryCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
        return category;
    }
}