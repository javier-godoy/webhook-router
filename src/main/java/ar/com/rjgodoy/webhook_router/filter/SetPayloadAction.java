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
import org.json.JSONObject;

@RequiredArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Getter(AccessLevel.PACKAGE)
final class SetPayloadAction implements Directive {

  private final String path;
  private final String type;
  private final MacroString macro;

  @Override
  public Result apply(WebHook webhook) {

    String var;
    int pos = path.lastIndexOf('.');
    JSONObject obj;
    if (pos < 0) {
      obj = webhook.getPayload();
      var = path;
    } else {
      obj = (JSONObject) webhook.getPayload(path.substring(0, pos - 1));
      var = path.substring(pos + 1);
    }

    String value = macro.eval(webhook);
    if (value == null) {
      System.err.println("[SET] Macro expanded to null: " + macro);
      return Result.FALSE;
    }

    String type = this.type;
    if (type == null) {
      type = switch (value) {
        case "true" -> "boolean";
        case "false" -> "boolean";
        case "null" -> "null";
        case "[]" -> "array";
        default -> value.matches("(-)?\\d+(\\.\\d+)?") ? "number" : "string";
      };
    }

    switch (type) {
      case "string":
        obj.put(var, value);
        break;
      case "number":
        obj.put(var, Double.parseDouble(value));
        break;
      case "boolean":
        obj.put(var, Boolean.parseBoolean(value));
        break;
      case "null":
        obj.put(var, JSONObject.NULL);
        break;
      case "array":;
        obj.put(var, new JSONObject("{\"x\":" + value + "}").getJSONArray("x"));
        break;
      case "object":
        obj.put(var, new JSONObject(value));
        break;
      default:
        return Result.FALSE;
    }

    return Result.NULL;
  }

  @Override
  public String toString() {
    return "SET $" + path + ":" + (type != null ? type : "") + " " + macro;
  }
}
