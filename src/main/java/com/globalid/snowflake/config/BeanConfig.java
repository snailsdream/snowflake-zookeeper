package com.globalid.snowflake.config;


import com.globalid.snowflake.exception.SnowFlakeException;
import com.globalid.snowflake.factory.NoRegistryCenterFactory;
import com.globalid.snowflake.factory.RegistryCenterFactory;
import com.globalid.snowflake.factory.ZookeeperRegistryCenterFactory;
import com.globalid.snowflake.node.SnowFlakeNodeEntity;
import com.globalid.snowflake.snowflake.SnowFlake;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;

@Configuration
public class BeanConfig {
    @Autowired
    private Environment env;
    @Autowired
    private RegistryCenterFactory registryCenterFactory;
    @Bean("snowFlake")
    @Lazy
    public SnowFlake getSnowFlake() throws Exception {
        SnowFlakeNodeEntity  entity= registryCenterFactory.getSnowFlakeEntity();
        if (entity.getMachineId() == null) {
            throw new SnowFlakeException("初始化机器id失败!");
        }
        return new SnowFlake(entity.getMachineId());
    }
    @Bean
    public RegistryCenterFactory getRegistryCenterFactory() throws SnowFlakeException {
        String snowflake_registry_center = env.getProperty("snowflake.registry_center.default");
        if (snowflake_registry_center == null){
            // 默认没有注册中心，单节点部署
            snowflake_registry_center = "none";
        }
        switch (snowflake_registry_center) {
            case "zookeeper":
               return new ZookeeperRegistryCenterFactory(env);
            case "none":
                return new NoRegistryCenterFactory(env);
            default:
               throw new SnowFlakeException("未配置注册中心，请配置注册中心！");
        }
    }
}
