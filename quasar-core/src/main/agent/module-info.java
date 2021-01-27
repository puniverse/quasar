module co.paralleluniverse.quasar.core.agent {
    requires java.instrument;

    exports co.paralleluniverse.common.asm;
    exports co.paralleluniverse.common.resource;
    exports co.paralleluniverse.fibers.instrument;
    exports co.paralleluniverse.fibers.suspend;

    opens co.paralleluniverse.fibers.suspend to co.paralleluniverse.fibers;
}

