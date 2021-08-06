package com.example.emos.wx.db.dao;

import com.example.emos.wx.db.pojo.MessageRef;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

@Repository
public class MessageRefDao {

    @Autowired
    private MongoTemplate mongoTemplate;

    public String insert(MessageRef entity) {
        entity = mongoTemplate.save(entity);
        return entity.get_id(); //记录主键值
    }

    /**
     * 查询未读消息数量
     *
     * @param userId
     * @return
     */
    public long searchUnreadCount(int userId) {
        Query query = new Query();  //查询
        // Query 是不是相当于 Example
        // where 语句
        query.addCriteria(
                Criteria.where("readFlag").is(false)
                        .and("receiverId").is(userId)
        );
//        mongoTemplate.find(query,MessageRef.class); 是查询语句
        long count = mongoTemplate.count(query, MessageRef.class);
        return count;
    }

    /**
     * 查询新接收消息数量
     *
     * @param userId
     * @return
     */
    public long searchLastCount(int userId) {
        Query query = new Query();
        // lastFlag 是做什么的 ？？？
        // 这个方法不是查询 这个是把状态变成已接收的消息 已接收 不是已读
        query.addCriteria(
                Criteria.where("lastFlag").is(true)
                        .and("receiverId").is(userId)
        );    // 添加查询条件
        Update update = new Update();   // 更新语句 为什么要更新 因为已读了 要更新状态

        update.set("lastFlag", false); // lastFlag 是 是否为新接收的消息
        UpdateResult result = mongoTemplate.updateMulti(query, update, "message_ref");
        long rows = result.getModifiedCount();
        return rows;
    }

    /**
     * 把未读消息变更为已读消息
     *
     * @param id
     * @return
     */
    public long updateUnreadMessage(String id) {
        Query query = new Query();      // 选择哪个一个
        query.addCriteria(Criteria.where("_id").is(id));
        Update update = new Update();       // 更新什么   
        update.set("readFlag", true);
        UpdateResult result = mongoTemplate.updateFirst(query, update, "message_ref");
        long rows = result.getModifiedCount();
        return rows;
    }

    /**
     * 根据ID删除ref消息
     * * @param id
     *
     * @return
     */
    public long deleteMessageRefById(String id) {   // id是消息id
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(id));
        DeleteResult result = mongoTemplate.remove(query, "message_ref");
        long rows = result.getDeletedCount();
        return rows;
    }


    public long deleteUserMessageRef(int userId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("receiverId").is(userId));
        DeleteResult result = mongoTemplate.remove(query, "message_ref");
        long rows = result.getDeletedCount();
        return rows;
    }
}
