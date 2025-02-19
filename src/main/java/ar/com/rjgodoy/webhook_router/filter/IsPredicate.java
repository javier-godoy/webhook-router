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
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONString;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode
@Getter
final class IsPredicate implements Directive {

  private final String path;
  private final String type;

  public static IsPredicate newInstance(String path, String type) {
    type = type.toLowerCase();
    switch (type) {
      case "null", "string", "number", "boolean", "object", "array":
        return new IsPredicate(path, type);
      default:
        return null;
    }
  }
  private String typeOf(Object value) {
    if (value == null || value.equals(null)) {
      return "null";
    }
    if (value instanceof JSONString || value instanceof String) {
      return "string";
    }
    if (value instanceof Number) {
      return "number";
    }
    if (value instanceof Boolean) {
      return "boolean";
    }
    if (value instanceof JSONObject) {
      return "object";
    }
    if (value instanceof JSONArray) {
      return "array";
    }
    throw new IllegalArgumentException(value.getClass().getSimpleName());
  }

  @Override
  public Result apply(WebHook webhook) {
    return Result.of(typeOf(webhook.getPayload(path)).equals(type));
  }

  @Override
  public String toString() {
    String op = "is";
    return Stream.of(path).collect(Collectors.joining(".", "$", ":")) + op + " " + type;
  }

}
