package com.kdt.nbe13152team07.Categories;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoriesRepository extends JpaRepository<Categories,Long> {
    List<Categories> findAllByOrderByIdAsc();
    boolean existsById(Long id);
    //카테고리 코드로 카테고리 가져오는 코드 추가
    Optional<Categories> findByCode(String code);
}
