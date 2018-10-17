package test;

import cn.dustlight.bucket.core.Bucket;
import cn.dustlight.bucket.core.BucketBuilder;
import cn.dustlight.bucket.core.config.BucketConfig;
import cn.dustlight.bucket.core.config.Config;
import cn.dustlight.bucket.other.CommonFuture;
import cn.dustlight.bucket.wrappers.HttpApiWrapper;
import cn.dustlight.bucket.wrappers.ZooKeeperWrapper;

import java.io.IOException;
import java.util.Scanner;

public class Test {

    public static void main(String[] args) throws IOException {

        BucketConfig config = Config.load("bucket.json", BucketConfig.class);

        CommonFuture<Bucket> bucket = BucketBuilder.create()
                .addWrapper("zookeeper", ZooKeeperWrapper.class)
                .addWrapper("http", HttpApiWrapper.class)
                .initialize(config);

        Scanner scanner = new Scanner(System.in);


        while (scanner.hasNext()){
            String cmd = scanner.next();
            if(cmd.equals("quit"))
                break;
        }
        scanner.close();

    }
}
