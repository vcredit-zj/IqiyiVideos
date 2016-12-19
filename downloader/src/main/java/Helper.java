import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.google.gson.Gson;
import com.google.gson.JsonParser;

/**
 * This class contains all helper methods for the program.
 * 
 * @author Zhen Chen
 *
 */

public final class Helper {
	private static final ExecutableFileFilter executableFileFilter = new ExecutableFileFilter();
	public static final BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
	public static final JsonParser jsonParser = new JsonParser();
	public static final Gson gson = new Gson();

	private static final class ExecutableFileFilter implements FileFilter {
		private static final String EXECUTABLE_PATTERN = "[\\S]+(\\.(?i)(exe))$";
		private static final Pattern P = Pattern.compile(EXECUTABLE_PATTERN);

		@Override
		public boolean accept(File pathname) {
			return P.matcher(pathname.getName()).matches();
		}
	}

	/**
	 * Return an array of executable programs in the given directory using the
	 * executableFileFilter. If the given path is already an executable program,
	 * return an array containing the path of this program only, otherwise
	 * return an empty array.
	 * 
	 * @param path
	 * @return an array of executable files in the given directory, or an array
	 *         containing the given path only if it is an executable program
	 */
	public static final File[] getExecutable(String path) {
		File file = new File(path);
		File[] files = new File[1];
		if (file.isDirectory()) {
			return file.listFiles(executableFileFilter);
		} else if (executableFileFilter.accept(file)) {
			files[0] = file;
		}
		return files;
	}

	/**
	 * This method calls getExecutable(String path) to get paths of all
	 * executable programs in the given path and return the first one.
	 * 
	 * @param path
	 *            a path to a directory containing executable programs or a path
	 *            of an executable program
	 * @return the path of first executable program found in the given path
	 * @throws FileNotFoundException
	 *             if the given path does not contain any executable programs
	 *             and the given path is not a valid executable program either
	 */
	public static final String getFirstExecutablePath(String path) throws FileNotFoundException {
		File firstExecutable = getExecutable(path)[0];
		if (firstExecutable == null) {
			throw new FileNotFoundException("No executable program found in the given path: " + path);
		}
		return firstExecutable.getAbsolutePath();
	}

	public static String getUserChoice(Set<String> options) throws IOException {
		String line;
		do {
			line = input.readLine().toLowerCase();
		} while (!options.contains(line));
		return line;
	}

	public static String getUserChoice(String message, Set<String> options) throws IOException {
		String line;
		do {
			System.out.printf(message);
			line = input.readLine().toLowerCase();
		} while (!options.contains(line));
		return line;
	}

	public static <V> V getUserChoice(String message, Map<String, V> options) throws IOException {
		String line;
		do {
			System.out.printf(message);
			line = input.readLine().toLowerCase();
		} while (!options.containsKey(line));
		return options.get(line);
	}

	public static final String load(String filename) {
		StringBuilder sb = new StringBuilder();
		String line;

		try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
			while ((line = br.readLine()) != null) {
				sb.append(line);
				sb.append(System.lineSeparator());
			}
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

		return sb.toString();
	}

	public static final void save(String filename, String content) {
		try (Writer writer = new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(filename), "UTF8"))) {
			writer.write(content);
			writer.write(System.lineSeparator());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
