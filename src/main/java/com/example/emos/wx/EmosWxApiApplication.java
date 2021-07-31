package com.example.emos.wx;

import cn.hutool.core.util.StrUtil;
import com.example.emos.wx.db.SystemConstants;
import com.example.emos.wx.db.dao.SysConfigDao;
import com.example.emos.wx.db.pojo.SysConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

import javax.annotation.PostConstruct;
import java.io.File;
import java.lang.reflect.Field;
import java.util.List;

@SpringBootApplication
@ServletComponentScan
@Slf4j
@EnableAsync
public class EmosWxApiApplication {

    @Autowired
    private SystemConstants systemConstants;

    @Autowired
    private SysConfigDao sysConfigDao;

    @Value("${emos.image-folder}")
    private String imageFolder;

    public static void main(String[] args) {
        SpringApplication.run(EmosWxApiApplication.class, args);
    }


    /**
     * 初始化方法  run以后就执行了 关键是@PostConstruct
     */
    @PostConstruct
    public void init() {
        List<SysConfig> list = sysConfigDao.selectAllParm();

        list.forEach(one -> {
            String key = one.getParamKey(); // 相关签到时间的名词
            String value = one.getParamValue(); // 时间
            key = StrUtil.toCamelCase(key); // 转换为驼峰命名法
            try {
                //getDeclaredFiled 仅能获取类本身的属性成员（包括私有、共有、保护）
                //getField 仅能获取类(及其父类可以自己测试) public属性成员
                Field field = systemConstants.getClass().getDeclaredField(key);
                field.set(systemConstants, value);
            } catch (Exception e) {
                log.error("执行异常", e);
            }
        });
        new File(imageFolder).mkdirs();
    }

}
