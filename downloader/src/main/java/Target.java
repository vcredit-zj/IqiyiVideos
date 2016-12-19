import java.util.Set;
import java.util.HashSet;
import java.net.URL;
import java.net.MalformedURLException;

/**
 * Each instance of this class represents a target to download.
 * 
 * @author Zhen Chen
 *
 */

public class Target {
	private URL url;
	private String title;
	private Set<String> formats = new HashSet<String>();

	public Target(String url) throws MalformedURLException {
		setUrl(url);
	}

	public Target(URL url) {
		setUrl(url);
	}

	public Target(String url, String title) throws MalformedURLException {
		setUrl(url);
		setTitle(title);
	}

	public final URL getUrl() {
		return url;
	}

	public final void setUrl(String url) throws MalformedURLException {
		this.url = new URL(url);
	}

	public final void setUrl(URL url) {
		this.url = url;
	}

	public final String getTitle() {
		return title;
	}

	public final void setTitle(String title) {
		this.title = title;
	}

	public final Set<String> getFormats() {
		return formats;
	}

	public final void addFormat(String format) {
		this.formats.add(format);
	}

	// two targets are considered equal if they have the same URL
	@Override
	public boolean equals(Object o) {
		if (o instanceof Target) {
			return url.toString().equals(((Target) o).getUrl().toString());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return url.toString().hashCode();
	}

}
