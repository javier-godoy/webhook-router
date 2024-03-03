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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

@Getter
@RequiredArgsConstructor
@EqualsAndHashCode
class MacroString {

  @Getter(AccessLevel.PACKAGE)
  @Accessors(fluent = true)
  private final List<MacroStringPart> parts;

  public String eval(WebHook webhook, boolean coerce) {
    if (parts.size() == 1) {
      return parts.get(0).eval(webhook, coerce);
    } else {
      StringBuilder sb = new StringBuilder();
      for (var part : parts) {
        String s = part.eval(webhook, coerce);
        if (s == null) {
          return null;
        }
        sb.append(s);
      }
      return sb.toString().replaceAll("\\s+", "\s");
    }
  }

  @Override
  public String toString() {
    return parts.stream().map(Object::toString).collect(Collectors.joining());
  }

  MacroString concat(MacroString other) {
    List<MacroStringPart> parts = new ArrayList<>(this.parts.size() + other.parts.size());
    parts.addAll(this.parts);
    parts.addAll(other.parts);
    return new MacroString(parts);
  }

  public MacroString(String string) {
    parts = Arrays.asList(new MacroLiteral(string));
  }

}