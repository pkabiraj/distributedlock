package org.distributedlock;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

import lombok.Data;

/**
 * Configuration for distributed locks.
 */
@Data
public class LockConfiguration {

    private final Instant lockedAt;

    /**
     * The name of the lock
     */
    private final String name;

    /**
     * The owner of the lock
     */
    private final String owner;

    /**
     * The duration to hold the lock
     */
    private final Duration lockFor;

    public LockConfiguration(final String name,
                             final String owner,
                             final Duration lockFor) {
        this(Clock.systemUTC().instant(), name, owner, lockFor);
    }

    public LockConfiguration(final Instant lockedAt,
                             final String name,
                             final String owner,
                             final Duration lockFor) {
        this.lockedAt = Objects.requireNonNull(lockedAt);
        this.name = Objects.requireNonNull(name);
        this.owner = Objects.requireNonNull(owner);
        this.lockFor = Objects.requireNonNull(lockFor);
        if (this.lockFor.isNegative()) {
            throw new IllegalArgumentException("lockFor is negative '" + name + "'.");
        }
    }

    public Instant lockUntil() {
        return lockedAt.plus(lockFor);
    }

    /**
     * Returns either now or <code>lockUntil</code> whichever is later.
     */
    public Instant getUnlockTime() {
        Instant now = Clock.systemUTC().instant();
        Instant lockUntil = lockUntil();
        return lockUntil.isAfter(now) ? lockUntil : now;
    }
}
