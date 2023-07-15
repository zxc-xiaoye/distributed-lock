package com.zxc.distributedlock.service;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * @author xiaoye
 * @create 7/13/23 11:21 AM
 */

@Service
public class CuratorService {

    private static final String STOCK_KEY = "STOCK";

    @Autowired
    private CuratorFramework curatorFramework;

    @Autowired
    private StringRedisTemplate redisTemplate;

    public void dedustZk() {
        String lockName = "LOCK";
        InterProcessMutex mutex = new InterProcessMutex(curatorFramework, "/curator/locks");

        try {
            mutex.acquire();
            String stock = redisTemplate.opsForValue().get(STOCK_KEY);
            if (stock != null && stock.length() > 0) {
                Integer st = Integer.valueOf(stock);
                redisTemplate.opsForValue().set(STOCK_KEY, String.valueOf(--st));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            try {
                mutex.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }
}
