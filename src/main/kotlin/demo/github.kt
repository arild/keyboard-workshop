package demo

//import com.github.salomonbrys.kotson.*
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.gson.reflect.TypeToken
import com.mashape.unirest.http.Unirest


fun getRepos(githubUser: String, maxNumRepos: Int, fileExtensionFilte: List<String>): List<Repo> {
    val repos = getAllReposMetadata(githubUser)
    return repos
}


private fun getAllReposMetadata(githubUser: String): List<Repo> {
    val data = Unirest.get("https://api.github.com/users/$githubUser/repos").asJson()
    val repos: List<Repo> = objectMapper().readValue(data.body.toString())
    return repos
}
private fun objectMapper() = ObjectMapper().registerModule(KotlinModule()).disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

class Repo(val name: String)

inline fun <reified T> genericType() = object: TypeToken<T>() {}.type

