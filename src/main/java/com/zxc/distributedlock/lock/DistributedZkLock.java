package com.zxc.distributedlock.lock;

import org.apache.commons.lang3.StringUtils;
import org.apache.zookeeper.*;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;


public class DistributedZkLock implements Lock {

    private ZooKeeper zooKeeper;

    private String lockName;

    private static final String ROOT_PATH = "/LOCKS";

    private String currentNodePath;

    private static final ThreadLocal<Integer> THREAD_LOCAL = new ThreadLocal<>();

    DistributedZkLock(ZooKeeper zooKeeper, String lockName) {
        this.zooKeeper = zooKeeper;
        this.lockName = lockName;

        // 创建初始化节点
        try {
            if (zooKeeper.exists(ROOT_PATH, false) == null) {
                zooKeeper.create(ROOT_PATH, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void lock() {
        tryLock();
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {

    }
    @Override
    public boolean tryLock() {


        try {
            Integer flag = THREAD_LOCAL.get();
            if (flag != null && flag > 0) {
                THREAD_LOCAL.set(flag + 1);
                return true;
            }
            // 创建znode节点
            // 防止死锁，创建临时节点
            currentNodePath = zooKeeper.create(ROOT_PATH + "/" + lockName + "->", null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
            String preNode = getPreNode();
            if (preNode != null) {
                // 利用闭锁实现阻塞
                CountDownLatch countDownLatch = new CountDownLatch(1);
                // 再次判断zk中前置节点是否存在
                if (zooKeeper.exists(ROOT_PATH + "/" + preNode, watchedEvent -> countDownLatch.countDown()) == null) {
                    // 前置节点不存在了 自身就是第一个
                    THREAD_LOCAL.set(1);
                    return true;
                }
                countDownLatch.await();
            }
            THREAD_LOCAL.set(1);
            return true;
        } catch (Exception e) {
            e.printStackTrace();

        }
        return false;
    }

    private String getPreNode(){
        try {
            List<String> children = zooKeeper.getChildren(ROOT_PATH, false);
            if (CollectionUtils.isEmpty(children)) {
                throw new IllegalMonitorStateException("非法操作");
            }
            List<String> nodes = children.stream().filter(node -> StringUtils.startsWith(node, lockName + "->")).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(nodes)) {
                throw new IllegalMonitorStateException("非法操作");
            }

            Collections.sort(nodes);

            int index = Collections.binarySearch(nodes, StringUtils.substringAfterLast(currentNodePath, "/"));
            if (index < 0) {
                throw new IllegalMonitorStateException("非法操作");
            } else if(index > 0) {
                return nodes.get(index - 1);
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalMonitorStateException("非法操作");

        }

    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return false;
    }

    @Override
    public void unlock() {
        try {
            THREAD_LOCAL.set(THREAD_LOCAL.get() - 1);
            if(THREAD_LOCAL.get() == 0 ) {
                zooKeeper.delete(currentNodePath, -1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Condition newCondition() {
        return null;
    }
}
