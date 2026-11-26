package com.kdt.nbe13152team07.Categories;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name="category")
@Getter
@Setter
@NoArgsConstructor(access= AccessLevel.PROTECTED)
public class Categories {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(name="code",nullable=false, unique = true)
    private String code;

    @Column(name="name",nullable=false)
    private String name;

    @Column(name="excludable",nullable=false)
    private Boolean excludable;
    public Categories(String code,String name,boolean excludable){
        this.code=code;
        this.name=name;
        this.excludable=excludable;
    }
}
