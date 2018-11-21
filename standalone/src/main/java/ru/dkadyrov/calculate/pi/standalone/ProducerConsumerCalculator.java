package ru.dkadyrov.calculate.pi.standalone;

import ru.dkadyrov.calculate.pi.api.Calculator;

import java.util.Iterator;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.DoubleAdder;

public class ProducerConsumerCalculator implements Calculator {

    class LeibnizSequenceIterator implements Iterator<Double> {

        AtomicInteger counter = new AtomicInteger(0);
        AtomicBoolean hasNext = new AtomicBoolean(true);
        private final int digits;

        LeibnizSequenceIterator(int digits) {
            this.digits = digits;
        }

        @Override
        public boolean hasNext() {
            return hasNext.get();
        }

        @Override
        public Double next() {
            double next = next(counter.getAndIncrement());
            if (Math.abs(next) < Math.pow(10, -digits) && hasNext.get()) {
                hasNext.set(false);
            }
            return next;
        }

        private double next(int i) {
            return (i % 2 == 0 ? 4.0 : -4.0) / (2.0 * i + 1);
        }
    }

    class Producer implements Runnable {
        private final BlockingQueue<Double> queue;
        private final AtomicBoolean alive;
        private final LeibnizSequenceIterator iterator;

        Producer(BlockingQueue<Double> q, AtomicBoolean alive, LeibnizSequenceIterator iterator) {
            queue = q;
            this.alive = alive;
            this.iterator = iterator;
        }

        public void run() {
            try {
                while (iterator.hasNext()) {
                    queue.put(iterator.next());
                }
            } catch (InterruptedException ignore) {
            }
            alive.set(true);

        }
    }

    class Consumer implements Runnable {
        private final BlockingQueue<Double> queue;
        private final DoubleAdder result;
        private final AtomicBoolean alive;

        Consumer(BlockingQueue<Double> q, DoubleAdder result, AtomicBoolean alive) {
            queue = q;
            this.result = result;
            this.alive = alive;
        }

        public void run() {
            try {
                while (true) {
                    Double value = queue.poll(100, TimeUnit.MILLISECONDS);
                    if (value != null) {
                        consume(value);
                    }
                    if (alive.get() && queue.size() == 0) {
                        return;
                    }
                }
            } catch (InterruptedException ignore) {
            }
        }

        void consume(double x) {
            result.add(x);
        }
    }

    @Override
    public double calculate(int digits) {
        DoubleAdder adder = new DoubleAdder();
        AtomicBoolean alive = new AtomicBoolean(false);
        LeibnizSequenceIterator iterator = new LeibnizSequenceIterator(digits);
        BlockingDeque<Double> deque = new LinkedBlockingDeque<>();
        ForkJoinPool pool = ForkJoinPool.commonPool();
        int producers = ForkJoinPool.getCommonPoolParallelism() * 2 / 3;
        int consumers = ForkJoinPool.getCommonPoolParallelism() / 3;
        for (int i = 0; i < producers; i++) {
            pool.execute(new Producer(deque, alive, iterator));
        }
        for (int i = 0; i < consumers; i++) {
            pool.execute(new Consumer(deque, adder, alive));
        }
        pool.shutdown();
        try {
            pool.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException ignore) {
        }
        return adder.sum();
    }
}
