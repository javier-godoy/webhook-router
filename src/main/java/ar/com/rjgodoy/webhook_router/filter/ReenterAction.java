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
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
final class ReenterAction implements Directive {

  @Getter(AccessLevel.PACKAGE)
  private final boolean copy;

  @Override
  public Result apply(WebHook webhook) {
    if (copy) {
      webhook = new WebHook(webhook);
    }

    try {
      if (!webhook.context.reenter(webhook, this)) {
        System.err.println("[REENTER] Loop detected");
        return Result.FALSE;
      }
    } catch (ExitActionException e) {
      if (!copy) {
        throw e;
      }
    }

    return Result.of(webhook.context.isConsumed());
  }

  @Override
  public String toString() {
    return "REENTER";
  }

}
