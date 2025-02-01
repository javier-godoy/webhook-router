/**
 * Copyright (C) 2024 Roberto Javier Godoy
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
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@EqualsAndHashCode
@Getter
final class PayloadPredicate implements Directive {

  private final String path;
  private final String value;
  private final PredicateOperator operator;

  @Override
  public Result apply(WebHook webhook) {
    return Result.of(Optional.ofNullable(webhook.getPayload(path)).map(Object::toString)
        .filter(s1 -> operator.test(s1, value)).isPresent());
  }

  @Override
  public String toString() {
    String op = operator == PredicateOperator.EQ ? "" : operator.toString().toLowerCase();
    return Stream.of(path).collect(Collectors.joining(".", "$", ":")) + op + " " + value;
  }
}
