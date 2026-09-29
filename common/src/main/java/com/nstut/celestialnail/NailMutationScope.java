package com.nstut.celestialnail;

/** Suppress native cascades only while a bounded nail operation is on the server stack. */
public final class NailMutationScope implements AutoCloseable {
    private static final ThreadLocal<Boolean> ACTIVE = ThreadLocal.withInitial(() -> false);
    private final boolean previous;
    private NailMutationScope() { previous=ACTIVE.get(); ACTIVE.set(true); }
    public static NailMutationScope enter() { return new NailMutationScope(); }
    public static boolean active() { return ACTIVE.get(); }
    @Override public void close() { if(previous)ACTIVE.set(true);else ACTIVE.remove(); }
}
