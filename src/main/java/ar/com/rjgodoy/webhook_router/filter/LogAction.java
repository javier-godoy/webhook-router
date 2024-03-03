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


@EqualsAndHashCode
class LogAction implements Directive {

  @Getter(AccessLevel.PACKAGE)
  private final MacroString macro;

  private final MacroString originalMacro;

  @Getter(AccessLevel.PACKAGE)
  private final Directive next;

  public LogAction(MacroString macro) {
    this(macro, null, null);
  }

  public LogAction(MacroString macro1, MacroString macro2, Directive next) {
    originalMacro = macro1;
    if (macro2 != null) {
      macro = macro1.concat(new MacroString(" ")).concat(macro2);
    } else {
      macro = macro1;
    }
    this.next = next;
  }

  @Override
  public boolean isIgnoreResult() {
    return next == null || next.isIgnoreResult();
  }

  @Override
  public boolean apply(WebHook webhook) {
    String s = macro.eval(webhook, true);
    if (s != null) {
      for (String secret : webhook.context.getSecrets().toList()) {
        s = s.replaceAll(secret, "***");
      }
      System.out.println(s);
    } else {
      System.err.println("[LOG] Macro expanded to null: " + macro);
    }

    if (next != null) {
      return next.apply(webhook);
    } else {
      return true;
    }
  }

  @Override
  public String toString() {
    if (next != null) {
      return "LOG " + originalMacro + " & " + next;
    } else {
      return "LOG " + originalMacro;
    }

  }

}
