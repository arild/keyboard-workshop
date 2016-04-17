package workshop;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.MappingManager;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.spark.SparkConf;
import org.apache.spark.SparkContext;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static com.datastax.driver.core.DataType.text;
import static com.datastax.driver.core.schemabuilder.SchemaBuilder.createTable;
import static com.datastax.spark.connector.japi.CassandraJavaUtil.javaFunctions;
import static com.datastax.spark.connector.japi.CassandraJavaUtil.mapRowTo;
import static com.google.common.collect.Lists.newArrayList;


public class Main {

	public static void main(String[] args) throws Exception {
		Mapper<Files> mapper = createCassandraSchema();

		fetchGithubReposAndSaveToCassandra(mapper);

		runSparkAnalytics();
	}

	private static Mapper<Files> createCassandraSchema() {
		Cluster cluster = Cluster.builder()
				.addContactPoint("127.0.0.1")
				.build();

		Session session = cluster.connect();
		session.execute("CREATE KEYSPACE IF NOT EXISTS workshop WITH replication "
				+ "= {'class':'SimpleStrategy', 'replication_factor':1};");
		session.execute("use workshop;");

		session.execute(createTable("files").ifNotExists()
				.addPartitionKey("reponame", text())
				.addColumn("filename", text())
				.addColumn("content", text())
		);

		return new MappingManager(session).mapper(Files.class);
	}

	private static void fetchGithubReposAndSaveToCassandra(Mapper<Files> mapper) throws UnirestException {
		String githubUser = "apache";
		int maxNumRepos = 4;
		List<String> fileExtensionFilter = newArrayList(".java", ".scala");
		Github.getRepos(githubUser, maxNumRepos, fileExtensionFilter)
				.stream()
				// Transform hierarchical to flat structure
				.flatMap(repo -> repo.files
						.stream()
						.map(file -> new Files(repo.name, file.name, file.content))
				)
				// Save to cassandra
				.forEach(mapper::save);
	}

	private static void runSparkAnalytics() {
		SparkConf conf = new SparkConf()
				.setMaster("local[2]")
				.setAppName("workshopApp")
				.set("spark.cassandra.connection.host", "127.0.0.1");

		SparkContext sc = new SparkContext(conf);
		Stream<Map.Entry<String, Long>> symbalFrequencies = javaFunctions(sc)
				.cassandraTable("workshop", "files", mapRowTo(Files.class))
				.flatMap(f -> newArrayList(f.getContent().toLowerCase().split("")))
				.countByValue()
				.entrySet()
				.stream()
				.sorted(Map.Entry.<String, Long>comparingByValue().reversed());

		System.out.println("\nTop 10 most frequent symbols:");
		System.out.println("-----------------------------");
		symbalFrequencies.limit(10).forEach(t -> System.out.println(t.getKey() + " -> " + t.getValue()));
	}
}
