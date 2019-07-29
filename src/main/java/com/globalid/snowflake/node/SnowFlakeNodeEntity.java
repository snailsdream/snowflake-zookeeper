package com.globalid.snowflake.node;

import java.io.Serializable;
import java.util.Objects;

/**
 * @Date: 2019/1/10 15:41
 * @Description: 节点信息实体，目前只包含了节点ip和节点的机器id。
 */
public class SnowFlakeNodeEntity implements Serializable {
    private String ip;
    private Integer port;
    private Long machineId;

    public SnowFlakeNodeEntity() {
    }

    public SnowFlakeNodeEntity(String ip, Long machineId) {
        this.ip = ip;
        this.machineId = machineId;
    }
    public SnowFlakeNodeEntity(String ip, Integer port, Long machineId) {
        this.ip = ip;
        this.machineId = machineId;
        this.port = port;
    }

    public SnowFlakeNodeEntity(String hostAddress, Integer port) {
        this.ip = hostAddress;
        this.port = port;
    }

    public Integer getPort() {
        return port;
    }
    public void setPort(Integer port) {
        this.port = port;
    }
    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public Long getMachineId() {
        return machineId;
    }

    public void setMachineId(Long machineId) {
        this.machineId = machineId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SnowFlakeNodeEntity entity = (SnowFlakeNodeEntity) o;
        return Objects.equals(ip, entity.ip) &&
                Objects.equals(port, entity.port) /*&&
                Objects.equals(machineId, entity.machineId)*/;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ip, port);
    }
}
