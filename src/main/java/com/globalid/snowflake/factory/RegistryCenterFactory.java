package com.globalid.snowflake.factory;

import com.globalid.snowflake.node.SnowFlakeNodeEntity;

import java.util.List;

/**
 * @Date: 2019/1/10 11:44
 * @Description: 注册中心工厂
 */
public interface RegistryCenterFactory {
    SnowFlakeNodeEntity getSnowFlakeEntity() throws Exception;
    List<SnowFlakeNodeEntity> getNodes() throws Exception;

}
