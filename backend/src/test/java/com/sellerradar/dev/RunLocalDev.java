package com.sellerradar.dev;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * IntelliJ current-file launcher for local development.
 *
 * Open this file and run it to start Docker PostgreSQL, Spring Boot, and Vite together.
 */
public final class RunLocalDev {
	private static final String FRONTEND_URL = "http://127.0.0.1:5173/";
	private static final String BACKEND_HEALTH_URL = "http://127.0.0.1:8080/actuator/health";
	private static final String JWT_SECRET = "local-dev-jwt-secret-for-seller-radar-32-bytes";
	private static final List<Process> CHILDREN = new CopyOnWriteArrayList<>();

	private RunLocalDev() {
	}

	public static void main(String[] args) throws Exception {
		Path root = findProjectRoot();
		Path backend = root.resolve("backend");
		Path frontend = root.resolve("frontend");

		Runtime.getRuntime().addShutdownHook(new Thread(RunLocalDev::stopChildren));

		log("project root: " + root);
		log("starting Docker PostgreSQL");
		runChecked("docker-db", root, List.of("docker", "compose", "up", "-d", "db"), Map.of());
		waitForDockerDb(root);

		List<Process> managedProcesses = new ArrayList<>();
		if (isHttpOk(BACKEND_HEALTH_URL)) {
			log("backend is already running: " + BACKEND_HEALTH_URL);
		} else {
			log("starting backend");
			Process backendProcess = start(
					"backend",
					backend,
					gradleBootRunCommand(backend),
					backendEnvironment()
			);
			managedProcesses.add(backendProcess);
			waitForHttp("backend", BACKEND_HEALTH_URL, Duration.ofSeconds(120));
		}

		ensureFrontendDependencies(frontend);
		if (isHttpOk(FRONTEND_URL)) {
			log("frontend is already running: " + FRONTEND_URL);
		} else {
			log("starting frontend");
			Process frontendProcess = start(
					"frontend",
					frontend,
					frontendDevCommand(),
					Map.of("VITE_API_BASE_URL", "http://localhost:8080/api/v1")
			);
			managedProcesses.add(frontendProcess);
			waitForHttp("frontend", FRONTEND_URL, Duration.ofSeconds(90));
		}

		log("ready");
		log("frontend: " + FRONTEND_URL);
		log("backend health: " + BACKEND_HEALTH_URL);
		log("test account: seller@example.com / password1234");
		log("Press Ctrl+C or stop this run configuration to stop managed backend and frontend processes.");
		log("Docker DB stays running. Stop it with: docker compose down");

		if (!managedProcesses.isEmpty()) {
			waitUntilChildExits(managedProcesses);
		} else {
			log("No managed backend or frontend process was started. Stop this run configuration when you are done.");
			waitUntilStopped();
		}
	}

	private static Path findProjectRoot() {
		Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
		for (Path path = current; path != null; path = path.getParent()) {
			if (Files.exists(path.resolve("docker-compose.yml"))
					&& Files.isDirectory(path.resolve("backend"))
					&& Files.isDirectory(path.resolve("frontend"))) {
				return path;
			}
		}
		throw new IllegalStateException("Project root not found from " + current);
	}

	private static List<String> gradleBootRunCommand(Path backend) {
		if (isWindows()) {
			return List.of("cmd.exe", "/c", backend.resolve("gradlew.bat").toString(), "bootRun");
		}
		return List.of(backend.resolve("gradlew").toString(), "bootRun");
	}

	private static Map<String, String> backendEnvironment() {
		Map<String, String> env = new HashMap<>();
		env.put("DB_URL", "jdbc:postgresql://127.0.0.1:5432/seller_radar");
		env.put("DB_USERNAME", "seller");
		env.put("DB_PASSWORD", "seller");
		env.put("JWT_SECRET", JWT_SECRET);
		env.put("NAVER_CLIENT_ID", "");
		env.put("NAVER_CLIENT_SECRET", "");
		env.put("NAVER_DATALAB_DAILY_QUOTA", "1000");
		return env;
	}

	private static void ensureFrontendDependencies(Path frontend) throws Exception {
		if (Files.isDirectory(frontend.resolve("node_modules"))) {
			return;
		}
		log("frontend node_modules not found. running npm install");
		runChecked("frontend-install", frontend, npmCommand("install"), Map.of());
	}

	private static List<String> frontendDevCommand() {
		List<String> command = npmCommand("run", "dev", "--", "--host", "127.0.0.1");
		if (isWindows()) {
			Optional<Path> bundledNode = bundledNode();
			Optional<Path> npmCli = windowsNpmCli();
			if (bundledNode.isPresent() && npmCli.isPresent()) {
				return List.of(
						bundledNode.get().toString(),
						npmCli.get().toString(),
						"run",
						"dev",
						"--",
						"--host",
						"127.0.0.1"
				);
			}
		}
		return command;
	}

	private static List<String> npmCommand(String... args) {
		List<String> command = new ArrayList<>();
		if (isWindows()) {
			command.add("cmd.exe");
			command.add("/c");
			command.add("npm.cmd");
		} else {
			command.add("npm");
		}
		command.addAll(List.of(args));
		return command;
	}

	private static Optional<Path> bundledNode() {
		Path path = Path.of(
				System.getProperty("user.home"),
				".cache",
				"codex-runtimes",
				"codex-primary-runtime",
				"dependencies",
				"node",
				"bin",
				"node.exe"
		);
		return Files.isRegularFile(path) ? Optional.of(path) : Optional.empty();
	}

	private static Optional<Path> windowsNpmCli() {
		Path path = Path.of("C:", "Program Files", "nodejs", "node_modules", "npm", "bin", "npm-cli.js");
		return Files.isRegularFile(path) ? Optional.of(path) : Optional.empty();
	}

	private static void waitForDockerDb(Path root) throws Exception {
		Duration timeout = Duration.ofSeconds(90);
		long deadline = System.nanoTime() + timeout.toNanos();
		while (System.nanoTime() < deadline) {
			CommandResult result = runCapture(
					root,
					List.of("docker", "inspect", "-f", "{{.State.Health.Status}}", "seller-radar-db")
			);
			if (result.exitCode() == 0 && result.output().trim().equals("healthy")) {
				log("Docker PostgreSQL is healthy");
				return;
			}
			Thread.sleep(2_000);
		}
		runChecked("docker-db-status", root, List.of("docker", "compose", "ps"), Map.of());
		throw new IllegalStateException("Docker PostgreSQL did not become healthy in " + timeout.toSeconds() + "s");
	}

	private static void waitForHttp(String name, String url, Duration timeout) throws Exception {
		long deadline = System.nanoTime() + timeout.toNanos();
		while (System.nanoTime() < deadline) {
			if (isHttpOk(url)) {
				log(name + " is ready: " + url);
				return;
			}
			Thread.sleep(2_000);
		}
		throw new IllegalStateException(name + " did not become ready in " + timeout.toSeconds() + "s: " + url);
	}

	private static boolean isHttpOk(String url) {
		try {
			HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
			connection.setConnectTimeout(1_500);
			connection.setReadTimeout(1_500);
			connection.setRequestMethod("GET");
			int code = connection.getResponseCode();
			return code >= 200 && code < 500;
		} catch (IOException exception) {
			return false;
		}
	}

	private static Process start(String name, Path workingDirectory, List<String> command, Map<String, String> env) throws IOException {
		ProcessBuilder builder = new ProcessBuilder(command);
		builder.directory(workingDirectory.toFile());
		builder.redirectErrorStream(true);
		builder.environment().putAll(env);
		Process process = builder.start();
		CHILDREN.add(process);
		stream(name, process.getInputStream());
		return process;
	}

	private static void runChecked(String name, Path workingDirectory, List<String> command, Map<String, String> env) throws Exception {
		Process process = start(name, workingDirectory, command, env);
		int exitCode = process.waitFor();
		CHILDREN.remove(process);
		if (exitCode != 0) {
			throw new IllegalStateException(name + " failed with exit code " + exitCode);
		}
	}

	private static CommandResult runCapture(Path workingDirectory, List<String> command) throws Exception {
		ProcessBuilder builder = new ProcessBuilder(command);
		builder.directory(workingDirectory.toFile());
		builder.redirectErrorStream(true);
		Process process = builder.start();
		String output;
		try (InputStream input = process.getInputStream()) {
			output = new String(input.readAllBytes(), StandardCharsets.UTF_8);
		}
		return new CommandResult(process.waitFor(), output);
	}

	private static void stream(String name, InputStream input) {
		Thread thread = new Thread(() -> {
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
				String line;
				while ((line = reader.readLine()) != null) {
					System.out.println("[" + name + "] " + line);
				}
			} catch (IOException exception) {
				System.out.println("[" + name + "] output closed");
			}
		}, "stream-" + name);
		thread.setDaemon(true);
		thread.start();
	}

	private static void waitUntilChildExits(List<Process> processes) throws InterruptedException {
		while (true) {
			for (Process process : processes) {
				if (!process.isAlive()) {
					throw new IllegalStateException("Child process exited with code " + process.exitValue());
				}
			}
			Thread.sleep(1_000);
		}
	}

	private static void waitUntilStopped() throws InterruptedException {
		while (true) {
			Thread.sleep(1_000);
		}
	}

	private static void stopChildren() {
		for (Process process : CHILDREN) {
			if (process.isAlive()) {
				process.destroy();
			}
		}
		for (Process process : CHILDREN) {
			try {
				process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
			} catch (InterruptedException exception) {
				Thread.currentThread().interrupt();
			}
			if (process.isAlive()) {
				process.destroyForcibly();
			}
		}
	}

	private static boolean isWindows() {
		return System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win");
	}

	private static void log(String message) {
		System.out.println("[local-dev] " + message);
	}

	private record CommandResult(int exitCode, String output) {
	}
}
