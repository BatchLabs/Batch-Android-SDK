package com.batch.android.messaging.css;

import android.text.TextUtils;
import androidx.annotation.NonNull;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parser {

    private static final Pattern IMPORT_PATTERN = Pattern.compile("@import sdk\\(\"([^\"]*)\"\\);");

    private ImportFileProvider importFileProvider;
    private String rawStylesheet;

    private State state;
    private Substate substate;

    private MediaQuery currentMediaQuery;
    private Ruleset currentRuleset;
    private Declaration currentDeclaration;
    private Document currentDocument;

    private String currentToken;

    // Used to force the parser to disregard a special token
    private boolean shouldMergePreviousToken = false;

    public Parser(@NonNull ImportFileProvider importFileProvider, String cssString) {
        this.importFileProvider = importFileProvider;
        rawStylesheet = cssString;
        fillImports();
    }

    public Document parse() throws CSSParsingException {
        reset();
        scan();
        final Document retVal = currentDocument;
        reset();
        return retVal;
    }

    private void reset() {
        state = State.ROOT;
        substate = Substate.SELECTOR;
        currentDocument = new Document();
        currentRuleset = null;
        currentMediaQuery = null;
        currentToken = null;
        currentDeclaration = null;
    }

    private void fillImports() {
        if (TextUtils.isEmpty(rawStylesheet)) {
            return;
        }

        final Matcher matcher = IMPORT_PATTERN.matcher(rawStylesheet);
        final StringBuffer fullStylesheet = new StringBuffer();
        while (matcher.find()) {
            final String importContent = importFileProvider.getContent(matcher.group(1));
            matcher.appendReplacement(
                fullStylesheet,
                importContent != null ? Matcher.quoteReplacement(importContent) : ""
            );
        }
        matcher.appendTail(fullStylesheet);
        rawStylesheet = fullStylesheet.toString();
    }

    private void scan() throws CSSParsingException {
        final StringTokenizer tokenizer = new StringTokenizer(rawStylesheet, ":;{}\n", true);
        while (tokenizer.hasMoreTokens()) {
            final String token = tokenizer.nextToken();
            if (token.length() == 1) {
                char tokenFirstChar = token.charAt(0);
                if (
                    tokenFirstChar == ':' ||
                    tokenFirstChar == ';' ||
                    tokenFirstChar == '{' ||
                    tokenFirstChar == '}' ||
                    tokenFirstChar == '\n'
                ) {
                    consumeSpecialToken(tokenFirstChar);
                    continue;
                }
            }

            consumeToken(token);
        }
    }

    private void consumeToken(String token) {
        currentToken = (shouldMergePreviousToken ? currentToken : "") + (token != null ? token.trim() : "");
        shouldMergePreviousToken = false;
    }

    private void consumeSpecialToken(char tokenFirstChar) throws CSSParsingException {
        SpecialToken specialToken = SpecialToken.fromCharacter(tokenFirstChar);

        switch (specialToken) {
            case UNKNOWN:
            default:
                break;
            case BLOCK_START:
                switchToRulesetState();
                break;
            case BLOCK_END:
                switchOutOfRulesetState();
                break;
            case PROPERTY_SEPARATOR:
                switchOutOfPropertyNameState();
                break;
            case PROPERTY_END:
                switchOutOfPropertyValueState();
                break;
            case NEW_LINE:
                recoverLineEndingIfPossible();
                break;
        }
    }

    //region State switching

    public void switchToRulesetState() throws CSSParsingException {
        if (substate != Substate.SELECTOR || currentRuleset != null || TextUtils.isEmpty(currentToken)) {
            throwGenericParsingException();
        }

        if (currentToken.startsWith("@")) {
            // It's a media query!

            // We don't support nested media queries
            if (state != State.ROOT) {
                throwGenericParsingException();
            }

            state = State.MEDIA_QUERY;

            if (currentMediaQuery != null) {
                // That's an invalid state
                throwGenericParsingException();
            }

            currentMediaQuery = new MediaQuery();
            currentMediaQuery.rule = currentToken;
        } else {
            currentRuleset = new Ruleset();
            currentRuleset.selector = currentToken;

            substate = Substate.PROPERTY_NAME;
        }
    }

    public void switchOutOfRulesetState() throws CSSParsingException {
        if (substate == Substate.PROPERTY_VALUE) {
            // Try to close the property value if we can by simulating a ;
            switchOutOfPropertyValueState();
        }

        if (substate != Substate.PROPERTY_NAME && (state == State.MEDIA_QUERY && substate != Substate.SELECTOR)) {
            throwGenericParsingException();
        }

        if (state == State.MEDIA_QUERY) {
            if (currentRuleset != null) {
                if (currentMediaQuery == null) {
                    throwGenericParsingException();
                }
                currentMediaQuery.rulesets.add(currentRuleset);
                currentRuleset = null;
            } else {
                if (currentDocument == null || currentMediaQuery == null) {
                    throwGenericParsingException();
                }
                currentDocument.mediaQueries.add(currentMediaQuery);
                currentMediaQuery = null;
                state = State.ROOT;
            }
        } else {
            if (currentDocument == null || currentRuleset == null) {
                throwGenericParsingException();
            }
            currentDocument.rulesets.add(currentRuleset);
            currentRuleset = null;
        }

        substate = Substate.SELECTOR;
    }

    public void switchOutOfPropertyNameState() throws CSSParsingException {
        // Encountering a ":" while reading a media query is valid so add it back, and proceed)
        if (state == State.ROOT && substate == Substate.SELECTOR) {
            shouldMergePreviousToken = true;
            currentToken = currentToken + ":";
            return;
        }

        if (
            substate != Substate.PROPERTY_NAME ||
            currentRuleset == null ||
            currentDeclaration != null ||
            TextUtils.isEmpty(currentToken)
        ) {
            throwGenericParsingException();
        }

        if (currentToken.startsWith("--")) {
            // -- defines a variable
            currentDeclaration = new Variable();
        } else {
            currentDeclaration = new Declaration();
        }

        currentDeclaration.name = currentToken.toLowerCase(Locale.US).trim();

        substate = Substate.PROPERTY_VALUE;
    }

    public void switchOutOfPropertyValueState() throws CSSParsingException {
        if (
            substate != Substate.PROPERTY_VALUE ||
            TextUtils.isEmpty(currentToken) ||
            currentDeclaration == null ||
            currentRuleset == null
        ) {
            throwGenericParsingException();
        }

        currentDeclaration.value = currentToken.trim();
        currentRuleset.declarations.add(currentDeclaration);
        currentDeclaration = null;

        substate = Substate.PROPERTY_NAME;
    }

    public void recoverLineEndingIfPossible() throws CSSParsingException {
        if (substate == Substate.PROPERTY_VALUE) {
            switchOutOfPropertyValueState();
        }
    }

    public void throwGenericParsingException() throws CSSParsingException {
        throw new CSSParsingException("Internal parsing error");
    }

    //endregion

    //region Enums

    private enum State {
        ROOT,
        MEDIA_QUERY,
    }

    private enum Substate {
        SELECTOR,
        RULESET,
        PROPERTY_NAME,
        PROPERTY_VALUE,
    }

    private enum SpecialToken {
        UNKNOWN,
        BLOCK_START,
        BLOCK_END,
        PROPERTY_SEPARATOR,
        PROPERTY_END,
        NEW_LINE;

        public static SpecialToken fromCharacter(char c) {
            switch (c) {
                case '{':
                    return BLOCK_START;
                case '}':
                    return BLOCK_END;
                case ';':
                    return PROPERTY_END;
                case ':':
                    return PROPERTY_SEPARATOR;
                case '\n':
                    return NEW_LINE;
                default:
                    return UNKNOWN;
            }
        }
    }
    //endregion
}
