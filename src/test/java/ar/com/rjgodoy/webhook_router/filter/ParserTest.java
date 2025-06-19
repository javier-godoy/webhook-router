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

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.isA;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals; // Added
import static org.junit.jupiter.api.Assertions.assertNull;   // Added

import ar.com.rjgodoy.webhook_router.Header;
import ar.com.rjgodoy.webhook_router.filter.CaseDirective.ElseClause;
import ar.com.rjgodoy.webhook_router.filter.CaseDirective.WhenClause;
import java.util.Arrays;
import java.util.List;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.MethodName.class)
public class ParserTest {

  private DirectiveParser parser(String lines) {
    return new DirectiveParser(Arrays.asList(lines.split("\n")).iterator());
  }

  private Matcher<Directive> isADropAction() {
    return isA(DropAction.class.asSubclass(Directive.class));
  }

  @Test
  public void testParseConfigurationEOF() {
    assertThat(assertThrows(RuntimeParserException.class, () -> {
      parser("drop\nfoobar").parseConfiguration();
    }).getMessage(), containsString("Expected end of file;"));
  }

  @Test
  public void testParseConfigurationEmpty() {
    assertThat(assertThrows(RuntimeParserException.class, () -> {
      parser("").parseConfiguration();
    }).getMessage(), containsString("Expected directive;"));
  }

  @Test
  public void testScanGroup0() {
    assertThat(assertThrows(RuntimeParserException.class, () -> {
      parser("{}").scanGroup();
    }).getMessage(), containsString("Expected directive;"));
  }

  @Test
  public void testScanGroup1() {
    var d = parser("{\ndrop\n}").scanGroup();
    assertThat(d, isADropAction());
    assertThat(parser("{ drop\n}").scanGroup(), is(d));
    assertThat(parser("{drop\n}").scanGroup(), is(d));
    assertThat(parser("{drop\n}").scanDirective(), is(d));
  }

  @Test
  public void testScanGroupAndSequence() {
    String line = "{drop\ndrop\n}";
    var d = parser(line).scanGroup();
    assertThat(d, isA(AndSequence.class.asSubclass(Directive.class)));
  }

  @Test
  public void testScanGroupOrSequence() {
    String line = "{drop\n\ndrop\n}";
    var d = parser(line).scanGroup();
    assertThat(d, isA(OrSequence.class.asSubclass(Directive.class)));
  }

  @Test
  public void testScanOrDirective() {
    assertThat(assertThrows(RuntimeParserException.class, () -> {
      parser("or {}").scanOrDirective();
    }).getMessage(), containsString("Expected directive;"));
  }

  @Test
  public void testScanOrDirective1() {
    var d = parser("or\n{\n drop\n }").scanOrDirective();
    assertThat(d, isADropAction());
    assertThat(parser("or {\ndrop\n}").scanOrDirective(), is(d));
    assertThat(parser("or {drop\n}").scanOrDirective(), is(d));
    assertThat(parser("or{drop\n}").scanOrDirective(), is(d));
    assertThat(parser("or{drop\n}").scanDirective(), is(d));
  }

  @Test
  public void testScanOrDirective2() {
    var d = parser("or\n {\n drop\n drop\n }").scanOrDirective();
    assertThat(d, isA(OrDirective.class.asSubclass(Directive.class)));
    var dd = ((OrDirective) d).getDirectives();
    assertThat(dd, hasSize(2));
    assertThat(dd.get(0), isADropAction());
    assertThat(dd.get(1), isADropAction());
    assertThat(parser("or {\ndrop\ndrop\n}").scanOrDirective(), is(d));
    assertThat(parser("or{drop\ndrop\n}").scanOrDirective(), is(d));
    assertThat(parser("or{drop\ndrop\n}").scanDirective(), is(d));
  }

  @Test
  public void testScanNorDirective() {
    assertThat(assertThrows(RuntimeParserException.class, () -> {
      parser("nor {}").scanNorDirective();
    }).getMessage(), containsString("Expected directive;"));
  }

  @Test
  public void testScanNorDirective1() {
    var d = parser("nor\n{\n drop\n }").scanNorDirective();
    assertThat(d, isA(NorDirective.class.asSubclass(Directive.class)));
    var dd = ((NorDirective) d).getDirectives();
    assertThat(dd, hasSize(1));
    assertThat(dd.get(0), isADropAction());
    assertThat(parser("nor {\ndrop\n}").scanNorDirective(), is(d));
    assertThat(parser("nor {drop\n}").scanNorDirective(), is(d));
    assertThat(parser("nor{drop\n}").scanNorDirective(), is(d));
    assertThat(parser("nor{drop\n}").scanDirective(), is(d));
  }

  @Test
  public void testScanNorDirective2() {
    var d = parser("nor\n {\n drop\n drop\n }").scanNorDirective();
    assertThat(d, isA(NorDirective.class.asSubclass(Directive.class)));
    var dd = ((NorDirective) d).getDirectives();
    assertThat(dd, hasSize(2));
    assertThat(dd.get(0), isADropAction());
    assertThat(dd.get(1), isADropAction());
    assertThat(parser("nor {\ndrop\ndrop\n}").scanNorDirective(), is(d));
    assertThat(parser("nor{drop\ndrop\n}").scanNorDirective(), is(d));
    assertThat(parser("nor{drop\ndrop\n}").scanDirective(), is(d));
  }

  @Test
  public void testAndSequence0() {
    assertThat(parser("").parseAndSequence(), is(nullValue()));
  }

  @Test
  public void testAndSequence1() {
    String lines = "drop";
    Directive d = parser(lines).parseAndSequence();
    assertThat(d, isADropAction());
    assertThat(parser(lines).parseOrSequence(), is(d));
  }

  @Test
  public void testAndSequence2() {
    String lines = "drop\ndrop";
    Directive d = parser(lines).parseAndSequence();
    assertThat(d, isA(AndSequence.class.asSubclass(Directive.class)));
    var dd = ((AndSequence) d).getDirectives();
    assertThat(dd, hasSize(2));
    assertThat(dd.get(1), isADropAction());
    assertThat(parser(lines).parseOrSequence(), is(d));
  }

  @Test
  public void testAndSequence2_1() {
    String lines = "drop\ndrop\n\ndrop";
    Directive d = parser(lines).parseAndSequence();
    assertThat(d, isA(AndSequence.class.asSubclass(Directive.class)));
    var dd = ((AndSequence) d).getDirectives();
    assertThat(dd, hasSize(2));
  }

  @Test
  public void testOrSequence2_1() {
    String lines = "drop\ndrop\n\ndrop";
    Directive d = parser(lines).parseOrSequence();
    assertThat(d, isA(OrSequence.class.asSubclass(Directive.class)));
    var dd = ((OrSequence) d).getDirectives();
    assertThat(dd, hasSize(2));
    assertThat(dd.get(0), is(parser("drop\ndrop").parseAndSequence()));
    assertThat(dd.get(1), isADropAction());
  }

  @Test
  public void testOrSequenceOtherwise() {
    String lines = "drop\n\notherwise drop\n\ndrop";
    Directive d = parser(lines).parseOrSequence();
    assertThat(d, isA(OrSequence.class.asSubclass(Directive.class)));
    var dd = ((OrSequence) d).getDirectives();
    assertThat(dd, hasSize(3));
    assertThat(dd.get(0), isADropAction());
    assertThat(dd.get(1), is(parser("otherwise drop").scanOtherwise()));
    assertThat(dd.get(2), isADropAction());
  }

  @Test
  public void testOrSequenceOtherwiseInvalid() {
    assertThat(assertThrows(RuntimeParserException.class, () -> {
      String lines = "drop\n\notherwise drop\ndrop";
      parser(lines).parseOrSequence();
    }).getMessage(), containsString("Expected blank line after otherwise;"));
  }

  @Test
  public void testScanOtherwise() {
    var d = parser("otherwise post x").scanOtherwise();
    assertThat(d.getDirective(), isA(PostAction.class.asSubclass(Directive.class)));
  }

  @Test
  public void testScanOtherwiseExpecteDirective() {
    assertThat(assertThrows(RuntimeParserException.class, () -> {
      parser("otherwise !!").scanOtherwise();
    }).getMessage(), containsString("Expected directive;"));
  }

  @Test
  public void testScanOtherwiseEOF() {
    assertThat(assertThrows(RuntimeParserException.class, () -> {
      parser("otherwise").scanOtherwise();
    }).getMessage(), containsString("Expected directive;"));
  }

  @Test
  public void testScanPredicateHeader() {
    String line = "x-foo-bar: x";
    var d = parser(line).scanPredicate();
    Header h = new Header(line);
    assertThat(d, is(new HeaderPredicate(0, h.name(), h.value(), PredicateOperator.EQ)));
    assertThat(parser(line).scanDirective(), is(d));
  }

  @Test
  public void testScanPredicateHeaderWithOperator() {
    String line = "x-foo-bar:contains x";
    var d = parser(line).scanPredicate();
    assertThat(d, is(new HeaderPredicate(0, "x-foo-bar", "x", PredicateOperator.CONTAINS)));
    assertThat(parser(line).scanDirective(), is(d));
  }

  @Test
  public void testScanPredicatePayload() {
    String line = "$foo.bar: x ";
    var d = parser(line).scanPredicate();
    assertThat(d, is(new PayloadPredicate(0, "$foo.bar", "x", PredicateOperator.EQ)));
    assertThat(parser(line).scanDirective(), is(d));
  }

  @Test
  public void testScanPredicatePayloadWithOperator() {
    String line = "$foo.bar:contains x ";
    var d = parser(line).scanPredicate();
    assertThat(d, is(new PayloadPredicate(0, "$foo.bar", "x", PredicateOperator.CONTAINS)));
    assertThat(parser(line).scanDirective(), is(d));
  }

  @Test
  public void testScanIsPredicate() {
    String line = "$foo.bar:is string";
    var d = parser(line).scanPredicate();
    assertThat(d, is(notNullValue()));
    assertThat(d, is(IsPredicate.newInstance("$foo.bar", "string")));
    assertThat(parser(line).scanDirective(), is(d));
  }

  @Test
  public void testScanIsPredicateInvalidType() {
    String line = "$foo.bar:is foo";
    var d = parser(line).scanPredicate();
    assertThat(d, is(nullValue()));
  }

  @Test
  public void testScanIsPredicateNiType() {
    String line = "$foo.bar:is";
    var d = parser(line).scanPredicate();
    assertThat(d, is(nullValue()));
  }

  @Test
  public void testScanPredicateNull() {
    String line = "NULL ${foo.bar}";
    var d = parser(line).scanPredicate();
    assertThat(d, is(new NullPredicate(new MacroString(List.of(new MacroExpansion("foo.bar"))))));
    assertThat(parser(line).scanDirective(), is(d));
  }

  @Test
  public void testScanPredicateTrue() {
    String line = "TRUE";
    var d = parser(line).scanPredicate();
    assertThat(d, is(TruePredicate.INSTANCE));
    assertThat(parser(line).scanDirective(), is(d));
  }

  @Test
  public void testScanPredicateInvalid() {
    assertThat(parser("x").scanPredicate(), is(nullValue()));
  }

  @Test
  public void testScanPredicateInvalidNot() {
    assertThat(assertThrows(RuntimeParserException.class, () -> {
      parser("NOT x").scanPredicate();
    }).getMessage(), containsString("Expected predicate;"));
  }

  @Test
  public void testScanPredicateEOF() {
    assertThat(parser("").scanPredicate(), is(nullValue()));
  }

  @Test
  public void testScanActionDrop() {
    String line = "DROP";
    var d = parser(line).scanAction();
    assertThat(d, isADropAction());
    assertThat(parser(line).scanDirective(), is(d));
  }

  @Test
  public void testScanActionDropNotEmpty() {
    assertThat(assertThrows(RuntimeParserException.class, () -> {
      parser("DROP foo").scanAction();
    }).getMessage(), containsString("Expected end of line;"));
  }

  @Test
  public void testScanActionExit() {
    String line = "EXIT";
    var d = parser(line).scanAction();
    assertThat(d, isA(ExitAction.class.asSubclass(Directive.class)));
    assertThat(parser(line).scanDirective(), is(d));
  }

  @Test
  public void testScanActionExitNotEmpty() {
    assertThat(assertThrows(RuntimeParserException.class, () -> {
      parser("EXIT foo").scanAction();
    }).getMessage(), containsString("Expected end of line;"));
  }

  @Test
  public void testScanActionLog() {
    String line = "LOG ${x}";
    var d = parser(line).scanAction();
    assertThat(d, isA(LogAction.class.asSubclass(Directive.class)));
    var parts = ((LogAction) d).getMacro().parts();
    assertThat(parts, hasSize(1));
    assertThat(parts.get(0), is(new MacroExpansion("x")));
    assertThat(parser(line).scanDirective(), is(d));
  }

  @Test
  public void testScanActionLogConcat() {
    String line = "LOG x && POST ${y}";
    Directive d = parser(line).scanAction();
    assertThat(d, isA(LogAction.class.asSubclass(Directive.class)));
    assertThat(((LogAction) d).getMacro().toString(), is("x POST ${y}"));
    assertThat(((LogAction) d).getNext(), is(parser("POST ${y}").scanAction()));
  }

  @Test
  public void testScanActionLogEscape() {
    String line = "LOG x\\&&y && POST w&&z";
    Directive d = parser(line).scanAction();
    assertThat(d, isA(LogAction.class.asSubclass(Directive.class)));
    assertThat(((LogAction) d).getMacro().toString(), is("x\\&&y POST w&&z"));
    assertThat(((LogAction) d).getNext(), is(parser("POST w&&z").scanAction()));
  }

  @Test
  public void testScanActionLogConcatIgnoreResult() {
    String line = "LOG x && LOG ${y}";
    Directive d = parser(line).scanAction();
    assertThat(d, isA(LogAction.class.asSubclass(Directive.class)));
    assertThat(((LogAction) d).getMacro().toString(), is("x LOG ${y}"));
    assertThat(((LogAction) d).getNext(), is(parser("LOG ${y}").scanAction()));
  }

  @Test
  public void testScanActionLogEmpty() {
    assertThat(assertThrows(RuntimeParserException.class, () -> {
      parser("LOG").scanAction();
    }).getMessage(), containsString("Expected macro-string;"));
  }

  @Test
  public void testScanActionPost() {
    String line = "POST ${x}";
    var d = parser(line).scanAction();
    assertThat(d, isA(PostAction.class.asSubclass(Directive.class)));
    var parts = ((PostAction) d).getMacro().parts();
    assertThat(parts, hasSize(1));
    assertThat(parts.get(0), is(new MacroExpansion("x")));
    assertThat(parser(line).scanDirective(), is(d));
  }

  @Test
  public void testScanActionPostEmpty() {
    assertThat(assertThrows(RuntimeParserException.class, () -> {
      parser("POST").scanAction();
    }).getMessage(), containsString("Expected macro-token;"));
  }

  @Test
  public void testScanActionUnknown() {
    assertThat(parser("x").scanAction(), is(nullValue()));
  }

  @Test
  public void testScanActionEOF() {
    assertThat(parser("").scanAction(), is(nullValue()));
  }

  @Test
  public void testScanActionDry() {
    String line = "DRY";
    var d = parser(line).scanAction();
    assertThat(d, isA(DryAction.class.asSubclass(Directive.class)));
    assertThat(parser(line).scanDirective(), is(d));
  }

  @Test
  public void testScanActionDryDry() {
    var parser = parser("DRY DRY");
    var d1 = parser.scanAction();
    var d2 = parser.scanAction();
    var d3 = parser.scanAction();
    assertThat(d1, isA(DryAction.class.asSubclass(Directive.class)));
    assertThat(d2, isA(DryAction.class.asSubclass(Directive.class)));
    assertThat(d3, is(nullValue()));
  }

  @Test
  public void testScanActionReenter() {
    String line = "REENTER";
    var d = parser(line).scanAction();
    assertThat(d, isA(ReenterAction.class.asSubclass(Directive.class)));
    assertFalse(((ReenterAction) d).isCopy());
  }

  @Test
  public void testScanActionReenterCopy() {
    String line = "REENTER COPY";
    var d = parser(line).scanAction();
    assertThat(d, isA(ReenterAction.class.asSubclass(Directive.class)));
    assertTrue(((ReenterAction) d).isCopy());
  }

  @Test
  public void testScanActionReenterNotEmpty() {
    assertThat(assertThrows(RuntimeParserException.class, () -> {
      parser("REENTER foo").scanAction();
    }).getMessage(), containsString("Expected end of line;"));
  }

  @Test
  public void testScanForAction() {
    String line = "FOR var in $payload.array { DROP\n }";
    var d = parser(line).scanAction();
    assertThat(d, isA(ForAction.class.asSubclass(Directive.class)));
    assertThat(((ForAction) d).getVariable(), is("var"));
    assertThat(((ForAction) d).getArrayName(), is("$payload.array"));
    assertThat(((ForAction) d).getBody(), isADropAction());
  }

  @Test
  public void testSecretAction() {
    String line = "SECRET ${x}";
    var d = parser(line).scanAction();
    assertThat(d, isA(SecretAction.class.asSubclass(Directive.class)));
    var parts = ((SecretAction) d).getMacro().parts();
    assertThat(parts, hasSize(1));
    assertThat(parts.get(0), is(new MacroExpansion("x")));
    assertThat(parser(line).scanDirective(), is(d));
  }

  @Test
  public void testSecretActionEmpty() {
    assertThat(assertThrows(RuntimeParserException.class, () -> {
      parser("SECRET").scanAction();
    }).getMessage(), containsString("Expected macro-string;"));
  }

  @Test
  public void testSetAction() {
    String line = "SET X-Header: ${value}";
    var d = parser(line).scanAction();
    assertThat(d, isA(SetHeaderAction.class.asSubclass(Directive.class)));
    assertThat(((SetHeaderAction) d).getName(), is("X-Header"));
    assertThat(((SetHeaderAction) d).getMacro(), is(parser("${value}").parseMacroString()));
  }

  @Test
  public void testSetActionIllegal() {
    assertThat(assertThrows(RuntimeParserException.class, () -> {
      parser("SET X.Header: ${value}").scanAction();
    }).getMessage(), containsString("Expected <SET action>"));
  }

  @Test
  public void testMacroLiteral() {
    var str = parser("foo bar").parseMacroString();
    assertThat(str.parts(), hasSize(1));
    assertThat(str.parts().get(0), is(new MacroLiteral("foo bar")));
  }

  @Test
  public void testMacroEscape5C() {
    var str = parser("\\\\").parseMacroString();
    assertThat(str.parts(), hasSize(1));
    assertThat(str.parts().get(0), is(new MacroEscape('\\')));
  }

  @Test
  public void testMacroEscape23() {
    var str = parser("\\#").parseMacroString();
    assertThat(str.parts(), hasSize(1));
    assertThat(str.parts().get(0), is(new MacroEscape('#')));
  }

  @Test
  public void testMacroEscape24() {
    var str = parser("\\$").parseMacroString();
    assertThat(str.parts(), hasSize(1));
    assertThat(str.parts().get(0), is(new MacroEscape('$')));
  }

  @Test
  public void testMacroEscape26() {
    var str = parser("\\&").parseMacroString();
    assertThat(str.parts(), hasSize(1));
    assertThat(str.parts().get(0), is(new MacroEscape('&')));
  }


  @Test
  public void testMacroExpansion() {
    var str = parser("${foo.bar}").parseMacroString();
    assertThat(str.parts(), hasSize(1));
    assertThat(str.parts().get(0), is(new MacroExpansion("foo.bar")));
  }

  @Test
  public void testIllegalMacroExpansion1() {
    assertThat(assertThrows(RuntimeParserException.class, () -> {
      parser("${!foobar}").parseMacroString();
    }).getMessage(), containsString("Illegal macro-expansion ${!foobar};"));
  }

  @Test
  public void testIllegalMacroExpansion2() {
    assertThat(assertThrows(RuntimeParserException.class, () -> {
      parser("${foobar!}").parseMacroString();
    }).getMessage(), containsString("Illegal macro-expansion ${foobar!};"));
  }

  @Test
  public void testIllegalMacroExpansion3() {
    assertThat(assertThrows(RuntimeParserException.class, () -> {
      parser("${foo!bar}").parseMacroString();
    }).getMessage(), containsString("Illegal macro-expansion ${foo!bar};"));
  }

  @Test
  public void testIllegalEscape() {
    assertThat(assertThrows(RuntimeParserException.class, () -> {
      parser("\\!").parseMacroString();
    }).getMessage(), containsString("Illegal escape \\!;"));
  }

  @Test
  public void testUnterminatedMacroExpand() {
    assertThat(assertThrows(RuntimeParserException.class, () -> {
      parser("${foo").parseMacroString();
    }).getMessage(), containsString("Unterminated macro-expand;"));
  }

  @Test
  public void testIllegalSequence() {
    assertThat(assertThrows(RuntimeParserException.class, () -> {
      parser("$x").parseMacroString();
    }).getMessage(), containsString("Illegal sequence $x;"));
  }

  @Test
  public void testMacroStringEOF() {
    assertThat(assertThrows(RuntimeParserException.class, () -> {
      parser("").parseMacroString();
    }).getMessage(), containsString("Expected macro-string;"));
  }

  @Test
  public void testProcedure() {
    var d = parser("procedure foo {}").scanProcedureDecl();
    assertThat(d, isA(ProcedureDecl.class.asSubclass(Directive.class)));
    assertThat(d, is(new ProcedureDecl("foo", new OrSequence(List.of()))));
  }

  @Test
  public void testProcedureIncomplete() {
    assertThat(assertThrows(RuntimeParserException.class, () -> {
      parser("procedure foo").scanProcedureDecl();
    }).getMessage(), containsString("Expected procedure body;"));
  }

  @Test
  public void testCall() {
    var d = parser("call foo").scanAction();
    assertThat(d, isA(CallAction.class.asSubclass(Directive.class)));
    assertThat(d, is(new CallAction(0, "foo")));
  }

  @Test
  public void testPost() {
    var d = parser("POST http://example.com").scanAction();
    assertThat(d, isA(PostAction.class.asSubclass(Directive.class)));
    assertThat(d, is(PostAction.builder().macro(new MacroString("http://example.com")).build()));
  }

  @Test
  public void testGet() {
    var d = parser("GET http://example.com").scanAction();
    assertThat(d, isA(GetAction.class.asSubclass(Directive.class)));
    assertThat(d, is(GetAction.builder().macro(new MacroString("http://example.com")).build()));
  }

  @Test
  public void testDelete() {
    var d = parser("DELETE http://example.com").scanAction();
    assertThat(d, isA(DeleteAction.class.asSubclass(Directive.class)));
    assertThat(d, is(DeleteAction.builder().macro(new MacroString("http://example.com")).build()));
  }

  @Test
  public void testPostInto() {
    var d = parser("POST http://example.com INTO $foo").scanAction();
    assertThat(d, isA(PostAction.class.asSubclass(Directive.class)));
    assertThat(d,
        is(PostAction.builder().macro(new MacroString("http://example.com")).into("$foo").build()));
  }

  @Test
  public void testPostIntoWith() {
    var d = parser("POST http://example.com INTO $foo WITH {}").scanAction();
    assertThat(d, isA(PostAction.class.asSubclass(Directive.class)));
    assertThat(d,
        is(PostAction.builder().macro(new MacroString("http://example.com")).into("$foo")
            .body(new OrSequence(List.of())).build()));
  }

  @Test
  public void testPostWithActions() {
    var d = parser("""
        POST http://example.com  WITH {
            SET X-Foo: foo
            SET $bar: bar
            SET $baz:string baz
        }""").scanAction();
    assertThat(d, isA(PostAction.class.asSubclass(Directive.class)));
    assertThat(d,
        is(PostAction.builder().macro(new MacroString("http://example.com"))
            .body(new OrSequence(
                List.<Directive>of(new AndSequence(
                    List.<Directive>of(
                        new SetHeaderAction(0, "X-Foo", new MacroString("foo")),
                        new SetPayloadAction(0, "$bar", null, new MacroString("bar")),
                        new SetPayloadAction(0, "$baz", "string", new MacroString("baz"))
                    )))))
            .build()));
  }

  @Test
  public void testCase() {
    var d = parser("""
        CASE
            WHEN X-Foo: foo
            THEN DROP
        ESAC
        }""").scanDirective();
    assertThat(d, isA(CaseDirective.class.asSubclass(Directive.class)));
    assertThat(d,
        is(new CaseDirective(
            List.of(new WhenClause(new HeaderPredicate(0, "X-Foo", "foo", PredicateOperator.EQ), new DropAction())),
            null)));
  }

  @Test
  public void testCase2() {
    var d = parser("""
        CASE
            WHEN X-Foo: foo
            THEN DROP
            WHEN X-Bar: bar
                 X-Baz: baz
            THEN DROP
            ELSE DROP
        ESAC
        }""").scanDirective();
    assertThat(d, isA(CaseDirective.class.asSubclass(Directive.class)));
    assertThat(d,
        is(new CaseDirective(
            List.of(
                new WhenClause(new HeaderPredicate(0, "X-Foo", "foo", PredicateOperator.EQ), new DropAction()),
                new WhenClause(new AndSequence(List.of(
                    new HeaderPredicate(0, "X-Bar", "bar", PredicateOperator.EQ),
                    new HeaderPredicate(0, "X-Baz", "baz", PredicateOperator.EQ)
                )), new DropAction())
            ),
            new ElseClause(new DropAction()))));
  }
/*
   @Test
   public void testEnqueueActionSimple() {
       String script = "ENQUEUE myTestQueue";
       Directive result = parser(script).parseConfiguration();
       assertTrue(result instanceof EnqueueAction);
       EnqueueAction enqueueAction = (EnqueueAction) result;
       assertEquals("myTestQueue", enqueueAction.getQueueName());
   }

   @Test
   public void testEnqueueActionMissingName() {
       String script = "ENQUEUE";
       Iterator<String> iterator = script.lines().iterator();
       RuntimeParserException e = assertThrows(RuntimeParserException.class, () -> {
           parser(iterator).parseConfiguration();
       });
       // token() will throw "Expected token; at line 1"
       // This will be caught by scanAction(), then chained by parseOrSequence(), then by parseConfiguration()
       // Each chain (if line numbers differ) prepends "at line X".
       // Assuming all start processing at line 1 for this single line script:
       assertEquals("Expected token; at line 1", e.getMessage());
       assertNull(e.getCause());
   }

   @Test
   public void testEnqueueActionWithExtraTokens() {
       String script = "ENQUEUE myQueue extraToken";
       Iterator<String> iterator = script.lines().iterator();
       RuntimeParserException e = assertThrows(RuntimeParserException.class, () -> {
           parser(iterator).parseConfiguration();
       });
       // assertEndOfLine() throws RPE(1, "Expected end of line") -> msg "Expected end of line; at line 1"
       // Chained...
       assertEquals("Expected end of line; at line 1", e.getMessage());
       assertNull(e.getCause());
   }
*/

  @Test
  public void testQueueDeclaration() {
    String script = "QUEUE my_queue { DROP\n }";
    Directive result = parser(script).scanQueueDecl();

    assertThat(result, isA(QueueDecl.class.asSubclass(Directive.class)));
    QueueDecl queueDecl = (QueueDecl) result;

    assertThat(queueDecl.getName(), is("my_queue"));
    assertThat(queueDecl.getBody(), isADropAction());
  }

  @Test
  public void testQueueDeclarationMissingBody() {
    String script = "QUEUE my_queue";
    RuntimeParserException e = assertThrows(RuntimeParserException.class, () -> {
      parser(script).parseConfiguration();
    });
    assertThat(e.getMessage(), containsString("Expected queue body; at line 1"));
    assertThat(e.getCause(), is(nullValue()));
  }

  @Test
  public void testQueueDeclarationEmptyBody() {
    String script = "QUEUE my_queue {}";
    Directive result = parser(script).scanQueueDecl();

    assertThat(result, isA(QueueDecl.class.asSubclass(Directive.class)));
    QueueDecl queueDecl = (QueueDecl) result;

    assertThat(queueDecl.getName(), is("my_queue"));
    assertThat(queueDecl.getBody(), isA(OrSequence.class.asSubclass(Directive.class)));
    OrSequence queueBody = (OrSequence) queueDecl.getBody();
    assertThat(queueBody.getDirectives(), hasSize(0));
  }

  // Tests for Retention Policy Parsing in scanQueueDecl

  @Test
  public void testQueueDeclaration_NoRetentionPolicy() {
    String script = "QUEUE no_retention_q { \n DROP \n }"; // Added newlines around DROP
    Directive result = parser(script).scanQueueDecl();

    assertTrue(result instanceof QueueDecl); // Changed from assertThat
    QueueDecl queueDecl = (QueueDecl) result;

    assertEquals("no_retention_q", queueDecl.getName()); // Using assertEquals
    assertNull(queueDecl.getMaxTasksRetention());
    assertNull(queueDecl.getMaxDaysRetention());
    assertThat(queueDecl.getBody(), isADropAction());
  }

  @Test
  public void testQueueDeclaration_RetentionLastTasks() {
    String script = "QUEUE tasks_q RETENTION LAST 150 { \n DROP \n }"; // Added newlines
    Directive result = parser(script).scanQueueDecl();

    assertTrue(result instanceof QueueDecl); // Changed from assertThat
    QueueDecl queueDecl = (QueueDecl) result;

    assertEquals("tasks_q", queueDecl.getName()); // Using assertEquals
    assertEquals(150, queueDecl.getMaxTasksRetention());
    assertNull(queueDecl.getMaxDaysRetention());
    assertThat(queueDecl.getBody(), isADropAction());
  }

  @Test
  public void testQueueDeclaration_RetentionDays() {
    String script = "QUEUE days_q RETENTION 25 DAYS { \n DROP \n }"; // Added newlines
    Directive result = parser(script).scanQueueDecl();

    assertTrue(result instanceof QueueDecl); // Changed from assertThat
    QueueDecl queueDecl = (QueueDecl) result;

    assertEquals("days_q", queueDecl.getName()); // Using assertEquals
    assertNull(queueDecl.getMaxTasksRetention());
    assertEquals(25, queueDecl.getMaxDaysRetention());
    assertThat(queueDecl.getBody(), isADropAction());
  }

  @Test
  public void testQueueDeclaration_RetentionTasksAndDays_TasksFirst() {
    String script = "QUEUE both_q RETENTION LAST 10 AND 5 DAYS { \n DROP \n }"; // Added newlines
    Directive result = parser(script).scanQueueDecl();

    assertTrue(result instanceof QueueDecl); // Changed from assertThat
    QueueDecl queueDecl = (QueueDecl) result;

    assertEquals("both_q", queueDecl.getName()); // Using assertEquals
    assertEquals(10, queueDecl.getMaxTasksRetention());
    assertEquals(5, queueDecl.getMaxDaysRetention());
    assertThat(queueDecl.getBody(), isADropAction());
  }

  @Test
  public void testQueueDeclaration_RetentionDaysAndTasks_DaysFirst_OR() {
    String script = "QUEUE other_both_q RETENTION 20 DAYS OR LAST 200 { \n DROP \n }"; // Added newlines
    Directive result = parser(script).scanQueueDecl();

    assertTrue(result instanceof QueueDecl); // Changed from assertThat
    QueueDecl queueDecl = (QueueDecl) result;

    assertEquals("other_both_q", queueDecl.getName()); // Using assertEquals
    assertEquals(200, queueDecl.getMaxTasksRetention());
    assertEquals(20, queueDecl.getMaxDaysRetention());
    assertThat(queueDecl.getBody(), isADropAction());
  }

  @Test
  public void testQueueDeclaration_InvalidRetentionSyntax_WrongKeyword() {
    String script = "QUEUE invalid_q RETENTION WRONG POLICY { \n DROP \n }"; // Added newlines
    RuntimeParserException e = assertThrows(RuntimeParserException.class, () -> {
      parser(script).scanQueueDecl();
    });
    assertThat(e.getMessage(), containsString("Invalid retention policy syntax. Expected 'LAST <number>' or '<number> DAYS'"));
  }

  @Test
  public void testQueueDeclaration_InvalidRetentionSyntax_LAST_NAN() {
    String script = "QUEUE invalid_q RETENTION LAST ABC { \n DROP \n }"; // Added newlines
    RuntimeParserException e = assertThrows(RuntimeParserException.class, () -> {
      parser(script).scanQueueDecl();
    });
    assertThat(e.getMessage(), containsString("Expected number after LAST for retention policy"));
  }

  @Test
  public void testQueueDeclaration_InvalidRetentionSyntax_DAYS_NAN() {
    String script = "QUEUE invalid_q RETENTION ABC DAYS { \n DROP \n }"; // Added newlines
    RuntimeParserException e = assertThrows(RuntimeParserException.class, () -> {
      parser(script).scanQueueDecl();
    });
    assertThat(e.getMessage(), containsString("Invalid retention policy syntax. Expected 'LAST <number>' or '<number> DAYS'"));
  }

  @Test
  public void testQueueDeclaration_InvalidRetentionSyntax_MissingDAYS() {
    String script = "QUEUE invalid_q RETENTION 123 NUMBER { \n DROP \n }"; // Added newlines
    RuntimeParserException e = assertThrows(RuntimeParserException.class, () -> {
      parser(script).scanQueueDecl();
    });
    assertThat(e.getMessage(), containsString("Expected DAYS after number for retention policy"));
  }

  @Test
  public void testQueueDeclaration_TooManyPolicies() {
    String script = "QUEUE too_many_q RETENTION LAST 10 AND 5 DAYS OR LAST 20 { \n DROP \n }"; // Added newlines
    RuntimeParserException e = assertThrows(RuntimeParserException.class, () -> {
      parser(script).scanQueueDecl();
    });
    assertThat(e.getMessage(), containsString("Unexpected token 'OR' after second retention policy. Maximum two policies allowed."));
  }

  @Test
  public void testQueueDeclaration_InvalidCombinator() {
    String script = "QUEUE invalid_comb_q RETENTION LAST 10 XOR 5 DAYS { \n DROP \n }"; // Added newlines
    RuntimeParserException e = assertThrows(RuntimeParserException.class, () -> {
      parser(script).scanQueueDecl();
    });
    // This will be caught as "Invalid token after retention policy" because XOR is not recognized
    // and then it expects a group directive.
    assertThat(e.getMessage(), containsString("Unexpected token 'XOR' after retention policy. Expected 'AND', 'OR', or start of queue body '{'."));
  }

  @Test
  public void testQueueDeclaration_TwoPoliciesSameType_Tasks() {
    String script = "QUEUE same_type_tasks_q RETENTION LAST 10 AND LAST 20 { \n DROP \n }"; // Added newlines
    RuntimeParserException e = assertThrows(RuntimeParserException.class, () -> {
      parser(script).scanQueueDecl();
    });
    // This check is performed in scanQueueDecl after scanRetentionPolicies returns.
    assertThat(e.getMessage(), containsString("Invalid combination of retention policies. If two policies are specified with a combinator, one must be for tasks (LAST <n>) and one for days (<n> DAYS)."));
  }

  @Test
  public void testQueueDeclaration_TwoPoliciesSameType_Days() {
    String script = "QUEUE same_type_days_q RETENTION 5 DAYS OR 10 DAYS { \n DROP \n }"; // Added newlines
    RuntimeParserException e = assertThrows(RuntimeParserException.class, () -> {
      parser(script).scanQueueDecl();
    });
    assertThat(e.getMessage(), containsString("Invalid combination of retention policies. If two policies are specified with a combinator, one must be for tasks (LAST <n>) and one for days (<n> DAYS)."));
  }

  @Test
  public void testQueueDeclaration_RetentionPolicyNotClosed() {
    String script = "QUEUE q RETENTION LAST 10 \n DROP \n"; // Missing { } for queue body, added newlines for DROP
    RuntimeParserException e = assertThrows(RuntimeParserException.class, () -> {
        parser(script).scanQueueDecl();
    });
    assertThat(e.getMessage(), containsString("Unexpected token 'DROP' after retention policy. Expected 'AND', 'OR', or start of queue body '{'."));
  }

  @Test
  public void testQueueDeclaration_RetentionPolicyLastTokenIsQueue() {
    String script = "QUEUE q1 RETENTION LAST 10 QUEUE q2 {}";
    RuntimeParserException e = assertThrows(RuntimeParserException.class, () -> {
        parser(script).scanQueueDecl();
    });
    // The new stricter scanRetentionPolicies catches this earlier.
    assertThat(e.getMessage(), containsString("Unexpected token 'QUEUE' after retention policy. Expected 'AND', 'OR', or start of queue body '{'."));
  }

  @Test
  public void testQueueDeclaration_MissingCombinator() {
    String script = "QUEUE missing_comb_q RETENTION LAST 10 5 DAYS { \n DROP \n }";
    RuntimeParserException e = assertThrows(RuntimeParserException.class, () -> {
      parser(script).scanQueueDecl();
    });
    assertThat(e.getMessage(), containsString("Unexpected token '5' after retention policy. Expected 'AND', 'OR', or start of queue body '{'."));
  }
}
