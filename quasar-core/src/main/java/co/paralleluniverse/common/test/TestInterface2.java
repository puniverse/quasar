package co.paralleluniverse.common.test;

import co.paralleluniverse.fibers.Suspendable;

import java.util.ArrayList;

public interface TestInterface2 {
    @Suspendable
    void test(ArrayList<String> results, TestInterface target);
}
