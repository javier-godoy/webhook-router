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
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;


@RequiredArgsConstructor
@EqualsAndHashCode
class SecretAction implements Directive {

  @Getter(AccessLevel.PACKAGE)
  private final MacroString macro;

  @Override
  public Result apply(WebHook webhook) {
    String s = macro.eval(webhook, true);
    if (s != null) {
      webhook.context.addSecret(s);
    } else {
      System.err.println("[SECRET] Macro expanded to null: " + macro);
    }
    return Result.NULL;
  }

  @Override
  public String toString() {
    return "SECRET " + macro;
  }

}
