package com.ruoyi.web.controller.monitor;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.system.service.impl.SysUserOnlineServiceImpl;

class OnlineExpiredTokenAuditTest {
    @Test void ipFilterFailsWhenTokenExpiresAfterKeyScan() {
        var controller = new SysUserOnlineController();
        var redis = mock(RedisCache.class);
        ReflectionTestUtils.setField(controller, "redisCache", redis);
        ReflectionTestUtils.setField(controller, "userOnlineService", new SysUserOnlineServiceImpl());
        when(redis.keys(anyString())).thenReturn(List.of("login_tokens:simulated"));
        when(redis.getCacheObject(anyString())).thenReturn(null);

        assertThatThrownBy(() -> controller.list("127.0.0.1", null))
                .isInstanceOf(NullPointerException.class);
    }
}
