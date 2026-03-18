package com.example.wallet_service.annotation;

import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@Retryable(
        retryFor = {
                CannotAcquireLockException.class,
                PessimisticLockingFailureException.class,
                DataAccessException.class,
                OptimisticLockingFailureException.class
        },
        maxAttemptsExpression = "${app.wallet.retry.max-attempts:5}",
        backoff = @Backoff(
                delayExpression = "${app.wallet.retry.delay:100}",
                multiplierExpression = "${app.wallet.retry.multiplier:2}",
                maxDelayExpression = "${app.wallet.retry.max-delay:5000}"
        )

)
public @interface WalletRetryable {

}