package com.example.emos.wx.service.impl;

import com.example.emos.wx.db.dao.MessageDao;
import com.example.emos.wx.db.dao.MessageRefDao;
import com.example.emos.wx.db.pojo.MessageEntity;
import com.example.emos.wx.db.pojo.MessageRef;
import com.example.emos.wx.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;

@Service
public class MessageServiceImpl implements MessageService {

    @Autowired
    private MessageDao messageDao;

    @Autowired
    private MessageRefDao messageRefDao;

    @Override
    public String insertMessage(MessageEntity entity) {
        String insert = messageDao.insert(entity);
        return insert;
    }

    @Override
    public String insertRef(MessageRef entity) {
        String insert = messageRefDao.insert(entity);
        return insert;
    }

    @Override
    public long searchUnreadCount(int userId) {
        return messageRefDao.searchUnreadCount(userId);
    }

    @Override
    public long searchLastCount(int userId) {
        return messageRefDao.searchLastCount(userId);
    }

    @Override
    public List<HashMap> searchMessageByPage(int userId, long start, int length) {
        List<HashMap> hashMaps = messageDao.searchMessageByPage(userId, start, length);
        return hashMaps;
    }

    @Override
    public HashMap searchMessageById(String id) {
        HashMap map = messageDao.searchMessageById(id);
        return map;
    }

    @Override
    public long updateUnreadMessage(String id) {
        long l = messageRefDao.updateUnreadMessage(id);
        return l;
    }

    @Override
    public long deleteMessageRefById(String id) {

        return messageRefDao.deleteMessageRefById(id);
    }

    @Override
    public long deleteUserMessageRef(int userId) {
        return messageRefDao.deleteUserMessageRef(userId);
    }
}
