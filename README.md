# snowflake-zookeeper
基于snowflake和zookeeper的分布式id生成器
#说明
snowflake算法的id生成主要有以下两点问题：
1. 依赖时钟
1. 多个节点的机器id确认
本文主要解决多个节点的机器id确认，并且应用重启不会使机器id重复。本例中使用的zookeeper实现的，但也可以基于其它实现，如redis等。
