package com.function.neepuacmv1.constant;

public final class JudgeQueueConstants {
    private JudgeQueueConstants() {}

    public static final String STREAM_KEY = "acm:judge:stream";
    public static final String GROUP = "judge_group";

    // 空转时接管 idle 超过这个阈值的 pending
    public static final long CLAIM_IDLE_MS = 30_000;

    // 读新消息 block 时间
    public static final long READ_BLOCK_MS = 2000;
}
