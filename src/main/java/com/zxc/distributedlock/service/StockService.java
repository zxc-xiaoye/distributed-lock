package com.zxc.distributedlock.service;

import com.zxc.distributedlock.entity.Stock;
import com.zxc.distributedlock.lock.DistributedLockClient;
import com.zxc.distributedlock.lock.DistributedRedisLock;
import com.zxc.distributedlock.lock.DistributedZkLock;
import com.zxc.distributedlock.lock.ZkClient;
import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author xiaoye
 * @create 6/26/23 5:10 PM
 */
@Slf4j
@Service
//@Scope(value = "prototype", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class StockService {


    private static final String STOCK_KEY = "STOCK";

    @Autowired
    private StringRedisTemplate redisTemplate;


    @Autowired
    private DistributedLockClient lockClient;


    @Autowired
    private RedissonClient redissonClient;


    @Autowired
    private ZkClient zkClient;

    public void dedustZk() {
        String lockName = "LOCK";
        DistributedZkLock lock = zkClient.getLock(lockName);
        lock.lock();

        try {
            String stock = redisTemplate.opsForValue().get(STOCK_KEY);
            if (stock != null && stock.length() > 0) {
                Integer st = Integer.valueOf(stock);
                redisTemplate.opsForValue().set(STOCK_KEY, String.valueOf(--st));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            lock.unlock();
        }

    }

    /**
     * redis终极版本 可重入 可自动续期
     */
    public void dedust() {

        String lockName = "LOCK";
        DistributedRedisLock redisLock = lockClient.getRedisLock(lockName);
        redisLock.lock();
        try {
            String stock = redisTemplate.opsForValue().get(STOCK_KEY);
            if (stock != null && stock.length() > 0) {
                Integer st = Integer.valueOf(stock);
                redisTemplate.opsForValue().set(STOCK_KEY, String.valueOf(--st));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            redisLock.unlock();
        }

    }

    public void test(){
        DistributedRedisLock lock = lockClient.getRedisLock("LOCK");
        lock.lock();
        lock.unlock();

    }

    public void dedust1() {
        String uuid = UUID.randomUUID().toString();
        while (!redisTemplate.opsForValue().setIfAbsent("LOCK", uuid, 10, TimeUnit.SECONDS)) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }

        try {
            String stock = redisTemplate.opsForValue().get(STOCK_KEY);
            if (stock != null && stock.length() > 0) {
                Integer st = Integer.valueOf(stock);
                redisTemplate.opsForValue().set(STOCK_KEY, String.valueOf(--st));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            // 先判断是否是自己的锁，再释放
            // 用lua脚本
            String script = "if redis.call('get', KEYS[1]) == ARGV[1] " +
                    "then" +
                    " return redis.call('del', KEYS[1]) " +
                    "else" +
                    " return 0 " +
                    "end";
            redisTemplate.execute(new DefaultRedisScript<>(script, Boolean.class), Collections.singletonList("lock"), uuid);
        }

    }

}
