package com.example.emos.wx.task;

import com.example.emos.wx.db.pojo.MessageEntity;
import com.example.emos.wx.db.pojo.MessageRef;
import com.example.emos.wx.exception.EmosException;
import com.example.emos.wx.service.MessageService;
import com.rabbitmq.client.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class MessageTask {

    @Autowired
    private ConnectionFactory connectionFactory;    // mq

    @Autowired
    private MessageService messageService;

    public void send(String topic, MessageEntity entity) {
        // 插入消息 ？ 为什么要插入到mongodb 不是说 接收者存吗
        String id = messageService.insertMessage(entity);
        try (//接收消息数据
             // 连接 Channel
             Connection connection = connectionFactory.newConnection();
             Channel channel = connection.createChannel(); // jdbc 联想一下mysql
        ) {
            // 从队列中获取消息，不自动确认
            channel.queueDeclare(topic, true, false, false, null);
            //Topic中有多少条数据未知，所以使用死循环接收数据，直到接收不到消息，退出死循环
            HashMap map = new HashMap(); // 属性?
            map.put("messageId", id);
            //创建AMQP协议参数对象，添加附加属性 附加属性 那主消息在哪
            AMQP.BasicProperties properties = new
                    AMQP.BasicProperties().builder().headers(map).build();
            // 发送消息 entity.getMsg().getBytes() 消息正文
            channel.basicPublish("",
                    topic,
                    properties,
                    entity.getMsg().getBytes()); // 这里是主消息
            log.debug("消息发送成功");
        } catch (Exception e) {
            log.error("执行异常");
            throw new EmosException("像mq发送消息失败");
        }
    }

    @Async // 异步 注册方法用了
    public void sendAsync(String topic, MessageEntity entity) {
        send(topic, entity);
    }

    /**
     * 接收消息后还要放到mongodb
     *
     * @return
     */
    public int receive(String topic) {
        int i = 0;
        try (//接收消息数据
             Connection connection = connectionFactory.newConnection(); // 新建一个链接
             Channel channel = connection.createChannel(); // jdbc 联想一下mysql    链接channel
        ) {
            // 1 队列名字 &&  2 是否持久化
            // 3 队列中的数据消费完成后是否自动删除队列 && 是否排外的
            // 4 是否等待服务器返回
            channel.queueDeclare(topic, true, false, false, null);
            while (true) {
                GetResponse getResponse = channel.basicGet(topic, false);//ack应答是接受者收到消息后的回复？？
                if (getResponse != null) {
                    AMQP.BasicProperties properties = getResponse.getProps();
                    Map<String, Object> map = properties.getHeaders();
                    // header 附加消息
                    String messageId = map.get("messageId").toString();
                    // 消息主体
                    byte[] body = getResponse.getBody();
                    String message = new String(body);
                    log.debug("从rabbitmq接收的消息" + message);
                    MessageRef entity = new MessageRef();
                    entity.setMessageId(messageId);
                    entity.setReceiverId(Integer.parseInt(topic));
                    entity.setReadFlag(false);
                    entity.setLastFlag(true);
                    messageService.insertRef(entity);
                    // 返回ack应答
                    long deliveryTag = getResponse.getEnvelope().getDeliveryTag();
                    channel.basicAck(deliveryTag, false);
                } else {
                    break; // 没收到消息 退出
                }
            }
        } catch (Exception e) {
            log.error("执行异常");
            throw new EmosException("像mq发送消息失败");
        }
        return i;
    }

    @Async // 异步
    public int receiveAsync(String topic) {
        return receive(topic);
    }

    public void deleteQueue(String topic) {
        try (Connection connection = connectionFactory.newConnection();
             Channel channel = connection.createChannel()) {
            channel.queueDelete(topic);
            log.debug("消息队列成功删除");
        } catch (Exception e) {
            log.error("删除队列失败", e);
            throw new EmosException("删除队列失败");
        }
    }


    @Async // 异步
    public void deleteQueueAsync(String topic) {
        deleteQueue(topic);
    }

}
