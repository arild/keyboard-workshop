package workshop;

import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.io.FilenameUtils.getExtension;

public class Github {

	public static List<Repo> getRepos(String githubUser, int maxNumRepos, List<String> fileExtensionFilter) throws UnirestException {

		return getAllReposMetaData(githubUser).stream().limit(maxNumRepos).map(repo -> {
			Optional<ZipInputStream> zipStream = getMasterBranch(githubUser, repo.name);
			if (!zipStream.isPresent()) {
				return null;
			}

			List<File> files = newArrayList();
			try {
				ZipEntry entry;
				while((entry = zipStream.get().getNextEntry()) != null) {
					if (!entry.isDirectory()) {
						ByteArrayOutputStream out = new ByteArrayOutputStream();
						byte data[] = new byte[1024];
						int count;
						while ((count = zipStream.get().read(data, 0, 1024)) != -1) {
							out.write(data, 0, count);
						}

						String extension = "." + getExtension(entry.getName());
						if (fileExtensionFilter.contains(extension)) {
							files.add(new File(entry.getName(), new String(out.toByteArray())));
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					zipStream.get().close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			return new Repo(repo.name, files);
		}).collect(toList());
	}

	private static List<RepoRepresentation> getAllReposMetaData(String user) throws UnirestException {
		HttpResponse<JsonNode> json = Unirest.get("https://api.github.com/users/" + user + "/repos").asJson();

		Type listType = new TypeToken<ArrayList<RepoRepresentation>>() {}.getType();
		return new GsonBuilder().create()
				.fromJson(json.getBody().toString(), listType);
	}

	private static Optional<ZipInputStream> getMasterBranch(String user, String repoName) {
		try {
			String url = "https://github.com/" + user + "/" + repoName + "/archive/master.zip";
			System.out.println("Fetching: " + url);
			InputStream inputStream = Unirest.get(url).asBinary().getRawBody();
			return Optional.of(new ZipInputStream(inputStream));
		} catch (UnirestException e) {
			return Optional.empty();
		}
	}

	public static class Repo {
		public String name;
		public List<File> files;

		public Repo(String name, List<File> files) {
			this.name = name;
			this.files = files;
		}
	}

	public static class File {
		public String name;
		public String content;

		public File(String name, String content) {
			this.name = name;
			this.content = content;
		}
	}

	private static class RepoRepresentation {
		public String name;
	}
}
