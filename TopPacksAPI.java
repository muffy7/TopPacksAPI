import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.simple.*;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.io.*;
import java.util.Timer;
import java.util.*;

public class TopPacksAPI {

	static int isImport = 0; /*
								 * This variable will check whether we are calling the import function or the
								 * toppacks function For Import function isImport = 0 and for the toppacks
								 * isImport = 1
								 */
	static OkHttpClient client = new OkHttpClient();
	static Map<String, Integer> packages = new HashMap<String, Integer>();

	/*
	 * This method Run if for OkHttp. We take an input as a http url and then store
	 * the returned message in our response variable Then return the body part of
	 * the response in String format
	 */
	public static String run(String url) throws IOException {
		Request request = new Request.Builder().url(url).build();

		try (Response response = client.newCall(request).execute()) {
			return response.body().string();
		} catch (java.net.UnknownHostException e) {
			System.out.println("Please Check your Internet Connection");
			return "";
		}

	}

	// Now from here I have defined all the three methods in same file. First method
	// is search
	public static void search(String keyWord) throws IOException {
		JSONParser parser = new JSONParser();
		BufferedWriter Repository_Id = new BufferedWriter(new FileWriter("D:/Repositories_Id.txt"));
		/*
		 * I am limiting my search for 5 pages ..... We can change it from here if we
		 * need to.
		 */
		for (int l = 0; l < 5; l++) {
			// From below part we can increase or decrease the result shown per_page value
			String response = run(
					"https://api.github.com/search/repositories?q=%22" + keyWord + "&page=" + l + "per_page=30");
			if (response.equals("")) {
				System.exit(0);
			}
			Object obj = null;
			try {
				obj = parser.parse(new String(response));
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			JSONObject jsonObject = (JSONObject) obj;
			JSONArray items = (JSONArray) jsonObject.get("items");

			for (int i = 0; i < items.size(); i++) {

				JSONObject ob = (JSONObject) items.get(i);
				String repo_Id = ob.get("id").toString();
				System.out.println("ID:" + repo_Id);
				Repository_Id.write(repo_Id);
				Repository_Id.newLine();/*
										 * We are writing the Repository_Id in a file and then try to use it for
										 * "toppacks" method defined below
										 */
				System.out.println("Name:" + ob.get("name"));
				String user_name = (String) ob.get("full_name");
				int j;
				// From here I am trying to extract the user_name from the fullName present in
				// the response variable
				for (j = 0; j < user_name.length(); j++) {
					if (user_name.charAt(j) == '/')
						break;
				}
				user_name = user_name.substring(0, j);
				System.out.println("Owner:" + user_name);
				System.out.println("Fork:" + ob.get("forks"));
				System.out.println("Starcount:" + ob.get("stargazers_count"));
				System.out.println();
			}

		}
		try {
			Repository_Id.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/*
	 * This method Import which takes repository Id as an input and then uses it to
	 * print the packages if package.json file is present
	 */
	public static void Import(String id) throws IOException {
		String response = null;
		try {
			response = run("https://api.github.com/repositories/" + id);
			if (response.equals("")) {
				System.exit(0);
			}
			if (response.contains("API rate limit exceeded")) {
				System.out.println("Network- Error: API rate limit exceeded");
				System.exit(0);
			}
		} catch (IOException e3) {
			e3.printStackTrace();
			System.exit(0);
		}
		JSONParser parser = new JSONParser();
		Object responseObject = null;
		try {
			responseObject = parser.parse(response);
		} catch (ParseException e3) {
			e3.printStackTrace();
		}
		JSONObject responseJSON = (JSONObject) responseObject;
		int j;
		String name = (String) (responseJSON).get("name");
		System.out.println("Name: " + name);

		String userName = (String) (responseJSON).get("full_name");
		for (j = 0; j < userName.length(); j++) {
			if (userName.charAt(j) == '/')
				break;
		}
		userName = userName.substring(0, j);
		System.out.println("Owner: " + userName);
		String RepositoryContentResponse = null;
		try {
			/*
			 * This will print the files and directories present inside the root directory
			 */
			RepositoryContentResponse = run("https://api.github.com/repos/" + userName + "/" + name + "/" + "contents");
			if (RepositoryContentResponse.equals("")) {
				System.exit(0);
			}
			if (RepositoryContentResponse.contains("API rate limit exceeded")) {
				System.out.println("Network- Error: API rate limit exceeded");
				System.exit(0);
			}
		} catch (IOException e2) {
			e2.printStackTrace();
			System.exit(0);
		}
		RepositoryPackageFinder(RepositoryContentResponse);
	}

	/*
	 * This method will search for the package.json file in the repository and if
	 * present it will further call another method depAndDevDepPackagesRetriever()
	 * which will retrieve the packages name present inside the package.json file
	 */
	public static void RepositoryPackageFinder(String RepositoryContentResponse) throws IOException {
		BufferedWriter packageWriter = new BufferedWriter(new FileWriter("D:/packages.txt"));
		Object RepositoryContentResponse_Object = null;
		JSONParser parser = new JSONParser();
		try {
			RepositoryContentResponse_Object = parser.parse(RepositoryContentResponse);
		} catch (ParseException e1) {
			e1.printStackTrace();
		}
		JSONArray RepositoryContentResponse_Array = (JSONArray) RepositoryContentResponse_Object;

		for (int i = 0; i < RepositoryContentResponse_Array.size(); i++) {

			JSONObject ContentArray_JSONObj = (JSONObject) RepositoryContentResponse_Array.get(i);
			if (((String) ContentArray_JSONObj.get("name")).equals("package.json")) {
				String downloadUrl = null;
				try {
					downloadUrl = run((String) ContentArray_JSONObj.get("download_url"));// This will help us see the
																							// contents of
					// package.json
					if (downloadUrl.equals("")) {
						System.exit(0);
					}
					if (downloadUrl.contains("API rate limit exceeded")) {
						System.out.println("Network- Error: API rate limit exceeded");
						System.exit(0);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				JSONParser JSONparser = new JSONParser();
				JSONObject parserObject = new JSONObject();
				try {
					parserObject = (JSONObject) JSONparser.parse(downloadUrl);
				} catch (ParseException e) {
					e.printStackTrace();
				}
				JSONObject dep = (JSONObject) parserObject.get("dependencies");
				JSONObject devDep = (JSONObject) parserObject.get("devDependencies");
				depAndDevDepPackagesRetriever(dep, devDep, packageWriter);
				break;
			}

		}
		packageWriter.close();
	}

	/*
	 * This method will retrieve the names of the packages present in the
	 * package.json file in the root folder of the Repository
	 */
	public static void depAndDevDepPackagesRetriever(JSONObject dep, JSONObject devDep, BufferedWriter packageWriter)
			throws IOException {
		try {
			for (Object key : dep.keySet()) {

				String keyStr = (String) key;
				if (isImport == 0)
					System.out.print(keyStr + ", ");
				packageWriter.write(keyStr);
				packageWriter.newLine();
				if (packages.containsKey(keyStr)) {
					packages.replace(keyStr, packages.get(keyStr) + 1);
				} else {
					packages.put(keyStr, 1);
				}

			}
		} catch (NullPointerException e) {

		}
		try {
			for (Object key : devDep.keySet()) {

				String keyStr = (String) key;
				if (isImport == 0)
					System.out.print(keyStr + ", ");
				packageWriter.write(keyStr);
				packageWriter.newLine();
				if (packages.containsKey(keyStr)) {
					packages.replace(keyStr, packages.get(keyStr) + 1);
				} else {
					packages.put(keyStr, 1);

				}
			}
		} catch (NullPointerException e) {

		}
	}

	/*
	 * This method toppacks will help me print the Top ten packs used in the
	 * package.json file of the repositories whose repositories_Id's are stored in
	 * the Repositories_Id.txt file. To search a keyword and see the results for the
	 * top ten packages we need to run a search method in main method and then use
	 * toppacks to perform it's function. We can also integrate it in one method but
	 * for simplicity and extendibilty I prefer to use it as different files
	 */
	public static void toppacks() throws IOException {
		// String TodayQuote = "Wear your failure as a badge of honour";

		isImport = 1;

		BufferedReader repositoryIdReader = null;
		int read_counter_repository = 0;
		try {
			repositoryIdReader = new BufferedReader(new FileReader("D:/Repositories_Id.txt"));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		RandomAccessFile packagesReader = null;
		try {
			packagesReader = new RandomAccessFile("D:/packages.txt", "rw");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		// br1 = new BufferedReader(new FileReader("D:/packages.txt"));

		String repoId = "";
		try {
			repoId = repositoryIdReader.readLine();
			read_counter_repository++;
		} catch (IOException e) {
			e.printStackTrace();
		}

		while (repoId != null) {

			System.out.println("Repositories_Id  :-" + repoId);
			try {
				packagesReader.seek(0);// For each repository_Id package.txt is updated so we need to go to the
				// starting of the file and for this I will need this
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				Import(repoId);
			} catch (IOException e) {
				e.printStackTrace();
			}

			if (read_counter_repository < 5) { /*
												 * This will limit to search for the packages in the first 5
												 * Repository_Id's present in the Repositories.txt. We can change it
												 * here if we want but we need to mind the API rate limit
												 */
				try {
					repoId = repositoryIdReader.readLine();
					read_counter_repository++;

				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {
				break;
			}

		}
		try {
			repositoryIdReader.close();
			packagesReader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("\n \n HURRAH WE GOT THIS THING AND NOW ---- \n \n");
		// This will print the top 10 used packages :)
		packages.entrySet().stream().sorted(Collections.reverseOrder(Map.Entry.comparingByValue())).limit(10)
				.forEach(System.out::println);
		isImport = 0;
	}

	public static void main(String args[]) throws IOException {
		Scanner sc = new Scanner(System.in);
		String keyword;
		int option;

		do {
			System.out.println("Menu");
			System.out.println("1.Search");
			System.out.println("2.Import");
			System.out.println("3.TopPacks");
			System.out.println("0.Exit");
			option = sc.nextInt();
			if (option == 1) {
				System.out.println("Enter keyword :");
				keyword = sc.next();
				search(keyword);
			} else if (option == 2) {
				System.out.println("Enter ID :");
				keyword = sc.next();
				Import(keyword);
			} else if (option == 3) {
				toppacks();
			}

		} while (option != 0);

		sc.close();

	}
}