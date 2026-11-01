package moneystreak.store;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * JSON 파일 기반 영속성 계층.
 * 각 Manager는 자신의 도메인 객체를 Map/List 구조로 변환한 뒤 이 클래스를 통해
 * 저장/로드한다 (실제 Transaction, Budget 등으로의 변환은 각 Manager가 담당).
 */
public class DataStore {

    private final Path baseDir;

    public DataStore(String baseDirPath) {
        this.baseDir = Paths.get(baseDirPath);
        try {
            Files.createDirectories(baseDir);
        } catch (IOException e) {
            throw new RuntimeException("데이터 폴더를 생성할 수 없음: " + baseDir, e);
        }
    }

    /** data 폴더 안의 해당 파일이 존재하는지 확인한다. */
    public boolean exists(String filename) {
        return Files.exists(baseDir.resolve(filename));
    }

    /** 주어진 객체(Map/List 구조)를 JSON으로 직렬화하여 파일에 저장한다. */
    public void write(String filename, Object data) {
        Path path = baseDir.resolve(filename);
        String json = SimpleJson.write(data);
        try {
            Files.write(path, json.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException("파일을 저장할 수 없음: " + path, e);
        }
    }

    /** 파일을 읽어 JSON을 파싱한 결과(Map 또는 List)를 반환한다. 파일이 없으면 null. */
    public Object read(String filename) {
        Path path = baseDir.resolve(filename);
        if (!Files.exists(path)) {
            return null;
        }
        try {
            String json = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            if (json.trim().isEmpty()) return null;
            return SimpleJson.parse(json);
        } catch (IOException e) {
            throw new RuntimeException("파일을 읽을 수 없음: " + path, e);
        }
    }

    /** 임의의 경로(그룹 경쟁 export/import처럼 data 폴더 밖 파일)에 JSON을 저장한다. */
    public static void writeToPath(Path path, Object data) {
        try {
            Files.write(path, SimpleJson.write(data).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException("파일을 저장할 수 없음: " + path, e);
        }
    }

    /** 임의의 경로에서 JSON을 읽어 파싱한다. */
    public static Object readFromPath(Path path) {
        try {
            String json = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            return SimpleJson.parse(json);
        } catch (IOException e) {
            throw new RuntimeException("파일을 읽을 수 없음: " + path, e);
        }
    }
}
