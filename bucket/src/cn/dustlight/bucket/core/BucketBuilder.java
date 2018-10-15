package cn.dustlight.bucket.core;

import cn.dustlight.bucket.core.config.BucketConfig;
import cn.dustlight.bucket.other.Utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class BucketBuilder {

    private Map<String, Class<?>> map;
    Bucket bucketBase;

    public static BucketBuilder create(Bucket bucketBase) {
        return new BucketBuilder(bucketBase);
    }

    public static BucketBuilder create() {
        return create(new BucketBase());
    }

    public BucketBuilder(Bucket bucketBase) {
        this.bucketBase = bucketBase;
        map = new HashMap<>();
    }


    public <T extends BucketWrapper> BucketBuilder addWrapper(String name, Class<T> wrapperClass) {
        map.put(name, wrapperClass);
        return this;
    }

    public Bucket build(BucketConfig bucketConfig) {
        if (map.size() == 0 || bucketConfig.wrappers == null)
            return bucketBase;
        Set<Map.Entry<String, Class<?>>> set = map.entrySet();
        Bucket bucket = bucketBase;
        for (Map.Entry<String, Class<?>> kv : set) {
            try{
                Class<?> wrapperClass = kv.getValue();
                String json = Utils.toJSON(bucketConfig.getWrappers().get(kv.getKey()));
                if (json == null || wrapperClass == null)
                    continue;

                BucketWrapper w = (BucketWrapper) Utils.loadFromJSON(json, wrapperClass);
                if(w.enable == null || !w.enable)
                    continue;
                w.setBucket(bucket);
                bucket = w;
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        return bucket;
    }

    public Bucket initialize(BucketConfig bucketConfig) {
        Bucket bucket = build(bucketConfig);
        bucket.initialize(bucketConfig);
        return bucket;
    }

}
