package ru.dkadyrov.calculate.pi.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.DoubleAdder;

/**
 * The implemeation alculator which calculate of π.
 * <p>
 * Uses the Gottfried Leibniz formula for calculation of π:
 * <p>
 * 1 -  1/3  + 1/5 - 1/7 + 1/9 - ... = π/4
 * <p>
 * Source: Wikipedia - Leibniz formula for π
 * https://en.wikipedia.org/wiki/Leibniz_formula_for_%CF%80
 */
public class Leibniz implements Calculator {

    private final Logger log = LoggerFactory.getLogger(Leibniz.class);

    public double calculate(int digits) {
        DoubleAdder adder = new DoubleAdder();
        int j = 0;
        double member = next(j);
        while (Math.abs(member) > Math.pow(10, -(digits + 1))) {
            adder.add(member);
            j++;
            member = next(j);
        }
        double sum = adder.sum();
        log.debug("{}: result -> {} j = {}", digits, sum, j);
        return sum;
    }

    private double next(int i) {
        return (i % 2 == 0 ? 4.0 : -4.0) / (2.0 * i + 1);
    }

}
