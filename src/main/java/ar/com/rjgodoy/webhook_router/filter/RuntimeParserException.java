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

import java.util.Arrays;

class RuntimeParserException extends RuntimeException {

  private static final long serialVersionUID = 1L;
  
  private int lineNumber;
  public RuntimeParserException(int lineNumber) {
    super("at line " + lineNumber);
    this.lineNumber = lineNumber;
  }

  public RuntimeParserException(int lineNumber, String message) {
    super(message + "; at line " + lineNumber);
    this.lineNumber = lineNumber;
  }

  public RuntimeParserException(int lineNumber, RuntimeParserException cause) {
    super("at line " + lineNumber, cause);
    this.lineNumber = lineNumber;
  }

  static RuntimeParserException chain(int lineNumber, RuntimeParserException e) {
    if (lineNumber > 0 && lineNumber != e.lineNumber) {
      e = new RuntimeParserException(lineNumber, e);
      StackTraceElement ste[] = e.getStackTrace();
      if (ste.length >= 2) {
        e.setStackTrace(Arrays.copyOfRange(ste, 2, ste.length));
      }
    }
    return e;
  }

}