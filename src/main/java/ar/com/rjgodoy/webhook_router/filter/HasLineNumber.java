package ar.com.rjgodoy.webhook_router.filter;

interface HasLineNumber {

  int getLineNumber();

  public default void logError(String message) {
    System.err.println(message + " at line " + getLineNumber());
  }

}
