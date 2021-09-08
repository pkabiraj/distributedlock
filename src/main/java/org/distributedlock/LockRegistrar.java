package org.distributedlock;

import java.time.Duration;
import java.util.Objects;

import org.apache.commons.lang3.tuple.Pair;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.distributedlock.util.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Aspect
@Component
@Slf4j
public class LockRegistrar {

    private final LockProvider lockProvider;

    @Autowired
    public LockRegistrar(final LockProvider lockProvider) {
        this.lockProvider = lockProvider;
    }

    @Around("@annotation(org.distributedlock.DistributedLock)")
    public void distributedLock(final ProceedingJoinPoint joinPoint) throws Throwable {
        final MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        final DistributedLock distributedLock = signature.getMethod().getAnnotation(DistributedLock.class);

        final LockConfiguration lockConfiguration = getLockConfigWithDefaults(distributedLock);
        log.debug("Trying to acquire lock {}", lockConfiguration);
        final Pair<Boolean, LockConfiguration> proceedPair = lockProvider.tryLock(lockConfiguration);
        log.debug("Received {} response to acquire lock with {} configuration", proceedPair, lockConfiguration);

        // If lock is acquired, then only we should proceed to execute the method
        if (proceedPair.getLeft()) {
            try {
                joinPoint.proceed();
            } finally {
                // Release the lock only if acquired and after the method is executed
                final LockConfiguration lockToRelease = Objects.nonNull(proceedPair.getRight()) ?
                                                        proceedPair.getRight() : lockConfiguration;
                log.debug("Releasing lock {}", lockToRelease);
                lockProvider.release(lockToRelease);
            }
        }
    }

    private LockConfiguration getLockConfigWithDefaults(final DistributedLock distributedLock) {
        final String owner = IOUtils.getHostDetail(distributedLock.owner());
        return new LockConfiguration(distributedLock.name(), owner, Duration.ofSeconds(distributedLock.lockFor()));
    }
}
