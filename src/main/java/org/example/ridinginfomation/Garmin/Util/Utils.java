package org.example.ridinginfomation.Garmin.Util;

import com.garmin.fit.*;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;
import org.example.ridinginfomation.Garmin.Entity.ActivityPointEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;

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
                    : "/Users/jihoon.shin/Desktop/development/key/riding_key_nopass";
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

    public static String encodePolyline(List<ActivityPointEntity> route) {
        StringBuilder encoded = new StringBuilder();
        long lastLat = 0, lastLng = 0;

        for (ActivityPointEntity point : route) {
            long lat = Math.round(point.getLatitude() * 1e5);
            long lng = Math.round(point.getLongitude() * 1e5);

            long dLat = lat - lastLat;
            long dLng = lng - lastLng;

            encodeValue(dLat, encoded);
            encodeValue(dLng, encoded);

            lastLat = lat;
            lastLng = lng;
        }
        return encoded.toString();
    }

    private static void encodeValue(long v, StringBuilder encoded) {
        v = v < 0 ? ~(v << 1) : (v << 1);
        while (v >= 0x20) {
            encoded.append((char)((0x20 | (v & 0x1f)) + 63));
            v >>= 5;
        }
        encoded.append((char)(v + 63));
    }

    public void convert(File gpxFile, File outputFitFile) throws Exception {
        List<RecordMesg> records = new ArrayList<>();

        // 1. GPX 파싱
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(gpxFile);
        NodeList trkpts = doc.getElementsByTagName("trkpt");

        Date startTime = null, endTime = null;
        double startLat = 0, startLon = 0, endLat = 0, endLon = 0;

        for (int i = 0; i < trkpts.getLength(); i++) {
            Element point = (Element) trkpts.item(i);
            double lat = Double.parseDouble(point.getAttribute("lat"));
            double lon = Double.parseDouble(point.getAttribute("lon"));
            double altitude = 0;
            Date timestamp = null;

            NodeList children = point.getChildNodes();
            for (int j = 0; j < children.getLength(); j++) {
                Node child = children.item(j);
                if ("ele".equals(child.getNodeName())) {
                    altitude = Double.parseDouble(child.getTextContent());
                }
                if ("time".equals(child.getNodeName())) {
                    timestamp = Date.from(Instant.parse(child.getTextContent()));
                }
            }

            if (timestamp != null) {
                if (startTime == null) {
                    startTime = timestamp;
                    startLat = lat;
                    startLon = lon;
                }
                endTime = timestamp;
                endLat = lat;
                endLon = lon;
            }

            RecordMesg record = new RecordMesg();
            record.setPositionLat(this.semicircles(lat));
            record.setPositionLong(this.semicircles(lon));
            record.setAltitude((float) altitude);
            if (null != timestamp) {
                record.setTimestamp(new DateTime(timestamp));
            }
            records.add(record);
        }

        // 2. FIT 파일 작성
        FileEncoder encoder = new FileEncoder(outputFitFile, Fit.ProtocolVersion.V2_0);

        // FileId
        FileIdMesg fileIdMesg = new FileIdMesg();
        fileIdMesg.setType(com.garmin.fit.File.ACTIVITY);
        fileIdMesg.setManufacturer(Manufacturer.DEVELOPMENT);
        fileIdMesg.setProduct(1);
        fileIdMesg.setTimeCreated(new DateTime(startTime));
        fileIdMesg.setSerialNumber(123456L);
        encoder.write(fileIdMesg);

        // RecordMesg
        for (RecordMesg mesg : records) {
            encoder.write(mesg);
        }

        // SessionMesg
        SessionMesg sessionMesg = new SessionMesg();
        sessionMesg.setSport(Sport.CYCLING);
        sessionMesg.setSubSport(SubSport.GENERIC);
        sessionMesg.setStartTime(new DateTime(startTime));
        sessionMesg.setTimestamp(new DateTime(endTime));
        sessionMesg.setTotalElapsedTime((float) ((endTime.getTime() - startTime.getTime()) / 1000.0));
        sessionMesg.setStartPositionLat(semicircles(startLat));
        sessionMesg.setStartPositionLong(semicircles(startLon));
        sessionMesg.setEndPositionLat(semicircles(endLat));
        sessionMesg.setEndPositionLong(semicircles(endLon));
        encoder.write(sessionMesg);

        // ActivityMesg
        ActivityMesg activityMesg = new ActivityMesg();
        activityMesg.setTimestamp(new DateTime(endTime));
        activityMesg.setTotalTimerTime((float) ((endTime.getTime() - startTime.getTime()) / 1000.0));
        activityMesg.setNumSessions(1);
        activityMesg.setType(Activity.MANUAL);
        activityMesg.setEvent(Event.ACTIVITY);
        activityMesg.setEventType(EventType.STOP);
        encoder.write(activityMesg);

        encoder.close();
        System.out.println("✅ 변환 완료: " + outputFitFile.getAbsolutePath());
    }

    private int semicircles(double degrees) {
        return (int) (degrees * (Math.pow(2, 31) / 180.0));
    }

}
