package com.codepresso.codepresso.service.payment;

import com.codepresso.codepresso.event.OrderStampEarnedEvent;
import com.codepresso.codepresso.service.coupon.StampService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderStampEventListener {

    private final StampService stampService;

    @Async("orderEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void earnStampsAfterOrderCommitted(OrderStampEarnedEvent event) {
        try {
            stampService.earnStampsFromOrder(event.memberId(), event.earnedStampCount());
        } catch (Exception e) {
            log.error("주문 커밋 후 스탬프 적립 실패. memberId={}, earnedStampCount={}",
                    event.memberId(), event.earnedStampCount(), e);
        }
    }
}
