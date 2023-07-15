package com.zxc.distributedlock.lock;

import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.CountDownLatch;

@Slf4j
@Component
public class ZkClient {

    private ZooKeeper zooKeeper;

    @PostConstruct
    public void init(){
        CountDownLatch countDownLatch = new CountDownLatch(1);
        try {
            zooKeeper  = new ZooKeeper("127.0.0.1:2181", 30000, watchedEvent -> {

                Watcher.Event.KeeperState state = watchedEvent.getState();
                if (Watcher.Event.KeeperState.SyncConnected.equals(state)
                        && Watcher.Event.EventType.None.equals(watchedEvent.getType())) {
                    log.info("获取到zookeeper链接 -> " + watchedEvent);
                    countDownLatch.countDown();
                } else if (Watcher.Event.KeeperState.Closed.equals(state)) {
                    log.info("关闭链接");
                }
            });
            countDownLatch.await();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @PreDestroy
    public void destroy(){
        if (zooKeeper != null) {
            try {
                zooKeeper.close();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }


    public DistributedZkLock getLock(String lockName) {
        return new DistributedZkLock(zooKeeper, lockName);
    }


}
