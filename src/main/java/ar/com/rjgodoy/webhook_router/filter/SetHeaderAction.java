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
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Getter(AccessLevel.PACKAGE)
final class SetHeaderAction implements Directive {

  private final String name;
  private final MacroString macro;

  @Override
  public boolean apply(WebHook webhook) {
    String value = macro.eval(webhook, false);
    if (value == null) {
      System.err.println("[SET] Macro expanded to null: " + macro);
      return false;
    }
    webhook.setHeader(name, value);
    return true;
  }

  @Override
  public boolean isIgnoreResult() {
    return true;
  }

  @Override
  public String toString() {
    return "SET " + name + ": " + macro;
  }
}
