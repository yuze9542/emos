package com.example.emos.wx.db.pojo;

import java.io.Serializable;
import lombok.Data;

/**
 * tb_face_model
 * @author 
 */
@Data
public class TbFaceModel implements Serializable {
    /**
     * 主键值
     */
    private Integer id;

    /**
     * 用户ID
     */
    private Integer userId;

    /**
     * 人脸图片路径
     */
    private String facePath;

    private static final long serialVersionUID = 1L;
}