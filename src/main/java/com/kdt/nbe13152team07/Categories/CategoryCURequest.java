package com.kdt.nbe13152team07.Categories;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CategoryCURequest(
        @NotBlank @NotNull @Size(max=10)String code,
        @NotBlank @NotNull @Size(max=30) @Pattern(regexp = "^[A-Za-z]+$", message = "코드는 영문만 입력할 수 있습니다.")String name,
        @NotNull boolean excludable

) {
    public CategoryCURequest {
        code = (code != null) ? code.trim() : null;
        name = (name != null) ? name.trim() : null;
    }
}
