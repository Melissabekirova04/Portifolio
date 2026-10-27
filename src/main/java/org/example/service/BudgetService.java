package org.example.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class BudgetService {

    public record BudgetResult(
            BigDecimal knownTotalAmount,
            BigDecimal totalAmount,
            BigDecimal remainingBudget,
            Boolean overBudget,
            boolean priceComplete
    ) {}

    public BudgetResult calculate(
            BigDecimal budgetLimit,
            BigDecimal flightAmount,
            BigDecimal hotelAmount,
            List<BigDecimal> activityPrices
    ) {
        if (activityPrices == null) {
            throw new IllegalArgumentException(
                    "Listen med aktivitetspriser mangler."
            );
        }

        if (budgetLimit != null
                && budgetLimit.signum() <= 0) {
            throw new IllegalArgumentException(
                    "Budgetgrænsen skal være positiv."
            );
        }

        BigDecimal knownTotal = BigDecimal.ZERO;
        boolean complete = true;

        if (flightAmount == null) {
            complete = false;
        } else {
            knownTotal = knownTotal.add(
                    validatePrice(flightAmount)
            );
        }

        if (hotelAmount == null) {
            complete = false;
        } else {
            knownTotal = knownTotal.add(
                    validatePrice(hotelAmount)
            );
        }

        for (BigDecimal price : activityPrices) {
            if (price == null) {
                complete = false;
            } else {
                knownTotal = knownTotal.add(
                        validatePrice(price)
                );
            }
        }

        knownTotal = money(knownTotal);

        BigDecimal total = null;
        BigDecimal remaining = null;
        Boolean overBudget = null;

        if (complete) {
            total = knownTotal;

            if (budgetLimit != null) {
                remaining = money(
                        budgetLimit.subtract(total)
                );

                overBudget = remaining.signum() < 0;
            }
        }

        return new BudgetResult(
                knownTotal,
                total,
                remaining,
                overBudget,
                complete
        );
    }

    private BigDecimal validatePrice(BigDecimal price) {
        if (price.signum() < 0) {
            throw new IllegalArgumentException(
                    "Priser må ikke være negative."
            );
        }

        return price;
    }

    private BigDecimal money(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }
}