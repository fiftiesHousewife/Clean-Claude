package org.fiftieshousewife.cleancode.annotations;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HeuristicCodeTest {

    @Test
    void allCommentCodesExist() {
        assertDoesNotThrow(() -> {
            HeuristicCode.valueOf("C1");
            HeuristicCode.valueOf("C2");
            HeuristicCode.valueOf("C3");
            HeuristicCode.valueOf("C4");
            HeuristicCode.valueOf("C5");
        });
    }

    @Test
    void allEnvironmentCodesExist() {
        assertDoesNotThrow(() -> {
            HeuristicCode.valueOf("E1");
            HeuristicCode.valueOf("E2");
        });
    }

    @Test
    void allFunctionCodesExist() {
        assertDoesNotThrow(() -> {
            HeuristicCode.valueOf("F1");
            HeuristicCode.valueOf("F2");
            HeuristicCode.valueOf("F3");
            HeuristicCode.valueOf("F4");
        });
    }

    @Test
    void allGeneralCodesExist() {
        for (int i = 1; i <= 36; i++) {
            String code = "G" + i;
            assertDoesNotThrow(() -> HeuristicCode.valueOf(code),
                    "Missing general code: " + code);
        }
    }

    @Test
    void allNamingCodesExist() {
        for (int i = 1; i <= 7; i++) {
            String code = "N" + i;
            assertDoesNotThrow(() -> HeuristicCode.valueOf(code),
                    "Missing naming code: " + code);
        }
    }

    @Test
    void allTestCodesExist() {
        for (int i = 1; i <= 9; i++) {
            String code = "T" + i;
            assertDoesNotThrow(() -> HeuristicCode.valueOf(code),
                    "Missing test code: " + code);
        }
    }

    @Test
    void allChapterCodesExist() {
        assertDoesNotThrow(() -> {
            HeuristicCode.valueOf("Ch3_1");
            HeuristicCode.valueOf("Ch3_2");
            HeuristicCode.valueOf("Ch3_3");
            HeuristicCode.valueOf("Ch6_1");
            HeuristicCode.valueOf("Ch7_1");
            HeuristicCode.valueOf("Ch7_2");
            HeuristicCode.valueOf("Ch10_1");
            HeuristicCode.valueOf("Ch10_2");
        });
    }

    @Test
    void allJavaCodesExist() {
        assertDoesNotThrow(() -> {
            HeuristicCode.valueOf("J1");
            HeuristicCode.valueOf("J2");
            HeuristicCode.valueOf("J3");
        });
    }

    @Test
    void metaCodesExist() {
        assertDoesNotThrow(() -> {
            HeuristicCode.valueOf("META_SUPPRESSION_EXPIRED");
            HeuristicCode.valueOf("META_SUPPRESSION_NO_REASON");
        });
    }

    @Test
    void totalEnumCount() {
        assertEquals(76, HeuristicCode.values().length);
    }
}
