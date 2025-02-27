/**
 * Copyright (C) 2023-2024 Roberto Javier Godoy
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
import java.math.BigDecimal;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@EqualsAndHashCode
final class MacroExpansion implements MacroStringPart {
  private final String expansion;

  @Override
  public String eval(WebHook webhook) {
    if (expansion.startsWith("env.")) {
      return System.getenv(expansion.substring(4));
    }
    Object value;
    value = webhook.resolve("%%" + expansion);
    if (value == null) {
      value = webhook.resolve("%" + expansion);
    }
    if (value == null) {
      value = webhook.resolve("$" + expansion);
    }
    if (value == null) {
      value = webhook.getHeader(expansion).orElse(null);
    }
    if (value instanceof Double d) {
      value = new BigDecimal(d).toPlainString();
    }
    if (value != null) {
      value = value.toString();
    }
    return (String) value;
  }

  @Override
  public String toString() {
    return "${" + expansion + "}";
  }
}
