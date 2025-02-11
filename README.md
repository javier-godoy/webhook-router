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

#### Predicates
A predicate examines the value of a request header or payload element. 
The result is true if the predicate matches and false otherwise. 
Payload predicates are identified by a $ prefix.

```
X-GitHub-Event: ping
$repository.full_name: javier-godoy/webhook-router
```

The `contains` operator can be specified immediately after the colon.
If no operator is specified, the predicate does an exact match (certainly, there is room for improvement here).
```
$repository.full_name:contains javier-
```

A `NULL` predicate evaluates as true if a macro expression is null.
```
NULL ${foo.bar}
```

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

A `PROCEDURE` declaration introduces a new procedure within the scope of an or sequence.

```
CALL foo

PROCEDURE foo {
   LOG foo
}
```

#### CALL action

The `CALL` action executes a named procedure and returns its result. 
This action returns `false` if the procedure does not exist.

#### GET/POST action
```
GET  <macro-token> ["INTO" $<json-path>] ["WITH" group-directive]
POST <macro-token> ["INTO" $<json-path>] ["WITH" group-directive]
```

The `POST` and `GET` actions send HTTP request to the URI specified by `<macro-token>` (which must expand to an absolute-URI with either `http://` or `https://` schemes).

The optional `INTO` clause specifies a property of the payload that will store the POST response.

The optional `WITH` clause specifies a group directive that will initialize a new request. If `WITH` is not specified, the current webhook will be forwarded.

If the `INTO` clause is not specified, the webhook is considered consumed upon if the request is successful.
In dry mode, as specified by the [DRY action](#dry-action), the request is not sent, but it is still considered consumed.

This action returns `true` if the request is successful (indicated by a response status code in the 2xx range) or a JSON response (with any status code) was captured `INTO` a variable.

TODO: store the response INTO a context variable instead of payload.

#### DROP action

```
DROP
```

The `DROP` action marks the webhook as consumed, without actually posting it anywhere.
Note that `DROP` does _not_ terminates the processing chain for that particular webhook instance: if there are other actions in the processing chain that involve posting a message, they can still be carried out.

This action does not return a logical value.


#### EXIT action

```
EXIT
```

The `EXIT` action terminates the processing chain for a particular webhook instance.
If the webhook instance was copied (see [FOR](#for-action), [REENTER COPY](#reenter-action)), the caller with resume processing of the original instance.

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
In each iteration, a _copy_ of the original webhook is initialized, and the named context variable is assigned with an element from the array. 
The modified webhook is processed by the body of the FOR action.

TODO: iterate over object properties.

TODO: iterate without copying.


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
