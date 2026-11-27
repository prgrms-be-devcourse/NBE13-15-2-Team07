package com.kdt.mock_pay.Payment.Repository;

import com.kdt.mock_pay.Payment.Entity.Delivery;
import com.kdt.mock_pay.Payment.Entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;


public interface DeliveryRepository extends JpaRepository<Delivery, Long> {

    // 결제 하나의 호출 이력 (망취소 확인, 트러블슈팅용)
    List<Delivery> findByPaymentOrderByIdAsc(Payment payment);
}
