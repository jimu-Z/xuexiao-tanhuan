package com.ruoyi.framework.aspectj;

import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessStatus;
import com.ruoyi.system.domain.SysOperLog;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import com.alibaba.fastjson2.JSON;
import com.ruoyi.common.annotation.Log;
import org.aspectj.lang.JoinPoint;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

class LogBusinessResultTest {
    private final LogAspect aspect = new LogAspect();

    @Test void cacheKeyIsNotExposedInLoggedUrl() {
        assertThat(aspect.safeOperUrl("/monitor/cache/clearCacheKey/login_tokens:secret"))
                .isEqualTo("/monitor/cache/clearCacheKey/[已隐藏]");
        assertThat(aspect.safeOperUrl("/ruoyi-system/talksession/detail/42"))
                .isEqualTo("/ruoyi-system/talksession/detail/42");
    }

    @Test void failedHttpResponseIsNotRecordedAsSuccess() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(404);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest(), response));
        try {
            assertThat(result(null, null).getStatus()).isEqualTo(BusinessStatus.FAIL.ordinal());
        } finally { RequestContextHolder.resetRequestAttributes(); }
    }

    @Test void sensitiveTalkFieldsAreExcludedEvenWhenNested() {
        String json = JSON.toJSONString(Map.of("sessionId", 42, "nested", Map.of(
                "talkContent", "private text", "studentFeedback", "private feedback", "token", "private token")),
                aspect.excludePropertyPreFilter(new String[0]));
        assertThat(json).contains("sessionId", "42").doesNotContain("private text", "private feedback", "private token");
    }

    @Log(title = "测试记录")
    void loggedMethod() { }

    @Test void requestAndResponseLogRetainIdentifiersButExcludeTalkContent() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/test");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, new MockHttpServletResponse()));
        try {
            JoinPoint joinPoint = mock(JoinPoint.class);
            when(joinPoint.getArgs()).thenReturn(new Object[] { Map.of("sessionId", 42, "talkContent", "request-secret") });
            SysOperLog log = new SysOperLog();
            log.setRequestMethod("POST");
            Log annotation = getClass().getDeclaredMethod("loggedMethod").getAnnotation(Log.class);
            aspect.getControllerMethodDescription(joinPoint, annotation, log,
                    AjaxResult.success(Map.of("sessionId", 42, "studentFeedback", "response-secret")));
            assertThat(log.getOperParam()).contains("sessionId", "42").doesNotContain("request-secret");
            assertThat(log.getJsonResult()).contains("sessionId", "42").doesNotContain("response-secret");
        } finally { RequestContextHolder.resetRequestAttributes(); }
    }

    @Test void rejectedBusinessOperationIsFailureWithReason() {
        SysOperLog log = result(null, AjaxResult.error("角色名称已存在"));
        assertThat(log.getStatus()).isEqualTo(BusinessStatus.FAIL.ordinal());
        assertThat(log.getErrorMsg()).isEqualTo("角色名称已存在");
    }

    @Test void validationWarningAndForbiddenResultAreFailures() {
        assertThat(result(null, AjaxResult.warn("已分配不能删除")).getStatus()).isEqualTo(1);
        assertThat(result(null, AjaxResult.error(403, "无权操作")).getStatus()).isEqualTo(1);
    }

    @Test void successfulResultKeepsSuccessEvenWithErrorWordsInData() {
        SysOperLog log = result(null, AjaxResult.success(Map.of("code", 500, "msg", "历史失败记录")));
        assertThat(log.getStatus()).isZero();
        assertThat(log.getErrorMsg()).isNull();
    }

    @Test void untypedPayloadAndNoResponseDoNotBecomeFailures() {
        assertThat(result(null, null).getStatus()).isZero();
        assertThat(result(null, Map.of("code", 500)).getStatus()).isZero();
        assertThat(result(null, new AjaxResult()).getStatus()).isZero();
    }

    @Test void thrownExceptionTakesPrecedenceOverReturnedResult() {
        SysOperLog log = result(new IllegalStateException("真实异常"), AjaxResult.error("返回提示"));
        assertThat(log.getStatus()).isEqualTo(1);
        assertThat(log.getErrorMsg()).isEqualTo("真实异常");
    }

    @Test void failureMessageRemainsBoundedForDatabaseColumn() {
        assertThat(result(null, AjaxResult.error("测".repeat(2500))).getErrorMsg()).hasSize(2000);
    }

    private SysOperLog result(Exception error, Object response) {
        SysOperLog log = new SysOperLog();
        aspect.setOperationResult(log, error, response);
        return log;
    }
}
