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

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.isA;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import ar.com.rjgodoy.webhook_router.Header;
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
    String line = "x-foo-bar:x";
    var d = parser(line).scanPredicate();
    Header h = new Header(line);
    assertThat(d, is(new HeaderPredicate(h.name(), h.value())));
    assertThat(parser(line).scanDirective(), is(d));
  }

  @Test
  public void testScanPredicatePayload() {
    String line = "$foo.bar: x ";
    var d = parser(line).scanPredicate();
    assertThat(d, is(new PayloadPredicate("foo.bar", "x")));
    assertThat(parser(line).scanDirective(), is(d));
  }

  @Test
  public void testScanPredicateNull() {
    assertThat(parser("x").scanPredicate(), is(nullValue()));
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
    }).getMessage(), containsString("Expected macro-string;"));
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
    assertThat(((ForAction) d).getArrayName(), is("payload.array"));
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
    }).getMessage(), containsString("Expected SET header: value;"));
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
    assertThat(d, is(new CallAction("foo")));
  }

}

