/**
 * Copyright (C) 2023 - 2024 Roberto Javier Godoy
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
import ar.com.rjgodoy.webhook_router.filter.HttpMethodAction.HttpMethodActionBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;
import lombok.NonNull;

public class DirectiveParser {

  private int lineNumber;
  private Iterator<String> iterator;

  private String next;
  private boolean eof;

  public DirectiveParser(Iterator<String> iterator) {
    this.iterator = iterator;
  }

  private String next() {
    if (next != null) {
      String result = next;
      next = null;
      return result;
    }

    while (iterator.hasNext()) {
      ++lineNumber;
      String line = iterator.next().trim();
      if (line.startsWith("#")) {
        continue;
      }
      if (line.isEmpty()) {
        return "";
      }
      return line.replace('\t', '\s') + " ";
    }
    eof = true;
    return "";
  }

  private String scan() {
    if (next == null) {
      next = next();
    }
    return next;
  }

  private String token() {
    next = scan();
    int n = next.length();
    int i = 0;
    while (i < n && next.charAt(i) == '\s') {
      i++;
    }
    int begin = i;
    while (i < n && next.charAt(i) != '\s') {
      i++;
    }
    String result = next.substring(begin, i);
    while (i < n && next.charAt(i) == '\s') {
      i++;
    }
    if (i < n) {
      next = next.substring(i);
    } else {
      next = null;
    }
    if (result.isEmpty()) {
      throw new RuntimeParserException(lineNumber, "Expected token");
    }
    return result;
  }

  private boolean skip(String token) {
    next = scan();
    if (!next.toUpperCase().startsWith(token.toUpperCase())) {
      return false;
    }

    int i = token.length();
    char c = next.charAt(i);
    if (c == '\s' || c == '{') {
      int n = next.length();
      while (i < n && next.charAt(i) == '\s') {
        i++;
      }
      if (i < n) {
        next = next.substring(i);
      } else {
        next = null;
      }
      return true;
    }

    return false;
  }

  private boolean skip(char c) {
    next = scan();
    int n = next.length();
    int i = 0;
    while (i < n && next.charAt(i) == '\s') {
      i++;
    }
    if (i < n && next.charAt(i) == c) {
      next = next.substring(i + 1);
      skip("");
      return true;
    } else {
      return false;
    }
  }


  private void assertEndOfLine() {
    if (next != null) {
      throw new RuntimeParserException(lineNumber, "Expected end of line");
    }
  }

  public static Directive dry(@NonNull Directive rules) {
    return new AndSequence(Arrays.asList(new DryAction(), rules));
  }

  public Directive parseConfiguration() {
    // configuration = or-sequence
    Directive configuration = parseOrSequence();
    if (!eof) {
      throw new RuntimeParserException(lineNumber, "Expected end of file");
    }
    if (configuration == null) {
      throw new RuntimeParserException(lineNumber, "Expected directive");
    }
    return configuration;
  }

  Directive parseOrSequence() {
    int lineNumber = this.lineNumber;
    try {
      // or-sequence = and-sequence/procedure-decl *(1*CRLF (otherwise-directive / and-sequence /
      // procedure-decl))
      List<Directive> directives = new ArrayList<>();
      Directive otherwise;

      while (true) {
        if (eof) {
          break;
        }
        if (scan().isEmpty()) {
          next();
          continue;
        }

        otherwise = scanOtherwise();
        if (otherwise != null) {
          directives.add(otherwise);
          continue;
        }

        Directive procedure = scanProcedureDecl();
        if (procedure != null) {
          directives.add(procedure);
          continue;
        }

        Directive and = parseAndSequence();
        if (and == null) {
          break;
        }
        directives.add(and);
      }

      switch (directives.size()) {
        case 0:
          return null;
        case 1:
          return directives.get(0);
        default:
          return new OrSequence(directives);
      }
    } catch (RuntimeParserException e) {
      throw RuntimeParserException.chain(lineNumber, e);
    }
  }


  Directive scanOrDirective() {
    return scanOrNorDirective("or", dd -> dd.size() == 1 ? dd.get(0) : new OrDirective(dd));
  }

  Directive scanNorDirective() {
    return scanOrNorDirective("nor", dd -> new NorDirective(dd));
  }

  private Directive scanOrNorDirective(String keyword, Function<List<Directive>, Directive> ctor) {
    int lineNumber = this.lineNumber;
    try {
      // or-directive = "or" 1*(directive CRLF)
      // nor-directive = "nor" 1*(directive CRLF)
      scan();
      if (skip(keyword)) {
        if (!skip('{')) {
          throw new RuntimeParserException(lineNumber);
        }
      } else {
        return null;
      }

      List<Directive> directives = new ArrayList<>();

      while (true) {
        Directive directive = scanDirective();
        if (directive == null) {
          break;
        }
        directives.add(directive);
      }

      if (!skip('}')) {
        throw new RuntimeParserException(lineNumber);
      }
      if (directives.isEmpty()) {
        throw new RuntimeParserException(lineNumber, "Expected directive");
      }
      return ctor.apply(directives);
    } catch (RuntimeParserException e) {
      throw RuntimeParserException.chain(lineNumber, e);
    }
  }

  Directive parseAndSequence() {
    int lineNumber = this.lineNumber;
    try {
      // and-sequence = *(directive CRLF)
      List<Directive> directives = new ArrayList<>();
      while (true) {
        Directive directive = scanDirective();
        if (directive == null) {
          break;
        }
        directives.add(directive);
      }
      switch (directives.size()) {
        case 0:
          return null;
        case 1:
          return directives.get(0);
        default:
          return new AndSequence(directives);
      }
    } catch (RuntimeParserException e) {
      throw RuntimeParserException.chain(lineNumber, e);
    }
  }

  OtherwiseDirective scanOtherwise() {
    int lineNumber = this.lineNumber;
    try {
      if (skip("otherwise")) {
        Directive directive = scanDirective();
        if (directive == null) {
          throw new RuntimeParserException(lineNumber, "Expected directive");
        }
        var otherwise = new OtherwiseDirective(directive);
        if (scan().isEmpty() || eof) {
          return otherwise;
        } else {
          throw new RuntimeParserException(lineNumber, "Expected blank line after otherwise");
        }
      } else {
        return null;
      }
    } catch (RuntimeParserException e) {
      throw RuntimeParserException.chain(lineNumber, e);
    }
  }

  Directive scanDirective() {
    int lineNumber = this.lineNumber;
    try {
      // directive = predicate / action / group / or-directive
      Directive directive;
      directive = scanAction();
      if (directive != null) {
        return directive;
      }
      directive = scanPredicate();
      if (directive != null) {
        return directive;
      }
      directive = scanOrDirective();
      if (directive != null) {
        return directive;
      }
      directive = scanNorDirective();
      if (directive != null) {
        return directive;
      }
      directive = scanGroup();
      return directive;
    } catch (RuntimeParserException e) {
      throw RuntimeParserException.chain(lineNumber, e);
    }
  }

  Directive scanGroup() {
    return scanGroup(false);
  }

  private Directive scanGroup(boolean canBeEmpty) {
    int lineNumber = this.lineNumber;
    try {
      // group = "{" or-sequence "}"
      if (skip('{')) {
        Directive orSeq = parseOrSequence();
        if (!skip('}')) {
          throw new RuntimeParserException(lineNumber);
        }
        if (orSeq == null) {
          if (canBeEmpty) {
            return new OrSequence(List.of());
          } else {
            throw new RuntimeParserException(lineNumber, "Expected directive");
          }
        }
        return orSeq;

      }
      return null;
    } catch (RuntimeParserException e) {
      throw RuntimeParserException.chain(lineNumber, e);
    }
  }

  Directive scanProcedureDecl() {
    // procedure-decl = ""PROCEDURE" <name> group-directive
    try {
      if (skip("PROCEDURE")) {
        String name = token();
        Directive body = scanGroup(true);
        if (body == null) {
          throw new RuntimeParserException(lineNumber, "Expected procedure body");
        }
        return new ProcedureDecl(name, body);
      }
      return null;
    } catch (RuntimeParserException e) {
      throw RuntimeParserException.chain(lineNumber, e);
    }
  }

  MacroString parseMacroString() {
    return parseMacroString(next());
  }

  MacroString parseMacroToken() {
    String token;
    try {
      token = token();
    } catch (RuntimeParserException e) {
      throw new RuntimeParserException(lineNumber, "Expected macro-token");
    }
    return parseMacroString(token);
  }

  private MacroString parseMacroString(String line) {
    List<MacroStringPart> parts = new LinkedList<>();

    // # macro-string = *( macro-expand / macro-literal / macro-escape)
    // # macro-expand = ( "${" variable-name "}" )
    // # macro-escape = "\\" / "\#" / "\$"
    // # macro-literal = %x20-22 / %x25-5B / %x5D-7E; space and visible characters except "#", "$",
    // "\"
    // # variable-name = ALPHA *(ALPHA / DIGIT / "-" / "_")

    line = line.trim();
    int i = 0, n = line.length();
    int begin = 0;

    while (i < n) {
      char c;
      switch (c = line.charAt(i++)) {
        case '\\': {
          parts.add(new MacroLiteral(line.substring(begin, i - 1)));
          // macro-escape = "\\" / "\#" / "\$"
          switch (c = line.charAt(i++)) {
            case '\\':
            case '#':
            case '$':
            case '&':
              begin = i;
              parts.add(new MacroEscape(c));
              continue;
            default:
              throw new RuntimeParserException(lineNumber, "Illegal escape \\" + c);
          }
        }
        case '$': {
          parts.add(new MacroLiteral(line.substring(begin, i - 1)));
          // macro-expand = ( "${" macro-expansion "}" )
          switch (c = line.charAt(i++)) {
            case '{':
              begin = i;
              i = line.indexOf('}', i);
              if (i < 0) {
                throw new RuntimeParserException(lineNumber, "Unterminated macro-expand");
              }
              String str = line.substring(begin, i);
              if (!str.matches("[\\w-\\.]+")) {
                throw new RuntimeParserException(lineNumber,
                    "Illegal macro-expansion ${" + str + "}");
              }
              parts.add(new MacroExpansion(str));
              begin = ++i;
              continue;
            default:
              throw new RuntimeParserException(lineNumber, "Illegal sequence $" + c);
          }
        }
      }
    }
    parts.add(new MacroLiteral(line.substring(begin, i)));
    parts.removeIf(part -> part instanceof MacroLiteral lit && lit.getLiteral().isEmpty());
    if (parts.isEmpty()) {
      throw new RuntimeParserException(lineNumber, "Expected macro-string");
    }
    return new MacroString(parts);
  }

  Directive scanPredicate() {
    int lineNumber = this.lineNumber;
    // # predicate = ["NOT"] <header> ":" <value> # /= "otherwise"
    if (skip("not")) {
      var d = scanPredicate();
      if (d == null) {
        throw new RuntimeParserException(lineNumber, "Expected predicate");
      }
      return new Not(d);
    }

    try {

      var m1 = HEADER_PREDICATE_PATTERN.matcher(scan());
      if (m1.matches()) {
        next();
        return new HeaderPredicate(m1.group(1), m1.group(3).trim(), parseOperator(m1.group(2)));
      }

      var m2 = PAYLOAD_PREDICATE_PATTERN.matcher(scan());
      if (m2.matches()) {
        String s = m2.group(1);
        if (!s.contains("..") && !s.endsWith(".") && !s.startsWith(".")) {
          next();
          if ("is".equalsIgnoreCase(m2.group(2))) {
            return IsPredicate.newInstance(s, m2.group(3).trim());
          } else {
            return new PayloadPredicate(s, m2.group(3).trim(), parseOperator(m2.group(2)));
          }
        }
      }

      if (skip("null")) {
        return new NullPredicate(parseMacroString());
      }

      if (skip("true")) {
        return TruePredicate.INSTANCE;
      }

      return null;
    } catch (RuntimeParserException e) {
      throw RuntimeParserException.chain(lineNumber, e);
    }
  }

  private PredicateOperator parseOperator(String s) {
    return switch (Optional.ofNullable(s).orElse("")) {
      case "" -> PredicateOperator.EQ;
      case "contains" -> PredicateOperator.CONTAINS;
      default -> throw new RuntimeParserException(lineNumber, "Expected ':', ':contains'");
    };
  }

  Directive scanAction() {
    int lineNumber = this.lineNumber;
    try {
      String line = scan().replaceFirst("\\s.*", "").toUpperCase();

      switch (line) {
        case "CALL":
          // action = "CALL" <name>
          skip(line);
          return new CallAction(token());
        case "DROP":
          skip(line);
          assertEndOfLine();
          return new DropAction();
        case "EXIT":
          skip(line);
          assertEndOfLine();
          return new ExitAction();
        case "DRY":
          skip(line);
          // end of line allowed
          return new DryAction();
        case "FOR":
          skip("FOR");
          return parseForAction();
        case "LOG":
          skip("LOG");
          return parseLogAction();
        case "POST", "GET", "DELETE": {
          // action = <method> <macro-string> ["INTO" <token>] ["WITH {" and-sequence "}"]
          skip(line);
          MacroString location = parseMacroToken();
          String into = null;
          Directive body = null;
          while (next != null) {
            if (skip("INTO")) {
              if (into != null) {
                throw new RuntimeParserException(this.lineNumber, "Duplicate clause INTO");
              }
              into = token();
              continue;
            }
            if (skip("WITH")) {
              if (body != null) {
                throw new RuntimeParserException(this.lineNumber, "Duplicate clause WITH");
              }
              body = scanGroup(true);
              if (!(body instanceof OrSequence)) {
                body = new OrSequence(List.of(body));
              }
              continue;
            }
          }
          assertEndOfLine();
          HttpMethodActionBuilder<?> builder = switch(line) {
            case "GET" -> GetAction.builder();
            case "POST" -> PostAction.builder();
            case "DELETE" -> DeleteAction.builder();
            default -> throw new AssertionError();
          };
          return builder.macro(location).into(into).body(body).build();
        }
        case "REENTER": {
          // action = "REENTER" ["COPY"]
          skip(line);
          if (next != null && skip("COPY")) {
            return new ReenterAction(true);
          } else {
            assertEndOfLine();
            return new ReenterAction(false);
          }
        }
        case "SECRET":
          // action = "SECRET" <macro-string>
          skip(line);
          return new SecretAction(parseMacroString());
        case "SET":
          skip(line);
          return parseSetAction();
        default:
          return null;
      }
    } catch (RuntimeParserException e) {
      throw RuntimeParserException.chain(lineNumber, e);
    }
  }

  private Directive parseLogAction() {
    // action = "LOG" <macro-string>
    String line = scan();
    if (line.contains("&&")) {
      do {

        String ss[] = line.split("&&",-1);
        StringBuilder sb1 = new StringBuilder();

        int i;
        for (i=0; i< ss.length; i++) {
          sb1.append(ss[i]);
          if (ss[i].endsWith("\\")) {
            sb1.append("&&");
          } else {
            break;
          }
        }

        if (i==ss.length) {
          break;
        }

        StringBuilder sb2 = new StringBuilder();
        sb2.append(ss[++i]);
        for (++i; i<ss.length;i++) {
          sb2.append("&&").append(ss[i]);
        }

        String a = sb1.toString().trim();
        String b = sb2.toString().trim();
        var macro1 = parseMacroString(a);
        var macro2 = parseMacroString(b);

        next = b + " ";
        var directive = scanAction();
        if (directive == null) {
          throw new RuntimeParserException(lineNumber, "Action expected");
        }
        return new LogAction(macro1, macro2, directive);
      }  while(false);
    }
    return new LogAction(parseMacroString());


  }


  private Directive parseForAction() {
    // "FOR" <variable> "IN" "$" <json-path> <group>
    String variable = token();
    if (!skip("IN")) {
      throw new RuntimeParserException(lineNumber, "Expected FOR variable IN ...");
    }
    String expression = token();
    if (!expression.matches("\\$[\\w\\.]+")) {
      throw new RuntimeParserException(lineNumber, "Expected FOR variable IN <json-path>");
    }
    Directive body = scanGroup();
    if (body == null) {
      throw new RuntimeParserException(lineNumber, "Expected FOR variable IN <json-path> { ... }");
    }
    return new ForAction(variable, expression.substring(1), body);
  }

  private final static Pattern HEADER_PREDICATE_PATTERN = Pattern.compile("([\\w-]+):(\\w+)?(.*)");
  private final static Pattern PAYLOAD_PREDICATE_PATTERN =
      Pattern.compile("\\$([\\w\\.]+):(\\w+)?(.*)");
  private final static Pattern SET_HEADER_PATTERN = Pattern.compile("[\\w-]+:.*");
  private final static Pattern SET_PAYLOAD_PATTERN = PAYLOAD_PREDICATE_PATTERN;

  private Directive parseSetAction() {
    // "SET" <header> ":" <macro-string>
    // "SET" "$"<json-path> ":"[type] <macro-string>

    var m1 = SET_HEADER_PATTERN.matcher(scan());
    if (m1.matches()) {
      Header h = new Header(next().trim());
      MacroString macro = parseMacroString(h.value());
      return new SetHeaderAction(h.name(), macro);
    }

    var m2 = SET_PAYLOAD_PATTERN.matcher(scan());
    if (m2.matches()) {
      String s = m2.group(1);
      if (!s.contains("..") && !s.endsWith(".") && !s.startsWith(".")) {
        String type = m2.group(2);
        MacroString macro = parseMacroString(m2.group(3));
        switch (Optional.ofNullable(type).orElse("")) {
          case "":
          case "string":
          case "number":
          case "boolean":
          case "null":
          case "array":;
          case "object":
            next();
            return new SetPayloadAction(s, type, macro);
          default:
            throw new RuntimeParserException(lineNumber,
                "Expected 'string', 'number', 'boolean', 'array', 'object', 'null'");
        }
      }
    }

    throw new RuntimeParserException(lineNumber, "Expected <SET action>");
  }

}
