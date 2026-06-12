package com.igoy86.nexttranslate.presentation.dialog;

import androidx.annotation.NonNull;

/**
 * Data model for a single conversation bubble in Dialog Mode.
 */
public class ConversationItem {

    private static int counter = 0;

    private final int id;
    private final boolean isUserA;
    private final String originalText;
    private final String translatedText;

    public ConversationItem(boolean isUserA,
                            @NonNull String originalText,
                            @NonNull String translatedText) {
        this.id           = counter++;
        this.isUserA      = isUserA;
        this.originalText = originalText;
        this.translatedText = translatedText;
    }

    public int getId()              { return id; }
    public boolean isUserA()        { return isUserA; }
    public String getOriginalText() { return originalText; }
    public String getTranslatedText() { return translatedText; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ConversationItem)) return false;
        ConversationItem that = (ConversationItem) o;
        return id == that.id
                && isUserA == that.isUserA
                && originalText.equals(that.originalText)
                && translatedText.equals(that.translatedText);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(id, isUserA, originalText, translatedText);
    }
}
