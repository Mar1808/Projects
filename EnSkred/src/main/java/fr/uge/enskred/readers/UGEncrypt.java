package fr.uge.enskred.readers;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Objects;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;

import fr.uge.enskred.utils.Utils;

/**
 * A utility class for RSA encryption and decryption operations.
 * This class provides methods for generating RSA key pairs, encrypting,
 * and decrypting data using RSA/ECB/OAEP with SHA-256 and MGF1 padding.
 * <p>
 * Due to the OAEP padding scheme with SHA-256, the maximum size of data
 * that can be encrypted in a single operation is 190 bytes for a 2048-bit key.
 */
public class UGEncrypt {


    private static final String ALGORITHM = "RSA";
    private static final String ENCRYPTION_SCHEME = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";
    public static final int KEY_SIZE_BITS = 2048;
    public static final int KEY_SIZE_BYTES = KEY_SIZE_BITS / 8;

    /**
     * Maximum size in bytes of data that can be encrypted in a single operation
     * with a 2048-bit key using OAEP padding with SHA-256.
     */
    public static final int MAX_ENCRYPT_BLOCK_SIZE = 190;
    /**
     * Maximum size in bytes of an encoded RSA public key (X.509 format) for a 2048-bit key.
     * The actual size may be smaller but will not exceed this value.
     */
    public static final int MAX_PUBLIC_KEY_SIZE = 400;
    /**
     * Maximum size in bytes of an encoded RSA private key (PKCS#8 format) for a 2048-bit key.
     * The actual size may be smaller but will not exceed this value.
     */
    public static final int MAX_PRIVATE_KEY_SIZE = 1400;

    /**
     * Represents an RSA key pair containing both public and private keys.
     *
     * @param publicKey the RSA public key
     * @param privateKey the RSA private key
     */
    public record KeyPairRSA(PublicKeyRSA publicKey, PrivateKeyRSA privateKey){
        public KeyPairRSA {
            Utils.requireNonNulls(publicKey, privateKey);
        }

        /**
         * Generates a new RSA key pair with the default key size.
         *
         * @return a new KeyPairRSA containing the generated public and private keys
         * @throws NoSuchAlgorithmException if the RSA algorithm is not available
         */
        public static KeyPairRSA generate() throws NoSuchAlgorithmException {
            var keyPairGenerator = KeyPairGenerator.getInstance(ALGORITHM);
            keyPairGenerator.initialize(KEY_SIZE_BITS);
            var keyPair = keyPairGenerator.generateKeyPair();
            var publicKey = new PublicKeyRSA(keyPair.getPublic());
            var privateKey = new PrivateKeyRSA(keyPair.getPrivate());
            return new KeyPairRSA(publicKey,privateKey);
        }
        
    };

    /**
     * Method to encrypt a ByteBuffer with a PublicKeyRSA.
     * Like following convention: 
     * IN  <= ByteBuffer =[No Encrypt DATA]
     * OUT => ByteBuffer =[NB_BLOCK | ENCRYPTED_BLOCK of MAX_ENCRYPT_BLOCK_SIZE]
     * ---
     * @param payload
     * @param recipient
     * @return
     * @throws InvalidKeyException
     * @throws ShortBufferException
     * @throws IllegalBlockSizeException
     */
    public static ByteBuffer encryptRSA(ByteBuffer payload, PublicKeyRSA recipient) throws InvalidKeyException, ShortBufferException, IllegalBlockSizeException {
        payload = payload.slice(); //éviter de consommer l'original
        var nbBlocks = (int) Math.ceil((double) payload.remaining() / MAX_ENCRYPT_BLOCK_SIZE);
        var buffer = ByteBuffer.allocate(Integer.BYTES + nbBlocks * KEY_SIZE_BYTES);
        var chunk = ByteBuffer.allocate(MAX_ENCRYPT_BLOCK_SIZE);
        var encryptedChunk = ByteBuffer.allocate(KEY_SIZE_BYTES);
        buffer.putInt(nbBlocks);
        for(var i = 0; i < nbBlocks; i++) {
            chunk.clear();
            encryptedChunk.clear();
            var length = Math.min(payload.remaining(), MAX_ENCRYPT_BLOCK_SIZE);
            var oldLimit = payload.limit();
            payload.limit(payload.position() + length);
            chunk.put(payload).flip();
            payload.limit(oldLimit);
            recipient.encrypt(chunk, encryptedChunk);
            buffer.put(encryptedChunk.flip());
        }
        return buffer;
    }
    
    /**
     * Method to decrypt a ByteBuffer with PrivateKeyRSA.
     * Like following convention: 
     * IN  <= ByteBuffer =[NB_BLOCK | ENCRYPTED_BLOCK of MAX_ENCRYPT_BLOCK_SIZE]
     * OUT => ByteBuffer =[decrypted DATA]
     * ---
     * @param encrypted
     * @param recipient
     * @return
     * @throws IllegalBlockSizeException
     * @throws ShortBufferException
     * @throws BadPaddingException
     */
	public static ByteBuffer decryptRSA(ByteBuffer encrypted, PrivateKeyRSA recipient) throws IllegalBlockSizeException, ShortBufferException, BadPaddingException {
		encrypted = encrypted.slice();
		var nbBlocks = encrypted.getInt();
		var buffer = ByteBuffer.allocate(nbBlocks * UGEncrypt.MAX_ENCRYPT_BLOCK_SIZE); //large mais safe
		var chunk = ByteBuffer.allocate(UGEncrypt.KEY_SIZE_BYTES);
		var decrypted = ByteBuffer.allocate(UGEncrypt.KEY_SIZE_BYTES);
		for(var i = 0; i < nbBlocks; ++i) {
		    decrypted.clear();
		    chunk.clear();
		    var oldLimit = encrypted.limit();
		    encrypted.limit(encrypted.position() + UGEncrypt.KEY_SIZE_BYTES);
		    chunk.put(encrypted).flip();
		    encrypted.limit(oldLimit);
		    recipient.decrypt(chunk, decrypted);
		    var length = Utils.min(decrypted.flip().remaining(), UGEncrypt.MAX_ENCRYPT_BLOCK_SIZE);
		    decrypted.limit(length);
		    buffer.put(decrypted);
		}
		return buffer;
	}
    
    /**
     * Wrapper class for RSA public key operations.
     */
    public static class PublicKeyRSA implements Comparable<PublicKeyRSA> {
        private final PublicKey publicKey;
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) { return true; }
            if (obj == null || getClass() != obj.getClass()) { return false; }
            PublicKeyRSA other = (PublicKeyRSA) obj;
            return Arrays.equals(publicKey.getEncoded(), other.publicKey.getEncoded());
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(publicKey.getEncoded());
        }


        private PublicKeyRSA(PublicKey publicKey) {
            this.publicKey = Objects.requireNonNull(publicKey);
        }

        /**
         * Creates a PublicKeyRSA instance from encoded key data in a ByteBuffer.
         * The key must be encoded in X.509 format as specified in RFC 5280.
         *
         * @param buffer ByteBuffer containing the X.509 encoded public key
         * @return a new PublicKeyRSA instance
         * @throws NoSuchAlgorithmException if the RSA algorithm is not available
         * @throws InvalidKeySpecException if the key specification is invalid
         * @see <a href="https://tools.ietf.org/html/rfc5280">RFC 5280</a>
         */
        static PublicKeyRSA from(ByteBuffer buffer) throws NoSuchAlgorithmException, InvalidKeySpecException {
            var encodedKey = new byte[buffer.remaining()];
            buffer.get(encodedKey);
            var keySpec = new X509EncodedKeySpec(encodedKey);
            var keyFactory = KeyFactory.getInstance("RSA");
            return new PublicKeyRSA(keyFactory.generatePublic(keySpec));
        }

        /**
         * Encrypts the data contained in the provided ByteBuffer using this public key.
         * Due to the OAEP padding scheme with SHA-256, the maximum size of data
         * that can be encrypted in a single operation is 190 bytes.
         *
         * @param bufferIn the ByteBuffer containing the plaintext data
         * @param bufferOut the ByteBuffer where the encrypted data will be written
         * @throws InvalidKeyException if the key is invalid
         * @throws IllegalBlockSizeException if the size of input data is incorrect
         * @throws ShortBufferException if the output buffer is too small
         */
        public void encrypt(ByteBuffer bufferIn, ByteBuffer bufferOut) throws InvalidKeyException, ShortBufferException, IllegalBlockSizeException {
            try {
                Cipher cipher = Cipher.getInstance(ENCRYPTION_SCHEME);
                cipher.init(Cipher.ENCRYPT_MODE, publicKey);
                cipher.doFinal(bufferIn, bufferOut);
            } catch (NoSuchAlgorithmException|NoSuchPaddingException|InvalidKeyException|BadPaddingException e){
                /* This algorithm is guarantied be available on any JVM (cf. Javadoc of Cypher) */
                throw new AssertionError(e);
            }
        }

        /**
         * Serializes this public key into the provided ByteBuffer using X.509 encoding
         * as specified in RFC 5280.
         *
         * @param buffer the ByteBuffer where the X.509 encoded key will be written
         * @throws IllegalArgumentException if the buffer has insufficient remaining space
         * @see <a href="https://tools.ietf.org/html/rfc5280">RFC 5280</a>
         */
        public void to(ByteBuffer buffer) {
            var encodedKey = publicKey.getEncoded();
            if (buffer.remaining() < encodedKey.length) {
                throw new IllegalArgumentException("Buffer has insufficient space");
            }
            buffer.put(encodedKey);
        }

        @Override
        public int compareTo(PublicKeyRSA other) {
            var aEncoded = this.publicKey.getEncoded();
            var bEncoded = other.publicKey.getEncoded();
            var len = Math.min(aEncoded.length, bEncoded.length);
            for(var i = 0; i < len; i++) {
                var cmp = Byte.compareUnsigned(aEncoded[i], bEncoded[i]);
                if(cmp != 0) { return cmp; }
            }
            return Integer.compare(aEncoded.length, bEncoded.length);
        }
        
        @Override
        public String toString() {
        	return getClass().getSimpleName() + "@" + Integer.toHexString(publicKey.hashCode());
        }
        
    }

    /**
     * Wrapper class for RSA private key operations.
     */
    public static class PrivateKeyRSA {
        private final PrivateKey privateKey;

        private PrivateKeyRSA(PrivateKey privateKey) {
            this.privateKey = Objects.requireNonNull(privateKey);
        }

        /**
         * Creates a PrivateKeyRSA instance from encoded key data in a ByteBuffer.
         * The key must be encoded in PKCS#8 format as specified in RFC 5208.
         *
         * @param buffer ByteBuffer containing the PKCS#8 encoded private key
         * @return a new PrivateKeyRSA instance
         * @throws NoSuchAlgorithmException if the RSA algorithm is not available
         * @throws InvalidKeySpecException if the key specification is invalid
         * @see <a href="https://tools.ietf.org/html/rfc5208">RFC 5208</a>
         */
        static PrivateKeyRSA from(ByteBuffer buffer) throws NoSuchAlgorithmException, InvalidKeySpecException {
            var encodedKey = new byte[buffer.remaining()];
            buffer.get(encodedKey);
            var keySpec = new PKCS8EncodedKeySpec(encodedKey);
            var keyFactory = KeyFactory.getInstance("RSA");
            return new PrivateKeyRSA(keyFactory.generatePrivate(keySpec));
        }

        /**
         * Decrypts the data contained in the provided ByteBuffer using this private key.
         *
         * @param bufferIn the ByteBuffer containing the encrypted data
         * @param bufferOut the ByteBuffer where the decrypted data will be written
         * @throws IllegalBlockSizeException if the size of input data is incorrect
         * @throws ShortBufferException if the output buffer is too small
         * @throws AssertionError if there is an unexpected error with the decryption algorithm or padding
         */
        public void decrypt(ByteBuffer bufferIn, ByteBuffer bufferOut) throws  ShortBufferException, IllegalBlockSizeException, BadPaddingException {
            try {
                Cipher cipher = Cipher.getInstance(ENCRYPTION_SCHEME);
                cipher.init(Cipher.DECRYPT_MODE, privateKey);
                cipher.doFinal(bufferIn, bufferOut);
            } catch (NoSuchAlgorithmException|NoSuchPaddingException|InvalidKeyException e) {
                /* This algorithm is guaranteed to be available on any JVM (cf. Javadoc of Cipher) */
                throw new AssertionError(e);
            }
        }

        /**
         * Serializes this private key into the provided ByteBuffer using PKCS#8 encoding
         * as specified in RFC 5208.
         *
         * @param buffer the ByteBuffer where the PKCS#8 encoded key will be written
         * @throws IllegalArgumentException if the buffer has insufficient remaining space
         * @see <a href="https://tools.ietf.org/html/rfc5208">RFC 5208</a>
         */
        public void to(ByteBuffer buffer) {
            var encodedKey = privateKey.getEncoded();
            if (buffer.remaining() < encodedKey.length) {
                throw new IllegalArgumentException("Buffer has insufficient space");
            }
            buffer.put(encodedKey);
        }
    }

    /**
     * Main method demonstrating the complete encryption/decryption workflow with key serialization.
     *
     * @param args command line arguments (not used)
     */
    public static void main(String[] args) throws Exception {
        // Generate a key pair
        var keyPair = KeyPairRSA.generate();
        System.out.println("RSA key pair generated successfully");

        // Original message to encrypt
        String originalMessage = "Hello, RSA!";
        ByteBuffer messageBuffer = StandardCharsets.UTF_8.encode(originalMessage);

        // Prepare buffers
        var encryptedBuffer = ByteBuffer.allocate(KEY_SIZE_BYTES); // Size of encrypted block is key size
        var decryptedBuffer = ByteBuffer.allocate(KEY_SIZE_BYTES);
        
        // Serialize private key
        var privateKeyBuffer = ByteBuffer.allocate(MAX_PRIVATE_KEY_SIZE);
        keyPair.privateKey().to(privateKeyBuffer);
        privateKeyBuffer.flip(); // Prepare for reading
        
        // Encrypt message with public key
        keyPair.publicKey().encrypt(messageBuffer, encryptedBuffer);
        encryptedBuffer.flip(); // Prepare for reading
        
        // Simulate storing and retrieving private key
        var retrievedPrivateKey = PrivateKeyRSA.from(privateKeyBuffer);
        
        // Decrypt message with retrieved private key
        retrievedPrivateKey.decrypt(encryptedBuffer, decryptedBuffer);
        decryptedBuffer.flip();
        
        // Convert decrypted bytes back to string using UTF-8
        CharBuffer charBuffer = StandardCharsets.UTF_8.decode(decryptedBuffer);
        String decryptedMessage = charBuffer.toString();
        
        // Print results
        System.out.println("Original message: " + originalMessage);
        System.out.println("Decrypted message: " + decryptedMessage);
        System.out.println("Encryption/Decryption " + 
            (originalMessage.equals(decryptedMessage) ? "successful!" : "failed!"));
    }


    
    
    
}
