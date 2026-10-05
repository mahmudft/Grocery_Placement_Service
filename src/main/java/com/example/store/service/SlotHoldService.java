package com.example.store.service;

import com.example.store.config.StoreProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;


@Component
public class SlotHoldService {

    private static final String HOLD_LUA = """
            local key, now, expiresAt, free, member, ttl = KEYS[1], tonumber(ARGV[1]), tonumber(ARGV[2]),
                  tonumber(ARGV[3]), ARGV[4], tonumber(ARGV[5])
            redis.call('ZREMRANGEBYSCORE', key, '-inf', now)
            local alreadyHeld = redis.call('ZSCORE', key, member)
            if alreadyHeld or redis.call('ZCARD', key) < free then
              redis.call('ZADD', key, expiresAt, member)
              redis.call('PEXPIRE', key, ttl)
              return 1
            end
            return 0
            """;

    private static final DefaultRedisScript<Long> HOLD_SCRIPT = new DefaultRedisScript<>(HOLD_LUA, Long.class);

    private final StringRedisTemplate redis;
    private final Clock clock;
    private final Duration ttl;

    public SlotHoldService(StringRedisTemplate redis, Clock clock, StoreProperties props) {
        this.redis = redis;
        this.clock = clock;
        this.ttl = props.slotHoldTtl();
    }

    public Optional<Instant> tryHold(long slotId, String customerId, int freeCapacity) {
        long now = clock.millis();
        long expiresAt = now + ttl.toMillis();
        Long result = redis.execute(HOLD_SCRIPT, List.of(key(slotId)),
                String.valueOf(now), String.valueOf(expiresAt), String.valueOf(freeCapacity),
                customerId, String.valueOf(ttl.toMillis()));
        return Long.valueOf(1L).equals(result) ? Optional.of(Instant.ofEpochMilli(expiresAt)) : Optional.empty();
    }

    public void release(long slotId, String customerId) {
        redis.opsForZSet().remove(key(slotId), customerId);
    }

    public int activeHolds(long slotId) {
        redis.opsForZSet().removeRangeByScore(key(slotId), 0, clock.millis());
        Long size = redis.opsForZSet().zCard(key(slotId));
        return size == null ? 0 : size.intValue();
    }

    private static String key(long slotId) {
        return "slot:holds:" + slotId;
    }
}
