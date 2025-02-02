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
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@EqualsAndHashCode(callSuper = false)
final class OrSequence extends LogicalDirective {

  @Getter(AccessLevel.PACKAGE)
  private final List<Directive> directives;

  @Override
  public Result apply(WebHook webhook) {
    procedures().forEach(webhook.context::declare);

    Result result = Result.NULL;
    for (Directive directive : directives) {
      if (result == Result.TRUE && directive instanceof OtherwiseDirective) {
        continue;
      }
      result = result.or(directive.apply(webhook));
    }

    procedures().forEach(webhook.context::undeclare);
    return result;
  }

  private Stream<ProcedureDecl> procedures() {
    return directives.stream().filter(ProcedureDecl.class::isInstance)
        .map(ProcedureDecl.class::cast);
  }

  @Override
  public String toString() {
    String s = directives.stream().map(Object::toString).collect(Collectors.joining("\n\n"));
    if (s.isEmpty()) {
      return "{}";
    } else {
      return "{\n  " + ToStringHelper.pad(s) + "\n}";
    }
  }

}
