package com.function.neepuacmv1.constant;

/**
 * Redis Key 规范
 */
public final class RedisKeys {
    private RedisKeys() {
    }

    /**
     * Token -> userId
     */
    public static final String TOKEN = "acm:auth:token:";
    /**
     * userId -> userInfo(json)
     */
    public static final String USER_INFO = "acm:user:info:";
    /**
     * userId -> roles(set)
     */
    public static final String USER_ROLES = "acm:user:roles:";
    /**
     * username -> failCount
     */
    public static final String LOGIN_FAIL = "acm:auth:fail:";
    /**
     * captchaId -> code
     */
    public static final String CAPTCHA = "acm:auth:captcha:";
    /**
     * phone/email -> verifyCode
     */
    public static final String VERIFY_CODE = "acm:auth:code:";
    /**
     * 题目详情缓存：problemId -> json
     */
    public static final String PROBLEM_DETAIL = "acm:problem:detail:";
    /**
     * 题目列表缓存：hash(page,size,keyword,sort,tagId) -> json
     */
    public static final String PROBLEM_LIST = "acm:problem:list:";
    /**
     * 标签树缓存
     */
    public static final String TAG_TREE = "acm:tag:tree";

    /**
     * 用户已AC题目集合（可选优化）：userId -> set(problemId)
     */
    public static final String USER_SOLVED_SET = "acm:user:solved:";
    /**
     * userId -> tokens(set) 用于禁用/删除时踢下线
     */
    public static final String USER_TOKENS = "acm:auth:user_tokens:";


    public static final String CONTEST_DETAIL = "acm:contest:detail:";
    public static final String CONTEST_LIST = "acm:contest:list:";
    public static final String CONTEST_PROBLEMS = "acm:contest:problems:";

    public static final String JUDGE_TEMPLATE_DETAIL = "acm:judgetpl:detail:";
    public static final String JUDGE_TEMPLATE_PAGE = "acm:judgetpl:page:";    // 可选：分页缓存
    public static final String JUDGE_TEMPLATE_OPTIONS = "acm:judgetpl:options:"; // 下拉选项

    public static final String SUBMISSION_DETAIL = "acm:submission:detail:"; // + submissionId
}
