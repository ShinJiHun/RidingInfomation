package org.example.ridinginfomation.Garmin.Util;

import org.example.ridinginfomation.fit.Decode;
import org.example.ridinginfomation.fit.MesgBroadcaster;
import org.example.ridinginfomation.fit.RecordMesg;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
public class Utils {
    private static final long FIT_EPOCH_OFFSET = 631065600L;

//    public static void uploadWithOsBasedKey(String host, int port, String user) {
//        try (SSHClient ssh = new SSHClient()) {
//            ssh.addHostKeyVerifier(new PromiscuousVerifier());
//            ssh.connect(host, port);
//
//            // OS 기반 설정
//            String os = System.getProperty("os.name").toLowerCase();
//            String privateKeyPath = os.contains("win")
//                    ? "D:/key/gcp/temp/riding_key_nopass"
//                    : "/Users/jihoon.shin/Desktop/development/key/riding_key_mac_nopass";
//            String localRoot = os.contains("win")
//                    ? "D:/development/project/RidingInfomation/src/main/resources/fit"
//                    : "/Users/jihoon.shin/Desktop/development/project/RidingInfomation/src/main/resources/fit";
//            String remoteRoot = "/home/tho881/NAS";
//
//            KeyProvider keyProvider = ssh.loadKeys(privateKeyPath, (char[]) null);
//            ssh.authPublickey(user, keyProvider);
//
//            // ✅ 서버 파일 목록 가져오기
//            Set<String> remoteFiles = fetchRemoteFiles(ssh, remoteRoot);
//
//            // ✅ 파일 전송 (이미 존재하는 파일 제외)
//            uploadNewFilesOnly(localRoot, remoteRoot, ssh, remoteFiles);
//
//            System.out.println("✅ 새로운 파일만 전송 완료");
//
//        } catch (Exception e) {
//            System.err.println("❌ 전송 실패: " + e.getMessage());
//            e.printStackTrace();
//        }
//    }

//    public static void uploadDirectoryRecursively(String localDirPath, String remoteDirPath, SSHClient ssh) throws IOException {
//        File dir = new File(localDirPath);
//        if (!dir.exists() || !dir.isDirectory()) {
//            throw new IOException(localDirPath + " is not a directory");
//        }
//
//        // 원격 디렉토리 생성
//        try (Session session = ssh.startSession()) {
//            session.exec("mkdir -p " + remoteDirPath).join();
//        }
//
//        for (File file : Objects.requireNonNull(dir.listFiles())) {
//            // ⛔ .DS_Store 및 숨김 파일 제외
//            if (file.getName().startsWith(".")) {
//                System.out.println("⏭️ 숨김 파일 제외: " + file.getName());
//                continue;
//            }
//
//            String remotePath = remoteDirPath + "/" + file.getName();
//            if (file.isDirectory()) {
//                uploadDirectoryRecursively(file.getAbsolutePath(), remotePath, ssh);
//            } else {
//                System.out.println("📤 " + file.getAbsolutePath() + " -> " + remotePath);
//                ssh.newSCPFileTransfer().upload(file.getAbsolutePath(), remotePath);
//            }
//        }
//    }
//
//    private static Set<String> fetchRemoteFiles(SSHClient ssh, String remoteRoot) throws IOException {
//        Set<String> fileSet = new HashSet<>();
//
//        try (Session session = ssh.startSession()) {
//            Session.Command cmd = session.exec("find " + remoteRoot + " -type f");
//            BufferedReader reader = new BufferedReader(new InputStreamReader(cmd.getInputStream()));
//
//            String line;
//            while ((line = reader.readLine()) != null) {
//                fileSet.add(line.trim());
//            }
//
//            cmd.join();
//        }
//
//        return fileSet;
//    }
//
//    private static void uploadNewFilesOnly(String localDirPath, String remoteDirPath, SSHClient ssh, Set<String> remoteFiles) throws IOException {
//        File dir = new File(localDirPath);
//        if (!dir.exists() || !dir.isDirectory()) {
//            throw new IOException(localDirPath + " is not a directory");
//        }
//
//        try (Session session = ssh.startSession()) {
//            session.exec("mkdir -p " + remoteDirPath).join();
//        }
//
//        for (File file : Objects.requireNonNull(dir.listFiles())) {
//            if (file.getName().equals(".DS_Store")) continue; // ⛔ 제외
//            String remotePath = remoteDirPath + "/" + file.getName();
//
//            if (file.isDirectory()) {
//                uploadNewFilesOnly(file.getAbsolutePath(), remotePath, ssh, remoteFiles);
//            } else {
//                if (!remoteFiles.contains(remotePath)) {
//                    System.out.println("📤 " + file.getAbsolutePath() + " -> " + remotePath);
//                    ssh.newSCPFileTransfer().upload(file.getAbsolutePath(), remotePath);
//                } else {
//                    System.out.println("⏭️ 이미 존재: " + remotePath);
//                }
//            }
//        }
//    }

    public static LocalDateTime convertFitTimestampToKST(long fitTimestamp) {
        long unixEpoch = fitTimestamp + FIT_EPOCH_OFFSET;
        return Instant.ofEpochSecond(unixEpoch)
                      .atZone(ZoneId.of("Asia/Seoul")) // ✅ 한국 표준시 기준으로
                      .toLocalDateTime();
    }

    public static LocalDateTime extractStartTime(Path filePath) {
        String name = filePath.getFileName().toString().toLowerCase();

        try {
            if (name.endsWith(".fit")) {
                return extractStartTimeFromFit(filePath);
            } else if (name.endsWith(".gpx")) {
                return extractStartTimeFromGpx(filePath);
            } else if (name.endsWith(".tcx")) {
                return extractStartTimeFromTcx(filePath);
            } else {
                System.out.println("❌ 지원하지 않는 파일 형식: " + name);
                return null;
            }
        } catch (Exception e) {
            System.out.println("❌ 시작 시간 추출 실패: " + filePath + " → " + e.getMessage());
            return null;
        }
    }

    public static LocalDateTime extractStartTimeFromFit(Path fitFile) throws IOException {
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
        }
    }

    public static LocalDateTime extractStartTimeFromGpx(Path gpxFile) throws Exception {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = builder.parse(gpxFile.toFile());

        NodeList times = doc.getElementsByTagName("time");
        if (times.getLength() > 0) {
            String timeStr = times.item(0).getTextContent();
            return LocalDateTime.parse(timeStr.replace("Z", ""));
        }
        return null;
    }

    public static LocalDateTime extractStartTimeFromTcx(Path tcxFile) throws Exception {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = builder.parse(tcxFile.toFile());

        NodeList times = doc.getElementsByTagName("Time");
        if (times.getLength() > 0) {
            String timeStr = times.item(0).getTextContent();
            return LocalDateTime.parse(timeStr.replace("Z", ""));
        }
        return null;
    }

}
