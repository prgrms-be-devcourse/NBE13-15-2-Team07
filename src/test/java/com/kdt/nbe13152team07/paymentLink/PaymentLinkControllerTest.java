package com.kdt.nbe13152team07.paymentLink;

import com.kdt.nbe13152team07.global.security.LoginUser;
import com.kdt.nbe13152team07.user.entity.Role;
import com.kdt.nbe13152team07.user.entity.User;
import com.kdt.nbe13152team07.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PaymentLinkControllerTest {

    private static final String NUM = "123456789012345678901234";
    private static final String OTHER_NUM = "999999999999999999999999";

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;

    Long userId;
    Long otherUserId;

    @BeforeEach
    void setUp() {
        userId = userRepository.save(
                User.create("1001", "테스터1", null, LocalDateTime.now())).getId();
        otherUserId = userRepository.save(
                User.create("1002", "테스터2", null, LocalDateTime.now())).getId();
    }

    // 가짜 로그인: 이 요청을 보낸 사람은 id번 회원
    private RequestPostProcessor loginAs(Long id) {
        LoginUser loginUser = new LoginUser(id, Role.USER);
        return authentication(new UsernamePasswordAuthenticationToken(loginUser, null, List.of()));
    }

    private String body(String fintechUseNum) {
        return "{\"fintechUseNum\":\"" + fintechUseNum + "\"}";
    }

    private void link(Long id, String num) throws Exception {
        mockMvc.perform(post("/api/payment-links").with(loginAs(id)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body(num)))
                .andExpect(status().isCreated());
    }

    @Test
    void 처음_연결하면_201() throws Exception {
        mockMvc.perform(post("/api/payment-links").with(loginAs(userId)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body(NUM)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fintechUseNum").value(NUM));
    }

    @Test
    void 이미_연결돼_있으면_409() throws Exception {
        link(userId, NUM);

        mockMvc.perform(post("/api/payment-links").with(loginAs(userId)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body(OTHER_NUM)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ALREADY_LINKED"));
    }

    @Test
    void 다른_사람이_쓰는_번호면_409() throws Exception {
        link(otherUserId, NUM);

        mockMvc.perform(post("/api/payment-links").with(loginAs(userId)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body(NUM)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("FINTECH_NUM_ALREADY_USED"));
    }

    @Test
    void 번호가_24자리가_아니면_400() throws Exception {
        mockMvc.perform(post("/api/payment-links").with(loginAs(userId)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body("1234")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 연결_후_조회하면_200() throws Exception {
        link(userId, NUM);

        mockMvc.perform(get("/api/payment-links/me").with(loginAs(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fintechUseNum").value(NUM));
    }

    @Test
    void 해제하면_204_이후_조회와_해제는_404() throws Exception {
        link(userId, NUM);

        mockMvc.perform(delete("/api/payment-links/me").with(loginAs(userId)).with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/payment-links/me").with(loginAs(userId)))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/payment-links/me").with(loginAs(userId)).with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void 해제_후_다시_연결하면_201() throws Exception {
        link(userId, NUM);
        mockMvc.perform(delete("/api/payment-links/me").with(loginAs(userId)).with(csrf()))
                .andExpect(status().isNoContent());

        link(userId, OTHER_NUM);

        mockMvc.perform(get("/api/payment-links/me").with(loginAs(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fintechUseNum").value(OTHER_NUM));
    }
}