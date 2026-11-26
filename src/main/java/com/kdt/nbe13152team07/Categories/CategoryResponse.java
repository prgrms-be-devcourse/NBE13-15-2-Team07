package com.kdt.nbe13152team07.Categories;

import java.io.Serializable;

public record CategoryResponse(Long id, String code, String name, boolean excludable) implements Serializable {
}
