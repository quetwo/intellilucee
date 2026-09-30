package com.quetwo.intellilucee.parser

import com.intellij.lang.cacheBuilder.WordOccurrence
import org.junit.Assert.*
import org.junit.Test

class CFMLWordsScannerTest {

    private fun scanWords(text: String): List<Pair<String, WordOccurrence.Kind?>> {
        val scanner = CFMLWordsScanner()
        val result = mutableListOf<Pair<String, WordOccurrence.Kind?>>()
        scanner.processWords(text) { occurrence ->
            val word = text.substring(occurrence.start, occurrence.end)
            result.add(Pair(word, occurrence.kind))
            true
        }
        return result
    }

    @Test
    fun testCodeWords() {
        val code = """
            component name="MyComponent" extends="Base" {
                public numeric function calculateTotal(required numeric price, numeric tax = 0.05) {
                    var subtotal = price * (1 + tax);
                    return subtotal;
                }
            }
        """.trimIndent()

        val words = scanWords(code)
        val wordTexts = words.map { it.first }

        assertTrue(wordTexts.contains("component"))
        assertTrue(wordTexts.contains("name"))
        assertTrue(wordTexts.contains("MyComponent"))
        assertTrue(wordTexts.contains("extends"))
        assertTrue(wordTexts.contains("Base"))
        assertTrue(wordTexts.contains("calculateTotal"))
        assertTrue(wordTexts.contains("subtotal"))
        assertTrue(wordTexts.contains("price"))
        assertTrue(wordTexts.contains("tax"))
        assertTrue(wordTexts.contains("return"))

        val totalKind = words.first { it.first == "calculateTotal" }.second
        assertEquals(WordOccurrence.Kind.CODE, totalKind)
    }

    @Test
    fun testTagAndAttributeWords() {
        val code = """
            <cffunction name="getUser" access="public" returntype="query">
                <cfargument name="userId" type="numeric" required="true">
                <cfquery name="qUser" datasource="myDSN">
                    SELECT id, username FROM users WHERE id = #arguments.userId#
                </cfquery>
                <cfreturn qUser>
            </cffunction>
        """.trimIndent()

        val words = scanWords(code)
        val wordTexts = words.map { it.first }

        assertTrue(wordTexts.contains("cffunction"))
        assertTrue(wordTexts.contains("name"))
        assertTrue(wordTexts.contains("getUser"))
        assertTrue(wordTexts.contains("cfargument"))
        assertTrue(wordTexts.contains("userId"))
        assertTrue(wordTexts.contains("cfquery"))
        assertTrue(wordTexts.contains("qUser"))
        assertTrue(wordTexts.contains("datasource"))
        assertTrue(wordTexts.contains("SELECT"))
        assertTrue(wordTexts.contains("username"))
        assertTrue(wordTexts.contains("users"))
        assertTrue(wordTexts.contains("arguments"))
        assertTrue(wordTexts.contains("cfreturn"))
    }

    @Test
    fun testCommentsWords() {
        val code = """
            <!--- CFML comment with nested <!--- inner comment ---> tag --->
            <!-- HTML comment here -->
            /* Block comment with details */
            // Line comment at end
            var active = true;
        """.trimIndent()

        val words = scanWords(code)

        val cfmlCommentWords = words.filter { it.second == WordOccurrence.Kind.COMMENTS }.map { it.first }
        assertTrue(cfmlCommentWords.contains("CFML"))
        assertTrue(cfmlCommentWords.contains("nested"))
        assertTrue(cfmlCommentWords.contains("inner"))
        assertTrue(cfmlCommentWords.contains("HTML"))
        assertTrue(cfmlCommentWords.contains("Block"))
        assertTrue(cfmlCommentWords.contains("Line"))

        val codeWords = words.filter { it.second == WordOccurrence.Kind.CODE }.map { it.first }
        assertTrue(codeWords.contains("var"))
        assertTrue(codeWords.contains("active"))
        assertTrue(codeWords.contains("true"))
    }

    @Test
    fun testStringLiteralsWords() {
        val code = """
            var message = "Hello, \"World\" from CFML!";
            var query = 'SELECT * FROM "table" WHERE name = ''John''';
        """.trimIndent()

        val words = scanWords(code)
        val literalWords = words.filter { it.second == WordOccurrence.Kind.LITERALS }.map { it.first }

        assertTrue(literalWords.contains("Hello"))
        assertTrue(literalWords.contains("World"))
        assertTrue(literalWords.contains("from"))
        assertTrue(literalWords.contains("CFML"))
        assertTrue(literalWords.contains("SELECT"))
        assertTrue(literalWords.contains("table"))
        assertTrue(literalWords.contains("WHERE"))
        assertTrue(literalWords.contains("John"))
    }

    @Test
    fun testEarlyCancellation() {
        val scanner = CFMLWordsScanner()
        val text = "one two three four five six seven eight nine ten"
        var count = 0
        scanner.processWords(text) {
            count++
            count < 3 // Cancel after 3 words
        }
        assertEquals(3, count)
    }

    @Test
    fun testBenchmarkThousandsOfFiles() {
        val scanner = CFMLWordsScanner()
        val template = """
            <!--- MasaCMS Content Template --->
            <cfoutput>
            <div class="mura-content" data-id="12345">
                <h1>#$.getTitle()#</h1>
                <p>#$.getBody()#</p>
                <cfloop query="qArticles">
                    <article class="article-item">
                        <h2>#qArticles.headline#</h2>
                        <span class="date">#DateFormat(qArticles.releaseDate, "yyyy-mm-dd")#</span>
                    </article>
                </cfloop>
            </div>
            </cfoutput>
        """.trimIndent()

        val start = System.currentTimeMillis()
        var totalWords = 0
        val fileCount = 2000
        for (f in 1..fileCount) {
            scanner.processWords(template) {
                totalWords++
                true
            }
        }
        val elapsed = System.currentTimeMillis() - start
        println("[BENCHMARK] CFMLWordsScanner scanned $fileCount files ($totalWords words) in ${elapsed}ms")
        assertTrue("Scanning $fileCount files should take < 500ms (was ${elapsed}ms)", elapsed < 500)
    }
}
