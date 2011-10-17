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
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class EphemeralContainerTest extends ClientBase {
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
            if (!latch.await(CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS)) {
                Assert.fail("No watch received within timeout period " + path);
            }
            return path.equals(eventPath);
        }
    }

    @Test
    public void testEphemeralContainer()
        throws IOException, InterruptedException, KeeperException
    {
        ZooKeeper zk1 = createClient();
        ZooKeeper zk2 = createClient();
        try {
            zk1.create("/container", null, Ids.OPEN_ACL_UNSAFE,
                    CreateMode.EPHEMERAL_CONTAINER);
            zk1.create("/container/child", null, Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
            zk1.delete("/container/#", -1);
            MyWatcher w1 = new MyWatcher("/container");
            Assert.assertNotNull(zk2.exists("/container", w1));
            MyWatcher w2 = new MyWatcher("/container/child");
            Assert.assertNotNull(zk2.exists("/container/child", w2));
            if(zk1 != null) {
                zk1.close();
                zk1 = null;
            }
            Assert.assertTrue(w1.matches());
            Assert.assertTrue(w2.matches());
            Assert.assertNull(zk2.exists("/container", false));
            Assert.assertNull(zk2.exists("/container/child", false));
        } finally {
            if(zk1 != null)
                zk1.close();
            if(zk2 != null)
                zk2.close();
        }
    }
    
    @Test
    public void testContainerMultipleClients()
        throws IOException, InterruptedException, KeeperException
    {
        ZooKeeper zk1 = createClient();
        ZooKeeper zk2 = createClient();
        try {
            zk1.create("/container", null, Ids.OPEN_ACL_UNSAFE,
                    CreateMode.EPHEMERAL_CONTAINER);
            zk2.create("/container/child", null, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            zk2.delete("/container/#", -1);
            MyWatcher w1 = new MyWatcher("/container");
            Assert.assertNotNull(zk1.exists("/container", w1));
            MyWatcher w2 = new MyWatcher("/container");
            Assert.assertNotNull(zk2.exists("/container", w2));
            zk1.delete("/container/child", -1);
            Assert.assertTrue(w1.matches());
            Assert.assertTrue(w2.matches());
            Assert.assertNull(zk1.exists("/container", false));
            Assert.assertNull(zk1.exists("/container/child", false));
            Assert.assertNull(zk2.exists("/container", false));
            Assert.assertNull(zk2.exists("/container/child", false));
        } finally {
            if(zk1 != null)
                zk1.close();
            if(zk2 != null)
                zk2.close();
        }
    }
    
    @Test
    public void testSequentialContainers()
        throws IOException, InterruptedException, KeeperException
    {
        final int SIZE = 10;
        ZooKeeper zk1 = createClient();
        try {
            for (int i = 0; i < SIZE; i++) {
                zk1.create("/container", null, Ids.OPEN_ACL_UNSAFE,
                        CreateMode.EPHEMERAL_SEQUENTIAL_CONTAINER);
            }
            List<String> containers = zk1.getChildren("/", false);
            Assert.assertNotNull(containers);
            for (String container : containers) {
                zk1.create("/" + container + "/child", null, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
            for (String container : containers) {
                Assert.assertNotNull(zk1.exists("/" + container + "/child", false));
            }
            for (String container : containers) {
                zk1.delete("/" + container + "/child", -1);
            }
            for (String container : containers) {
                Assert.assertNull(zk1.exists("/" + container + "/child", false));
            }
        } finally {
            if(zk1 != null)
                zk1.close();
        }
    }
    
    @Test
    public void testRecursiveDeletion()
        throws IOException, InterruptedException, KeeperException
    {
        final int SIZE = 10;
        ZooKeeper zk1 = createClient();
        try {
            String path = "/c";
            zk1.create(path, null, Ids.OPEN_ACL_UNSAFE,
                    CreateMode.EPHEMERAL_CONTAINER);
            for (int i = 0; i < SIZE; i++) {
                String childPath = path + "/c";
                zk1.create(childPath, null, Ids.OPEN_ACL_UNSAFE,
                        CreateMode.EPHEMERAL_CONTAINER);
                zk1.delete(path + "/#", -1);
                path = childPath;
            }
            MyWatcher w1 = new MyWatcher("/c");
            MyWatcher w2 = new MyWatcher(path);
            Assert.assertNotNull(zk1.exists("/c", w1));
            Assert.assertNotNull(zk1.exists(path, w2));
            zk1.delete(path + "/#", -1);
            Assert.assertTrue(w1.matches());
            Assert.assertTrue(w2.matches());
            Assert.assertNull(zk1.exists("/c", false));
            Assert.assertNull(zk1.exists(path, false));
        } finally {
            if(zk1 != null)
                zk1.close();
        }
    }

    @Test
    public void testEphemeralBehaviour()
        throws IOException, InterruptedException, KeeperException
    {
        ZooKeeper zk1 = createClient();
        ZooKeeper zk2 = createClient();
        try {
            zk1.create("/container", null, Ids.OPEN_ACL_UNSAFE,
                    CreateMode.EPHEMERAL_CONTAINER);
            zk1.create("/container2", null, Ids.OPEN_ACL_UNSAFE,
                    CreateMode.EPHEMERAL_CONTAINER);
            zk2.create("/container/child", null, Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
            MyWatcher w1 = new MyWatcher("/container");
            Assert.assertNotNull(zk2.exists("/container", w1));
            MyWatcher w2 = new MyWatcher("/container2");
            Assert.assertNotNull(zk2.exists("/container2", w2));
            zk1.close();
            Assert.assertTrue(w2.matches());
            zk2.delete("/container/child", -1);
            Assert.assertTrue(w1.matches());
            
            Assert.assertNull(zk2.exists("/container", false));
            Assert.assertNull(zk2.exists("/container/child", false));
            Assert.assertNull(zk2.exists("/container2", false));
        } finally {
            if(zk1 != null)
                zk1.close();
            if(zk2 != null)
                zk2.close();
        }
    }
}
