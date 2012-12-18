package net.schmizz.sshj;

import java.io.File;
import java.io.IOException;
import java.security.PublicKey;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.common.KeyType;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Command;
import net.schmizz.sshj.transport.verification.HostKeyVerifier;
import net.schmizz.sshj.transport.verification.OpenSSHKnownHosts;

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

	@Argument(alias = "P", description = "Remote port", required = false)
	private static Integer port = 22;
	

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

		final File khFile = new File(OpenSSHKnownHosts.detectSSHDir(), "known_hosts");
        ssh.addHostKeyVerifier(new SimpleKnownHostsVerifier(khFile));

		ssh.connect(info.host, port);
		ssh.addHostKeyVerifier(new NullHostKeyVerifier());
		ssh.authPassword(info.username, info.password.toCharArray());
		try {
			final Session session = ssh.startSession();
			try {
				final Command cmd = session.exec("mkdir -p " + info.remotePath);
				System.out.println(IOUtils.readFully(cmd.getInputStream()).toString());
				cmd.join(1, TimeUnit.SECONDS);
				System.out.println("\n** exit status: " + cmd.getExitStatus());
			} finally {
				session.close();
			}
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

	public static class SimpleKnownHostsVerifier extends OpenSSHKnownHosts {

		public SimpleKnownHostsVerifier(File khFile) throws IOException {
			super(khFile);
		}

		@Override
		protected boolean hostKeyUnverifiableAction(String hostname, PublicKey key) {
			try {
				entries().add(new SimpleEntry(null, hostname, KeyType.fromKey(key), key));
				write();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			return true;
		}

		@Override
		protected boolean hostKeyChangedAction(HostEntry entry, String hostname, PublicKey key) {
			return false;
		}

	}

}
