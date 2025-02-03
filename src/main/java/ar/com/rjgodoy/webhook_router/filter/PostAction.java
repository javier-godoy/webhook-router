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
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = false)
@Builder
final class PostAction extends HttpMethodAction {

  @Getter(AccessLevel.PROTECTED)
  private final MacroString macro;

  @Getter(AccessLevel.PROTECTED)
  private final String into;

  @Getter(AccessLevel.PROTECTED)
  private final Directive body;

  @Override
  protected String getMethodName() {
    return "POST";
  }

  @Override
  protected HttpResponse<String> send(HttpClient client, HttpRequest.Builder request,
      WebHook webhook)
      throws IOException, InterruptedException {

    String payload = webhook.getPayload().toString();
    request.POST(BodyPublishers.ofString(payload));

    return client.send(request.build(), BodyHandlers.ofString());
  }

}
