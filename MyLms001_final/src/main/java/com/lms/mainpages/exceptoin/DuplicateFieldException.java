package com.lms.mainpages.exceptoin;

/**
 * DB 유니크 제약(예: email, nickname) 위반을 표현하는 런타임 예외.
 * 컨트롤러에서 catch 하여 필드 에러로 바인딩하거나,
 * 잡지 않으면 HTTP 409(CONFLICT)로 내려갑니다.
 */

public class DuplicateFieldException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    private final String field;
    private final String value;

    public DuplicateFieldException(String field, String value) {
        super(buildMessage(field, value));
        this.field = field;
        this.value = value;
    }
    public DuplicateFieldException(String field, String value, Throwable cause) {
        super(buildMessage(field, value), cause);
        this.field = field;
        this.value = value;
    }
    private static String buildMessage(String field, String value) {
        String f = field == null ? "unknown" : field;
        String v = value == null ? "" : " (" + value + ")";
        return "Duplicate value for field: " + f + v;
    }
    public String getField() { return field; }
    public String getValue() { return value; }
}