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
package ar.com.rjgodoy.webhook_router;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class WebHook {

  private final String requestUri;
  private final List<Header> headers;
  private final JSONObject payload;

  public final Context context;

  public WebHook(String requestUri, List<Header> headers, JSONObject payload) {
    this(requestUri, new ArrayList<>(headers), payload, new Context());
  }

  public WebHook(WebHook webhook) {
    this(webhook.requestUri,
        webhook.headers.stream().map(h -> new Header(h)).collect(Collectors.toList()),
        new JSONObject(webhook.payload.toString()), new Context(webhook.context));
  }

  public Optional<String> getHeader(String name) {
    return headers.stream().filter(Header.is(name)).findFirst().map(Header::value);
  }

  public void setHeader(String name, String value) {
    headers.stream().filter(Header.is(name)).findFirst().ifPresentOrElse(
        header -> header.value(value),
        () -> headers.add(new Header(name,value)));
  }

  public Object getPayload(String expansion) {
    String ss[] = expansion.split("\\.");
    JSONObject obj = payload;
    int n = ss.length - 1;
    for (int i = 0; i < n; i++) {
      if (!obj.has(ss[i])) {
        return null;
      }
      obj = obj.getJSONObject(ss[i]);
    }
    if (!obj.has(ss[n])) {
      return null;
    } else {
      return obj.get(ss[n]);
    }
  }

}
