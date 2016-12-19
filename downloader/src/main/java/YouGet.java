import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.Map;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Each instance of this class represents a You-Get process.
 * 
 * @author Zhen Chen
 *
 */

public class YouGet extends Thread {
	private static final int MAX_ATTEMPTS = 3;
	private static String executable;
	// charset of the output of YouGet process, platform dependent
	private static String charset;
	private Target target;
	private Task task;
	private String url;
	private String path;
	private String preferredFormat;
	private boolean forceWrite;
	private boolean success;

	public static enum Task {
		INFO, DOWNLOAD;
	}

	public static final String getExecutable() {
		return executable;
	}

	/**
	 * If the given path is a directory, locate the first executable program in
	 * it. If the given path is a valid executable program, set it to the
	 * executable.
	 * 
	 * @param path
	 *            a path to a directory containing executable programs or a path
	 *            of an executable program
	 * @throws FileNotFoundException
	 *             if the given path does not contain any executable programs
	 *             and the given path is not a valid executable program either
	 */
	public static final void setExecutable(String path) throws FileNotFoundException {
		executable = Helper.getFirstExecutablePath(path);
	}

	public static final String getCharset() {
		return charset;
	}

	public static final void setCharset(String charset) {
		YouGet.charset = charset;
	}

	public YouGet(Target target, Task task) {
		setTarget(target);
		setTask(task);
		setPath(null);
	}

	public YouGet(Target target, Task task, String path, String preferredFormat, boolean forceWrite) {
		setTarget(target);
		setTask(task);
		setPath(path);
		setPreferredFormat(preferredFormat);
		setForceWrite(forceWrite);
	}

	public final Target getTarget() {
		return target;
	}

	public final void setTarget(Target target) {
		this.target = target;
		setUrl(target);
	}

	public final Task getTask() {
		return task;
	}

	public final void setTask(Task task) {
		this.task = task;
	}

	public final String getUrl() {
		return url;
	}

	private final void setUrl(Target target) {
		this.url = target.getUrl().toString();
	}

	public final String getPath() {
		return path;
	}

	public final void setPath(String path) {
		if (path != null) {
			this.path = path;
		} else {
			this.path = "/";
		}
	}

	public final String getPreferredFormat() {
		return preferredFormat;
	}

	public final void setPreferredFormat(String preferredFormat) {
		this.preferredFormat = preferredFormat;
	}

	public final boolean getForceWrite() {
		return forceWrite;
	}

	public final void setForceWrite(boolean forceWrite) {
		this.forceWrite = forceWrite;
	}

	public final boolean isSuccess() {
		return success;
	}

	/**
	 * Only MAX_ATTEMPTS number of running times are allowed. If there is a
	 * major exception happened, stop with no more attempts.
	 * 
	 * If there is no task set, the running will be considered as a success.
	 */
	@Override
	public void run() {
		success = false;
		if (executable == null) {
			System.err.println("You must call setExecutable(String path) method before run it!");
			return;
		}
		for (int failedAttempts = 0; failedAttempts < MAX_ATTEMPTS; failedAttempts++) {
			try {
				switch (task) {
				case INFO:
					info();
					break;
				case DOWNLOAD:
					download();
					break;
				}
				success = true;
				break;
			} catch (IOException e) {
				synchronized (Controller.printLock) {
					e.printStackTrace();
				}
				break;
			} catch (InterruptedException e) {
				if (failedAttempts == MAX_ATTEMPTS - 1) {
					// only print error message when failed MAX_ATTEMPTS times
					synchronized (Controller.printLock) {
						e.printStackTrace();
					}
				}
			} catch (ProcessErrorException e) {
				if (failedAttempts == MAX_ATTEMPTS - 1) {
					// only print error message when failed MAX_ATTEMPTS times
					synchronized (Controller.printLock) {
						System.err.println(e.getMessage());
					}
				}
			}
		}
	}

	/**
	 * It will run the YouGet program to get the info of the target URL if it
	 * has not been fetched yet. It will update the title of the target using
	 * the returned Json data.
	 * 
	 * It needs a user specified charset to read the output of the YouGet
	 * program correctly.
	 * 
	 * @throws ProcessErrorException
	 *             if YouGet failed to get info of the target
	 * @throws IOException
	 *             if failed to access or run the program, or if the specified
	 *             charset for ProcessReader is invalid
	 * @throws InterruptedException
	 */
	private void info() throws ProcessErrorException, IOException, InterruptedException {
		if (target.getTitle() != null) {
			return;
		}
		Process p = new ProcessBuilder(executable, "--json", "\"" + url + "\"").start();
		ProcessReader pr = readProcess(p);
		p.waitFor();
		if (p.exitValue() != 0) {
			throw new ProcessErrorException(pr.getError());
		} else {
			JsonObject jo = Helper.jsonParser.parse(pr.getOutput()).getAsJsonObject();
			target.setTitle(jo.get("title").getAsString());
			Set<Map.Entry<String, JsonElement>> formats = jo.getAsJsonObject("streams").entrySet();
			for (Map.Entry<String, JsonElement> format : formats) {
				target.addFormat(format.getKey());
			}
		}
	}

	/**
	 * It will run the YouGet program to download the target URL.
	 * 
	 * It needs a user specified charset to read the output of the YouGet
	 * program correctly.
	 * 
	 * @throws ProcessErrorException
	 *             if YouGet failed in downloading the target
	 * @throws IOException
	 *             if failed to access or run the program, or if the specified
	 *             charset for ProcessReader is invalid
	 * @throws InterruptedException
	 */
	private void download() throws ProcessErrorException, IOException, InterruptedException {
		List<String> command = new ArrayList<String>();
		command.add(executable);
		command.add("-o");
		command.add("\"" + path + "\"");
		if (preferredFormat != null && !preferredFormat.equals("")) {
			command.add("-F");
			command.add(preferredFormat);
		}
		if (forceWrite) {
			command.add("-f");
		}
		command.add("\"" + url + "\"");
		Process p = new ProcessBuilder(command).start();
		ProcessReader pr = readProcess(p);
		p.waitFor();
		if (p.exitValue() != 0) {
			throw new ProcessErrorException(pr.getError());
		}
	}

	private ProcessReader readProcess(Process p) throws UnsupportedEncodingException {
		ProcessReader pr;
		if (charset == null) {
			pr = new ProcessReader(p);
		} else {
			pr = new ProcessReader(p, charset);
		}
		return pr;
	}

}
