package ru.dkadyrov.calculate.pi.api;

import java.util.concurrent.CompletableFuture;

/**
 * Solution interface for calculations π.
 */
public interface Solution {

    /**
     * Asynchronous calculation of PI number
     *
     * @param digits number of correct digits
     * @return
     */
    CompletableFuture<Double> calculatePiAsync(int digits);

}
