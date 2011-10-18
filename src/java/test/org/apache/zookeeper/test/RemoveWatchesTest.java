/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.zookeeper.test;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.WatcherType;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.junit.Assert;
import org.junit.Test;

public class RemoveWatchesTest extends ClientBase {
    private class MyWatcher implements Watcher {
        private final String path;
        private String eventPath;
        private CountDownLatch latch = new CountDownLatch(1);

        public MyWatcher(String path) {
            this.path = path;
        }

        public void process(WatchedEvent event) {
            System.out.println("latch:" + path + " " + event.getPath());
            this.eventPath = event.getPath();
            latch.countDown();
        }

        public boolean matches() throws InterruptedException {
            if (!latch.await(CONNECTION_TIMEOUT / 60, TimeUnit.MILLISECONDS)) {
                return false;
            }
            return path.equals(eventPath);
        }
    }

    @Test
    public void testRemoveWatcher() throws IOException, InterruptedException, KeeperException {
        ZooKeeper zk1 = createClient();
        ZooKeeper zk2 = createClient();
        try {
            zk1.create("/node1", null, Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
            zk1.create("/node2", null, Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
            MyWatcher w1 = new MyWatcher("/node1");
            Assert.assertNotNull(zk2.exists("/node1", w1));
            MyWatcher w2 = new MyWatcher("/node2");
            Assert.assertNotNull(zk2.exists("/node2", w2));
            zk2.removeWatches("/node2", w2, WatcherType.Data);
            try {
                zk2.removeWatches("/node2", w2, WatcherType.Children);
                Assert.fail("Didn't complain about unexisting watcher.");
            } catch (KeeperException e) {
                // should always happen
            }
            if (zk1 != null) {
                zk1.close();
                zk1 = null;
            }
            Assert.assertTrue(w1.matches());
            Assert.assertFalse(w2.matches());
            Assert.assertNull(zk2.exists("/node1", false));
            Assert.assertNull(zk2.exists("/node2", false));
        } finally {
            if (zk1 != null)
                zk1.close();
            if (zk2 != null)
                zk2.close();
        }
    }

    @Test
    public void testMultipleWatchers() throws IOException, InterruptedException, KeeperException {
        ZooKeeper zk1 = createClient();
        ZooKeeper zk2 = createClient();
        try {
            zk1.create("/node1", null, Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
            MyWatcher w1 = new MyWatcher("/node1");
            Assert.assertNotNull(zk2.exists("/node1", w1));
            MyWatcher w2 = new MyWatcher("/node1");
            Assert.assertNotNull(zk2.exists("/node1", w2));
            zk2.removeWatches("/node1", w2, WatcherType.Data);
            if (zk1 != null) {
                zk1.close();
                zk1 = null;
            }
            Assert.assertTrue(w1.matches());
            Assert.assertFalse(w2.matches());
            Assert.assertNull(zk2.exists("/node1", false));
        } finally {
            if (zk1 != null)
                zk1.close();
            if (zk2 != null)
                zk2.close();
        }
    }
    
    @Test
    public void testMultipleTypes() throws IOException, InterruptedException, KeeperException {
        ZooKeeper zk1 = createClient();
        ZooKeeper zk2 = createClient();
        try {
            zk1.create("/node1", null, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            MyWatcher w1 = new MyWatcher("/node1");
            Assert.assertNotNull(zk2.exists("/node1", w1));
            Assert.assertNotNull(zk2.getChildren("/node1", w1));
            zk2.removeWatches("/node1", w1, WatcherType.Data);
            zk1.create("/node1/child", null, Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
            Assert.assertTrue(w1.matches());
            Assert.assertNotNull(zk2.exists("/node1", false));
            Assert.assertNotNull(zk2.exists("/node1/child", false));
        } finally {
            if (zk1 != null)
                zk1.close();
            if (zk2 != null)
                zk2.close();
        }
    }

    @Test
    public void testRemoveAll() throws IOException, InterruptedException, KeeperException {
        ZooKeeper zk1 = createClient();
        ZooKeeper zk2 = createClient();
        try {
            zk1.create("/node1", null, Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
            MyWatcher w1 = new MyWatcher("/node1");
            Assert.assertNotNull(zk2.exists("/node1", w1));
            MyWatcher w2 = new MyWatcher("/node1");
            Assert.assertNotNull(zk2.exists("/node1", w2));
            zk2.removeWatches("/node1", null, WatcherType.Data);
            if (zk1 != null) {
                zk1.close();
                zk1 = null;
            }
            Assert.assertFalse(w1.matches());
            Assert.assertFalse(w2.matches());
            Assert.assertNull(zk2.exists("/node1", false));
        } finally {
            if (zk1 != null)
                zk1.close();
            if (zk2 != null)
                zk2.close();
        }
    }
    
}
