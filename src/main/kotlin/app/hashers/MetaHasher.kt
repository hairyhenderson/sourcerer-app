// Copyright 2017 Sourcerer Inc. All Rights Reserved.
// Author: Liubov Yaronskaya (lyaronskaya@sourcerer.io)

package app.hashers

import app.FactCodes
import app.Logger
import app.api.Api
import app.model.Author
import app.model.Fact
import app.model.Repo

/**
 * CommitHasher hashes repository and uploads stats to server.
 */
class MetaHasher(private val serverRepo: Repo = Repo(),
                 private val api: Api) {
    fun calculateAndSendFacts(authors: HashSet<Author>) {
        val facts = mutableListOf<Fact>()
        facts.add(Fact(serverRepo, FactCodes.REPO_TEAM_SIZE, 0,
            getAuthorsNum(authors).toString()))
        postFactsToServer(facts)
    }

    private fun getAuthorsNum(authors: HashSet<Author>): Int {
        val names = authors.map { it.name }
        val emails = authors.map { it.email.split("@")[0] }

        val results = Array(authors.size) { Array(authors.size) {0} }

        for (i in 0..authors.size-2) {
            for (j in i+1..authors.size-1) {
                if (isSameAuthor(names[i], names[j])) {
                    results[j][i] = 1
                }
                if (isSameAuthor(emails[i], emails[j])) {
                    results[j][i] = 1
                }
            }
        }

        return results.filter { it.sum() == 0 }.size
    }

    private fun isSameAuthor(first: String, second: String): Boolean {
        val firstThreegrams = getThreegrams(first)
        val secondThreegrams = getThreegrams(second)

        val intersectionSize = firstThreegrams.intersect(secondThreegrams).size
        val unionSize = firstThreegrams.union(secondThreegrams).size
        val jaccardValue = intersectionSize.toFloat() / unionSize
        return jaccardValue >= 0.3
    }

    private fun getThreegrams(str: String): Set<String> {
        val threegrams = mutableSetOf<String>()
        for (i in 0..str.length-3) {
            threegrams.add(listOf(str[i], str[i+1], str[i+2]).joinToString(""))
        }
        return threegrams
    }

    private fun postFactsToServer(facts: List<Fact>) {
        if (facts.isNotEmpty()) {
            api.postFacts(facts).onErrorThrow()
            Logger.info { "Sent ${facts.size} facts to server" }
        }
    }
}
