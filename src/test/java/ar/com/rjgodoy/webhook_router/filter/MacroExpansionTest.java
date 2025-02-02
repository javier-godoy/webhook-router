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

import static org.junit.jupiter.api.Assertions.assertEquals;
import ar.com.rjgodoy.webhook_router.WebHook;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

public class MacroExpansionTest {

  @Test
  public void test() {
    JSONObject payload = new JSONObject();
    payload.put("a", new JSONObject(Map.of("b", new JSONObject(Map.of("c", 1)))));
    WebHook w = new WebHook("", List.of(), payload);

    assertEquals("1", new MacroExpansion("a.b.c").eval(w, true));
  }

}

