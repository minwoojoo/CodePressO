package com.codepresso.codepresso.event;

public record OrderStampEarnedEvent(
        Long memberId,
        int earnedStampCount
) {
}
