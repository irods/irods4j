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
		final int MAX_PASSWORD_LEN = 50;

		StringBuilder plainTextPasswordSb = new StringBuilder();
		plainTextPasswordSb.append(passwordToObfuscate);
		plainTextPasswordSb.setLength(MAX_PASSWORD_LEN + 10);

		int count = MAX_PASSWORD_LEN - 10 - passwordToObfuscate.getBytes(StandardCharsets.UTF_8).length;
		if (count > 15) {
			// The random sequence of characters is used for padding and must match
			// what is defined on the server-side.
			plainTextPasswordSb.append("1gCBizHWbwIYyWLoysGzTe6SyzqFKMniZX05faZHWAwQKXf6Fs", 0, count);
		}

		StringBuilder keySb = new StringBuilder();
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
		int[] keyBuf = new int[100];
		byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
		int length = Math.min(keyBuf.length, key.getBytes(StandardCharsets.UTF_8).length);
		for (int x = 0; x < length; ++x) {
			keyBuf[x] = keyBytes[x] & 0xff;
		}

		// TODO(#191): Must also support SHA1.
		// Get the MD5 digest of the key to get some bytes with many different values.
		byte[] hexKey = obfMakeOneWayHash(hashAlgo, keyBuf, keyBuf.length);
		int[] buffer = new int[64]; // Each digest is 16 bytes, 4 of them.
		copyIntArrayToByteArray(buffer, 0, hexKey, hexKey.length);

		// Hash of the hash.
		byte[] v = obfMakeOneWayHash(hashAlgo, buffer, 16);
		copyIntArrayToByteArray(buffer, 16, v, v.length);

		// Hash of two hashes.
		v = obfMakeOneWayHash(hashAlgo, buffer, 32);
		copyIntArrayToByteArray(buffer, 32, v, v.length);

		// Hash of two hashes.
		v = obfMakeOneWayHash(hashAlgo, buffer, 32);
		copyIntArrayToByteArray(buffer, 48, v, v.length);

		final int MAX_PASSWORD_LEN = 50;

		StringBuilder inSb = new StringBuilder(data);
		inSb.setLength(MAX_PASSWORD_LEN + 10);

		StringBuilder outSb = new StringBuilder();
		outSb.setLength(MAX_PASSWORD_LEN + 100);

		// TODO(#191): Must also support SHA1.
//		if (HASH_ALGO_SHA1 == defaultHashAlgo) {
//			inSb.setCharAt(cpOut++, 's');
//			inSb.setCharAt(cpOut++, 'h');
//			inSb.setCharAt(cpOut++, 'a');
//			inSb.setCharAt(cpOut++, '1');
//		}

		int[] wheel = new int[26 + 26 + 10 + 15];
		int j = 0;
		for (int i = 0; i < 10; ++i) {
			wheel[j++] = (int) '0' + i;
		}
		for (int i = 0; i < 26; ++i) {
			wheel[j++] = (int) 'A' + i;
		}
		for (int i = 0; i < 26; ++i) {
			wheel[j++] = (int) 'a' + i;
		}
		for (int i = 0; i < 15; ++i) {
			wheel[j++] = (int) '!' + i;
		}

		int cpKey = 0;
		int pc = 0; // Previous character.
		for (int cpIn = 0, cpOut = 0; ; ++cpIn) {
			int k = buffer[cpKey++];
			if (cpKey > 60) {
				cpKey = 0;
			}

			boolean found = false;
			for (int i = 0; i < wheel.length; ++i) {
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
		byte[] bufUnsigned = new byte[bufferLength];
		for (int x = 0; x < bufferLength; ++x) {
			bufUnsigned[x] = (byte) (buffer[x] & 0xff);
		}

		// Hash the buffer.
		MessageDigest hasher = MessageDigest.getInstance(hashAlgo);
		return hasher.digest(bufUnsigned);
	}

	/**
	 * @since 0.8.0
	 */
	public static void copyIntArrayToByteArray(int[] dst, int dstOffset, byte[] src, int count) {
		// TODO(#192): count isn't used. Is this a bug?
		for (int i = 0; i < src.length; ++i) {
			dst[i + dstOffset] = src[i] & 0xff;
		}
	}
}
