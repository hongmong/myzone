package com.imgeek.locks;

import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * auth:    xiemin
 * date:    2018-07-19
 * desc:    基于zookeeper实现的分布式锁
 */

@Slf4j
public class MyZookeeperLock implements IDistributedLock {
    private ZooKeeper zooKeeper;
    private final String rootPath = "/dislock";
    private final String preNodeName = "lock-";
    private String currentNodeName = "";

    public MyZookeeperLock(String host, int sessionTimeout, Watcher watcher) throws IOException, KeeperException, InterruptedException {
        zooKeeper = new ZooKeeper(host, sessionTimeout, watcher);
        init();
    }

    /**
     * todo
     * 暂时写死，日后通过配置文件或者配置类实现
     * @throws IOException
     * @throws KeeperException
     * @throws InterruptedException
     */
    public MyZookeeperLock() throws IOException, KeeperException, InterruptedException {
        zooKeeper = new ZooKeeper("10.0.1.9:2181", 8000, null);
        init();
    }

    private void init() throws KeeperException, InterruptedException {
        currentNodeName = createNode();
    }

    /**
     * 阻塞锁
     *
     * @throws Exception
     */
    @Override
    public void lock() throws Exception {
        while (true) {
            List<String> childNodeNames = getSortedChildNodeNames();
            int index = getSelfIndexofChildrenNodeNames(childNodeNames, currentNodeName);
            log.debug("currentNode : ".concat(currentNodeName).concat(" index : ").concat(String.valueOf(index)));
            if (index <= 0) {
                break;
            } else {
                // 如果次小的节点被删除了，则表示当前客户端的节点应该是最小的了，所以使用CountDownLatch来实现等待
                String preNodeName = getPreNodeNameOfChildrenNodes(childNodeNames, currentNodeName);
                final CountDownLatch latch = new CountDownLatch(1);
                final Watcher previousListener = new Watcher() {
                    public void process(WatchedEvent event) {
                        log.debug("fire Event1 : ".concat(event.getType().toString()));
                        if (event.getType() == Event.EventType.NodeDeleted) {
                            latch.countDown();
                            log.debug("fire Event2 : ".concat(event.getType().toString()));
                        }
                    }
                };
                log.debug("WholePreNodeName : ".concat(rootPath).concat("/").concat(preNodeName));
                zooKeeper.exists(rootPath.concat("/").concat(preNodeName), previousListener);
                latch.await();
                log.debug("".concat("preNodeName :".concat(preNodeName)).concat(" awit"));
            }
        }
    }

    @Override
    public boolean lock(long startmillis, TimeUnit timeUnit) throws Exception {
        return true;
    }

    @Override
    public boolean trylock() throws Exception {
        return false;
    }

    @Override
    public void unlock() {
        log.debug("unlock :".concat(currentNodeName));
        if (!currentNodeName.isEmpty()) {
            try {
                Stat stat = zooKeeper.exists(rootPath.concat("/").concat(currentNodeName), false);
                if (stat != null) {
                    log.debug("delete : ".concat(rootPath).concat("/").concat(currentNodeName));
                    zooKeeper.delete(rootPath.concat("/").concat(currentNodeName), -1);
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    /**
     * 获取排序（升序）后的全部子节点信息
     *
     * @return
     * @throws KeeperException
     * @throws InterruptedException
     */
    private List<String> getSortedChildNodeNames() throws KeeperException, InterruptedException {
        List<String> childrenNodes = zooKeeper.getChildren(rootPath, false);
        childrenNodes.sort(String::compareTo);
        return childrenNodes;
    }

    /**
     * 获取当前节点在全部节点(升序排序后)当中的位置
     *
     * @param childSortedNodes
     * @param selfNode
     * @return
     */
    private int getSelfIndexofChildrenNodeNames(List<String> childSortedNodes, String selfNode) {
        return childSortedNodes.indexOf(selfNode);
    }

    /**
     * 获取当前节点的前一个节点的名称
     *
     * @param childSortedNodes
     * @param selfNode
     *
     * @return
     */
    private String getPreNodeNameOfChildrenNodes(List<String> childSortedNodes, String selfNode) {
        int index = getSelfIndexofChildrenNodeNames(childSortedNodes, selfNode);
        if (index > 0) {
            return childSortedNodes.get(index - 1);
        }
        return null;
    }

    /**
     * 创建节点并获取节点名称
     *
     * @return
     * @throws KeeperException
     * @throws InterruptedException
     */
    private String createNode() throws KeeperException, InterruptedException {
        Stat stat = zooKeeper.exists(rootPath, false);
        if (stat == null) {
            zooKeeper.create(rootPath, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        }
        String createRet = zooKeeper.create(rootPath.concat("/").concat(preNodeName), null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
        log.debug("create : ".concat(createRet));
        return createRet.substring(createRet.lastIndexOf("/") + 1, createRet.length());
    }
}
