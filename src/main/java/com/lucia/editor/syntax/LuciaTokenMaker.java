package com.lucia.editor.syntax;

import java.util.Set;
import org.fife.ui.rsyntaxtextarea.AbstractTokenMaker;
import org.fife.ui.rsyntaxtextarea.RSyntaxUtilities;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenMap;
import org.fife.ui.rsyntaxtextarea.TokenTypes;

public class LuciaTokenMaker extends AbstractTokenMaker {

    private static final Set<String> BOOLEAN_LITERALS = Set.of("true", "false");

    private static boolean isSeparator(char c) {
        return c == '{' || c == '}'
                || c == '(' || c == ')'
                || c == '[' || c == ']'
                || c == ',' || c == ';';
    }

    @Override
    public TokenMap getWordsToHighlight() {
        TokenMap tokenMap = new TokenMap();

        // Control flow and structure keywords
        tokenMap.put("func",        TokenTypes.RESERVED_WORD);
        tokenMap.put("if",          TokenTypes.RESERVED_WORD);
        tokenMap.put("else",        TokenTypes.RESERVED_WORD);
        tokenMap.put("while",       TokenTypes.RESERVED_WORD);
        tokenMap.put("do",          TokenTypes.RESERVED_WORD);
        tokenMap.put("for",         TokenTypes.RESERVED_WORD);
        tokenMap.put("return",      TokenTypes.RESERVED_WORD);
        tokenMap.put("import",      TokenTypes.RESERVED_WORD);
        tokenMap.put("break",       TokenTypes.RESERVED_WORD);
        tokenMap.put("continue",    TokenTypes.RESERVED_WORD);
        tokenMap.put("case",        TokenTypes.RESERVED_WORD);
        tokenMap.put("default",     TokenTypes.RESERVED_WORD);
        tokenMap.put("switch",      TokenTypes.RESERVED_WORD);
        tokenMap.put("try",         TokenTypes.RESERVED_WORD);
        tokenMap.put("catch",       TokenTypes.RESERVED_WORD);
        tokenMap.put("finally",     TokenTypes.RESERVED_WORD);
        tokenMap.put("class",       TokenTypes.RESERVED_WORD);
        tokenMap.put("constructor", TokenTypes.RESERVED_WORD);
        tokenMap.put("let",         TokenTypes.RESERVED_WORD);
        tokenMap.put("in",          TokenTypes.RESERVED_WORD);
        tokenMap.put("this",        TokenTypes.RESERVED_WORD);
        tokenMap.put("not",         TokenTypes.RESERVED_WORD);
        tokenMap.put("null",        TokenTypes.RESERVED_WORD_2);

        // Built-in functions
        tokenMap.put("print",   TokenTypes.FUNCTION);
        tokenMap.put("len",     TokenTypes.FUNCTION);
        tokenMap.put("input",   TokenTypes.FUNCTION);
        tokenMap.put("str",     TokenTypes.FUNCTION);
        tokenMap.put("type_of", TokenTypes.FUNCTION);
        tokenMap.put("abs",     TokenTypes.FUNCTION);
        tokenMap.put("min",     TokenTypes.FUNCTION);
        tokenMap.put("max",     TokenTypes.FUNCTION);
        tokenMap.put("pow",     TokenTypes.FUNCTION);
        tokenMap.put("round",   TokenTypes.FUNCTION);
        tokenMap.put("floor",   TokenTypes.FUNCTION);
        tokenMap.put("ceil",    TokenTypes.FUNCTION);
        tokenMap.put("random",  TokenTypes.FUNCTION);
        tokenMap.put("today",   TokenTypes.FUNCTION);
        tokenMap.put("now",     TokenTypes.FUNCTION);

        // Built-in types
        tokenMap.put("int",    TokenTypes.DATA_TYPE);
        tokenMap.put("float",  TokenTypes.DATA_TYPE);
        tokenMap.put("string", TokenTypes.DATA_TYPE);
        tokenMap.put("bool",   TokenTypes.DATA_TYPE);
        tokenMap.put("void",   TokenTypes.DATA_TYPE);
        tokenMap.put("any",    TokenTypes.DATA_TYPE);
        tokenMap.put("list",   TokenTypes.DATA_TYPE);
        tokenMap.put("dict",   TokenTypes.DATA_TYPE);
        tokenMap.put("date",   TokenTypes.DATA_TYPE);
        tokenMap.put("datetime", TokenTypes.DATA_TYPE);

        return tokenMap;
    }

    @Override
    public void addToken(char[] segment, int start, int end, int tokenType, int startOffset) {
        if (tokenType == TokenTypes.IDENTIFIER) {
            int value = wordsToHighlight.get(segment, start, end);
            if (value != -1) {
                tokenType = value;
            } else {
                String word = new String(segment, start, end - start + 1);
                if (BOOLEAN_LITERALS.contains(word)) {
                    tokenType = TokenTypes.LITERAL_BOOLEAN;
                }
            }
        }
        super.addToken(segment, start, end, tokenType, startOffset);
    }

    @Override
    public Token getTokenList(javax.swing.text.Segment text, int initialTokenType, int startOffset) {
        resetTokenList();

        char[] array = text.array;
        int offset = text.offset;
        int count = text.count;
        int end = offset + count;

        // initialTokenType carries multi-line state from the previous line
        int tokenStart = offset;
        int tokenType = initialTokenType;

        for (int i = offset; i < end; i++) {
            char c = array[i];

            switch (tokenType) {
                case TokenTypes.NULL:
                    tokenStart = i;
                    if (Character.isWhitespace(c)) {
                        tokenType = TokenTypes.WHITESPACE;
                    } else if (c == '/' && i + 1 < end && array[i + 1] == '/') {
                        tokenType = TokenTypes.COMMENT_EOL;
                    } else if (c == '/' && i + 1 < end && array[i + 1] == '*') {
                        tokenType = TokenTypes.COMMENT_MULTILINE;
                        i++; // consume '*'
                    } else if (i + 1 < end && isCompoundOperator(c, array[i + 1])) {
                        addToken(array, i, i + 1, TokenTypes.OPERATOR, startOffset + i - offset);
                        i++; // consume compound operator second char
                        tokenType = TokenTypes.NULL;
                    } else if (c == '"') {
                        tokenType = TokenTypes.LITERAL_STRING_DOUBLE_QUOTE;
                    } else if (RSyntaxUtilities.isDigit(c)) {
                        tokenType = TokenTypes.LITERAL_NUMBER_DECIMAL_INT;
                    } else if (RSyntaxUtilities.isLetter(c) || c == '_') {
                        tokenType = TokenTypes.IDENTIFIER;
                    } else {
                        int symbolType = isSeparator(c) ? TokenTypes.SEPARATOR : TokenTypes.OPERATOR;
                        addToken(array, i, i, symbolType, startOffset + i - offset);
                        tokenType = TokenTypes.NULL;
                    }
                    break;

                case TokenTypes.WHITESPACE:
                    if (!Character.isWhitespace(c)) {
                        addToken(array, tokenStart, i - 1, tokenType, startOffset + tokenStart - offset);
                        tokenType = TokenTypes.NULL;
                        i--;
                    }
                    break;

                case TokenTypes.COMMENT_EOL:
                    // Rest of line is a comment — consume it all
                    addToken(array, tokenStart, end - 1, tokenType, startOffset + tokenStart - offset);
                    addNullToken();
                    return firstToken;

                case TokenTypes.COMMENT_MULTILINE:
                    // Look for closing */
                    if (c == '*' && i + 1 < end && array[i + 1] == '/') {
                        i++; // consume '/'
                        addToken(array, tokenStart, i, tokenType, startOffset + tokenStart - offset);
                        tokenType = TokenTypes.NULL;
                    }
                    break;

                case TokenTypes.LITERAL_STRING_DOUBLE_QUOTE:
                    if (c == '"') {
                        addToken(array, tokenStart, i, tokenType, startOffset + tokenStart - offset);
                        tokenType = TokenTypes.NULL;
                    }
                    break;

                case TokenTypes.LITERAL_NUMBER_DECIMAL_INT:
                    if (!RSyntaxUtilities.isDigit(c) && c != '.') {
                        addToken(array, tokenStart, i - 1, tokenType, startOffset + tokenStart - offset);
                        tokenType = TokenTypes.NULL;
                        i--;
                    }
                    break;

                case TokenTypes.IDENTIFIER:
                    if (!RSyntaxUtilities.isLetterOrDigit(c) && c != '_') {
                        addToken(array, tokenStart, i - 1, tokenType, startOffset + tokenStart - offset);
                        tokenType = TokenTypes.NULL;
                        i--;
                    }
                    break;

                default:
                    tokenType = TokenTypes.NULL;
                    break;
            }
        }

        if (tokenType != TokenTypes.NULL) {
            addToken(array, tokenStart, end - 1, tokenType, startOffset + tokenStart - offset);
        }

        // For block comments that span to the next line, omit the null token so
        // RSyntaxTextArea passes COMMENT_MULTILINE as initialTokenType on the next line.
        if (tokenType != TokenTypes.COMMENT_MULTILINE) {
            addNullToken();
        }
        return firstToken;
    }

    private static boolean isCompoundOperator(char first, char second) {
        if ((first == '+' || first == '-') && second == first) {
            return true;
        }
        if (second != '=') {
            return false;
        }
        return first == '+' || first == '-' || first == '*' || first == '/' || first == '%';
    }
}
