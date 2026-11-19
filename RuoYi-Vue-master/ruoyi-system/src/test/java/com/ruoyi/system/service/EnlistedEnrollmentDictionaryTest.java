package com.ruoyi.system.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;
import com.ruoyi.common.core.domain.entity.SysDictData;
import com.ruoyi.common.utils.DictUtils;
import com.ruoyi.system.mapper.SysDictDataMapper;
import com.ruoyi.system.service.impl.SysDictTypeServiceImpl;
import com.ruoyi.talk.service.impl.StudentImportOptions;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;

class EnlistedEnrollmentDictionaryTest {
    @Test
    @SuppressWarnings("unchecked")
    void startupSuppliesEnlistedStatusBeforeLoadingCacheAndChineseImportsUseItsCode() {
        List<SysDictData> stored = new ArrayList<>();
        SysDictDataMapper mapper = mock(SysDictDataMapper.class, invocation -> {
            if (invocation.getMethod().getName().equals("insertEnlistedEnrollmentIfMissing")) {
                stored.add(invocation.getArgument(0)); return 1;
            }
            if (invocation.getMethod().getName().equals("selectDictDataList")) return List.copyOf(stored);
            if (invocation.getMethod().getName().equals("selectDictDataByType"))
                return "enrollment_status".equals(invocation.getArgument(0)) ? List.copyOf(stored) : List.of();
            return RETURNS_DEFAULTS.answer(invocation);
        });
        SysDictTypeServiceImpl service = new SysDictTypeServiceImpl();
        ReflectionTestUtils.setField(service, "dictDataMapper", mapper);
        try (MockedStatic<DictUtils> cache = mockStatic(DictUtils.class)) {
            service.init();
            assertThat(stored).hasSize(1);
            assertThat(stored.get(0).getDictLabel()).isEqualTo("入伍");
            assertThat(stored.get(0).getStatus()).isEqualTo("0");
            cache.verify(() -> DictUtils.setDictCache(eq("enrollment_status"), anyList()));
            StudentImportOptions options = new StudentImportOptions();
            ReflectionTestUtils.setField(options, "dictDataMapper", mapper);
            Map<String, String> row = new HashMap<>(Map.of("enrollment_status", "入伍"));
            options.normalize(row, options.load());
            assertThat(row.get("enrollment_status")).isEqualTo("enlisted");
        }
    }
}
