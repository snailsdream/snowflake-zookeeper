package com.globalid.snowflake.factory;

import com.globalid.snowflake.node.SnowFlakeNodeEntity;
import com.globalid.snowflake.utils.NetUtil;
import org.springframework.core.env.Environment;

import java.util.ArrayList;
import java.util.List;

/**
 * @Date: 2019/1/11 13:14
 * @Description: 不用注册中心，单节点部署
 */
public class NoRegistryCenterFactory implements RegistryCenterFactory {
    private SnowFlakeNodeEntity entity;
    public NoRegistryCenterFactory(Environment env) {
        Integer port = env.getProperty("server.port",Integer.class);
        if (port == null) {
            port = 8080;
        }
        entity = new SnowFlakeNodeEntity(NetUtil.getLocalAddress().getHostAddress(),port,0L);
    }

    @Override
    public SnowFlakeNodeEntity getSnowFlakeEntity(){
        return entity;
    }

    @Override
    public List<SnowFlakeNodeEntity> getNodes() {
        List<SnowFlakeNodeEntity> list = new ArrayList<>();
        list.add(entity);
        return list;
    }
}
