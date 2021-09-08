package org.distributedlock.provider.sql;

import java.time.Clock;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.distributedlock.DistributedLockData;
import org.distributedlock.LockConfiguration;
import org.distributedlock.LockProvider;
import org.distributedlock.util.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class SQLDBLockProvider implements LockProvider {

    private final DistributedLockRepository distributedLockRepository;

    @Autowired
    public SQLDBLockProvider(final DistributedLockRepository distributedLockRepository) {
        this.distributedLockRepository = distributedLockRepository;
    }

    @Override
    public Pair<Boolean, LockConfiguration> tryLock(final LockConfiguration lockConfiguration) {
        final DistributedLockData lockData = toLockData(lockConfiguration);
        boolean acquiredLock = false;
        LockConfiguration updatedLockConfig = lockConfiguration;

        try {
            DistributedLockData distributedLock = distributedLockRepository.findById(lockData.getId());
            log.debug("Found lock {} for {}", distributedLock, lockData.getId());
            if (Objects.isNull(distributedLock)) {
                log.debug("Creating lock {}, did not pre-exist", lockData);
                distributedLockRepository.create(lockData);
                acquiredLock = true;
            } else {
                final String hostname = IOUtils.getHostname();
                boolean tobeUpdated = false;
                String lockedBy = distributedLock.getLockedBy();
                if (StringUtils.equalsIgnoreCase(hostname, distributedLock.getLockedBy())) {
                    // Already the owner of the lock, just extend it further
                    log.debug("Lock {} is already owned, extending it further", distributedLock);
                    tobeUpdated = true;
                } else if (distributedLock.getLockedTill().longValue() < Clock.systemUTC().instant().toEpochMilli()) {
                    /*
                     * If lock has expired, we are free to change the owner.<p/>
                     * This is critical for the scenario where for some reason if delete/release of lock has failed,
                     * then it will be unnecessary blocked by the same service instance, instead of giving someone
                     * else a chance.<p/>
                     * Take another scenario, what if before released, the instance died, then no other instance
                     * would be able to pick it up, unless the lock is handed over to someone else, hence taken care.
                     */
                    log.debug("Lock {} has expired, handing over the lock to {}", distributedLock, hostname);
                    tobeUpdated = true;
                    lockedBy = hostname;
                }

                if (tobeUpdated) {
                    updatedLockConfig = new LockConfiguration(Clock.systemUTC().instant(), lockConfiguration.getName(),
                                                              lockedBy, lockConfiguration.getLockFor());

                    distributedLock.setLockedAt(Clock.systemUTC().millis());
                    distributedLock.setLockedTill(updatedLockConfig.getUnlockTime().toEpochMilli());
                    distributedLock.setLockedBy(updatedLockConfig.getOwner());
                    distributedLockRepository.update(distributedLock);
                    log.debug("Successfully updated lock {}", distributedLock);

                    /*
                     * Just to be double sure that the lock is updated by current instance and not someone else in
                     * the mean time, read again and confirm
                     */
                    distributedLock = distributedLockRepository.findById(lockData.getId());
                    log.debug("Current instance = {}, lock updated {}", hostname, distributedLock);
                    if (StringUtils.equalsIgnoreCase(hostname, distributedLock.getLockedBy())) {
                        acquiredLock = true;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to acquire lock with configuration {}", lockData, e);
        }

        return Pair.of(acquiredLock, updatedLockConfig);
    }

    @Override
    public void release(final LockConfiguration lockConfiguration) {
        final String hostname = IOUtils.getHostname();
        log.debug("{} is checking and trying to release lock {}", hostname, lockConfiguration);
        if (StringUtils.equalsIgnoreCase(hostname, lockConfiguration.getOwner())) {
            try {
                distributedLockRepository.deleteById(lockConfiguration.getName());
                log.debug("Successfully released lock {} by {}", lockConfiguration, hostname);
            } catch (Exception e) {
                log.warn("Failed to release the lock {}", lockConfiguration, e);
            }
        }
    }

    private DistributedLockData toLockData(final LockConfiguration lockConfiguration) {
        final DistributedLockData distributedLock = new DistributedLockData();
        distributedLock.setId(lockConfiguration.getName());
        distributedLock.setLockedBy(lockConfiguration.getOwner());
        distributedLock.setLockedAt(lockConfiguration.getLockedAt().toEpochMilli());
        distributedLock.setLockedTill(lockConfiguration.getUnlockTime().toEpochMilli());

        return distributedLock;
    }
}
