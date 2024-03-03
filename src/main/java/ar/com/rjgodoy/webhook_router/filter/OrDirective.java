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
import java.util.List;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@EqualsAndHashCode(callSuper = false)
final class OrDirective extends LogicalDirective {

  @Getter(AccessLevel.PACKAGE)
  private final List<Directive> directives;

  @Override
  public boolean apply(WebHook webhook) {
    for (Directive directive : directives) {
      if (eval(directive, webhook, true)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public String toString() {
    String s = directives.stream().map(Object::toString).collect(Collectors.joining("\n"));
    return "OR {\n  " + ToStringHelper.pad(s) + "\n}";
  }

}
