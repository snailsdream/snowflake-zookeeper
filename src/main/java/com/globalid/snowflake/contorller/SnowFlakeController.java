package com.globalid.snowflake.contorller;


import com.globalid.snowflake.exception.SnowFlakeException;
import com.globalid.snowflake.factory.RegistryCenterFactory;
import com.globalid.snowflake.node.SnowFlakeNodeEntity;
import com.globalid.snowflake.snowflake.SnowFlake;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

@RestController
public class SnowFlakeController {
    @Autowired
    private SnowFlake snowFlake;
    @Autowired
    private RegistryCenterFactory registryCenterFactory;
    /**
     * @param  length； 获取序列号的个数，默认为1
     * @Return 返回length个序列号
     * @Date: 2019/1/10 17:13
     * @Description: 获取序列id
     */
    @RequestMapping(value = {"nextIds","nextIds/{length}"},method = GET )
    public List getIds(@PathVariable(required = false) Integer length) {
        length = length == null ? 1 : length;
        List list=new ArrayList();
        try {
            SnowFlakeNodeEntity entity = registryCenterFactory.getSnowFlakeEntity();
            Long id = entity.getMachineId();
            if (id == null) {
                throw new SnowFlakeException("程序异常，获取机器id失败！");
            }
            snowFlake.setMachineId(id);
        }catch (SnowFlakeException se){
            throw se;
        }catch (Exception e) {
            throw new SnowFlakeException("程序异常，获取序列号失败");
        }
        while (length>0){
            list.add(snowFlake.nextId());
            length--;
        }
        return list;
    }

    @Autowired
    private RegistryCenterFactory centerFactory;
    /**
     * @Return  所有在线节点的信息
     * @Date: 2019/1/10 17:14
     * @Description: 获取各个节点的信息
     */
    @RequestMapping(value = "nodeInfo",method = GET )
    public List<SnowFlakeNodeEntity> getAllNode() {
        try {
            return centerFactory.getNodes();
        } catch (Exception e) {
            return null;
        }
    }
}
