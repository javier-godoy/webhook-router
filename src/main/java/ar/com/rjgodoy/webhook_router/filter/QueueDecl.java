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
import lombok.Getter;
import lombok.NonNull;

@Getter
// @AllArgsConstructor(access = AccessLevel.PACKAGE)
public class QueueDecl implements Directive {

  @NonNull
  private final String name;

  @NonNull
  private final Directive body;

  private final RetentionTask maxTasksRetention;
  private final RetentionDays maxDaysRetention;
  private final String retentionPolicyCombinator;

  // Updated constructor to include new fields
  QueueDecl(@NonNull String name, RetentionTask maxTasksRetention, RetentionDays maxDaysRetention,
      String retentionPolicyCombinator, @NonNull Directive body) {
    this.name = name;
    this.maxTasksRetention = maxTasksRetention;
    this.maxDaysRetention = maxDaysRetention;
    this.retentionPolicyCombinator = retentionPolicyCombinator; // Assign new field
    this.body = body;
  }

  public QueueDecl(QueueDecl other, Directive body) {
    this(other.name, other.maxTasksRetention, other.maxDaysRetention, other.retentionPolicyCombinator, body);
  }

  @Override
  public Result apply(WebHook webhook) {
    return Result.NULL;
  }

  public Result call(WebHook webhook) {
    return body == null ? Result.NULL : body.apply(webhook);
  }

}
