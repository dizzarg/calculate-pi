package ru.dkadyrov.calculate.pi.api;

import org.junit.Assert;
import org.junit.Test;

public class LeibnizTest {

    Leibniz leibniz = new Leibniz();

    @Test
    public void calculate_ok() {
        double calculate = leibniz.calculate(3);
        Assert.assertEquals(calculate, Math.PI, 0.001);
    }
}