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

package com.google.crypto.tink.subtle;

import com.google.errorprone.annotations.Immutable;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import javax.crypto.KeyAgreement;

/**
 * HKDF-based KEM (key encapsulation mechanism) for ECIES sender.
 */
public final class EciesHkdfSenderKem {
  private ECPublicKey recipientPublicKey;

  /**
   * A container for key parts generated by the KEM.
   */
  @Immutable
  public static final class KemKey {
    private final ImmutableByteArray kemBytes;
    private final ImmutableByteArray symmetricKey;
    public KemKey(final byte[] kemBytes, final byte[] symmetricKey) {
      this.kemBytes = ImmutableByteArray.of(kemBytes);
      this.symmetricKey = ImmutableByteArray.of(symmetricKey);
    }
    public byte[] getKemBytes() {
      if (kemBytes == null) {
        return null;
      } else {
        return kemBytes.getBytes();
      }
    }
    public byte[] getSymmetricKey() {
      if (symmetricKey == null) {
        return null;
      } else {
        return symmetricKey.getBytes();
      }
    }
  }

  public EciesHkdfSenderKem(final ECPublicKey recipientPublicKey) {
    this.recipientPublicKey = recipientPublicKey;
  }

  public KemKey generateKey(String hmacAlgo, final byte[] hkdfSalt, final byte[] hkdfInfo,
      int keySizeInBytes, EcUtil.PointFormat pointFormat) throws GeneralSecurityException {
    KeyPair ephemeralKeyPair = generateEphemeralKey();
    ECPublicKey ephemeralPublicKey = (ECPublicKey) ephemeralKeyPair.getPublic();
    ECPrivateKey ephemeralPrivateKey = (ECPrivateKey) ephemeralKeyPair.getPrivate();

    byte[] sharedSecret = getSharedSecret(ephemeralPrivateKey);
    byte[] kemBytes = EcUtil.ecPointEncode(
        ephemeralPublicKey.getParams().getCurve(), pointFormat, ephemeralPublicKey.getW());
    byte[] symmetricKey = Hkdf.computeEciesHkdfSymmetricKey(kemBytes, sharedSecret,
         hmacAlgo, hkdfSalt, hkdfInfo, keySizeInBytes);
    return new KemKey(kemBytes, symmetricKey);
  }

  private KeyPair generateEphemeralKey()
      throws GeneralSecurityException {
    ECParameterSpec spec = recipientPublicKey.getParams();
    KeyPairGenerator keyGen = EngineFactory.KEY_PAIR_GENERATOR.getInstance("EC");
    keyGen.initialize(spec);
    return keyGen.generateKeyPair();
  }

  private byte[] getSharedSecret(final ECPrivateKey senderPrivateKey)
      throws GeneralSecurityException {
    ECPoint publicPoint = recipientPublicKey.getW();
    ECParameterSpec spec = recipientPublicKey.getParams();
    EcUtil.checkPointOnCurve(publicPoint, spec.getCurve());
    KeyAgreement ka = EngineFactory.KEY_AGREEMENT.getInstance("ECDH");
    ka.init(senderPrivateKey);
    ka.doPhase(recipientPublicKey, true);
    return ka.generateSecret();
  }
}