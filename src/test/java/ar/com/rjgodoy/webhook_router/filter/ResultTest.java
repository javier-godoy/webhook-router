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

import static ar.com.rjgodoy.webhook_router.filter.Result.FALSE;
import static ar.com.rjgodoy.webhook_router.filter.Result.NULL;
import static ar.com.rjgodoy.webhook_router.filter.Result.TRUE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class ResultTest {

  @Test
  public void testAnd() {
    assertEquals(TRUE, TRUE.and(TRUE));
    assertEquals(FALSE, TRUE.and(FALSE));
    assertEquals(FALSE, FALSE.and(TRUE));
    assertEquals(FALSE, FALSE.and(FALSE));

    assertEquals(TRUE, TRUE.and(NULL));
    assertEquals(TRUE, NULL.and(TRUE));
    assertEquals(NULL, NULL.and(NULL));
    assertEquals(FALSE, NULL.and(FALSE));
    assertEquals(FALSE, FALSE.and(NULL));
  }

  @Test
  public void testOr() {
    assertEquals(TRUE, TRUE.or(TRUE));
    assertEquals(TRUE, TRUE.or(FALSE));
    assertEquals(TRUE, FALSE.or(TRUE));
    assertEquals(FALSE, FALSE.or(FALSE));

    assertEquals(TRUE, TRUE.or(NULL));
    assertEquals(TRUE, NULL.or(TRUE));
    assertEquals(NULL, NULL.or(NULL));
    assertEquals(FALSE, NULL.or(FALSE));
    assertEquals(FALSE, FALSE.or(NULL));
  }

  @Test
  public void testNot() {
    assertEquals(FALSE, TRUE.negate());
    assertEquals(TRUE, FALSE.negate());
    assertEquals(NULL, NULL.negate());
  }

  @Test
  public void testOf() {
    assertEquals(TRUE, Result.of(true));
    assertEquals(FALSE, Result.of(false));
  }

}

