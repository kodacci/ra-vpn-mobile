package pro.ra_tech.ra_vpn.mobile.client.common.crypto;

import androidx.annotation.Nullable;

import java.nio.charset.StandardCharsets;

import pro.ra_tech.ra_vpn.mobile.client.common.exceptions.CipherConfigException;

public interface EncryptorFactory {
    static PacketEncryptor ofType(EncryptorType type, @Nullable String encryptorBase64Key) {
        switch (type) {
            case DUMMY: return new DummyPacketEncryptor();
            case XOR: return new XorPacketEncryptor();
            case AES: {
                if (encryptorBase64Key == null) {
                    throw new CipherConfigException("No cipher key file provided");
                }

                try {
                    return new AesPacketEncryptor(encryptorBase64Key.getBytes(StandardCharsets.US_ASCII));
                } catch (Exception ex) {
                    throw new CipherConfigException("Error reading cipher key file:", ex);
                }
            }
        }

        throw new CipherConfigException("Unexpected encryptor type");
    }
}
