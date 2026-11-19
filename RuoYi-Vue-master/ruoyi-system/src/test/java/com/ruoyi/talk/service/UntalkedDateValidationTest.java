package com.ruoyi.talk.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import com.ruoyi.talk.utils.UntalkedDateValidation;

class UntalkedDateValidationTest {
    @Test void acceptsEmptyOrCompleteValidOrderedDates() {
        assertThatCode(() -> UntalkedDateValidation.validate(null, null)).doesNotThrowAnyException();
        assertThatCode(() -> UntalkedDateValidation.validate("2024-02-29", "2026-10-31")).doesNotThrowAnyException();
    }
    @Test void rejectsIncompleteInvalidOrReversedDates() {
        assertThatThrownBy(() -> UntalkedDateValidation.validate("2026-01-01", null)).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> UntalkedDateValidation.validate("2026-02-30", "2026-03-01")).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> UntalkedDateValidation.validate("2026-10-01", "2026-09-30")).isInstanceOf(RuntimeException.class);
    }
}
