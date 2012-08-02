package net.schmizz.sshj;

import java.io.IOException;

import net.schmizz.sshj.CommandLineRunner.ServerInfo;

public class ComandLineRunnerTest {

	public static void main(String[] args) throws IOException, ClassNotFoundException {
		ServerInfo info = new ServerInfo();
		info.setHost("static.arale.alipay.net");
		info.setUsername("admin");
		info.setPassword("3.45[]$%");
		info.setLocalPath("/Users/kanghui/Projects/alipay-project2/alipay/package.json");
		info.setRemotePath("/tmp");

		CommandLineRunner.upload(info);
	}
}
