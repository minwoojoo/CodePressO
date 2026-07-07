package com.codepresso.codepresso.dto.payment;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 토스페이먼츠 결제 성공 후 주문 생성 결과 응답 DTO.
 * 주문 상세 데이터는 /orders/{orderId}에서 별도로 조회한다.
 */
@Data
@Builder
public class TossPaymentSuccessResponse {
    private Long orderId;
    private String productionStatus;
    private LocalDateTime orderDate;
    private Integer finalAmount;
}
