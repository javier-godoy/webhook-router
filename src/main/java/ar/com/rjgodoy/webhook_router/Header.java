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
package ar.com.rjgodoy.webhook_router;

import java.util.function.Predicate;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
@EqualsAndHashCode
@AllArgsConstructor
public class Header {

  @NonNull
  private String name;

  @Setter
  @NonNull
  private String value;

  public Header(String line) {
    String ss[] = line.split(":", 2);
    name = ss[0];
    value = ss[1].trim();
  }

  public Header(Header header) {
    name = header.name;
    value = header.value;
  }

  @Override
  public String toString() {
    return name + ": " + value;
  }

  public static Predicate<Header> is(String name) {
    return header -> header.name.equalsIgnoreCase(name);
  }

}
