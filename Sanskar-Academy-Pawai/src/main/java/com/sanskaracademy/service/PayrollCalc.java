package com.sanskaracademy.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * The salary rule in one place (easy to test).
 *   leave to cut = absent + leave + 0.5 x half-days
 *   days to deduct = max(0, leave to cut - paid leave allowed in the month)   (default 2)
 *   per day = monthly salary / divisor                                         (default 30)
 *   net = salary - per day x days to deduct - other deduction + bonus
 */
public final class PayrollCalc {

    public static final class Result {
        public BigDecimal takenDays;
        public BigDecimal deductDays;
        public BigDecimal perDay;
        public BigDecimal leaveDeduction;
        public BigDecimal net;
    }

    private PayrollCalc() {}

    public static Result compute(BigDecimal gross, int divisor, BigDecimal paidLeave,
                                 int absent, int leave, int half, BigDecimal other, BigDecimal bonus) {
        if (divisor < 1) divisor = 30;
        if (paidLeave == null) paidLeave = BigDecimal.ZERO;
        if (other == null) other = BigDecimal.ZERO;
        if (bonus == null) bonus = BigDecimal.ZERO;
        Result r = new Result();
        r.takenDays = BigDecimal.valueOf(absent + leave).add(new BigDecimal("0.5").multiply(BigDecimal.valueOf(half)));
        r.deductDays = r.takenDays.subtract(paidLeave).max(BigDecimal.ZERO);
        r.perDay = gross.divide(BigDecimal.valueOf(divisor), 2, RoundingMode.HALF_UP);
        r.leaveDeduction = r.perDay.multiply(r.deductDays).setScale(2, RoundingMode.HALF_UP);
        r.net = gross.subtract(r.leaveDeduction).subtract(other).add(bonus).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        return r;
    }
}
