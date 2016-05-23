/*
 * Copyright Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ehcache.clustered.client.internal.store.operations;

import org.ehcache.impl.serialization.LongSerializer;
import org.ehcache.impl.serialization.StringSerializer;
import org.ehcache.spi.serialization.Serializer;
import org.junit.Test;

import java.nio.ByteBuffer;

import static org.ehcache.clustered.client.internal.store.operations.BaseOperation.BYTE_SIZE_BYTES;
import static org.ehcache.clustered.client.internal.store.operations.BaseOperation.INT_SIZE_BYTES;
import static org.ehcache.clustered.client.internal.store.operations.BaseOperation.LONG_SIZE_BYTES;
import static org.junit.Assert.*;

public class ConditionalReplaceOperationTest {

  private static final Serializer<Long> keySerializer = new LongSerializer();
  private static final Serializer<String> valueSerializer = new StringSerializer();

  @Test
  public void testEncode() throws Exception {
    Long key = 1L;
    String newValue = "The one";
    String oldValue = "Another one";
    ConditionalReplaceOperation<Long, String> operation = new ConditionalReplaceOperation<Long, String>(key, oldValue, newValue);
    ByteBuffer byteBuffer = operation.encode(keySerializer, valueSerializer);

    ByteBuffer expected = ByteBuffer.allocate(BYTE_SIZE_BYTES +
                                              INT_SIZE_BYTES + LONG_SIZE_BYTES +
                                              INT_SIZE_BYTES + oldValue.length() +
                                              newValue.length());
    expected.put(OperationCode.REPLACE_CONDITIONAL.getValue());
    expected.putInt(LONG_SIZE_BYTES);
    expected.putLong(key);
    expected.putInt(oldValue.length());
    expected.put(oldValue.getBytes());
    expected.put(newValue.getBytes());
    expected.flip();
    assertArrayEquals(expected.array(), byteBuffer.array());
  }

  @Test
  public void testDecode() throws Exception {
    Long key = 1L;
    String newValue = "The one";
    String oldValue = "Another one";

    ByteBuffer blob = ByteBuffer.allocate(BYTE_SIZE_BYTES +
                                          INT_SIZE_BYTES + LONG_SIZE_BYTES +
                                          INT_SIZE_BYTES + oldValue.length() +
                                          newValue.length());
    blob.put(OperationCode.REPLACE_CONDITIONAL.getValue());
    blob.putInt(LONG_SIZE_BYTES);
    blob.putLong(key);
    blob.putInt(oldValue.length());
    blob.put(oldValue.getBytes());
    blob.put(newValue.getBytes());
    blob.flip();

    ConditionalReplaceOperation<Long, String> operation = new ConditionalReplaceOperation<Long, String>(blob, keySerializer, valueSerializer);
    assertEquals(key, operation.getKey());
    assertEquals(newValue, operation.getValue());
    assertEquals(oldValue, operation.getOldValue());
  }

  @Test
  public void testEncodeDecodeInvariant() throws Exception {
    Long key = 1L;
    String newValue = "The value";
    String oldValue = "Another one";
    ConditionalReplaceOperation<Long, String> operation = new ConditionalReplaceOperation<Long, String>(key, oldValue, newValue);

    ConditionalReplaceOperation<Long, String> decodedOperation =
        new ConditionalReplaceOperation<Long, String>(operation.encode(keySerializer, valueSerializer), keySerializer, valueSerializer);
    assertEquals(key, decodedOperation.getKey());
    assertEquals(newValue, decodedOperation.getValue());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testDecodeThrowsOnInvalidType() throws Exception {
    ByteBuffer buffer = ByteBuffer.wrap(new byte[] {10});
    new ConditionalReplaceOperation<Long, String>(buffer, keySerializer, valueSerializer);
  }

  @Test
  public void testApply() throws Exception {
    ConditionalReplaceOperation<Long, String> operation = new ConditionalReplaceOperation<Long, String>(1L, "one", "two");
    Operation<Long, String> applied = operation.apply(null);
    assertNull(applied);

    Operation<Long, String> anotherOperation = new PutOperation<Long, String>(1L, "one");
    applied = operation.apply(anotherOperation);
    assertSame(operation, applied);

    anotherOperation = new PutOperation<Long, String>(1L, "another one");
    applied = operation.apply(anotherOperation);
    assertSame(anotherOperation, applied);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testApplyOnDifferentkey() throws Exception {
    ConditionalReplaceOperation<Long, String> operation = new ConditionalReplaceOperation<Long, String>(1L, "one", "two");
    Operation<Long, String> anotherOperation = new PutOperation<Long, String>(2L, "another one");
    operation.apply(anotherOperation);
  }

}