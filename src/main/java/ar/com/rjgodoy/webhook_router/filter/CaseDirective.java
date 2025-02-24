/**
 * Copyright (C) 2024-2025 Roberto Javier Godoy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ar.com.rjgodoy.webhook_router.filter;

import ar.com.rjgodoy.webhook_router.WebHook;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@EqualsAndHashCode
final class CaseDirective implements Directive {

  @RequiredArgsConstructor
  @EqualsAndHashCode
  static final class WhenClause {
    private final Directive predicate;
    private final Directive actions;

    @Override
    public String toString() {
      return "\nWHEN " + predicate + "\nTHEN " + actions;
    }
  }

  @RequiredArgsConstructor
  @EqualsAndHashCode
  static final class ElseClause {
    private final Directive actions;

    @Override
    public String toString() {
      return "\nELSE " + actions;
    }
  }

  private final List<WhenClause> whenClauses;
  private final ElseClause elseClause;

  @Override
  public Result apply(WebHook webhook) {
    for (WhenClause when : whenClauses) {
      if (when.predicate.apply(webhook) == Result.TRUE) {
        return when.actions.apply(webhook);
      }
    }
    if (elseClause != null) {
      return elseClause.actions.apply(webhook);
    }
    return Result.NULL;
  }

  @Override
  public String toString() {
    String w = ToStringHelper.pad(whenClauses.stream().map(Object::toString).collect(Collectors.joining("")));
    String e = Optional.ofNullable(elseClause).map(Object::toString).map(ToStringHelper::pad).orElse("");
    return "CASE" + w + e + "\nESAC";
  }

}
