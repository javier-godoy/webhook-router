package ar.com.rjgodoy.webhook_router.filter;

import java.util.Objects;

enum PredicateOperator {
  EQ {
    @Override
    boolean test(String s1, String s2) {
      return Objects.equals(s1, s2);
    }
  },

  CONTAINS {
    @Override
    boolean test(String s1, String s2) {
      return s1.contains(s2);
    }
  };

  abstract boolean test(String s1, String s2);
}
