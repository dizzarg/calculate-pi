package ru.dkadyrov.calculate.pi.standalone;

public interface Consumer<MESSAGE, RESULT> {

    RESULT compute(MESSAGE message);

    default void onComplete() {
    }
}
