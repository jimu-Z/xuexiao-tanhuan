package com.ruoyi.framework.web.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.system.service.ISysConfigService;

class CaptchaMissingCodeAuditTest {
    @Test void loginMissingCodeConsumesExistingCaptchaThenThrowsNullPointer() {
        var cache = mock(RedisCache.class); var config = mock(ISysConfigService.class);
        var service = new SysLoginService();
        ReflectionTestUtils.setField(service, "redisCache", cache);
        ReflectionTestUtils.setField(service, "configService", config);
        when(config.selectCaptchaEnabled()).thenReturn(true);
        when(cache.getCacheObject(CacheConstants.CAPTCHA_CODE_KEY + "simulated-id")).thenReturn("abcd");
        assertThatThrownBy(() -> service.validateCaptcha("simulated", null, "simulated-id"))
                .isInstanceOf(NullPointerException.class);
        verify(cache).deleteObject(CacheConstants.CAPTCHA_CODE_KEY + "simulated-id");
    }

    @Test void registerMissingCodeConsumesExistingCaptchaThenThrowsNullPointer() {
        var cache = mock(RedisCache.class); var service = new SysRegisterService();
        ReflectionTestUtils.setField(service, "redisCache", cache);
        when(cache.getCacheObject(CacheConstants.CAPTCHA_CODE_KEY + "simulated-id")).thenReturn("abcd");
        assertThatThrownBy(() -> service.validateCaptcha("simulated", null, "simulated-id"))
                .isInstanceOf(NullPointerException.class);
        verify(cache).deleteObject(CacheConstants.CAPTCHA_CODE_KEY + "simulated-id");
    }
}
