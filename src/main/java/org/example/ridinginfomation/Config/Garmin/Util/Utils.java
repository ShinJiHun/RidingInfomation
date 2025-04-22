package org.example.ridinginfomation.Config.Garmin.Util;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;
import org.example.ridinginfomation.fit.Decode;
import org.example.ridinginfomation.fit.MesgBroadcaster;
import org.example.ridinginfomation.fit.RecordMesg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Component
public class Utils {
    private static final Logger logger = LoggerFactory.getLogger(Utils.class);
    private static final long FIT_EPOCH_OFFSET = 631065600L;

    public static void uploadWithOsBasedKey(String host, int port, String user) {
        try (SSHClient ssh = new SSHClient()) {
            ssh.addHostKeyVerifier(new PromiscuousVerifier());
            ssh.connect(host, port);

            String os = System.getProperty("os.name").toLowerCase();
            String privateKeyPath = os.contains("win")
                    ? "D:\\key\\gcp\\temp\\riding_key_nopass"
                    : "/Users/jihoon.shin/Desktop/development/key/riding_key_mac_nopass";
            String localRoot = os.contains("win")
                    ? "D:/development/project/RidingInfomation/src/main/resources/fit"
                    : "/Users/jihoon.shin/Desktop/development/project/RidingInfomation/src/main/resources/fit";
            String remoteRoot = "/home/tho881/NAS";

            KeyProvider keyProvider = ssh.loadKeys(privateKeyPath, (char[]) null);
            ssh.authPublickey(user, keyProvider);

            Set<String> remoteFiles = fetchRemoteFiles(ssh, remoteRoot);
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

        try (Session session = ssh.startSession()) {
            session.exec("mkdir -p " + remoteDirPath).join();
        }

        for (File file : Objects.requireNonNull(dir.listFiles())) {
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

    public static LocalDateTime convertFitTimestamp(long fitTimestamp) {
        long unixEpoch = fitTimestamp + FIT_EPOCH_OFFSET;
        return LocalDateTime.ofEpochSecond(unixEpoch, 0, ZoneOffset.UTC);
    }

    public static LocalDateTime convertFitTimestampToKST(long fitTimestamp) {
        long unixEpoch = fitTimestamp + FIT_EPOCH_OFFSET;
        return Instant.ofEpochSecond(unixEpoch)
                      .atZone(ZoneId.of("Asia/Seoul")) // ✅ 한국 표준시 기준으로
                      .toLocalDateTime();
    }

    public static LocalDateTime extractStartTime(Path fitFile) {
        try (InputStream in = Files.newInputStream(fitFile)) {
            Decode decode = new Decode();
            MesgBroadcaster broadcaster = new MesgBroadcaster(decode);

            final LocalDateTime[] startTime = {null};

            broadcaster.addListener((RecordMesg mesg) -> {
                if (startTime[0] == null && mesg.getTimestamp() != null) {
                    long fitTimestamp = mesg.getTimestamp().getTimestamp();
                    startTime[0] = convertFitTimestampToKST(fitTimestamp);
                }
            });

            decode.read(in, broadcaster);
            return startTime[0];

        } catch (Exception e) {
            System.out.println("❌ FIT 시간 추출 실패: " + fitFile + " → " + e.getMessage());
            return null;
        }
    }
}
