package com.ruoyi.talk.utils;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import com.ruoyi.common.exception.ServiceException;

public final class UntalkedDateValidation {
    private UntalkedDateValidation() {}
    public static void validate(String begin, String end) {
        boolean noBegin = begin == null || begin.isBlank();
        boolean noEnd = end == null || end.isBlank();
        if (noBegin && noEnd) return;
        if (noBegin || noEnd) throw new ServiceException("请选择完整的开始和结束日期");
        try {
            if (!begin.matches("\\d{4}-\\d{2}-\\d{2}") || !end.matches("\\d{4}-\\d{2}-\\d{2}")
                    || LocalDate.parse(begin).isAfter(LocalDate.parse(end))) {
                throw new ServiceException("日期范围无效");
            }
        } catch (DateTimeParseException ex) {
            throw new ServiceException("日期范围无效");
        }
    }
}
