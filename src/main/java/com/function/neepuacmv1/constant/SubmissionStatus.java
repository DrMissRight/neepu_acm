package com.function.neepuacmv1.constant;

import java.util.Set;

public final class SubmissionStatus {
    private SubmissionStatus() {}

    public static final String PENDING = "PENDING";
    public static final String RUNNING = "RUNNING";
    public static final String AC = "AC";
    public static final String WA = "WA";
    public static final String TLE = "TLE";
    public static final String MLE = "MLE";
    public static final String RE = "RE";
    public static final String CE = "CE";
    public static final String SE = "SE";

    public static final Set<String> ALL = Set.of(
            PENDING, RUNNING, AC, WA, TLE, MLE, RE, CE, SE
    );
}
