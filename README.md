# webhook-router

This project is specifically designed for a particular purpose and is a weekend endeavor. Consequently, it might not directly meet your requirements. However, if you're interested, please feel free to [create an issue](https://github.com/javier-godoy/webhook-router/issues), and let's discuss your use case further.

The counterpart of this router is an Nginx+Lua reverse proxy that exclusively allows connections from [GitHub IP addresses](https://api.github.com/meta), [validates the webhook signature](https://stackoverflow.com/a/43146712/1297272), and stores the headers and payload to a designated spool directory. The webhook router then reads files from that directory and processes them according to predefined configurations, extracting relevant information such as headers and payloads. Subsequently, it directs the processed requests to the appropriate handlers or endpoints for further action, facilitating the execution of associated tasks or processes. Additionally, the router may log information about the handling of each file for monitoring and auditing purposes.

## Features
- Retrieve webhooks from a specified directory.
- Apply filtering and rewriting rules to the webhooks.
- Forward the modified webhooks to their intended destinations.
- Automatically remove the processed webhook upon successful consumption.

## Configuration

A simple configuration file may look as follows:

- Handle `push` events for `example.org`, forwarding them to `http://jenkins:8080` after adjusting the URL path to include the SSH URL from the webhook payload.
- Conduct a dry-run on `ping` events by adjusting the event's header and processing it internally to evaluate whether any relevant actions will be triggered based on the modified event.
- Log, but don't consume, any other events.
 
```
X-GitHub-Event: ping
LOG [${X-GitHub-Delivery}] PING ${host} ${repository.full_name} ${hook.events}
DRY FOR event in $hook.events {
	SET X-GitHub-Event: ${event}
	REENTER
}
DROP

Host: example.org
{
	X-GitHub-Event: push
	LOG [${X-GitHub-Delivery}] & POST http://jenkins:8080/git/notifyCommit?url=${repository.ssh_url}
}

otherwise LOG [${X-GitHub-Delivery}] ${host} ${X-GitHub-Event} from ${repository.full_name} was not handled
```

### Configuration syntax

The configuration is defined through directives, primarily predicates and actions, which are interconnected using logical operators.

#### Logical operators

Logical operators are newline and double-newline (yes you read it right).
Consecutive lines imply short-circuiting AND operations, while empty lines denote the non short-circuiting OR operator. 
For instance, the following configuration is interpreted as `line1 OR (line2 AND line3)`:
```
line1

line2
line3
```

There are also OR/NOR directives, where consecutive lines imply a short-circuiting OR/NOR operation. The following configuration is interpreted as `line1 AND (line2 OR line3)`, where `line3` does not execute if `line2` evaluates as `true` :
```
line1
or {
  line2
  line3
}
```

A standalone brace introduces a new sequence where again consecutive lines imply a short-circuiting AND operation, while empty lines denote the non short-circuiting OR operator. 
The following configuration is interpreted as `line1 AND (line2 OR line3)`, but in this case `line3` executes regardless of the result of `line2`: 
```
line1
{
line2

line3
}
```

Short-circuit OR operations can be alternatively expressed using the `otherwise` keyword, which is constrained to blank-line delimited sequences and dictates that the following directive should execute only if the result so-far is false.

```
line1

otherwise {
 line2
 line3
}
```

Note that the following configuration is ambiguous (and results in a parse error): 
```
line1

otherwise line2
line3
```

#### Payload elements and variables

Payload elements are identified by a `$` prefix (e.g. `${repository.full_name}`).

Global variables are identified by a `%` prefix.

Local variables are identified by a `%%` prefix. A local variable is scoped to the or-sequence (i.e. brace) where it's defined.

#### Predicates
A predicate examines the value of a request header, variable or payload element. 
The result is true if the predicate matches and false otherwise.

```
X-GitHub-Event: ping
$repository.full_name: javier-godoy/webhook-router
%foo: bar
```

The `contains` or `startswith` operators can be specified immediately after the colon.
If no operator is specified, the predicate does an exact match (certainly, there is room for improvement here).

```
$repository.full_name:contains javier-
```

The `is` operator evaluates as true if the payload element is of the corresponding type.

```
$repository:is object
```

A `NULL` predicate evaluates as true if a macro expression is null.

```
NULL ${foo.bar}
```

A `TRUE` predicate always evaluates as true

```
TRUE
```

#### Case directive

The `CASE` directive defines a conditional structure that consists of multiple `WHEN` clause statements (which specify a condition and an associated action), and an optional `ELSE` clause (which defines an alternative action if no when-clause matches). The directive egins with the keyword `CASE` and ends with `ESAC`.

```
CASE
  WHEN X-Foo: foo
  THEN LOG foo
  WHEN X-Bar: bar
       X-Baz: baz
  THEN LOG bar, baz
  ELSE LOG other
ESAC
```

A `CASE` directive executes the first `WHEN` clause that evaluates as true and returns the result of the corresponding actions. If no WHEN clause evaluates as true, the CASE expression returns null.

The `CASE` directive defines a conditional structure consisting of multiple `WHEN` clause statements, each specifying a condition and an associated action, and an optional `ELSE` clause that defines an alternative action if no `WHEN` clause evaluates as true. 
The directive begins with the keyword `CASE` and ends with `ESAC`. 

A `CASE` directive executes and returns the result of the `THEN` actions corresponding to the first `WHEN` clause which evaluates as true. 
If no `WHEN` clause evaluates as true, the `ELSE` clause executes if present; otherwise, the `CASE` directive returns null.

#### Macro Strings

Some actions utilize string-valued arguments, which are represented as the concatenation of literal strings (without quotation marks), escape sequences (`\\`, `\$`, `\#`, `\&`), and macro expansions written as `${macro}`. A macro expansion may resolve to a context variable, a request header, or a payload element. Depending on the action, a macro that resolves to an object or array may be coerced to string (e.g. the LOG action) or cause the action to fail (e.g. the POST action)

```
# A macro string with a single literal string
LOG Hello World

# A macro string with a literal string followed by a escape sequence
LOG Hello World\$

# A macro string with a single macro expansion that resolves to the context variable "event"
LOG ${event}

# A macro string with a single macro expansion that resolves to the request header "X-GitHub-Delivery"
LOG ${X-GitHub-Delivery}

# A macro string with a single macro expansion that resolves to the payload element "repository.full_name"
LOG ${repository.full_name}

# A macro string with a single macro expansion that resolves to the HOSTNAME environment variable
LOG ${env.HOSTNAME}
```
#### PROCEDURE declaration

A `PROCEDURE` declaration introduces a new procedure within the scope of an or-sequence.

```
CALL foo

PROCEDURE foo {
   LOG foo
}
```

#### QUEUE declaration

A `QUEUE` declaration defines a named queue that can be used to store webhooks for asynchronous processing. The body of the `QUEUE` directive specifies the actions to be performed on the webhooks dequeued from this queue.

Syntax:
```
QUEUE <queue-name> [RETENTION (<number_of_tasks> | <number_of_days> DAYS | <number_of_tasks> <number_of_days> DAYS)] {
  # Actions to perform on webhooks from this queue
  LOG Processing webhook from ${queue-name}
}
```

The optional `RETENTION` clause specifies the policy for removing already processed webhooks from the queue's history.
-   `<number_of_tasks>`: If specified, the history will not retain more than this number of processed tasks. If new tasks are processed and the count exceeds this limit, the oldest processed tasks are removed.
-   `<number_of_days> DAYS`: If specified, processed tasks older than this number of days are removed from the history.
-   If both `<number_of_tasks>` and `<number_of_days> DAYS` are specified, both conditions apply independently. A processed task is removed if its age exceeds `<number_of_days> DAYS`, OR if it needs to be removed because the total number of tasks in history would otherwise exceed `<number_of_tasks>` (oldest are removed first). Thus, a task is retained only if it meets both criteria: it is not older than the specified days AND it is recent enough to be within the task count limit.

For example:
- `QUEUE my_queue RETENTION 1000`: Keeps the history of the last 1000 processed tasks. If more tasks are processed, the oldest ones are removed.
- `QUEUE my_queue RETENTION 7 DAYS`: Keeps the history of processed tasks for the last 7 days. Tasks older than 7 days are removed.
- `QUEUE my_queue RETENTION 500 30 DAYS`: Processed tasks older than 30 days are removed. Additionally, if the total number of tasks in the history exceeds 500, the oldest tasks are removed to keep the count at 500. A task is only kept in the history if it is no older than 30 days AND it is among the 500 most recent tasks (if the history size were to exceed 500).

The body of the `QUEUE` directive specifies the actions to be performed on the webhooks dequeued from this queue for active processing.

#### CALL action

The `CALL` action executes a named procedure and returns its result. 
This action returns `false` if the procedure does not exist.

#### HTTP actions
```
<http-method> <macro-token> ["INTO" $<json-path>] ["WITH" group-directive]; token must expand to an absolute-URI
http-method = "POST" / "GET" / "DELETE"
```

The `POST`, `GET` and `DELETE` actions send HTTP request to the URI specified by `<macro-token>` (which must expand to an absolute-URI with either `http://` or `https://` schemes).

The optional `INTO` clause specifies a property of the payload that will store the POST response.

The optional `WITH` clause specifies a group directive that will initialize a new request. If `WITH` is not specified and `http-method` is `POST`, the current webhook will be forwarded.
If `http-method` is `GET`, payload elements will be appended to the query string.

If the `INTO` clause is not specified, the webhook is considered consumed upon if the request is successful.
In dry mode, as specified by the [DRY action](#dry-action), the request is not sent, but it is still considered consumed.

This action returns `true` if the request is successful (indicated by a response status code in the 2xx range) or a JSON response (with any status code) was captured `INTO` a variable.

TODO: store the response INTO a context variable instead of payload.
TODO: `<http-method> ["COPY"]`

#### DROP action

```
DROP
```

The `DROP` action marks the webhook as consumed, without actually posting it anywhere.
Note that `DROP` does _not_ terminates the processing chain for that particular webhook instance: if there are other actions in the processing chain that involve posting a message, they can still be carried out.

This action does not return a logical value.


#### ENQUEUE action

The `ENQUEUE` action places the current webhook into a named queue for asynchronous processing. This queue must be defined using a `QUEUE` declaration. The action returns `true` if the webhook was successfully enqueued, and `false` otherwise.

Syntax:
```
ENQUEUE <queue-name>
```

#### EXIT action

```
EXIT
```

The `EXIT` action terminates the processing chain for a particular webhook instance.
If the webhook instance was copied (see [REENTER COPY](#reenter-action)), the caller with resume processing of the original instance.

This action does not return a logical value.


#### DRY action

```
DRY
```

The DRY action enters "dry-run" mode. During dry-run mode, the webhook undergoes normal processing internally. However, in this mode, GET/POST actions are not executed. This allows for the evaluation of potential triggered actions based on the webhook without actually carrying them out.


#### FOR action

```
FOR variable IN $json.path {...}
```

The FOR action iterates over all the values of an array from the webhook payload. 
In each iteration, the named context variable is assigned with an element from the array. 
The modified webhook is processed by the body of the FOR action.
The FOR action evaluates as null if the json path resolves to an array, and it's null otherwise.

TODO: iterate over object properties.

#### SECRET action
```
SECRET <macro-string>
```

The SECRET action defines a string as secret, so that it's not logged by the LOG action.
This action does not return a logical value.

#### SET action
```
SET header: <macro-string>
```

The SET action sets a response header with the the expansion of a macro string. If the header had already been set, the new value overwrites the previous one.
This action does not return a logical value.

TODO: set payload elements.


#### REENTER action
```
REENTER [COPY]
```

The `REENTER` action submit the webhook message back into the system for further processing. 
`REENTER COPY` duplicates and submits a copy of the original message.
The choice between `REENTER` and `REENTER COPY` affects whether changes will be visible by the caller, as well as the behavior of the [EXIT](#exit-action) action).

In order to avoid potential infinite loops, the resubmission will skip the caller `REENTER` or `REENTER COPY` action.

The action evaluates as `true` if the webhook was submitted (i.e. not a recursive call) and the submission consumed the webhook.

#### LOG action
```
LOG <macro-string>
```

The `LOG` action is a directive that writes text to the standard output (see examples under [macro strings](#macro-strings) above). 
This action does not return a logical value.

A special form `LOG <macro-string> && <action>` allows including another action within the logged string and executing that action simultaneously. In this case, the "LOG" action returns the result of the second action.
This special form is convenient for logging the action that will be executed. For instance, the following actions are equivalent:

```
LOG [${X-GitHub-Delivery}] & POST http://jenkins:8080/git/notifyCommit?url=${repository.ssh_url}

LOG [${X-GitHub-Delivery}] POST http://jenkins:8080/git/notifyCommit?url=${repository.ssh_url}
POST http://jenkins:8080/git/notifyCommit?url=${repository.ssh_url}
```

When using the special form, a double ampersand must be escaped (`\&&`) in the macro-string part.

#### QUEUE and ENQUEUE Example

This example demonstrates how to define a queue and enqueue webhooks to it based on a condition.

```
# Define a queue for processing important jobs
QUEUE my-processing-queue {
  LOG [${X-GitHub-Delivery}] Processing high-priority job from ${repository.full_name} via queue.
  POST http://internal-processor/notifyJob
  # Further actions for items from this queue can be defined here.
}

# Main processing logic
X-Priority-Job: true
ENQUEUE my-processing-queue  # Send to the queue if X-Priority-Job is true

# Fallback for non-priority jobs or if the above condition is false
otherwise {
  LOG [${X-GitHub-Delivery}] Handling ${X-GitHub-Event} from ${repository.full_name} directly.
  POST http://default-handler/webhook
}
```

## Webhook format

A webhook file is structured into three distinct sections, each demarcated by a blank line: the request URI, followed by request headers, and finally, the payload.

```
/hook

Date: Fri, 02 Mar 2024 13:33:50 GMT
X-GitHub-Hook-Installation-Target-ID: 765451645
X-GitHub-Hook-Installation-Target-Type: repository
X-Hub-Signature: sha1=6b5448f7273e6cc74eff619e42a2e6ee0131b70e
X-Hub-Signature-256: sha256=83336403886f724cf9bf5ac10c2b48beab3a5f693341b819b1e6474ab268c491
X-Forwarded-For: 140.82.115.91, 108.162.238.117, 172.16.1.2, 172.16.1.3
X-GitHub-Delivery: b4de50c4-d776-11ee-8ec2-3d36f1b568ae
X-GitHub-Event: push
X-GitHub-Hook-ID: 463906470
CF-Connecting-IP: 140.82.115.91
CF-IPCountry: US
CF-RAY: 85d985648cd207ec-ATL
Host: example.org
Content-Length: 7370
User-Agent: GitHub-Hookshot/940ffa2
Content-Type: application/json

{... JSON payload ...}
```

Given such a file, the webhook router serves as an alternative to less flexible approaches such like:
```
 awk 'BEGIN{FS="\n\n", RS="^$"}{for(i=4; i<=NF; i++) {$3 = $3"\n\n"$i}}{print $1 > "request-uri"}{print $2 > "headers"}{ print $3 > "payload" }' $FILE
 WEBHOOK_URL=http://jenkins:8080$(cat request-uri)
 curl -sS -H @headers --data-binary @payload $WEBHOOK_URL
```
