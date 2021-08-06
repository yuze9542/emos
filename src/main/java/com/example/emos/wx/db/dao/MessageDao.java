package com.example.emos.wx.db.dao;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.json.JSONObject;
import com.example.emos.wx.db.pojo.MessageEntity;
import com.example.emos.wx.db.pojo.MessageRef;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

@Repository // 不放数据库 所以不用mapper注解
public class MessageDao {

    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * 保存到MongoDB上
     *
     * @param entity
     * @return
     */
    public String insert(MessageEntity entity) {
        Date sendTime = entity.getSendTime();
        sendTime = DateUtil.offset(sendTime, DateField.HOUR_OF_DAY, 8);
        entity.setSendTime(sendTime);
        entity = mongoTemplate.save(entity);
        return entity.get_id();
    }

    /**
     * @param userId
     * @param start
     * @param length
     * @return
     */
    public List<HashMap> searchMessageByPage(int userId, long start, int length) {

        JSONObject json = new JSONObject();
        json.set("$toString", "$_id");      // 放到string上
        // 联合查询 ? 相当于
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.addFields().addField("id").withValue(json).build(),
                Aggregation.lookup("message_ref", "id", "messageId", "ref"),
                Aggregation.match(Criteria.where("ref.receiverId").is(userId)), // where 语句
                Aggregation.sort(Sort.by(Sort.Direction.DESC, "sendTime")),
                Aggregation.skip(start),
                Aggregation.limit(length)
        );
        AggregationResults<HashMap> message = mongoTemplate.aggregate(aggregation, "message", HashMap.class);
        List<HashMap> list = message.getMappedResults();    // 拿到信息了
        list.forEach(one -> {
            List<MessageRef> refList = (List<MessageRef>) one.get("ref");
            MessageRef entity = refList.get(0);
            boolean isRead = entity.getReadFlag();
            String refId = entity.get_id();
            one.put("readFlag", isRead);
            one.put("refId", refId);
            one.remove("ref");
            one.remove("_id"); //messageRef Id
            Date sendTime = (Date) one.get("sendTime");
            sendTime = DateUtil.offset(sendTime, DateField.HOUR, -8);
            String today = DateUtil.today();
            //如果是今天的消息，只显示发送时间，不需要显示日期
            // today 和 DateUtil.date(sendTime).toDateStr() 都是hashmap
            if (today.equals(DateUtil.date(sendTime).toDateStr())) {
                one.put("sendTime", DateUtil.format(sendTime, "HH:mm"));
            }
            //如果是以往的消息，只显示日期，不显示发送时间
            else {
                one.put("sendTime", DateUtil.format(sendTime, "yyyy/MM/dd"));
            }
        });
        return list;
    }


    public HashMap searchMessageById(String id) {
        HashMap map = mongoTemplate.findById(id, HashMap.class, "message");
        Date sendTime = (Date) map.get("sendTime");
        DateTime offset = DateUtil.offset(sendTime, DateField.HOUR, -8);
        map.replace("sendTime", DateUtil.format(offset, "yyyy-MM-dd HH:mm"));
        return map;
    }

}
