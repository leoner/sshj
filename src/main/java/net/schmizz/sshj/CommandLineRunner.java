package net.schmizz.sshj;

import java.io.File;
import java.io.IOException;
import java.security.PublicKey;
import java.util.LinkedList;
import java.util.List;

import net.schmizz.sshj.transport.verification.HostKeyVerifier;

import com.sampullara.cli.Args;
import com.sampullara.cli.Argument;

public class CommandLineRunner {

	@Argument(alias = "l", description = "Local file path.", required = true)
	private static String localPath;

	@Argument(alias = "r", description = "Remote file path.", required = true)
	private static String remotePath;

	@Argument(alias = "u", description = "Username", required = true)
	private static String username;

	@Argument(alias = "p", description = "Password", required = true)
	private static String password;

	@Argument(alias = "h", description = "Remote host", required = true)
	private static String host;

	/**
	 * @param args
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException, ClassNotFoundException {
		@SuppressWarnings("unused")
		final List<String> parse;
		try {
			parse = Args.parse(CommandLineRunner.class, args);
		} catch (IllegalArgumentException e) {
			Args.usage(CommandLineRunner.class);
			System.exit(1);
			return;
		}
		ServerInfo info = new ServerInfo();
		info.setHost(host);
		info.setUsername(username);
		info.setPassword(password);
		info.setLocalPath(localPath);
		info.setRemotePath(remotePath);

		upload(info);
	}

	public static void upload(ServerInfo info) throws IOException, ClassNotFoundException {
		SSHClient ssh = new SSHClient();
		ssh.loadKnownHosts();
		ssh.connect(info.host);
		ssh.addHostKeyVerifier(new NullHostKeyVerifier());
		ssh.authPassword(info.username, info.password.toCharArray());
		try {
			// ssh.authPublickey("admin");

			// Present here to demo algorithm renegotiation - could have just
			// put this before connect()
			// Make sure JZlib is in classpath for this to work
			ssh.useCompression();
			List<String> fileSet = getFileSet(info.localPath);
			for (String filepath : fileSet) {
				ssh.newSCPFileTransfer().upload(filepath, info.remotePath);
			}

		} finally {
			System.out.println("success upload " + info.localPath + " to " + info.username + "@" + info.host + ":"
					+ info.remotePath);
			ssh.disconnect();
		}
	}

	public static List<String> getFileSet(String localPath) {
		List<String> fileSet = new LinkedList<String>();
		if (new File(localPath).isDirectory()) {
			File file = new File(localPath);
			File[] files = file.listFiles();
			for (File f : files) {
				fileSet.add(f.getAbsolutePath());
			}
		}
		return fileSet;
	}

	static class ServerInfo {
		private String username;
		private String password;
		private String host;
		private String localPath;

		public ServerInfo() {

		}

		public String getUsername() {
			return username;
		}

		public void setUsername(String username) {
			this.username = username;
		}

		public String getPassword() {
			return password;
		}

		public void setPassword(String password) {
			this.password = password;
		}

		public String getHost() {
			return host;
		}

		public void setHost(String host) {
			this.host = host;
		}

		public String getLocalPath() {
			return localPath;
		}

		public void setLocalPath(String localPath) {
			this.localPath = localPath;
		}

		public String getRemotePath() {
			return remotePath;
		}

		public void setRemotePath(String remotePath) {
			this.remotePath = remotePath;
		}

		private String remotePath;

	}

	static class NullHostKeyVerifier implements HostKeyVerifier {
		@Override
		public boolean verify(String arg0, int arg1, PublicKey arg2) {
			return true;
		}

	}

}