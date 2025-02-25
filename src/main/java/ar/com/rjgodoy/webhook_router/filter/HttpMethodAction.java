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
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import org.json.JSONArray;
import org.json.JSONObject;

abstract class HttpMethodAction implements Directive, HasLineNumber {

  protected abstract MacroString getMacro();

  protected abstract String getInto();

  protected abstract Directive getBody();

  protected abstract String getMethodName();

  public abstract static class HttpMethodActionBuilder<T extends HttpMethodActionBuilder<?>> {
    public abstract T macro(MacroString value);

    public abstract T into(String value);

    public abstract T body(Directive value);

    public abstract T lineNumber(int lineNumber);

    public abstract HttpMethodAction build();
  }


  @Override
  public final Result apply(WebHook webhook) {
    String location = getMacro().eval(webhook);
    if (location == null) {
      logError("Macro expanded to null: " + getMacro());
      return Result.FALSE;
    }
    if (location.matches("\\w+::.*")) {
      location = location.replaceFirst("::", "://");
    }
    URI uri;
    try {
      uri = new URI(location);
    } catch (URISyntaxException e) {
      logError("URISyntaxException " + e.getMessage());
      return Result.FALSE;
    }
    if (!uri.isAbsolute()) {
      logError("URI must be absolute");
      return Result.FALSE;
    }
    if (!uri.getScheme().equals("http") && !uri.getScheme().equals("https")) {
      logError("URI scheme must be either http or https");
      return Result.FALSE;
    }
    if (!webhook.context.isDry()) {
      if (!execute(uri, webhook)) {
        return Result.FALSE;
      }
    } else {
      webhook.context.consume();
    }
    return Result.TRUE;
  }

  protected abstract HttpResponse<String> send(HttpClient client, HttpRequest.Builder request,
      WebHook webhook)
      throws IOException, InterruptedException;

  protected boolean forwardCurrentWebhook() {
    return false;
  }

  protected URI decorateURI(URI uri, WebHook webhook) {
    return uri;
  }

  private boolean execute(URI uri, WebHook webhook) {

    WebHook original = webhook;

    if (!forwardCurrentWebhook()) {
      webhook = new WebHook(webhook.context) {
        // MacroExpansion resolves against the original webhook
        @Override
        public Optional<String> getHeader(String name) {
          return original.getHeader(name);
        }

        @Override
        public Object resolve(String expansion) {
          return original.resolve(expansion);
        }
      };
    }

    if (getBody() != null && getBody().apply(webhook) == Result.FALSE) {
      logError("Failed to apply body");
      return false;
    }

    if (getBody() != null) {
      uri = decorateURI(uri, webhook);
    }

    HttpClient client = HttpClient.newBuilder().version(Version.HTTP_1_1)
        .connectTimeout(Duration.ofSeconds(10)).build();

    HttpRequest.Builder request = HttpRequest.newBuilder().uri(uri).timeout(Duration.ofSeconds(60))
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

    HttpResponse<String> response;
    try {
      response = send(client, request, webhook);
    } catch (IOException e) {
      logError(e + " " + uri);
      return false;
    } catch (InterruptedException e) {
      e.printStackTrace();
      return false;
    }

    int sc = response.statusCode();

    boolean into_json = false;

    if (getInto() != null) {
      String contentType = response.headers().firstValue("Content-Type")
          .map(s -> s.replaceFirst(";.*", "")).orElse("");
      if (contentType.equals("application/json")) {
        String responseBody = response.body().trim();
        if (responseBody.startsWith("[")) {
          original.getPayload().put(getInto(), new JSONArray(response.body()));
        } else {
          original.getPayload().put(getInto(), new JSONObject(response.body()));
        }
        into_json = true;
      } else {
        original.getPayload().put(getInto(), response.body());
      }
    }

    if (sc >= 200 && sc < 300) {
      if (getInto() == null) {
        System.out.println("[" + getMethodName() + "] " + response.body());
        original.context.consume();
      }
      return true;
    } else if (into_json) {
      return true;
    } else {
      logError(sc + " " + uri + " " + response.body());
      return false;
    }
  }

  @Override
  public final void logError(String msg) {
    HasLineNumber.super.logError("[" + getMethodName() + "] " + msg);
  }

  @Override
  public String toString() {
    String s = getMethodName() + " " + getMacro();
    if (getInto() != null) {
      s += " INTO " + getInto();
    }
    if (getBody() != null) {
      s += " WITH " + getBody() + "\n";
    }
    return s;
  }

}
