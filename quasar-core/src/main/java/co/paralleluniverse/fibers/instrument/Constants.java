package co.paralleluniverse.fibers.instrument;

import co.paralleluniverse.fibers.suspend.Instrumented;

public final class Constants {
    /**
     * @see co.paralleluniverse.fibers.Stack
     */
    public static final int STACK_MAX_ENTRY = (1 << 14) - 1;
    public static final int STACK_MAX_SLOTS = (1 << 16) - 1;

    /**
     * {@link Instrumented#methodOptimized()}
     */
    public static final String FIELD_NAME_METHOD_OPTIMIZED = "methodOptimized";

    /**
     * {@link Instrumented#suspendableCallSites()}
     */
    public static final String FIELD_NAME_SUSPENDABLE_CALL_SITES = "suspendableCallSites";

    /**
     * {@link Instrumented#methodStart()}
     */
    public static final String FIELD_NAME_METHOD_START = "methodStart";

    /**
     * {@link Instrumented#methodEnd()}
     */
    public static final String FIELD_NAME_METHOD_END = "methodEnd";

    /**
     * {@link Instrumented#suspendableCallSiteNames()}
     */
    public static final String FIELD_NAME_SUSPENDABLE_CALL_SITE_NAMES = "suspendableCallSiteNames";

    /**
     * {@link Instrumented#suspendableCallSitesOffsetsAfterInstr()}
     */
    public static final String FIELD_NAME_SUSPENDABLE_CALL_SITES_OFFSETS_AFTER_INSTR = "suspendableCallSitesOffsetsAfterInstr";

    private Constants() {
    }
}
