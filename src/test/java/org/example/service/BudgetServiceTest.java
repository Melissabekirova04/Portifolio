package org.example.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BudgetServiceTest {

    private BudgetService service;

    @BeforeEach
    void setUp() {
        service = new BudgetService();
    }

    @Test
    void calculatesTotalAndRemainingBudget() {
        BudgetService.BudgetResult result = service.calculate(
                new BigDecimal("8000"),
                BigDecimal.ZERO,
                new BigDecimal("4125"),
                List.of(new BigDecimal("150"))
        );

        assertEquals(
                new BigDecimal("4275.00"),
                result.totalAmount()
        );

        assertEquals(
                new BigDecimal("3725.00"),
                result.remainingBudget()
        );

        assertEquals(Boolean.FALSE, result.overBudget());
        assertTrue(result.priceComplete());
    }

    @Test
    void detectsExceededBudget() {
        BudgetService.BudgetResult result = service.calculate(
                new BigDecimal("4000"),
                BigDecimal.ZERO,
                new BigDecimal("4125"),
                List.of()
        );

        assertEquals(
                new BigDecimal("-125.00"),
                result.remainingBudget()
        );

        assertEquals(Boolean.TRUE, result.overBudget());
    }

    @Test
    void unknownActivityPriceMakesTotalIncomplete() {
        // Arrays.asList tillader null; List.of gør ikke.
        List<BigDecimal> prices = Arrays.asList(
                new BigDecimal("100"),
                null
        );

        BudgetService.BudgetResult result = service.calculate(
                new BigDecimal("8000"),
                BigDecimal.ZERO,
                new BigDecimal("4125"),
                prices
        );

        assertEquals(
                new BigDecimal("4225.00"),
                result.knownTotalAmount()
        );

        assertFalse(result.priceComplete());
        assertNull(result.totalAmount());
        assertNull(result.remainingBudget());
        assertNull(result.overBudget());
    }

    @Test
    void unknownFlightPriceMakesTotalIncomplete() {
        BudgetService.BudgetResult result = service.calculate(
                new BigDecimal("8000"),
                null,
                new BigDecimal("4125"),
                List.of()
        );

        assertFalse(result.priceComplete());
        assertNull(result.totalAmount());

        assertEquals(
                new BigDecimal("4125.00"),
                result.knownTotalAmount()
        );
    }

    @Test
    void zeroPriceCountsAsFree() {
        BudgetService.BudgetResult result = service.calculate(
                new BigDecimal("8000"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                List.of(BigDecimal.ZERO)
        );

        assertTrue(result.priceComplete());

        assertEquals(
                new BigDecimal("0.00"),
                result.totalAmount()
        );

        assertEquals(
                new BigDecimal("8000.00"),
                result.remainingBudget()
        );
    }

    @Test
    void calculatesTotalWithoutBudgetLimit() {
        BudgetService.BudgetResult result = service.calculate(
                null,
                new BigDecimal("1000"),
                new BigDecimal("2000"),
                List.of(new BigDecimal("100"))
        );

        assertEquals(
                new BigDecimal("3100.00"),
                result.totalAmount()
        );

        assertTrue(result.priceComplete());
        assertNull(result.remainingBudget());
        assertNull(result.overBudget());
    }

    @Test
    void rejectsNegativePrices() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.calculate(
                        new BigDecimal("8000"),
                        BigDecimal.ZERO,
                        new BigDecimal("-100"),
                        List.of()
                )
        );
    }

    @Test
    void rejectsNonPositiveBudgetLimit() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.calculate(
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        List.of()
                )
        );
    }
}