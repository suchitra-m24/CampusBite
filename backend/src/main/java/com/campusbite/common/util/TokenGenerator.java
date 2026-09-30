package com.campusbite.common.util;

import java.security.SecureRandom;

/**
 * Generates short, human-friendly pickup tokens (e.g. "CB-7F3K9Q") that are
 * encoded into a QR code and also shown as plain text so staff can type it
 * manually if a scanner isn't handy.
 */
public final class TokenGenerator {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // no O/0/I/1 ambiguity
    private static final SecureRandom RANDOM = new SecureRandom();

    private TokenGenerator() {
    }

    public static String generatePickupToken() {
        StringBuilder sb = new StringBuilder("CB-");
        for (int i = 0; i < 6; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    public static String generateOpaqueToken() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 40; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
