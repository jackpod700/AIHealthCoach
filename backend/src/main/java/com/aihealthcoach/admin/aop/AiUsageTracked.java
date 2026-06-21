package com.aihealthcoach.admin.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * AI 호출의 용도를 aspect에 전달하기 위한 표시 annotation이다.
 * 실제 사용량 metric 기록은 이 annotation이 붙은 gateway 메서드를 AiUsageLoggingAspect가 감싸서 처리한다.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AiUsageTracked {
    String requestType();
}
