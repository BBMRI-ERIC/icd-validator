package eu.bbmri_eric;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ICD10ValidatorTests {
    private ICD10Validator validator;

    @BeforeEach
    void setUp() {
        validator = new ICD10Validator();
        ICD10Validator.resetForTesting();
    }

    private void debugParsedCodes() {
        Set<String> codes = ICD10Validator.getValidCodesForTesting();
        System.out.println("Parsed codes size: " + (codes != null ? codes.size() : "null"));
        if (codes != null && codes.size() < 100) {
            System.out.println("Parsed codes: " + codes);
        } else if (codes != null) {
            System.out.println("First 10 codes: " + codes.stream().limit(10).toList());
        }
    }

    @Test
    void testValidExactCodes() {
        assertTrue(validator.isValid("A00"), "A00 should be valid");
        assertTrue(validator.isValid("B18"), "B18 should be valid");
        assertTrue(validator.isValid("E11"), "E11 should be valid");
        if (!validator.isValid("A00")) {
            debugParsedCodes();
        }
    }

    @Test
    void testValidExtendedCodes() {
        assertTrue(validator.isValid("A00.0"), "A00.0 should be valid");
        assertTrue(validator.isValid("B18.0"), "B18.0 should be valid");
        assertTrue(validator.isValid("E10.9"), "E10.9 should be valid");
        assertTrue(validator.isValid("J96.0"), "J96.0 should be valid");
        if (!validator.isValid("A00.0")) {
            debugParsedCodes();
        }
    }

    @Test
    void testValidCodeRanges() {
        assertTrue(validator.isValid("A05"), "A05 should be valid (within A00-A09)");
        assertTrue(validator.isValid("A05.1"), "A05.1 should be valid");
        assertTrue(validator.isValid("F15.2"), "F15.2 should be valid (within F10-F19)");
        if (!validator.isValid("A05")) {
            debugParsedCodes();
        }
    }

    @Test
    void testInvalidCodes() {
        assertFalse(validator.isValid("Z99.99"), "Z99.99 should be invalid");
        assertFalse(validator.isValid("ABC"), "ABC should be invalid");
        assertFalse(validator.isValid("A00.99"), "A00.99 should be invalid");
        assertFalse(validator.isValid("E10.A"), "E10.A should be invalid");
        if (validator.isValid("Z99.99")) {
            debugParsedCodes();
        }
    }

    @Test
    void testInvalidCodeEdgeCases() {
        assertFalse(validator.isValid("A00.999"), "A00.999 should be invalid (too long)");
        assertFalse(validator.isValid("A00."), "A00. should be invalid (trailing dot)");
        assertFalse(validator.isValid("123"), "123 should be invalid (no letter)");
        assertFalse(validator.isValid("A..0"), "A..0 should be invalid (invalid format)");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void testNullAndEmptyInputs(String input) {
        assertFalse(validator.isValid(input), "Null or empty input should be invalid");
    }

    @ParameterizedTest
    @ValueSource(strings = {" a00 ", "A00 ", " A00", "a00.0", "A00.0 "})
    void testInputNormalization(String input) {
        assertTrue(validator.isValid(input), "Input should be normalized and valid");
        if (!validator.isValid(input)) {
            debugParsedCodes();
        }
    }

    @Test
    void testInvalidFormat() {
        assertFalse(validator.isValid("12"), "Too short code should be invalid");
        assertFalse(validator.isValid("A123"), "Too long base code should be invalid");
        assertFalse(validator.isValid("A0"), "Incomplete base code should be invalid");
        assertFalse(validator.isValid("A00..0"), "Double dot should be invalid");
    }

    @Test
    void testThreadSafety() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        Runnable task = () -> {
            try {
                assertTrue(validator.isValid("A00.0"), "Concurrent validation should succeed");
            } catch (AssertionError e) {
                debugParsedCodes();
                throw e;
            } finally {
                latch.countDown();
            }
        };

        for (int i = 0; i < threadCount; i++) {
            executor.submit(task);
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS), "All threads should complete");
        executor.shutdown();
    }

    @Test
    void testInitializationWithoutDtd() {
        assertDoesNotThrow(() -> validator.isValid("A00"),
                "Initialization should succeed without DTD");
        assertTrue(validator.isValid("A00"), "A00 should be valid after initialization");
        if (!validator.isValid("A00")) {
            debugParsedCodes();
        }
    }

    @Test
    void testParsedCodesCount() {
        validator.isValid("A00");
        Set<String> codes = ICD10Validator.getValidCodesForTesting();
        assertNotNull(codes, "Valid codes set should not be null");
        assertTrue(codes.size() > 1000, "Should parse at least 1000 codes, got: " + codes.size());
    }
}