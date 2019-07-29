package com.globalid.snowflake.factory;

import com.alibaba.fastjson.JSON;
import com.globalid.snowflake.exception.SnowFlakeException;
import com.globalid.snowflake.node.SnowFlakeNodeEntity;
import com.globalid.snowflake.utils.NetUtil;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.retry.RetryNTimes;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.core.env.Environment;

import java.util.ArrayList;
import java.util.List;

/**
 * @Date: 2019/1/10 11:50
 * @Description: zookeeper注册中心实现
 */
public class ZookeeperRegistryCenterFactory implements RegistryCenterFactory{
    private String address;
    private Integer retryNum;
    private Integer sleepMsBetweenRetries;
    private String config_root;
    private Integer max_machine_num;
    private CuratorFramework client;
    private SnowFlakeNodeEntity entity;
    private Logger logger = LoggerFactory.getLogger(ZookeeperRegistryCenterFactory.class);
    public ZookeeperRegistryCenterFactory(String address, int retryNum, int sleepMsBetweenRetries,String config_root,int max_machine_num,int port) {
        this.address = address;
        this.retryNum = retryNum;
        this.sleepMsBetweenRetries = sleepMsBetweenRetries;
        this.config_root = config_root;
        this.max_machine_num = max_machine_num;
        init();
    }

    public ZookeeperRegistryCenterFactory(Environment env) {
        address = env.getProperty("snowflake.registry_center.zookeeper.address");
        retryNum = env.getProperty("snowflake.registry_center.zookeeper.retryNum",Integer.class);
        sleepMsBetweenRetries=env.getProperty("snowflake.registry_center.zookeeper.sleepMsBetweenRetries",Integer.class);
        config_root = env.getProperty("snowflake.registry_center.zookeeper.config_root");
        max_machine_num =  env.getProperty("snowflake.max_machine_num",Integer.class);
        Integer port =  env.getProperty("server.port",Integer.class);
        if (address == null || retryNum == null || sleepMsBetweenRetries ==null || config_root == null || max_machine_num == null) {
            throw new SnowFlakeException( MessageFormatter.arrayFormat("缺少必要的配置信息：address：{}，retryNum：{}，sleepMsBetweenRetries：{}, config_root：{}，max_machine_num：{}，port；{}"
                    ,new Object[]{address,retryNum,sleepMsBetweenRetries,config_root,max_machine_num,port}).getMessage());
        }
        entity = new SnowFlakeNodeEntity(NetUtil.getLocalAddress().getHostAddress(),port);
        init();
    }

    /**
     * @Date: 2019/1/10 14:15
     * @Description:  初始化连接
     */
    private void init() {
        this.client = CuratorFrameworkFactory.newClient(
                address,
                new RetryNTimes(retryNum, sleepMsBetweenRetries)
        );
        // 添加连接监听，解决断线重连zookeeper节点丢失的情况
        client.getConnectionStateListenable().addListener((client, newState) -> {
            switch (newState) {
                case CONNECTED:
                case READ_ONLY:
                    logger.debug("{} 连接成功",address);
                    break;
                case RECONNECTED:
                    logger.debug("{}重连成功",address);
                    if (entity.getMachineId() != null){
                        try {
                            // 当前节点存在，删除节点
                            if (checkSelfNode()){
                                client.delete().forPath(config_root + "/" + entity.getMachineId());
                            }
                        }catch (KeeperException ke){
                            if (KeeperException.Code.NONODE.equals(ke.code())){
                                logger.debug("要删除的节点：{}，不存在，无须删除！",entity.getMachineId());
                            }
                        } catch (Exception e) {
                            // 删除出错machineId置为空
                            entity.setMachineId(null);
                            logger.error(this.getClass().getName(),e);
                        }
                    }
                    try {
                        initMachineEntity();
                    } catch (Exception e) {
                        // 初始化出错machineId置为空
                        entity.setMachineId(null);
                        logger.error(this.getClass().getName(),e);
                    }
                    break;
                case SUSPENDED:
                case LOST:
                    logger.warn("{} 连接丢失",address);
            }
        });
    }
    /**
     * @Date: 2019/1/10 15:43
     * @Description: 获取机器id
     */
    @Override
    public SnowFlakeNodeEntity getSnowFlakeEntity() throws Exception {
        if (entity.getMachineId() ==null) {
            initMachineEntity();
        }
        return entity;
    }
    /**
     * @Date: 2019/1/10 15:43
     * @Description:  获取所有的节点信息
     */
    @Override
    public List<SnowFlakeNodeEntity> getNodes() throws Exception {
       start();
       List<String> nodes = client.getChildren().forPath(config_root);
       List<SnowFlakeNodeEntity> info = new ArrayList();
       nodes.forEach(e -> {
           try {
               info.add(getNodeEntity(Long.valueOf(e)));
           } catch (Exception e1) {
               logger.error("获取---{}---节点数据失败",e,e1);
           }
       });
       return info;
    }

    /**
    * @Date: 2019/1/10 14:15
    * @Description: 初始化机器id
    */
    private void initMachineEntity() throws Exception {
        start();
        logger.debug("/*********初始化机器信息*****/");
        // 判断主目录是否存在
        if (client.checkExists().forPath(config_root) == null){
            try {
                client.create().withMode(CreateMode.PERSISTENT).forPath(config_root);
            } catch (KeeperException e){
                if (KeeperException.Code.NODEEXISTS.equals(e.code())) {
                    logger.warn("{}节点已经被其它系统创建，无须在创建",config_root);
                }else {
                    logger.warn("未知异常，程序无法继续进行：{}",e);
                    entity.setMachineId(null);
                    throw e;
                }
            }
        }
        for (long i=0;i<max_machine_num;i++) {
            String temp = config_root + "/" + i;
            if (client.checkExists().forPath(temp) == null){
                try {
                    entity.setMachineId(i);
                    client.create().withMode(CreateMode.EPHEMERAL).forPath(temp,JSON.toJSONBytes(entity));
                    break;
                }catch (KeeperException e) {
                    entity.setMachineId(null);
                    if (KeeperException.Code.NODEEXISTS.equals(e.code())) {
                        logger.warn("{}节点已经被其它系统创建，无法创建,继续轮询...",temp);
                    }else {
                        logger.warn("未知异常，程序无法继续进行：{}",e);
                        throw e;
                    }
                }
            }else {
                if (entity.equals(getNodeEntity(i))){
                    logger.debug("沿用上次注册的id：{}",i);
                    // 为解决两次连接的session不一致，将上一次的节点删除在重新创建
                    try {
                        client.delete().forPath(config_root + "/" + i);
                    }catch (KeeperException ke){
                        if (KeeperException.Code.NONODE.equals(ke.code())){
                            logger.debug("要删除的节点：{}，不存在，无须删除！",i);
                        }
                    }catch (Exception e) {
                        logger.error("未知异常，系统无法进行:{}",e);
                        entity.setMachineId(null);
                    }
                    // 删除和创建节点的异常要分开处理，不能再同一个catch中
                    try {
                        entity.setMachineId(i);
                        client.create().withMode(CreateMode.EPHEMERAL).forPath(config_root + "/" + i,JSON.toJSONBytes(entity));
                    }catch (KeeperException ke){
                        entity.setMachineId(null);
                        if (KeeperException.Code.NODEEXISTS.equals(ke.code())){
                            logger.debug("要创建的节点：{} 已存在，可能被其它机器创建，继续轮询",i);
                            continue;
                        }
                    }catch (Exception e) {
                        logger.error("未知异常，系统无法进行:{}",e);
                        entity.setMachineId(null);
                    }
                    break;
                }
            }
        }
    }
    private void start() {
        if (client == null){
            throw new SnowFlakeException("zookeeper连接初始化失败:地址"+address);
        }
        // 判断连接状态是否为start
        if (!CuratorFrameworkState.STARTED.equals(client.getState())){
            client.start();
        }
    }
    /**
     * 判断自身的节点在zookeeper是否存在
     * @Date: 2019/1/10 14:15
     * @throws Exception
     */
    private boolean checkSelfNode() throws Exception {
        return client.checkExists().forPath(config_root + "/" +entity.getMachineId())!=null;
    }
    /**
     * @param  id：节点名称
     * @Return  节点数据
     * @Date: 2019/1/10 17:19
     * @Description:  获取节点 数据
     */
    private SnowFlakeNodeEntity getNodeEntity(Long id) throws Exception {
        return  JSON.parseObject(client.getData().forPath(config_root + "/" + id),SnowFlakeNodeEntity.class);
    }
}
