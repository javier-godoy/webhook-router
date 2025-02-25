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
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.json.JSONArray;
import org.json.JSONObject;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = false, exclude = "lineNumber")
@Builder
final class GetAction extends HttpMethodAction {

  public final static class GetActionBuilder extends HttpMethodActionBuilder<GetActionBuilder> {}

  @Getter
  private final int lineNumber;

  @Getter(AccessLevel.PROTECTED)
  private final MacroString macro;

  @Getter(AccessLevel.PROTECTED)
  private final String into;

  @Getter(AccessLevel.PROTECTED)
  private final Directive body;

  @Override
  protected String getMethodName() {
    return "GET";
  }

  private static String encode(String s) {
    return URLEncoder.encode(s, StandardCharsets.ISO_8859_1)
        .replace("%2F", "/")
        .replace("%3F", "?");
  }

  @Override
  protected URI decorateURI(URI uri, WebHook webhook) {
    JSONObject payload = webhook.getPayload();
    StringBuilder query = new StringBuilder(Optional.ofNullable(uri.getQuery()).orElse(""));
    for (Iterator<String> it = payload.keys(); it.hasNext();) {
      String key = it.next();
      Object value = payload.get(key);
      List<Object> values;
      if (value instanceof JSONArray array) {
        values = array.toList();
      } else {
        values = List.of(value);
      }

      String encodedKey = encode(key);

      for (Object v : values) {
        if (!query.isEmpty()) {
          query.append("&");
        }
        query.append(encodedKey).append("=");
        if (v instanceof Double d) {
          v = new BigDecimal(d).toPlainString();
        }
        query.append(encode(String.valueOf(v)));
      }
    }
    try {
      return new URI(uri.getScheme(), uri.getAuthority(), uri.getPath(), query.toString(), null);
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException(e);
    }
  }

  @Override
  protected HttpResponse<String> send(HttpClient client, HttpRequest.Builder request,
      WebHook webhook)
      throws IOException, InterruptedException {
    request.GET();
    return client.send(request.build(), BodyHandlers.ofString());
  }

}
