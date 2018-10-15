package cn.dustlight.bucket.other;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.ACL;

import java.util.List;

public class ZooKeeperUtils {

    public static String creat(ZooKeeper zooKeeper,byte[] data, String path, List<ACL> acls, CreateMode createMode) throws KeeperException, InterruptedException {
        try {
            return  zooKeeper.create(path,data,acls,createMode);
        } catch (KeeperException.NodeExistsException e) {
            // 忽略已存在异常
            return path;
        }
    }
}
