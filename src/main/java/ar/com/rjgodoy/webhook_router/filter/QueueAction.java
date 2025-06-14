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

public class QueueAction implements Directive {

    private final String queueName;

    public QueueAction(String queueName) {
        if (queueName == null) {
            throw new NullPointerException("queueName cannot be null");
        }
        this.queueName = queueName;
    }

    public String getQueueName() {
        return queueName;
    }

    @Override
    public Result apply(WebHook webhook) {
        // Stub implementation: Print queue name to standard output
        System.out.println("QueueAction: QUEUE " + queueName);
        // TODO: Implement actual queueing logic here
        return Result.NULL; // Or an appropriate Result based on the action's outcome
    }

    @Override
    public String toString() {
        return "QUEUE " + queueName;
    }
}
