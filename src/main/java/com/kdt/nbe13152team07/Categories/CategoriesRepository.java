package com.kdt.nbe13152team07.Categories;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoriesRepository extends JpaRepository<Categories,Long> {
    List<Categories> findAllByOrderByIdAsc();
}
