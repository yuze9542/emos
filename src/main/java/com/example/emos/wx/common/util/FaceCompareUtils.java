package com.example.emos.wx.common.util;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.exceptions.ServerException;
import com.aliyuncs.profile.DefaultProfile;

import java.io.IOException;

import com.aliyuncs.facebody.model.v20191230.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
@Component
public class FaceCompareUtils {

    @Value("${aliyun.alikey}")
    private String key;

    @Value("${aliyun.alisecret}")
    private  String secret;

    public  int compare(String imgA, String imgB) throws IOException {
        DefaultProfile profile = DefaultProfile.getProfile("cn-shanghai",
                key,
                secret);
        IAcsClient client = new DefaultAcsClient(profile);

        CompareFaceRequest request = new CompareFaceRequest();
        imgA = imgA.replace("\n","");
        imgB = imgB.replace("\n","");

        request.setImageDataA(imgA);
        request.setImageDataB(imgB);
        request.setQualityScoreThreshold((float) 60);
        try {
            CompareFaceResponse response = client.getAcsResponse(request);
            int confidence = Math.round(response.getData().getConfidence());
            return confidence;

        } catch (ServerException e) {
            e.printStackTrace();
        } catch (ClientException e) {
            System.out.println("ErrCode:" + e.getErrCode());
            System.out.println("ErrMsg:" + e.getErrMsg());
            System.out.println("RequestId:" + e.getRequestId());
        }

        return 0;
    }

}

