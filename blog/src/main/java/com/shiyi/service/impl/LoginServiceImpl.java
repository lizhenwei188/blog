package com.shiyi.service.impl;

import cn.dev33.satoken.stp.SaLoginModel;
import cn.dev33.satoken.stp.StpUtil;
import com.google.code.kaptcha.Producer;
import com.shiyi.common.ResponseResult;
import com.shiyi.common.Constants;
import com.shiyi.entity.User;
import com.shiyi.mapper.UserMapper;
import com.shiyi.service.LoginService;
import com.shiyi.util.AesEncryptUtils;
import com.shiyi.dto.LoginDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.shiyi.common.ResultCode.ERROR_PASSWORD;

/**
 * @author blue
 * @description:
 * @date 2021/7/30 14:59
 */
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class LoginServiceImpl implements LoginService {

    @Resource(name = "captchaProducerMath")
    private Producer captchaProducerMath;

    private final UserMapper userMapper;

    @Override
    public Map<String, String> getCode(HttpServletResponse response) throws IOException {
        Map<String, String> result = new HashMap<String,String>();
      /*  // 生成验证码的UUID
        String uuid = UUIDUtil.getUuid();
        String capStr = null;
        String code = null;
        BufferedImage image = null;
        // 生成验证码
        String capText = captchaProducerMath.createText();
        capStr = capText.substring(0, capText.lastIndexOf("@"));
        code = capText.substring(capText.lastIndexOf("@") + 1);
        image = captchaProducerMath.createImage(capStr);
        redisCache.setCacheObject(RedisConstants.CAPTCHA_CODE+uuid,code,RedisConstants.CAPTCHA_EXPIRATION, TimeUnit.MINUTES);
        // 转换流信息写出
        FastByteArrayOutputStream os = new FastByteArrayOutputStream();
        ImageIO.write(image, "jpg", os);
        result.put("uuid",uuid);
        result.put("img", Base64.encode(os.toByteArray()));*/
        return result;
    }

    @Override
    public ResponseResult login(LoginDTO vo) {
        //校验用户名和密码
        User user = userMapper.selectNameAndPassword(vo.getUsername(), AesEncryptUtils.aesEncrypt(vo.getPassword()));
        Assert.isTrue(user != null, ERROR_PASSWORD.getDesc());

        if (vo.getRememberMe()) {
            StpUtil.login(user.getId().longValue(),new SaLoginModel().setTimeout(60 * 60 * 24 * 7));
        }else {
            StpUtil.login(user.getId().longValue());
        }
        StpUtil.getSession().set(Constants.CURRENT_USER,userMapper.getById(user.getId()));
        return ResponseResult.success(StpUtil.getTokenValue());
    }
}
