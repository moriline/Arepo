package org.arepo.common;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

public class AppUtils {
    static Checksum crc32 = new CRC32();
    public static long getCRC32Checksum(byte[] bytes) {
        crc32.reset();
        crc32.update(bytes, 0, bytes.length);
        return crc32.getValue();
    }
    public static Gson gson = new GsonBuilder().serializeNulls().setPrettyPrinting().create();
    public static Gson gson2 = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().serializeNulls().create();
    public static String toJson(Object object){
        return gson.toJson(object);
    }
    public static String generateHashByContent(String filename, byte[] content) {
        byte[] first = filename.getBytes();
        byte[] resultBytes = ByteBuffer.allocate(first.length + content.length)
                .put(first)
                .put(content)
                .array();
        return generateHashByContent(resultBytes);
    }
    public static String generateHashByContent(byte[] content) {
        var result = "";
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            result = Base64.getEncoder().encodeToString(md.digest(content));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return result;
    }
    public static String getTimeInSec(){
        Instant instant = Instant.now();
        return String.valueOf(instant.getEpochSecond());
    }
    public static Long getCurrentTimeInSeconds() {
        Instant instant = Instant.now();
        return instant.getEpochSecond();
    }
    public static Long getCurrentTime() {
        Instant instant = Instant.now();
        Timestamp timestamp = Timestamp.from(instant);
        return timestamp.getTime();
    }
    public static String convertInMilli(Long timestamp) {
        var instant = Instant.ofEpochMilli(timestamp);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        formatter = formatter.withZone(TimeZone.getTimeZone("UTC").toZoneId());
        return formatter.format(instant);
    }
    public static String convertPatch(byte[] array)throws IOException, ClassNotFoundException{
        try (ByteArrayInputStream bis = new ByteArrayInputStream(array);
             ObjectInputStream ois = new ObjectInputStream(bis)) {
            return (String) ois.readObject();
        }
    }
    public static ArrayList<byte[]> convert2ListOfBytes(byte[] array) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(array);
             ObjectInputStream ois = new ObjectInputStream(bis)) {
            return (ArrayList<byte[]>) ois.readObject();
        }
    }
    public static byte[] convert2byteArray(Object anyObject) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos;
        oos = new ObjectOutputStream(baos);
        oos.writeObject(anyObject);
        // Convert to Byte Array
        return baos.toByteArray();
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        ArrayList<byte[]> list = new ArrayList<>();
        byte[] res = convert2byteArray(list);
        System.out.println("hash:"+generateHashByContent("a b c".getBytes(StandardCharsets.UTF_8)));
        var logIds = new ArrayList<Long>();
        var logValues = new ArrayList<String>();
        logIds.add(33L);
        list.add("33".getBytes());
        logIds.add(2L);
        list.add("2".getBytes());
        System.out.println("json map:"+toJson(logIds));
        System.out.println("content size:"+list.size());

        var delimiter = "\n";
        var text = """
                aaa
                bbb
                ccc
                """;
        var list2 = Arrays.asList(text.split(delimiter));
        var b2 = convert2byteArray(text);
        var list3 = convertPatch(b2);
        System.out.println("final patch:"+list3);
        logValues.add("aaa");
        logValues.add("bbb");
        System.out.println("str from list:"+String.join(delimiter, logValues));
        var mapUniDiff = new HashMap<Long, byte[]>();
        mapUniDiff.put(22L, String.join(delimiter, logValues).getBytes());
        System.out.println("from map:"+mapUniDiff.getOrDefault(22L, new byte[0]));

        System.out.println("json list new:"+toJson(logIds.stream().map(it -> it.toString()).toList()));

    }
}
