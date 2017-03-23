// Copyright 2017 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
////////////////////////////////////////////////////////////////////////////////

package com.google.cloud.crypto.tink;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.cloud.crypto.tink.TestUtil.DummyMacKeyManager;
import com.google.cloud.crypto.tink.TinkProto.KeyFormat;
import com.google.cloud.crypto.tink.TinkProto.Keyset;
import java.security.GeneralSecurityException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for CleartextKeysetHandle.
 */
@RunWith(JUnit4.class)
public class CleartextKeysetHandleTest {
  private final String macTypeUrl = DummyMacKeyManager.class.getSimpleName();
  @Before
  public void setUp() throws GeneralSecurityException {
    Registry.INSTANCE.registerKeyManager(macTypeUrl, new DummyMacKeyManager());
  }

  @Test
  public void testBasic() throws Exception {
    // Create a keyset that contains a single DummyMacKey.
    KeyFormat format = KeyFormat.newBuilder().setTypeUrl(macTypeUrl).build();
    KeysetManager manager = new KeysetManager.Builder()
        .setKeyFormat(format)
        .build();
    manager.rotate();

    assertNull(manager.getKeysetHandle().getEncryptedKeyset());
    Keyset keyset1 = manager.getKeysetHandle().getKeyset();
    KeysetHandle handle1 = CleartextKeysetHandle.parseFrom(keyset1.toByteArray());
    assertEquals(keyset1, handle1.getKeyset());
  }

  @Test
  public void testInvalidKeyset() throws Exception {
    // Create a keyset that contains a single DummyMacKey.
    KeyFormat format = KeyFormat.newBuilder().setTypeUrl(macTypeUrl).build();
    KeysetManager manager = new KeysetManager.Builder()
        .setKeyFormat(format)
        .build();
    manager.rotate();
    Keyset keyset = manager.getKeysetHandle().getKeyset();

    byte[] proto = keyset.toByteArray();
    proto[0] = (byte) ~proto[0];
    try {
      KeysetHandle unused = CleartextKeysetHandle.parseFrom(proto);
      fail("Expected GeneralSecurityException");
    } catch (GeneralSecurityException e) {
      assertTrue(e.toString().contains("invalid keyset"));
    }
  }
}