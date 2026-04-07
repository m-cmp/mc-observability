package com.mcmp.o11ymanager.manager.service.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.mcmp.o11ymanager.manager.dto.tumblebug.TumblebugMCI;
import com.mcmp.o11ymanager.manager.service.interfaces.TumblebugService;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Resolves the {@code createdTime} of a VM from CB-Tumblebug and caches it locally so that the
 * monitoring metric cache can compute per-entry TTLs without hitting Tumblebug on every query.
 *
 * <p>Tumblebug returns {@code createdTime} as a string in {@code "yyyy-MM-dd HH:mm:ss"} format
 * (local server time). We treat it as system-default timezone since Tumblebug does not include a
 * timezone in the value.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VmCreatedTimeResolver {

    private static final DateTimeFormatter TUMBLEBUG_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final TumblebugService tumblebugService;

    /**
     * Caches the resolved {@link Instant} per (ns, mci, vm) for 1 hour. Negative results (VM lookup
     * failed or createdTime missing) are cached as {@link Optional#empty()} to avoid hammering
     * Tumblebug on every metric request for VMs that simply don't expose the field.
     */
    private final Cache<String, Optional<Instant>> cache =
            Caffeine.newBuilder().maximumSize(10_000).expireAfterWrite(1, TimeUnit.HOURS).build();

    /** Returns the VM creation instant if known, or empty if Tumblebug doesn't expose it. */
    public Optional<Instant> resolve(String nsId, String mciId, String vmId) {
        if (nsId == null || mciId == null || vmId == null) {
            return Optional.empty();
        }
        String key = nsId + "/" + mciId + "/" + vmId;
        Optional<Instant> cached = cache.getIfPresent(key);
        if (cached != null) {
            return cached;
        }
        Optional<Instant> resolved = fetchFromTumblebug(nsId, mciId, vmId);
        cache.put(key, resolved);
        return resolved;
    }

    private Optional<Instant> fetchFromTumblebug(String nsId, String mciId, String vmId) {
        try {
            TumblebugMCI.Vm vm = tumblebugService.getVm(nsId, mciId, vmId);
            if (vm == null || vm.getCreatedTime() == null || vm.getCreatedTime().isBlank()) {
                return Optional.empty();
            }
            LocalDateTime ldt = LocalDateTime.parse(vm.getCreatedTime().trim(), TUMBLEBUG_FORMAT);
            return Optional.of(ldt.atZone(ZoneId.systemDefault()).toInstant());
        } catch (Exception e) {
            log.debug(
                    "[VM-CREATED] failed to resolve createdTime ns={}, mci={}, vm={}, err={}",
                    nsId,
                    mciId,
                    vmId,
                    e.toString());
            return Optional.empty();
        }
    }

    /** Test/admin hook to clear the resolver cache. */
    public void invalidateAll() {
        cache.invalidateAll();
    }
}
