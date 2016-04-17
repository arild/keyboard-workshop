package workshop;

import com.datastax.driver.mapping.annotations.Table;

import java.io.Serializable;

@Table(keyspace = "workshop", name = "files")
public class Files implements Serializable {
	private String reponame;
	private String filename;
	private String content;

	public Files() {
	}

	public Files(String reponame, String filename, String content) {
		this.reponame = reponame;
		this.filename = filename;
		this.content = content;
	}

	public String getReponame() {
		return reponame;
	}

	public void setReponame(String reponame) {
		this.reponame = reponame;
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}
}