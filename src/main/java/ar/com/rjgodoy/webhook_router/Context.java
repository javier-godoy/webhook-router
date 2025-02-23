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

import ar.com.rjgodoy.webhook_router.filter.Directive;
import ar.com.rjgodoy.webhook_router.filter.ProcedureDecl;
import ar.com.rjgodoy.webhook_router.filter.Result;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.Getter;
import org.json.JSONObject;

public class Context {

  @Getter(AccessLevel.PACKAGE)
  private final Context parent;

  private final JSONObject variables = new JSONObject();

  private final Set<Directive> reenter = new HashSet<>();

  private final Set<String> secrets = new HashSet<>();

  @Getter
  private Directive rules;

  @Getter
  private boolean consumed;

  @Getter
  private boolean dry;

  private List<ProcedureDecl> procedures = new ArrayList<>();

  Context() {
    parent = null;
  }

  Context(Context parent) {
    this.parent = parent;
    dry = parent.dry;
    consumed = parent.consumed;
    rules = parent.rules;
    procedures = parent.procedures;
  }

  void setRules(Directive rules) {
    this.rules = Objects.requireNonNull(rules);
  }

  public void consume() {
    consumed = true;
  }

  public void dry() {
    dry = true;
  }

  public void set(String name, Object value) {
    variables.put(name, value);
  }

  public void unset(String name) {
    variables.remove(name);
  }

  public class LocalScope implements AutoCloseable {
    private final Set<String> names;

    private LocalScope() {
      names = new HashSet<>(variables.keySet());
    }

    @Override
    public void close() {
      variables.keySet().removeIf(name -> name.startsWith("%") && !names.contains(name));
    }
  }

  public LocalScope newLocalScope() {
    return new LocalScope();
  }

  public Object get(String name) {
    String ss[] = name.split("\\.");

    JSONObject obj = variables;
    int n = ss.length - 1;
    for (int i = 0; i < n; i++) {
      if (!obj.has(ss[i])) {
        return parent != null ? parent.get(name) : null;
      }
      obj = obj.getJSONObject(ss[i]);
    }

    if (!obj.has(ss[n])) {
      return parent != null ? parent.get(name) : null;
    } else {
      return obj.get(ss[n]);
    }
  }

  public boolean reenter(WebHook webhook, Directive directive) {
    if (parent != null && parent.reenter.contains(directive)) {
      return false;
    }
    if (!reenter.add(directive)) {
      return false;
    }

    webhook.context.getRules().apply(webhook);
    return true;
  }

  public void addSecret(String string) {
    if (string != null && !string.isEmpty()) {
      secrets.add(string);
    }
  }

  public Stream<String> getSecrets() {
    Stream<String> stream = secrets.stream();
    if (parent != null) {
      stream = Stream.concat(stream, parent.getSecrets());
    }
    return stream;
  }

  public void declare(ProcedureDecl proc) {
    procedures.add(proc);
  }

  public void undeclare(ProcedureDecl proc) {
    procedures.remove(proc);
  }

  public Result call(WebHook webhook, String procedureName) {
    for (int i = procedures.size(); i-- > 0;) {
      ProcedureDecl proc = procedures.get(i);
      if (proc.getName().equals(procedureName)) {
        return proc.call(webhook);
      }
    }
    System.err.println("No such procedure: " + procedureName);
    return Result.FALSE;
  }

}
