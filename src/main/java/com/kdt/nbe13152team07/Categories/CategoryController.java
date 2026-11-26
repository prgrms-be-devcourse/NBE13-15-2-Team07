package com.kdt.nbe13152team07.Categories;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {
    private final CategoryService categoryService;
    @Autowired
    public CategoryController(CategoryService service){
        this.categoryService=service;
    }
    //E6 카테고리 전부 조회
    @GetMapping
    public ResponseEntity<List<CategoryResponse>> findAll() {
        return ResponseEntity.ok(categoryService.findAll().content());
    }

    //[명세외] id로 카테고리 조회
    @GetMapping("/{categoryId}")
    public CategoryResponse getMenuById(@PathVariable("categoryId") Long id){
        return categoryService.findOne(id);
    }

    //[명세외] 카테고리 추가
    @PostMapping
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CategoryCURequest Request){
        CategoryResponse res=categoryService.create(Request);
        URI location=URI.create("/api/categories/"+res.id());
        return ResponseEntity.created(location).body(res);
    }
    //[명세외] 카테고리 변경
    @PutMapping("/{categoryId}")
    public CategoryResponse update(@PathVariable("categoryId") Long id,@Valid @RequestBody CategoryCURequest request){
        return this.categoryService.update(id,request);
    }
    //[명세외]카테고리 삭제
    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Void> delete(@PathVariable("categoryId") Long id){
        this.categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
    //[명세외] code로 카테고리 찾기
    @GetMapping("/code")
    public List<CategoryResponse> getByCode(@RequestParam String code){
        return this.categoryService.findByCode(code);
    }


}
