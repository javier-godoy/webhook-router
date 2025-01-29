package ar.com.rjgodoy.webhook_router.filter;

public enum Result {

  NULL, FALSE, TRUE;

  public static Result of(boolean value) {
    return value ? TRUE : FALSE;
  }

  public Result and(Result x) {
    return and(this, x);
  }

  public Result or(Result x) {
    return or(this, x);
  }

  public Result negate() {
    return switch (this) {
      case TRUE -> FALSE;
      case FALSE -> TRUE;
      case NULL -> NULL;
    };
  }

  static Result and(Result x, Result y) {
    return x == NULL ? y : y == NULL ? x : of(x == TRUE && y == TRUE);
  }

  static Result or(Result x, Result y) {
    return x == NULL ? y : y == NULL ? x : of(x == TRUE || y == TRUE);
  }

}
