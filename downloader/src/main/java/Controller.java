import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;

import com.google.gson.JsonElement;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * This is the controller for downloading videos from multiple URLs given by the
 * user and putting them into separate folders named after corresponding video
 * titles or into a single folder based on user instruction.
 * 
 * By default, it uses You-Get as the downloading tool.
 * 
 * @author Zhen Chen
 * 
 */

public class Controller {
	// at least 1 thread
	private static final int MAX_NUMBER_OF_THREADS = 1;
	// location of the downloading engine
	private static final String LOCATION = "D:/Program Files/you-get";
	// Windows platform uses GBK as charset in Chinese version
	private static final String CHARSET = "GBK";
	// path to load and save target list
	private static final String TARGET_LIST_PATH = "target.json";
	private static final String INVALID_DIRECTORY_CHARACTER_PATTERN = "[/\\:*?\"<>|]";
	private static Set<YouGet> threadPool = new HashSet<YouGet>();
	private static Set<Target> targetSet = new LinkedHashSet<Target>();
	// store failed targets temporarily after running a set of targets each time
	private static Set<Target> failedTargetSet = new HashSet<Target>();
	public static Object printLock = new Object();

	protected static enum Choice {
		ADD, DELETE, TITLE, DOWNLOAD, LOAD, SAVE, EXIT, YES, NO, CANCEL, OVERWRITE, APPEND, MULTIPLE, SINGLE;
	}

	/**
	 * Get all target URLs from user.
	 * 
	 * An empty line indicates the end of input.
	 * 
	 * @throws IOException
	 */
	protected static void
	addTarget() throws IOException {
		int count = 0;
		String line;
		System.out.println("Please enter all target URLs, one line for each:");
		while (!(line = Helper.input.readLine()).equals("")) {
			try {
				if (targetSet.add(new Target(line))) {
					count++;
				}
			} catch (MalformedURLException e) {
				System.err.println("Invalid URL.");
				e.printStackTrace();
			}
		}
		System.out.printf("%d URLs added, %d URLs in target list now.%n", count, targetSet.size());
	}

	/**
	 * Delete all target URLs specified by user.
	 * 
	 * An empty line indicates the end of input.
	 * 
	 * @throws IOException
	 */
	protected static void deleteTarget() throws IOException {
		if (targetSet.isEmpty()) {
			return;
		}
		Set<String> options = new HashSet<String>();
		options.add("");
		options.add("all");
		for (int i = 1; i <= targetSet.size(); i++) {
			// for user, index starts from 1
			options.add(Integer.toString(i));
		}
		int count = 0;
		String line;
		System.out.println(
				"Please enter ids of all target URLs to delete, one line for each, enter \"all\" to delete all targets:");
		Set<Integer> toRemove = new HashSet<Integer>();
		boolean removeAll = false;
		while (!(line = Helper.getUserChoice(options)).equals("")) {
			if (line.equals("all")) {
				removeAll = true;
				count = targetSet.size();
				break;
			}
			// for program, index starts from 0
			if (toRemove.add(Integer.parseInt(line) - 1)) {
				count++;
			}
		}
		if (removeAll) {
			targetSet.clear();
		} else {
			removeTarget(toRemove);
		}
		System.out.printf("%d URLs deleted, %d URLs in target list now.%n", count, targetSet.size());
	}

	/**
	 * It fires each process in the provided list in a controlled manner. Only
	 * MAX_NUMBER_OF_THREADS number of threads are allowed to be running at the
	 * same time.
	 * 
	 * @param processes
	 *            a list of prepared processes to be fired and managed by a
	 *            threading pool
	 * @param mute
	 *            if true, no info messages will be displayed to the user
	 */
	protected static void startTaskAll(List<YouGet> processes, boolean mute) {
		int total = processes.size();
		int i = 0;
		for (YouGet yg : processes) {
			if (threadPool.size() == MAX_NUMBER_OF_THREADS) {
				clearThreadPool();
			}
			threadPool.add(yg);
			yg.start();
			if (!mute) {
				System.out.printf("%d of %d...%n", ++i, total);
			}
		}
		clearThreadPool();
	}

	/**
	 * It waits for each thread in the threadPool to finish and then makes the
	 * threadPool clear.
	 * 
	 * It adds all failed targets to failedTargetSet.
	 */
	protected static void clearThreadPool() {
		for (YouGet yg : threadPool) {
			try {
				yg.join();
				if (!yg.isSuccess()) {
					failedTargetSet.add(yg.getTarget());
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		threadPool.clear();
	}

	/**
	 * It shows the URL of each failed target and asks user whether to delete
	 * these failed targets from the target list.
	 */
	protected static void reportFailure() {
		synchronized (printLock) {
			for (Target target : failedTargetSet) {
				System.out.printf("%s has failed.%n", target.getUrl().toString());
			}
			String message = "";
			message += "Do you want to delete these failed URLs from the target list? (y/n)%n";
			Map<String, Choice> options = new HashMap<String, Choice>();
			options.put("y", Choice.YES);
			options.put("n", Choice.NO);
			try {
				if (Helper.getUserChoice(message, options) == Choice.YES) {
					removeFailed();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * It removes all failed targets in the failedTargetSet from targetSet.
	 */
	private static void removeFailed() {
		for (Target target : failedTargetSet) {
			targetSet.remove(target);
		}
		failedTargetSet.clear();
	}

	/**
	 * It removes all succeeded targets from targetSet.
	 */
	private static void removeSucceeded() {
		Target target;
		Iterator<Target> it = targetSet.iterator();
		while (it.hasNext()) {
			target = it.next();
			if (!failedTargetSet.contains(target)) {
				it.remove();
			}
		}
	}

	/**
	 * It creates an empty set and put all targets that are not asked to remove
	 * into this new set. At the end, it replaces the old targetSet with the new
	 * one.
	 * 
	 * Note: remove() method will not work if the hashCode() value of an object
	 * has been changed after it was added to the set.
	 * 
	 * @param toRemove
	 *            indexes of all targets to be removed
	 */
	protected static void removeTarget(Set<Integer> toRemove) {
		Target target;
		Set<Target> temp = new HashSet<Target>();
		Iterator<Target> it = targetSet.iterator();
		for (int i = 0; it.hasNext(); i++) {
			target = it.next();
			if (toRemove.contains(i)) {
				System.out.printf("%s has been removed.%n", target.getUrl().toString());
			} else {
				temp.add(target);
			}
		}
		targetSet = temp;
	}

	protected static Choice displayMenu() throws IOException {
		String message = "";
		message += "Menu:%n";
		message += "1. Add target URLs%n";
		message += "2. Delete target URLs%n";
		message += "3. Show titles of targets%n";
		message += "4. Download targets%n";
		message += "5. Load target list%n";
		message += "6. Save target list%n";
		message += "0. Exit%n";

		Map<String, Choice> options = new HashMap<String, Choice>();
		options.put("1", Choice.ADD);
		options.put("2", Choice.DELETE);
		options.put("3", Choice.TITLE);
		options.put("4", Choice.DOWNLOAD);
		options.put("5", Choice.LOAD);
		options.put("6", Choice.SAVE);
		options.put("0", Choice.EXIT);
		options.put("e", Choice.EXIT);
		options.put("q", Choice.EXIT);

		return Helper.getUserChoice(message, options);
	}

	protected static void displayTarget() {
		int id = 0;
		System.out.println("Targets:");
		for (Target target : targetSet) {
			System.out.printf("%d. %s%n", ++id, target.getUrl().toString());
		}
	}

	protected static void getInfo(boolean mute) {
		List<YouGet> processes = new ArrayList<YouGet>();
		for (Target target : targetSet) {
			processes.add(new YouGet(target, YouGet.Task.INFO));
		}
		startTaskAll(processes, mute);
	}

	protected static void displayTitle() throws IOException {
		getInfo(false);
		if (!failedTargetSet.isEmpty()) {
			reportFailure();
		}
		failedTargetSet.clear();
		// some failed targets may have been removed from targetSet
		if (targetSet.isEmpty()) {
			System.out.println("Target list is empty.");
			return;
		}
		int id = 0;
		System.out.println("Titles:");
		for (Target target : targetSet) {
			System.out.printf("%d. %s    %s%n", ++id, target.getTitle(), target.getUrl().toString());
		}
		String message = "";
		message += "Do you want to delete URLs from the target list? (y/n)%n";
		Map<String, Choice> options = new HashMap<String, Choice>();
		options.put("y", Choice.YES);
		options.put("n", Choice.NO);
		if (Helper.getUserChoice(message, options) == Choice.YES) {
			deleteTarget();
		}
	}

	protected static void download() throws IOException {
		String root = "";
		String folder = "";
		String path = "";
		String preferredFormat = "";
		boolean separateFolder;
		boolean forceWrite;

		System.out.println("Please enter the savepath:");
		root = Helper.input.readLine();
		if (root.length() == 0 || root.charAt(root.length() - 1) != '/') {
			root += "/";
		}

		String message = "";
		message += "Do you want to put each target into separate folders or into one single folder?%n";
		message += "1. Separate folders for each target%n";
		message += "2. All targets into one single folder%n";
		Map<String, Choice> options = new HashMap<String, Choice>();
		options.put("1", Choice.MULTIPLE);
		options.put("2", Choice.SINGLE);
		Choice choice = Helper.getUserChoice(message, options);
		if (choice == Choice.MULTIPLE) {
			separateFolder = true;
		} else {
			separateFolder = false;
			System.out.println("Please enter the folder name:");
			folder = Helper.input.readLine();
		}

		System.out.println("Please enter the preferred quality, hit enter to use the highest quality by default:");
		preferredFormat = Helper.input.readLine();

		message = "";
		message += "Do you want to force overwrite any existing file? (y/n)%n";
		options = new HashMap<String, Choice>();
		options.put("y", Choice.YES);
		options.put("n", Choice.NO);
		choice = Helper.getUserChoice(message, options);
		if (choice == Choice.YES) {
			forceWrite = true;
		} else {
			forceWrite = false;
		}

		// confirm
		message = "";
		message += "Please check the above settings, do you want to start downloading? (y/n)%n";
		options = new HashMap<String, Choice>();
		options.put("y", Choice.YES);
		options.put("n", Choice.NO);
		choice = Helper.getUserChoice(message, options);
		if (choice == Choice.NO) {
			return;
		}

		// get titles of targets for folder names and get allowed formats of
		// each target
		if (separateFolder || !preferredFormat.equals("")) {
			System.out.println("Preparing...");
			getInfo(true);
		}

		List<YouGet> processes = new ArrayList<YouGet>();
		for (Target target : targetSet) {
			if (failedTargetSet.contains(target)) {
				continue;
			}
			// get rid of invalid characters in the folder name
			if (separateFolder) {
				path = root + target.getTitle().replaceAll(INVALID_DIRECTORY_CHARACTER_PATTERN, "");
			} else {
				path = root + folder.replaceAll(INVALID_DIRECTORY_CHARACTER_PATTERN, "");
			}
			// check if preferredFormat is valid
			String format = null;
			if (target.getFormats().contains(preferredFormat)) {
				format = preferredFormat;
			}
			processes.add(new YouGet(target, YouGet.Task.DOWNLOAD, path, format, forceWrite));
		}
		startTaskAll(processes, false);

		if (!failedTargetSet.isEmpty()) {
			reportFailure();
		}
		// some failed targets may have been removed from targetSet
		if (!targetSet.isEmpty() && failedTargetSet.size() != targetSet.size()) {
			System.out.println("Downloading finished.");
			message = "";
			message += "Do you want to delete all successfully downloaded URLs from the target list? (y/n)%n";
			options = new HashMap<String, Choice>();
			options.put("y", Choice.YES);
			options.put("n", Choice.NO);
			if (Helper.getUserChoice(message, options) == Choice.YES) {
				removeSucceeded();
			}
		}
		failedTargetSet.clear();
	}

	protected static void load() throws IOException {
		String json = Helper.load(TARGET_LIST_PATH);
		if (json == null) {
			System.out.println("No target list file found.");
			return;
		}

		Choice choice;
		if (targetSet.isEmpty()) {
			choice = Choice.APPEND;
		} else {
			String message = "";
			message += "Discard all current targets or Append to current target list?%n";
			message += "1. Discard all current targets%n";
			message += "2. Append to current target list%n";
			message += "3. Cancel%n";
			Map<String, Choice> options = new HashMap<String, Choice>();
			options.put("1", Choice.OVERWRITE);
			options.put("2", Choice.APPEND);
			options.put("3", Choice.CANCEL);
			choice = Helper.getUserChoice(message, options);
		}

		Target target;
		String url;
		String title = null;
		switch (choice) {
		case OVERWRITE:
			targetSet.clear();
		case APPEND:
			JsonArray array = Helper.jsonParser.parse(json).getAsJsonArray();
			for (JsonElement je : array) {
				JsonObject jo = je.getAsJsonObject();
				url = jo.get("url").getAsString();
				if (jo.get("title") != null) {
					title = jo.get("title").getAsString();
				}
				target = new Target(url, title);
				JsonArray ja = jo.getAsJsonArray("formats");
				if (ja != null) {
					for (JsonElement format : ja) {
						target.addFormat(format.getAsString());
					}
				}
				targetSet.add(target);
			}
			break;
		default:
			return;
		}

		System.out.printf("Target list loaded, %d URLs in target list now.%n", targetSet.size());
	}

	protected static void save() throws IOException {
		String message = "";
		message += "You are going to overwrite the existing target list, continue? (y/n)%n";
		Map<String, Choice> options = new HashMap<String, Choice>();
		options.put("y", Choice.YES);
		options.put("n", Choice.NO);

		if (Helper.getUserChoice(message, options) == Choice.YES) {
			Helper.save(TARGET_LIST_PATH, Helper.gson.toJson(targetSet));
			System.out.println("Target list saved.");
		}
	}

	protected static void displayExit() {
		System.out.println("Exit. Thank you!");
	}

	public static void main(String[] args) {
		try {
			YouGet.setExecutable(LOCATION);
			YouGet.setCharset(CHARSET);

			boolean again = true;
			do {
				switch (displayMenu()) {
				case ADD:
					addTarget();
					break;
				case DELETE:
					if (!targetSet.isEmpty()) {
						displayTarget();
						deleteTarget();
					} else {
						System.out.println("Target list is empty.");
					}
					break;
				case TITLE:
					if (!targetSet.isEmpty()) {
						displayTitle();
					} else {
						System.out.println("Target list is empty.");
					}
					break;
				case DOWNLOAD:
					if (!targetSet.isEmpty()) {
						download();
					} else {
						System.out.println("Target list is empty.");
					}
					break;
				case LOAD:
					load();
					break;
				case SAVE:
					if (!targetSet.isEmpty()) {
						save();
					} else {
						System.out.println("Target list is empty.");
					}
					break;
				case EXIT:
					again = false;
					displayExit();
					break;
				default:
					break;
				}
			} while (again);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
