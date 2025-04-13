package org.example.ridinginfomation.Garmin.Util;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Component
public class Utils {

    private static final Logger logger = LoggerFactory.getLogger(Utils.class);

    public static void uploadWithOsBasedKey(String host, int port, String user) {
        try (SSHClient ssh = new SSHClient()) {
            ssh.addHostKeyVerifier(new PromiscuousVerifier());
            ssh.connect(host, port);

            // OS 기반 설정
            String os = System.getProperty("os.name").toLowerCase();
            String privateKeyPath = os.contains("win")
                    ? "D:/key/gcp/temp/riding_key_nopass"
                    : "/Users/jihoon.shin/Desktop/development/key/riding_key_mac_nopass";
            String localRoot = os.contains("win")
                    ? "D:/development/project/RidingInfomation/src/main/resources/fit"
                    : "/Users/jihoon.shin/Desktop/development/project/RidingInfomation/src/main/resources/fit";
            String remoteRoot = "/home/tho881/NAS";

            KeyProvider keyProvider = ssh.loadKeys(privateKeyPath, (char[]) null);
            ssh.authPublickey(user, keyProvider);

            // ✅ 서버 파일 목록 가져오기
            Set<String> remoteFiles = fetchRemoteFiles(ssh, remoteRoot);

            // ✅ 파일 전송 (이미 존재하는 파일 제외)
            uploadNewFilesOnly(localRoot, remoteRoot, ssh, remoteFiles);

            System.out.println("✅ 새로운 파일만 전송 완료");

        } catch (Exception e) {
            System.err.println("❌ 전송 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void uploadDirectoryRecursively(String localDirPath, String remoteDirPath, SSHClient ssh) throws IOException {
        File dir = new File(localDirPath);
        if (!dir.exists() || !dir.isDirectory()) {
            throw new IOException(localDirPath + " is not a directory");
        }

        // 원격 디렉토리 생성
        try (Session session = ssh.startSession()) {
            session.exec("mkdir -p " + remoteDirPath).join();
        }

        for (File file : Objects.requireNonNull(dir.listFiles())) {
            // ⛔ .DS_Store 및 숨김 파일 제외
            if (file.getName().startsWith(".")) {
                System.out.println("⏭️ 숨김 파일 제외: " + file.getName());
                continue;
            }

            String remotePath = remoteDirPath + "/" + file.getName();
            if (file.isDirectory()) {
                uploadDirectoryRecursively(file.getAbsolutePath(), remotePath, ssh);
            } else {
                System.out.println("📤 " + file.getAbsolutePath() + " -> " + remotePath);
                ssh.newSCPFileTransfer().upload(file.getAbsolutePath(), remotePath);
            }
        }
    }

    private static Set<String> fetchRemoteFiles(SSHClient ssh, String remoteRoot) throws IOException {
        Set<String> fileSet = new HashSet<>();

        try (Session session = ssh.startSession()) {
            Session.Command cmd = session.exec("find " + remoteRoot + " -type f");
            BufferedReader reader = new BufferedReader(new InputStreamReader(cmd.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                fileSet.add(line.trim());
            }

            cmd.join();
        }

        return fileSet;
    }

    private static void uploadNewFilesOnly(String localDirPath, String remoteDirPath, SSHClient ssh, Set<String> remoteFiles) throws IOException {
        File dir = new File(localDirPath);
        if (!dir.exists() || !dir.isDirectory()) {
            throw new IOException(localDirPath + " is not a directory");
        }

        try (Session session = ssh.startSession()) {
            session.exec("mkdir -p " + remoteDirPath).join();
        }

        for (File file : Objects.requireNonNull(dir.listFiles())) {
            if (file.getName().equals(".DS_Store")) continue; // ⛔ 제외
            String remotePath = remoteDirPath + "/" + file.getName();

            if (file.isDirectory()) {
                uploadNewFilesOnly(file.getAbsolutePath(), remotePath, ssh, remoteFiles);
            } else {
                if (!remoteFiles.contains(remotePath)) {
                    System.out.println("📤 " + file.getAbsolutePath() + " -> " + remotePath);
                    ssh.newSCPFileTransfer().upload(file.getAbsolutePath(), remotePath);
                } else {
                    System.out.println("⏭️ 이미 존재: " + remotePath);
                }
            }
        }
    }
}
