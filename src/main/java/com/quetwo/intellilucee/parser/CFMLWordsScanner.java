package com.quetwo.intellilucee.parser;

import com.intellij.lang.cacheBuilder.WordOccurrence;
import com.intellij.lang.cacheBuilder.WordsScanner;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;

public class CFMLWordsScanner implements WordsScanner
{

    @Override
    public void processWords(@NotNull CharSequence fileText, @NotNull Processor<? super WordOccurrence> processor)
    {
        int len = fileText.length();
        if (len == 0)
        {
            return;
        }

        WordOccurrence occurrence = new WordOccurrence(fileText, 0, 0, null);
        int i = 0;
        boolean inCfqueryBody = false;

        while (i < len)
        {
            char c = fileText.charAt(i);

            // 1. Whitespace
            if (c <= ' ' && (c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\f'))
            {
                i++;
                while (i < len)
                {
                    char ch = fileText.charAt(i);
                    if (ch <= ' ' && (ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r' || ch == '\f'))
                    {
                        i++;
                    }
                    else
                    {
                        break;
                    }
                }
                continue;
            }

            // 2. CFML Tag comment: <!--- ... ---> (supports nesting)
            if (c == '<' && i + 4 < len && fileText.charAt(i + 1) == '!' && fileText.charAt(i + 2) == '-' && fileText.charAt(i + 3) == '-' && fileText.charAt(i + 4) == '-')
            {
                int depth = 1;
                i += 5;
                while (i < len && depth > 0)
                {
                    if (i + 4 < len && fileText.charAt(i) == '<' && fileText.charAt(i + 1) == '!' && fileText.charAt(i + 2) == '-' && fileText.charAt(i + 3) == '-' && fileText.charAt(i + 4) == '-')
                    {
                        depth++;
                        i += 5;
                        continue;
                    }
                    if (i + 3 < len && fileText.charAt(i) == '-' && fileText.charAt(i + 1) == '-' && fileText.charAt(i + 2) == '-' && fileText.charAt(i + 3) == '>')
                    {
                        depth--;
                        i += 4;
                        continue;
                    }

                    char ch = fileText.charAt(i);
                    if (Character.isJavaIdentifierStart(ch) || ch == '$')
                    {
                        int wordStart = i;
                        i++;
                        while (i < len && (Character.isJavaIdentifierPart(fileText.charAt(i)) || fileText.charAt(i) == '$'))
                        {
                            if (i + 3 < len && fileText.charAt(i) == '-' && fileText.charAt(i + 1) == '-' && fileText.charAt(i + 2) == '-' && fileText.charAt(i + 3) == '>')
                            {
                                break;
                            }
                            i++;
                        }
                        occurrence.init(fileText, wordStart, i, WordOccurrence.Kind.COMMENTS);
                        if (!processor.process(occurrence))
                        {
                            return;
                        }
                        continue;
                    }

                    i++;
                }
                continue;
            }

            // 3. HTML comment: <!-- ... -->
            if (c == '<' && i + 3 < len && fileText.charAt(i + 1) == '!' && fileText.charAt(i + 2) == '-' && fileText.charAt(i + 3) == '-')
            {
                i += 4;
                while (i < len)
                {
                    if (i + 2 < len && fileText.charAt(i) == '-' && fileText.charAt(i + 1) == '-' && fileText.charAt(i + 2) == '>')
                    {
                        i += 3;
                        break;
                    }

                    char ch = fileText.charAt(i);
                    if (Character.isJavaIdentifierStart(ch) || ch == '$')
                    {
                        int wordStart = i;
                        i++;
                        while (i < len && (Character.isJavaIdentifierPart(fileText.charAt(i)) || fileText.charAt(i) == '$'))
                        {
                            if (i + 2 < len && fileText.charAt(i) == '-' && fileText.charAt(i + 1) == '-' && fileText.charAt(i + 2) == '>')
                            {
                                break;
                            }
                            i++;
                        }
                        occurrence.init(fileText, wordStart, i, WordOccurrence.Kind.COMMENTS);
                        if (!processor.process(occurrence))
                        {
                            return;
                        }
                        continue;
                    }

                    i++;
                }
                continue;
            }

            // 4. Script block comment: /* ... */
            if (c == '/' && i + 1 < len && fileText.charAt(i + 1) == '*')
            {
                i += 2;
                while (i < len)
                {
                    if (i + 1 < len && fileText.charAt(i) == '*' && fileText.charAt(i + 1) == '/')
                    {
                        i += 2;
                        break;
                    }

                    char ch = fileText.charAt(i);
                    if (Character.isJavaIdentifierStart(ch) || ch == '$')
                    {
                        int wordStart = i;
                        i++;
                        while (i < len && (Character.isJavaIdentifierPart(fileText.charAt(i)) || fileText.charAt(i) == '$'))
                        {
                            if (i + 1 < len && fileText.charAt(i) == '*' && fileText.charAt(i + 1) == '/')
                            {
                                break;
                            }
                            i++;
                        }
                        occurrence.init(fileText, wordStart, i, WordOccurrence.Kind.COMMENTS);
                        if (!processor.process(occurrence))
                        {
                            return;
                        }
                        continue;
                    }

                    i++;
                }
                continue;
            }

            // 5. Script line comment: // ...
            if (c == '/' && i + 1 < len && fileText.charAt(i + 1) == '/')
            {
                i += 2;
                while (i < len)
                {
                    char ch = fileText.charAt(i);
                    if (ch == '\n' || ch == '\r')
                    {
                        break;
                    }

                    if (Character.isJavaIdentifierStart(ch) || ch == '$')
                    {
                        int wordStart = i;
                        i++;
                        while (i < len)
                        {
                            char wch = fileText.charAt(i);
                            if (wch == '\n' || wch == '\r')
                            {
                                break;
                            }
                            if (Character.isJavaIdentifierPart(wch) || wch == '$')
                            {
                                i++;
                            }
                            else
                            {
                                break;
                            }
                        }
                        occurrence.init(fileText, wordStart, i, WordOccurrence.Kind.COMMENTS);
                        if (!processor.process(occurrence))
                        {
                            return;
                        }
                        continue;
                    }

                    i++;
                }
                continue;
            }

            // 5.1 SQL line comment inside <cfquery> body: -- ...
            if (inCfqueryBody && c == '-' && i + 1 < len && fileText.charAt(i + 1) == '-')
            {
                i += 2;
                while (i < len)
                {
                    char ch = fileText.charAt(i);
                    if (ch == '\n' || ch == '\r')
                    {
                        break;
                    }

                    if (Character.isJavaIdentifierStart(ch) || ch == '$')
                    {
                        int wordStart = i;
                        i++;
                        while (i < len)
                        {
                            char wch = fileText.charAt(i);
                            if (wch == '\n' || wch == '\r')
                            {
                                break;
                            }
                            if (Character.isJavaIdentifierPart(wch) || wch == '$')
                            {
                                i++;
                            }
                            else
                            {
                                break;
                            }
                        }
                        occurrence.init(fileText, wordStart, i, WordOccurrence.Kind.COMMENTS);
                        if (!processor.process(occurrence))
                        {
                            return;
                        }
                        continue;
                    }

                    i++;
                }
                continue;
            }

            // 6. Strings: "..." or '...'
            if (c == '"' || c == '\'')
            {
                char quote = c;
                i++;
                while (i < len)
                {
                    char ch = fileText.charAt(i);
                    if (ch == quote)
                    {
                        if (i + 1 < len && fileText.charAt(i + 1) == quote)
                        {
                            i += 2;
                            continue;
                        }
                        i++;
                        break;
                    }

                    if (Character.isJavaIdentifierStart(ch) || ch == '$')
                    {
                        int wordStart = i;
                        i++;
                        while (i < len)
                        {
                            char wch = fileText.charAt(i);
                            if (wch == quote)
                            {
                                break;
                            }
                            if (Character.isJavaIdentifierPart(wch) || wch == '$')
                            {
                                i++;
                            }
                            else
                            {
                                break;
                            }
                        }
                        occurrence.init(fileText, wordStart, i, WordOccurrence.Kind.LITERALS);
                        if (!processor.process(occurrence))
                        {
                            return;
                        }
                        continue;
                    }

                    i++;
                }
                continue;
            }

            // 7. Code words: identifiers, function names, variable names, tag names, attribute names, keywords
            if (Character.isJavaIdentifierStart(c) || c == '$')
            {
                int wordStart = i;
                i++;
                while (i < len)
                {
                    char ch = fileText.charAt(i);
                    if (Character.isJavaIdentifierPart(ch) || ch == '$')
                    {
                        i++;
                    }
                    else
                    {
                        break;
                    }
                }
                occurrence.init(fileText, wordStart, i, WordOccurrence.Kind.CODE);
                if (!processor.process(occurrence))
                {
                    return;
                }
                continue;
            }

            // 8. Delimiters, operators, punctuation, numbers, and tags
            if (c == '<' && i + 8 < len && fileText.subSequence(i, i + 9).toString().equalsIgnoreCase("</cfquery"))
            {
                inCfqueryBody = false;
            }
            else if (c == '<' && i + 7 < len && fileText.subSequence(i, i + 8).toString().equalsIgnoreCase("<cfquery"))
            {
                inCfqueryBody = true;
            }
            i++;
        }
    }
}
