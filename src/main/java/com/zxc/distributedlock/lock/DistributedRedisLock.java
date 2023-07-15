package com.zxc.distributedlock.lock;

import com.oracle.tools.packager.Log;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * @author xiaoye
 * @create 7/5/23 1:20 PM
 */
@Slf4j
public class DistributedRedisLock implements Lock {

    private StringRedisTemplate redisTemplate;

    private String lockName;

    private String serverId;

    private long expire = 10;


    private static final String LOCK_LUA_STR =
            "if " +
            "   redis.call('exists', KEYS[1]) == 0 or redis.call('hexists', KEYS[1], ARGV[1]) == 1 " +
            "then " +
            "   redis.call('hincrby', KEYS[1], ARGV[1], 1) " +
            "   redis.call('expire', KEYS[1], ARGV[2]) " +
            "   return 1 " +
            "else " +
            "   return 0 " +
            "end";

    private static final String UNLOCK_LUA_STR =
            "if redis.call('hexists', KEYS[1], ARGV[1]) == 0 " +
            "then " +
            "   return nil " +
            "elseif redis.call('hincrby', KEYS[1], ARGV[1], -1) == 0 " +
            "then " +
            "   return redis.call('del', KEYS[1])" +
            "else" +
            "   return 0 " +
            "end";

    private static final String RENEW_EXPIRE_LUA_STR =
            "if redis.call('hexists', KEYS[1], ARGV[1]) == 1 " +
            "then " +
            "   return redis.call('expire', KEYS[1], ARGV[2]) " +
            "else " +
            "   return 0 " +
            "end";

    public DistributedRedisLock(StringRedisTemplate redisTemplate, String lockName, String serverId) {
        this.redisTemplate = redisTemplate;
        this.lockName = lockName;
        this.serverId = serverId + Thread.currentThread().getId();
        log.info(this.serverId);
    }


    @Override
    public void lock() {
        this.tryLock();
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {

    }

    @Override
    public boolean tryLock() {
        try {
            return this.tryLock(-1L, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }

    // 带过期时间
    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        if (time != -1) {
            this.expire = unit.toSeconds(time);
        }
        while(!this.redisTemplate.execute(new DefaultRedisScript<>(LOCK_LUA_STR, Boolean.class),
                Arrays.asList(this.lockName), serverId, String.valueOf(expire))){
            Thread.sleep(50);
        }
        this.renewExpire();
        return false;
    }

    @Override
    public void unlock() {
        Long flag = this.redisTemplate.execute(new DefaultRedisScript<>(UNLOCK_LUA_STR, Long.class), Arrays.asList(lockName), serverId);

        log.info("flag -> " + flag.toString());

        if (flag == null) {
            throw new IllegalMonitorStateException();
        }
    }


    /**
     * 自动续期
     */
    private void renewExpire() {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                if(redisTemplate.execute(new DefaultRedisScript<>(RENEW_EXPIRE_LUA_STR, Boolean.class),Arrays.asList(lockName), serverId, String.valueOf(expire))) {
                    renewExpire();
                }
            }
        }, expire * 1000 / 3);
    }

    @Override
    public Condition newCondition() {
        return null;
    }
}
