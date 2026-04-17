package org.fiftieshousewife.cleancode.recipes;

import java.util.Arrays;

final class CommentWordOverlap {

    private CommentWordOverlap() {}

    static long countSignificantWords(final String[] words) {
        return Arrays.stream(words)
                .filter(w -> w.length() >= MumblingThresholds.MIN_SIGNIFICANT_WORD_LENGTH)
                .count();
    }

    static long countSignificantWordsFoundIn(final String[] words, final String text) {
        return Arrays.stream(words)
                .filter(w -> w.length() >= MumblingThresholds.MIN_SIGNIFICANT_WORD_LENGTH)
                .filter(text::contains)
                .count();
    }

    static boolean hasEnoughOverlap(final long significantCodeWords, final long codeWordsInComment) {
        final boolean enoughCodeWords = significantCodeWords >= MumblingThresholds.MIN_WORDS;
        final double requiredOverlap = significantCodeWords * MumblingThresholds.RESTATEMENT_RATIO;
        return enoughCodeWords && codeWordsInComment >= requiredOverlap;
    }

    static boolean allWordsPresent(final String text, final String[] words) {
        if (words.length < MumblingThresholds.MIN_WORDS) {
            return false;
        }
        final String[] textWords = MumblingCommentRecipe.WHITESPACE.split(text);
        for (final String word : words) {
            if (word.length() < MumblingThresholds.MIN_MEANINGFUL_WORD_LENGTH) {
                continue;
            }
            if (!hasPrefixMatch(textWords, word)) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasPrefixMatch(final String[] textWords, final String word) {
        return Arrays.stream(textWords)
                .anyMatch(tw -> tw.startsWith(word) || word.startsWith(tw));
    }
}
