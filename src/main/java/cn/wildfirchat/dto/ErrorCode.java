package cn.wildfirchat.dto;

import lombok.Getter;

@Getter
public enum ErrorCode {
    // 成功
    SUCCESS(0, "success"),

    // 系统错误 1-999
    SYSTEM_ERROR(1, "系统错误"),
    PARAM_ERROR(2, "参数错误"),
    UNKNOWN_ERROR(999, "未知错误"),

    // 认证错误 1000-1999
    UNAUTHORIZED(1000, "未授权"),
    AUTH_CODE_MISSING(1001, "缺少authCode"),
    AUTH_CODE_INVALID(1002, "无效的authCode"),

    // 接龙错误 2000-2999
    COLLECTION_NOT_FOUND(2000, "接龙不存在"),
    COLLECTION_CLOSED(2001, "接龙已结束"),
    COLLECTION_CANCELLED(2002, "接龙已取消"),
    COLLECTION_EXPIRED(2003, "接龙已过期"),
    COLLECTION_FULL(2004, "人数已满"),

    // 参与错误 3000-3999
    ENTRY_NOT_FOUND(3000, "没有参与记录"),
    NO_PERMISSION(3001, "没有权限"),
    ALREADY_JOINED(3002, "已经参与过了"),
    NOT_IN_GROUP(3003, "用户不在群组中");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
