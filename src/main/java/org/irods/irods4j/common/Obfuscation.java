package org.irods.irods4j.common;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Obfuscation {

	/**
	 * Obfuscate a password.
	 *
	 * @param passwordToObfuscate The password to obfuscate.
	 * @param requesterPassword   The password of the rodsadmin performing the
	 *                            obfuscation.
	 * @param hashAlgo            The name of the hashing algorithm. This function
	 *                            only supports "md5".
	 * @return The obfuscated string.
	 * @throws NoSuchAlgorithmException If the hashing algorithm is not supported.
	 *
	 * @since 0.8.0
	 */
	public static String obfuscatePassword(String passwordToObfuscate, String requesterPassword, String hashAlgo) throws NoSuchAlgorithmException {
		final var MAX_PASSWORD_LEN = 50;

		var plainTextPasswordSb = new StringBuilder();
		plainTextPasswordSb.append(passwordToObfuscate);
		plainTextPasswordSb.setLength(MAX_PASSWORD_LEN + 10);

		var count = MAX_PASSWORD_LEN - 10 - passwordToObfuscate.getBytes(StandardCharsets.UTF_8).length;
		if (count > 15) {
			// The random sequence of characters is used for padding and must match
			// what is defined on the server-side.
			plainTextPasswordSb.append("1gCBizHWbwIYyWLoysGzTe6SyzqFKMniZX05faZHWAwQKXf6Fs", 0, count);
		}

		var keySb = new StringBuilder();
		if (requesterPassword.getBytes(StandardCharsets.UTF_8).length >= MAX_PASSWORD_LEN) {
			throw new IllegalArgumentException("Requester password exceeds max key size: " + MAX_PASSWORD_LEN);
		}
		keySb.append(requesterPassword, 0, Math.min(requesterPassword.getBytes(StandardCharsets.UTF_8).length,
				MAX_PASSWORD_LEN));

		return obfEncodeByKey(plainTextPasswordSb.toString(), keySb.toString(), hashAlgo);
	}

	/**
	 * This function is a port of obfEncodeByKey() in irods/irods/lib/core/src/obf.cpp.
	 *
	 * @since 0.8.0
	 */
	public static String obfEncodeByKey(String data, String key, String hashAlgo) throws NoSuchAlgorithmException {
		var keyBuf = new int[100];
		var keyBytes = key.getBytes(StandardCharsets.UTF_8);
		var length = Math.min(keyBuf.length, key.getBytes(StandardCharsets.UTF_8).length);
		for (int x = 0; x < length; ++x) {
			keyBuf[x] = keyBytes[x] & 0xff;
		}

		// TODO(#191): Must also support SHA1.
		// Get the MD5 digest of the key to get some bytes with many different values.
		var hexKey = obfMakeOneWayHash(hashAlgo, keyBuf, keyBuf.length);
		var buffer = new int[64]; // Each digest is 16 bytes, 4 of them.
		copyByteArrayToIntArray(buffer, 0, hexKey, hexKey.length);

		// Hash of the hash.
		var v = obfMakeOneWayHash(hashAlgo, buffer, 16);
		copyByteArrayToIntArray(buffer, 16, v, v.length);

		// Hash of two hashes.
		v = obfMakeOneWayHash(hashAlgo, buffer, 32);
		copyByteArrayToIntArray(buffer, 32, v, v.length);

		// Hash of two hashes.
		v = obfMakeOneWayHash(hashAlgo, buffer, 32);
		copyByteArrayToIntArray(buffer, 48, v, v.length);

		final var MAX_PASSWORD_LEN = 50;

		var inSb = new StringBuilder(data);
		inSb.setLength(MAX_PASSWORD_LEN + 10);

		var outSb = new StringBuilder();
		outSb.setLength(MAX_PASSWORD_LEN + 100);

		// TODO(#191): Must also support SHA1.
//		if (HASH_ALGO_SHA1 == defaultHashAlgo) {
//			inSb.setCharAt(cpOut++, 's');
//			inSb.setCharAt(cpOut++, 'h');
//			inSb.setCharAt(cpOut++, 'a');
//			inSb.setCharAt(cpOut++, '1');
//		}

		var wheel = new int[26 + 26 + 10 + 15];
		int j = 0;
		for (var i = 0; i < 10; ++i) {
			wheel[j++] = (int) '0' + i;
		}
		for (var i = 0; i < 26; ++i) {
			wheel[j++] = (int) 'A' + i;
		}
		for (var i = 0; i < 26; ++i) {
			wheel[j++] = (int) 'a' + i;
		}
		for (var i = 0; i < 15; ++i) {
			wheel[j++] = (int) '!' + i;
		}

		var cpKey = 0;
		var pc = 0; // Previous character.
		for (int cpIn = 0, cpOut = 0; ; ++cpIn) {
			var k = buffer[cpKey++];
			if (cpKey > 60) {
				cpKey = 0;
			}

			var found = false;
			for (var i = 0; i < wheel.length; ++i) {
				if ((int) inSb.charAt(cpIn) == wheel[i]) {
					j = i + k + pc;
					j %= wheel.length;
					outSb.setCharAt(cpOut++, (char) (wheel[j] & 0xff));

					// "cipherBlockChaining" is not supported by this implementation. Based on the C
					// implementation, it is only enabled through use of functions which are not
					// supported by this implementation, therefore it is irrelevant.
//					if (cipherBlockChaining) {
//						pc = cpOut - 1;
//					}

					found = true;
					break;
				}
			}

			if (!found) {
				if (inSb.charAt(cpIn) == 0) {
					outSb.setLength(cpOut);
					return outSb.toString();
				} else {
					outSb.setCharAt(cpOut++, inSb.charAt(cpIn));
				}
			}
		}
	}

	/**
	 * @since 0.8.0
	 */
	public static byte[] obfMakeOneWayHash(String hashAlgo, int[] buffer, int bufferLength)
			throws NoSuchAlgorithmException {
		// Convert int array to an unsigned byte array.
		var bufUnsigned = new byte[bufferLength];
		for (int x = 0; x < bufferLength; ++x) {
			bufUnsigned[x] = (byte) (buffer[x] & 0xff);
		}

		// Hash the buffer.
		var hasher = MessageDigest.getInstance(hashAlgo);
		return hasher.digest(bufUnsigned);
	}

	/**
	 * Copy range from byte buffer to an integer buffer.
	 * <p>
	 * This function intentionally DOES NOT perform any bounds checking.
	 *
	 * @param dst    The integer buffer to copy into.
	 * @param offset The offset into the integer buffer to start copying.
	 * @param src    The byte buffer to copy from.
	 * @param count  The number of bytes to copy from the byte buffer.
	 *
	 * @since 0.8.0
	 */
	public static void copyByteArrayToIntArray(int[] dst, int offset, byte[] src, int count) {
		for (int i = 0; i < count; ++i) {
			dst[i + offset] = src[i] & 0xff;
		}
	}
}
