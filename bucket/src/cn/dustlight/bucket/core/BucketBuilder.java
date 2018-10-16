package cn.dustlight.bucket.core;

import cn.dustlight.bucket.core.config.BucketConfig;
import cn.dustlight.bucket.other.CommonFuture;
import cn.dustlight.bucket.other.Utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A Bucket Builder
 */
public class BucketBuilder {

    /**
     * Wrapper map
     */
    private Map<String, Class<?>> map;

    /**
     * Root Bucket
     */
    private Bucket bucketBase;

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

    /**
     * Add a Wrapper
     *
     * @param name         key of config
     * @param wrapperClass class of wrapper
     * @param <T>
     * @return
     */
    public <T extends BucketWrapper> BucketBuilder addWrapper(String name, Class<T> wrapperClass) {
        map.put(name, wrapperClass);
        return this;
    }

    /**
     * Build Bucket
     *
     * @param bucketConfig configure of Bucket
     * @return
     */
    public Bucket build(BucketConfig bucketConfig) {
        if (map.size() == 0 || bucketConfig.wrappers == null)
            return bucketBase;
        Set<Map.Entry<String, Class<?>>> set = map.entrySet();
        Bucket bucket = bucketBase;
        for (Map.Entry<String, Class<?>> kv : set) {
            try {
                Class<?> wrapperClass = kv.getValue();
                String json = Utils.toJSON(bucketConfig.getWrappers().get(kv.getKey()));
                if (json == null || wrapperClass == null)
                    continue;

                BucketWrapper w = (BucketWrapper) Utils.loadFromJSON(json, wrapperClass);
                if (w.enable == null || !w.enable)
                    continue;
                w.setBucket(bucket);
                bucket = w;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return bucket;
    }

    /**
     * Build and initialize the Bucket
     *
     * @param bucketConfig configure of Bucket
     * @return
     */
    public CommonFuture<Bucket> initialize(BucketConfig bucketConfig) {
        Bucket bucket = build(bucketConfig);
        return bucket.initialize(bucketConfig).start();
    }

}
