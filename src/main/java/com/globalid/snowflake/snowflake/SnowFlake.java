package com.globalid.snowflake.snowflake;

import java.math.BigInteger;
/**
 * Twitter_Snowflake<br>
 * SnowFlake的结构如下(每部分用-分开):<br>
 * 0000000000 0000000000 0000000000 0000000000 0 - 0000000000 0 - 0000000000 00 <br>
 * 该结构不包含符号位br>
 * 41位时间截(毫秒级)，注意，41位时间截不是存储当前时间的时间截，而是存储时间截的差值（当前时间截 - 开始时间截)
 * 得到的值），未保证生产的序列号是20位的值，这里的的开始时间截，有个最大值，可由MaxStartTimeUtil的printStartTime方法得到，需要注意开始时间戳一旦确定不可更改。<br>
 * 41位的时间截，可以使用69年，年T = (1L << 41) / (1000L * 60 * 60 * 24 * 365) = 69。为保证开始时间戳满足生产的序列号为20位，将导致可以使用的时间变为31年<br>
 * 11位的数据机器位，可以部署在2048个节点。<br>
 * 12位序列，毫秒内的计数，12位的计数顺序号支持每个节点每毫秒(同一机器，同一时间截)产生4096个ID序号<br>
 * 加起来刚好64位。<br>
 * SnowFlake的优点是，整体上按照时间自增排序，并且整个分布式系统内不会产生ID碰撞(由机器ID作区分)，并且效率较高。
 */
public class SnowFlake {

    // ==============================Fields===========================================
    /** 开始时间截 保障生成的id为20位 */
    private final long twepoch = 353115728625L;
    /** 机器id所占的位数 */
    private final long machineIdBits = 11L;
    /** 支持的最大机器id */
    private final long maxMachineId = -1L ^ (-1L << machineIdBits);
    /** 序列在id中占的位数 */
    private final long sequenceBits = 12L;
    /** 机器ID向左移12位 */
    private final long machineIdShift = sequenceBits;
    /** 时间截向左移22位(5+5+13) */
    private final long timestampLeftShift = sequenceBits + machineIdBits;
    /** 生成序列的掩码， */
    private final long sequenceMask = -1L ^ (-1L << sequenceBits);
    /** 工作机器ID(0~2047) */
    private long machineId;
    /** 毫秒内序列(0~4095) */
    private long sequence = 0L;
    /** 上次生成ID的时间截 */
    private long lastTimestamp = -1L;
    //==============================Constructors=====================================
    /**
     * 构造函数
     * @param machineId 机器id （0~2047）
     */
    public SnowFlake(long machineId) {
        if (machineId > maxMachineId || machineId < 0) {
            throw new IllegalArgumentException(String.format("datacenter Id can't be greater than %d or less than 0", maxMachineId));
        }
        this.machineId = machineId;
    }
    // ==============================Methods==========================================
    /**
     * 获得下一个ID (该方法是线程安全的)
     * @return SnowflakeId
     */
    public synchronized String nextId() {
        long timestamp = timeGen();
        //如果当前时间小于上一次ID生成的时间戳，说明系统时钟回退过这个时候应当抛出异常
        if (timestamp < lastTimestamp) {
            throw new RuntimeException(
                    String.format("Clock moved backwards.  Refusing to generate id for %d milliseconds", lastTimestamp - timestamp));
        }
        //如果是同一时间生成的，则进行毫秒内序列
        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & sequenceMask;
            //毫秒内序列溢出
            if (sequence == 0) {
                //阻塞到下一个毫秒,获得新的时间戳
                timestamp = tilNextMillis(lastTimestamp);
            }
        }
        //时间戳改变，毫秒内序列重置
        else {
            sequence = 0L;
        }

        //上次生成ID的时间截
        lastTimestamp = timestamp;
        BigInteger result =new BigInteger(String.valueOf(timestamp - twepoch),10).shiftLeft((int) timestampLeftShift)
                .add(new BigInteger(String.valueOf(machineId<<machineIdShift|sequence),10));

        return result.toString(10);
    }

    /**
     * 阻塞到下一个毫秒，直到获得新的时间戳
     * @param lastTimestamp 上次生成ID的时间截
     * @return 当前时间戳
     */
    protected long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }
    /**
     * 返回以毫秒为单位的当前时间
     * @return 当前时间(毫秒)
     */
    protected long timeGen() {
        return System.currentTimeMillis();
    }

    public long getMachineId() {
        return machineId;
    }

    public void setMachineId(long machineId) {
        if (machineId > maxMachineId || machineId < 0) {
            throw new IllegalArgumentException(String.format("datacenter Id can't be greater than %d or less than 0", maxMachineId));
        }
        this.machineId = machineId;
    }
}
