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
import org.json.JSONArray;

@RequiredArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Getter(AccessLevel.PACKAGE)
final class ForAction implements Directive, HasLineNumber {

  @Getter
  private final int lineNumber;

  private final String variable;
  private final String arrayName;
  private final Directive body;

  @Override
  public Result apply(WebHook webhook) {
    Object obj = webhook.resolve(arrayName);
    if (obj instanceof JSONArray array) {
      int n = array.length();
      for (int i = 0; i < n; i++) {
        webhook.context.set("%" + variable, array.get(i));
        try {
          body.apply(webhook);
        } finally {
          webhook.context.unset("%" + variable);
        }
      }
      return Result.NULL;
    } else {
      logError("[FOR] " + arrayName + " is not an array");
      return Result.FALSE;
    }
  }

  @Override
  public String toString() {
    String str = body.toString();
    if (!str.startsWith("{")) {
      str = ToStringHelper.pad("{\n" + str) + "\n}";
    } else {
      str = ToStringHelper.pad(str);
    }
    return "FOR " + variable + " IN " + arrayName + " " + str;
  }
}
