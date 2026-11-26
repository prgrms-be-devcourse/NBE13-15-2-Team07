package com.kdt.nbe13152team07.user.repository;

import com.kdt.nbe13152team07.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // 카카오 회원번호로 회원 조회 (탈퇴 회원은 kakaoId가 null이라 조회되지 않음)
    Optional<User> findByKakaoId(String kakaoId);

}
