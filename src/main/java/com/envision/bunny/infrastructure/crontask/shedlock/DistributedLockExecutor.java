package com.envision.bunny.infrastructure.crontask.shedlock;

import com.envision.bunny.infrastructure.response.BizException;
import com.envision.bunny.infrastructure.response.ErrorCode;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * 可以通过代码的方式，直接调用锁
 * distributedLockExecutor.executeWithLock(() -> "Hello World.",
 * new LockConfiguration("key", Instant.now().plusSeconds(60)))
 * @author jingjing.dong
 * @since 2021/4/19-22:48
 */
public class DistributedLockExecutor {
    private final LockProvider lockProvider;

    public DistributedLockExecutor(LockProvider lockProvider) {
        this.lockProvider = lockProvider;
    }

    public <T> T executeWithLock(Supplier<T> supplier, LockConfiguration configuration) {
        Optional<SimpleLock> lock = lockProvider.lock(configuration);
        if (!lock.isPresent()) {
            throw new BizException(ErrorCode.LOCK_ALREADY_OCCUPIED, configuration.getName());
        }
        try {
            return supplier.get();
        } finally {
            lock.get().unlock();
        }
    }

}
