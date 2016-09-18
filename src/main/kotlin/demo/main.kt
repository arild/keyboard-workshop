package demo

fun main(args: Array<String>) {
    println(getRepos(
            githubUser = "apache",
            maxNumRepos = 1,
            fileExtensionFilte = listOf(".java", ".scala")))
}
