package org.springframework.cloud.dataflow.server.repository;

import org.springframework.cloud.dataflow.core.StreamAppDefinition;
import org.springframework.stereotype.Repository;

import java.util.Map;

/**
 * Created by A626200 on 23/11/2016.
 */
@Repository
public interface StreamAppPropertiesRepository extends org.springframework.data.repository.Repository<String, StreamAppDefinition>{
    /**
     * Associates a given runtime deployment key with his properties.
     *
     * @param key the runtime deployment key
     * @param properties the app deployment properties
     */
    void save(String key, StreamAppDefinition properties);

    /**
     * Find an identifier by its key.
     *
     * @param key the runtime deployment key
     * @return the app deployment properties
     */
    StreamAppDefinition findOne(String key);

    /**
     * Delete the entries associated with the runtime deployment key.
     *
     * @param key the app deployment key
     */
    void delete(String key);
}
