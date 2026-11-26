package com.kdt.nbe13152team07.room;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class InviteCodeGeneratorTest {
    @Test
    void codeIsEightUppercaseLettersOrDigits() {
        InviteCodeGenerator generator = new InviteCodeGenerator();

        for (int i = 0; i < 100; i++) {
            String code = generator.generate();
            assertTrue(code.matches("[A-Z0-9]{8}"));
        }
    }

    @Test
    void codesAreNotAllTheSame() {
        InviteCodeGenerator generator = new InviteCodeGenerator();
        Set<String> codes = new HashSet<>();

        for (int i = 0; i < 100; i++) {
            codes.add(generator.generate());
        }

        assertTrue(codes.size() > 1);
    }
}
