package com.ruoyi.talk.service;

import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.test.util.ReflectionTestUtils;
import com.ruoyi.talk.service.impl.*;
import com.ruoyi.system.mapper.SysDictDataMapper;
import com.ruoyi.common.core.domain.entity.SysDictData;
import java.util.*;

final class StudentImportTestSupport {
    static PlatformTransactionManager transactionManager(TalkStudentServiceImpl service) {
        dictionary(service);
        PlatformTransactionManager transactions=mock(PlatformTransactionManager.class);
        when(transactions.getTransaction(any())).thenAnswer(call->new SimpleTransactionStatus());
        ReflectionTestUtils.setField(service,"transactionManager",transactions);
        return transactions;
    }
    static void unrelatedRelations(TalkStudentServiceImpl service) {
        transactionManager(service);
        ReflectionTestUtils.setField(service,"importRelations",mock(StudentImportRelationsService.class));
    }

    static SysDictDataMapper dictionary(Object target) {
        SysDictDataMapper mapper = mock(SysDictDataMapper.class);
        when(mapper.selectDictDataByType("sys_user_sex")).thenReturn(entries("0", "男", "1", "女", "2", "未知"));
        when(mapper.selectDictDataByType("political_status")).thenReturn(entries("people", "群众", "league_member", "共青团员"));
        when(mapper.selectDictDataByType("enrollment_status")).thenReturn(entries("active", "在读", "suspended", "休学"));
        when(mapper.selectDictDataByType("mental_health_status")).thenReturn(entries("normal", "正常", "mild_warning", "轻度干预"));
        when(mapper.selectDictDataByType("poverty_level")).thenReturn(entries("none", "无", "general", "家庭经济一般困难", "severe", "C.家庭经济一般困难"));
        StudentImportOptions options = new StudentImportOptions();
        ReflectionTestUtils.setField(options, "dictDataMapper", mapper);
        ReflectionTestUtils.setField(target, "importOptions", options);
        return mapper;
    }

    static List<SysDictData> entries(String... pairs) {
        List<SysDictData> entries = new ArrayList<>();
        for (int i = 0; i < pairs.length; i += 2) {
            SysDictData entry = new SysDictData();
            entry.setDictValue(pairs[i]); entry.setDictLabel(pairs[i + 1]); entry.setStatus("0");
            entries.add(entry);
        }
        return entries;
    }
}
