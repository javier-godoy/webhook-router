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
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@EqualsAndHashCode
final class PostAction implements Directive {

  @Getter(AccessLevel.PACKAGE)
  private final MacroString macro;

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
    }
    webhook.context.consume();
    return Result.TRUE;
  }

  private boolean post(URI uri, WebHook webhook) {
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

    request.POST(BodyPublishers.ofString(webhook.getPayload().toString()));

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
    if (sc >= 200 && sc < 300) {
      System.out.println(response.body());
      return true;
    } else {
      System.err.println("[POST] " + sc + " " + uri + " " + response.body());
      return false;
    }
  }

  @Override
  public String toString() {
    return "POST " + macro;
  }
}
