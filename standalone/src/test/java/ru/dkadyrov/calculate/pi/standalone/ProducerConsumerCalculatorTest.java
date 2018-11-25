package ru.dkadyrov.calculate.pi.standalone;

import org.junit.Assert;
import org.junit.Test;
import ru.dkadyrov.calculate.pi.api.Calculator;

public class ProducerConsumerCalculatorTest {

    private Calculator calculator = new ProducerConsumerCalculator();

    @Test
    public void should_calculate_ok() {
        double calculate = calculator.calculate(6);
        Assert.assertEquals(Math.PI, calculate, 0.00001);
    }

    @Test(timeout = 3_000L)
    public void should_calculate_many_values_ok() {
        for (int i = 0; i < 20; i++) {
            double calculate = calculator.calculate(5);
            Assert.assertEquals(Math.PI, calculate, 0.0001);
        }
    }
}