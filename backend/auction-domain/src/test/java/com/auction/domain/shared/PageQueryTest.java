package com.auction.domain.shared;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PageQueryTest {

    @Test
    void acceptsFirstPageAndMaxSize() {
        PageQuery query = new PageQuery(0, PageQuery.MAX_SIZE);

        assertThat(query.page()).isZero();
        assertThat(query.size()).isEqualTo(100);
        assertThat(query.offset()).isZero();
    }

    @Test
    void computesOffset() {
        assertThat(new PageQuery(3, 20).offset()).isEqualTo(60);
    }

    @ParameterizedTest
    @CsvSource({"-1, 20", "0, 0", "0, -5", "0, 101"})
    void rejectsInvalidPageOrSize(int page, int size) {
        assertThatThrownBy(() -> new PageQuery(page, size))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void pageComputesTotalPages() {
        assertThat(new Page<>(List.of("a", "b"), 0, 2, 5).totalPages()).isEqualTo(3);
        assertThat(new Page<>(List.of(), 0, 20, 0).totalPages()).isZero();
    }

    @Test
    void pageContentIsImmutable() {
        Page<String> page = new Page<>(new ArrayList<>(List.of("a")), 0, 20, 1);

        assertThatThrownBy(() -> page.content().add("b")).isInstanceOf(UnsupportedOperationException.class);
    }
}
