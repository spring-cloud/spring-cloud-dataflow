package org.springframework.cloud.dataflow.server.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.cloud.dataflow.core.StreamAppDefinition;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.util.Assert;

/**
 * RDBMS implementation of {@link DeploymentIdRepository}.
 *
 * @author Ilayaperumal Gopinathan
 */
public class InMemoryStreamAppPropertiesRepository implements StreamAppPropertiesRepository {

    private Map<String, StreamAppDefinition> data;

    @Override
    public void save(String key, StreamAppDefinition properties) {
        this.getData().put(key, properties);
    }

    @Override
    public StreamAppDefinition findOne(String key) {
        return this.getData().get(key);
    }

    @Override
    public void delete(String key) {
        if(this.findOne(key) != null)
            this.getData().remove(key);
    }

    public Map<String, StreamAppDefinition> getData() {
        if(data == null)
            data = new HashMap<>();
        return data;
    }
}