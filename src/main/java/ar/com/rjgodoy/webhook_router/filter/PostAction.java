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

import ar.com.rjgodoy.webhook_router.Header;
import ar.com.rjgodoy.webhook_router.WebHook;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode
@Builder
final class PostAction implements Directive {

  @Getter(AccessLevel.PACKAGE)
  private final MacroString macro;

  private final Directive body;

  private final String into;

  @Override
  public Result apply(WebHook webhook) {
    String location = macro.eval(webhook, false);
    if (location == null) {
      System.err.println("[POST] Macro expanded to null: " + macro);
      return Result.FALSE;
    }
    URI uri;
    try {
      uri = new URI(location);
    } catch (URISyntaxException e) {
      System.err.println("[POST] URISyntaxException " + e.getMessage());
      return Result.FALSE;
    }
    if (!uri.isAbsolute()) {
      System.err.println("[POST] URI must be absolute");
      return Result.FALSE;
    }
    if (!uri.getScheme().equals("http") && !uri.getScheme().equals("https")) {
      System.err.println("[POST] URI scheme must be either http or https");
      return Result.FALSE;
    }
    if (!webhook.context.isDry()) {
      if (!post(uri, webhook)) {
        return Result.FALSE;
      }
    } else {
      webhook.context.consume();
    }
    return Result.TRUE;
  }

  private boolean post(URI uri, WebHook webhook) {

    WebHook original = webhook;

    if (body!=null) {
      webhook = new WebHook(null, List.of(), new JSONObject()) {
        // MacroExpansion resolves against the original webhook
        @Override
        public Optional<String> getHeader(String name) {
          return original.getHeader(name);
        }

        @Override
        public Object getPayload(String expansion) {
          return original.getPayload(expansion);
        }
      };

      if (body.apply(webhook) == Result.FALSE) {
        return false;
      }
    }

    HttpClient client = HttpClient.newBuilder()
          .version(Version.HTTP_1_1)
          .connectTimeout(Duration.ofSeconds(10))
          .build();

    HttpRequest.Builder request = HttpRequest.newBuilder().uri(uri)
        .timeout(Duration.ofSeconds(60))
        .header("Content-Type", "application/json");

    for (Header header : webhook.getHeaders()) {
      String name = header.name().toLowerCase();
      if (name.equals("content-length")) {
        continue;
      }
      if (name.equals("host")) {
        continue;
      }
      request.header(header.name(), header.value());
    }

    String payload = webhook.getPayload().toString();
    request.POST(BodyPublishers.ofString(payload));

    HttpResponse<String> response;
    try {
      response = client.send(request.build(), BodyHandlers.ofString());
    } catch (IOException e) {
      System.err.println("[POST] " + e + " " + uri);
      return false;
    } catch (InterruptedException e) {
      e.printStackTrace();
      return false;
    }

    int sc = response.statusCode();

    boolean into_json = false;

    if (into != null) {
      String contentType = response.headers().firstValue("Content-Type")
          .map(s -> s.replaceFirst(";.*", "")).orElse("");
      if (contentType.equals("application/json")) {
        original.getPayload().put(into, new JSONObject(response.body()));
        into_json = true;
      } else {
        original.getPayload().put(into, response.body());
      }
    }

    if (sc >= 200 && sc < 300) {
      if (into != null) {
        System.out.println(response.body());
        webhook.context.consume();
      }
      return true;
    } else if (into_json) {
      return true;
    } else {
      System.err.println("[POST] " + sc + " " + uri + " " + response.body());
      return false;
    }
  }

  @Override
  public String toString() {
    String s = "POST " + macro;
    if (into != null) {
      s += " INTO " + into;
    }
    if (body != null) {
      s += " WITH " + body + "\n";
    }
    return s;

  }
}
