package org.arepo;

import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.google.gson.annotations.Expose;
import org.arepo.common.*;
import org.arepo.entites.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

import static java.nio.file.FileVisitResult.CONTINUE;

public class Model {
    Path filename;
    Path repo;
    DataSource dataSource;
    JdbcTemplate template;
    NamedParameterJdbcTemplate namedParamJdbcTemplate;
    static String driver = "org.sqlite.JDBC";
    public static String FILE_TABLE = "files";
    public static String LOG_TABLE = "logs";
    public static String TEMP_TABLE = "tempfiles";
    public static String PATCH_TABLE = "patches";
    public static String PATCH_HISTORY_TABLE = "history";
    public static String SETTINGS_TABLE = "settings";
    public static String createTABLE = """
            CREATE TABLE IF NOT EXISTS %s
                        (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL UNIQUE,
                        content BLOB,
                        hash INTEGER,
                        parent INTEGER,
                        version INTEGER NOT NULL DEFAULT 0,
                        charset INTEGER
                        );
            """;
    public static String createLOG_TABLE = """
            CREATE TABLE IF NOT EXISTS %s
                        (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        comment TEXT NOT NULL UNIQUE,
                        email TEXT NOT NULL,
                        files JSON NOT NULL,
                        types JSON NOT NULL,
                        content BLOB,
                        modified INTEGER
                        );
            """;
    public static String createTEMP_TABLE = """
            CREATE TABLE IF NOT EXISTS %s 
            (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            name TEXT NOT NULL UNIQUE,
            content BLOB,
            hash INTEGER
            );
            """;
    public static String createPATCH_TABLE = """
            CREATE TABLE IF NOT EXISTS %s 
            (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            name TEXT NOT NULL UNIQUE,
            content BLOB,
            hash INTEGER,
            version INTEGER NOT NULL DEFAULT 0,
            charset INTEGER NOT NULL DEFAULT 0,
            updated INTEGER NOT NULL DEFAULT 0,
            comment TEXT NOT NULL DEFAULT ''
            );
            """;
    public static String createPATCH_HISTORY_TABLE = """
            CREATE TABLE IF NOT EXISTS %s
            (
            name TEXT NOT NULL,
            content BLOB,
            hash INTEGER,
            updated INTEGER NOT NULL DEFAULT 0
            );
            """;
    public static String createSETTINGS_TABLE = """
            CREATE TABLE IF NOT EXISTS %s
            (
            content JSON NOT NULL
            );
            """;
    public Patch patch;
    public Text text;
    public Logs log;
    public Temp temp;
    public Status status;
    //public Settings settings;

    public Model(Path repo) {
        this.repo = repo;
        try {

            this.filename = Files.createDirectories(repo.toAbsolutePath().resolve(ASettings.DIR_NAME)).resolve(ASettings.DB_NAME);
            System.out.println("file 1:" + filename.getFileName());
            System.out.println("base 1:" + filename.getParent());
            dataSource = getDataSource(driver, "jdbc:sqlite:" + filename.toString(), "", "");
            //template = new JdbcTemplate(dataSource);
            this.namedParamJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);

            this.text = new Text(namedParamJdbcTemplate);
            this.log = new Logs(namedParamJdbcTemplate);
            this.temp = new Temp(namedParamJdbcTemplate);
            this.status = new Status(namedParamJdbcTemplate);

            this.patch = new Patch(namedParamJdbcTemplate, repo);

            //this.settings = new Settings(namedParamJdbcTemplate);



        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static DataSource getDataSource(String driver, String url, String username, String password) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(driver);
        dataSource.setUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        return dataSource;
    }
    public void create(){
        text.createTable();
        log.createTable();
        temp.createTable();
        //settings.createTable();
        patch.create();
    }
    public void save(RepoSettings settings) throws IOException {
        Files.write(repo.toAbsolutePath().resolve(ASettings.DIR_NAME).resolve(ASettings.FILE_SETTINGS), AppUtils.toJson(settings).getBytes());
    }
    public RepoSettings get() throws IOException {
        return AppUtils.gson.fromJson(Files.readString(repo.toAbsolutePath().resolve(ASettings.DIR_NAME).resolve(ASettings.FILE_SETTINGS)), RepoSettings.class);
    }
}

class Patch {
    public static RowMapper<APatches> patchRowMapper = (resultSet, rowNum) -> {
        var file = new APatches(resultSet.getString("name"),
                resultSet.getBytes("content"),
                resultSet.getLong("hash"),
                resultSet.getInt("charset")
        );
        file.setVersion(resultSet.getLong("version"));
        file.setUpdated(resultSet.getLong("updated"));
        file.setComment(resultSet.getString("comment"));
        return file;
    };
    public static RowMapper<APatchHistory> patchHistoryRowMapper = (resultSet, rowNum) -> {
        var file = new APatchHistory(resultSet.getString("name"),
                resultSet.getBytes("content"),
                resultSet.getLong("updated")
        );
        return file;
    };
    //public static List<String> names = List.of("a", "b", "c");
    NamedParameterJdbcTemplate namedParamJdbcTemplate;
    Path repo;
    public Patch(NamedParameterJdbcTemplate namedParamJdbcTemplate, Path repo) {
        this.namedParamJdbcTemplate = namedParamJdbcTemplate;
        this.repo = repo;
    }
    public void create(){

        for(var name : ASettings.defindedPatches){
            try {
                createPatchDB(name);
            } catch (IOException e) {
                System.err.println("Patch not found:"+name);
            }
        }
    }
    public void createPatchDB(String name) throws IOException {
        var named = new NamedParameterJdbcTemplate(build2(name+ASettings.PATCH_EXTENSION, false));
        named.getJdbcTemplate().execute(Model.createPATCH_TABLE.formatted(Model.PATCH_TABLE));
        named.getJdbcTemplate().execute(Model.createPATCH_HISTORY_TABLE.formatted(Model.PATCH_HISTORY_TABLE));
    }
    public DataSource build2(String name, boolean checkFile) throws IOException {
        if(checkFile){
            if(Files.notExists(repo.resolve(ASettings.DIR_NAME).resolve(name))) throw new IOException("Patch is not exists:"+name);
        }
        return Model.getDataSource(Model.driver, "jdbc:sqlite:"+repo.resolve(ASettings.DIR_NAME).resolve(name).toString(), "", "");
    }
    public void clearData(String patchname) throws IOException {
        var named = new NamedParameterJdbcTemplate(build2(patchname+ASettings.PATCH_EXTENSION, true));
        named.getJdbcTemplate().execute("DROP TABLE IF EXISTS %s;".formatted(Model.PATCH_TABLE));
        named.getJdbcTemplate().execute("DROP TABLE IF EXISTS %s;".formatted(Model.PATCH_HISTORY_TABLE));
        named.getJdbcTemplate().execute(Model.createPATCH_TABLE.formatted(Model.PATCH_TABLE));
        named.getJdbcTemplate().execute(Model.createPATCH_HISTORY_TABLE.formatted(Model.PATCH_HISTORY_TABLE));
    }
    /**
     * Add file name to patch with prefix name.
     *
     * @return
     * @throws IOException
     */
    public int addFile(NamedParameterJdbcTemplate named, ATempFiles temp, Long version, Long time) throws IOException {
        APatches file = new APatches(temp.getName(), temp.getContent(), temp.getHash(), temp.getCharset());
        file.setVersion(version);
        var sql = """
                    INSERT INTO %s 
            (name, content, hash, version, charset, updated) 
            VALUES (:name, :content, :hash, :version, :charset, :updated) ON CONFLICT(name) DO UPDATE SET hash=excluded.hash, content=excluded.content, updated = excluded.updated;
                    """.formatted(Model.PATCH_TABLE);
        int result = named.update(sql, new BeanPropertySqlParameterSource(file));
        var sql2 = """
                INSERT INTO %s 
            (name, content, updated) VALUES (:name, :content, :updated);
                """.formatted(Model.PATCH_HISTORY_TABLE);
        var history = new APatchHistory(file.getName(), file.getContent(), time);
        var result2 = named.update(sql2, new BeanPropertySqlParameterSource(history));
        return result;
    }
    public int deleteFile(NamedParameterJdbcTemplate named, String filename, Long time) throws IOException {
        var founded = findByName(named, filename);
        if(founded.isEmpty()) throw new IOException(GlobalValues.FNF+filename);
        String comment = "";
        var pf = founded.iterator().next();
        if(!pf.getComment().isEmpty()){
            comment = pf.getComment();
        }
        var sql = "DELETE FROM %s WHERE name = :name;".formatted(Model.PATCH_TABLE);
        var parameters = new HashMap<String, Object>();
        parameters.put("name", filename);
        int result = named.update(sql, parameters);
        if(result > 0 && !comment.isEmpty()){
            System.out.println("Move comment to new file:"+comment);
            try{
                System.out.println("new comment:"+appendComment(named, comment));
            } catch (IOException e) {
                //ignore it - we lost comment in patch!
                System.err.println(e.getMessage());
            }
        }
        var sql2 = """
                INSERT INTO %s 
            (name, content, updated) VALUES (:name, :content, :updated);
                """.formatted(Model.PATCH_HISTORY_TABLE);
        var history = new APatchHistory(filename, null, time);
        var result2 = named.update(sql2, new BeanPropertySqlParameterSource(history));
        return result;
    }
    public Path createPatchName(String patchname, String email) throws IOException {
        var named = new NamedParameterJdbcTemplate(build2(patchname+ASettings.PATCH_EXTENSION, true));
        var orig = repo.resolve(ASettings.DIR_NAME).resolve(patchname+ASettings.PATCH_EXTENSION);
        return repo.resolve(ASettings.DIR_NAME).resolve(email+"_"+AppUtils.getTimeInSec()+ASettings.PATCH_EXTENSION);
        //Files.copy(orig, dest, StandardCopyOption.REPLACE_EXISTING);
    }
    public void deletePatch(String patchname) throws IOException {
        if(!ASettings.defindedPatches.contains(patchname)) Files.deleteIfExists(repo.resolve(ASettings.DIR_NAME).resolve(patchname+ASettings.PATCH_EXTENSION));
    }
    public List<APatches> findByName(NamedParameterJdbcTemplate named, String filename){
        var sql = "SELECT * FROM %s WHERE name = :name ;".formatted(Model.PATCH_TABLE);
        var params = new MapSqlParameterSource("name", filename);
        return named.query(
                sql,
                params,
                patchRowMapper
        );
    }
    public int createComment(String patchname, String comment) throws IOException {
        var named = new NamedParameterJdbcTemplate(build2(patchname+ASettings.PATCH_EXTENSION, true));
        return appendComment(named, comment);
    }
    private int appendComment(NamedParameterJdbcTemplate named, String comment) throws IOException {
        //named.getJdbcTemplate().execute("DELETE FROM %s;".formatted(Model.PATCH_TABLE));
        var sql2 = "SELECT COALESCE(MIN(id), 0) FROM %s ;".formatted(Model.PATCH_TABLE);
        var minIdForComment = named.queryForObject(sql2, Map.of(), Long.class);
        if(minIdForComment == 0L) throw new IOException("Files in patch are empty. Add new files first!");
        var sql = "UPDATE %s SET comment = :comment WHERE id = (SELECT MIN(id) FROM %s );".formatted(Model.PATCH_TABLE, Model.PATCH_TABLE);
        var parameters = new HashMap<String, Object>();
        parameters.put("comment", comment);
        return named.update(sql, parameters);
    }
    public String getComment(String patchname) throws IOException {
        var named = new NamedParameterJdbcTemplate(build2(patchname+ASettings.PATCH_EXTENSION, true));
        var sql = "SELECT comment FROM %s WHERE id = (SELECT MIN(id) FROM %s );".formatted(Model.PATCH_TABLE, Model.PATCH_TABLE);
        //return named.queryForObject(sql, Map.of(), String.class);
        var rs = named.queryForRowSet(sql, Map.of());
        return rs.first()?rs.getString("comment"):"";
    }
    public Path getPatchPath(String patchname) throws IOException {
        var named = new NamedParameterJdbcTemplate(build2(patchname+ASettings.PATCH_EXTENSION, true));
        return repo.resolve(ASettings.DIR_NAME).resolve(patchname+ASettings.PATCH_EXTENSION);
    }
    static String SELECT_FROM_PATCH = "SELECT * FROM %s ORDER BY updated ASC;".formatted(Model.PATCH_TABLE);
    public List<APatches> getFilesFromPatch(String patchname) throws IOException {
        var named = new NamedParameterJdbcTemplate(build2(patchname+ASettings.PATCH_EXTENSION, true));
        return named.query(
                SELECT_FROM_PATCH,
                patchRowMapper);
    }
    public List<APatches> getFilesFromPatch(NamedParameterJdbcTemplate named) {
        return named.query(
                SELECT_FROM_PATCH,
                patchRowMapper);
    }
    //TODO ignore patch extension
    public Long addPatchToLogs(String patchname, String email, Text text1, Logs log1) throws IOException {
        //var named = new NamedParameterJdbcTemplate(build(patchname, true));
        //patchname = patchname.substring(0, patchname.lastIndexOf(ASettings.PATCH_EXTENSION));
        var textFiles = new ArrayList<TextFiles>();
        var files = getFilesFromPatch(patchname);
        if(files.isEmpty()) throw new IOException("Files is empty in patch:"+patchname);
        System.out.println("mer files:"+files);
        var mapUniDiff = new HashMap<String, byte[]>();
        String comment = "";//comment
        Long time = AppUtils.getCurrentTimeInSeconds();
        List<String> types = new ArrayList<String>();
        for(var file : files){
            if(!file.getComment().isEmpty()) comment = file.getComment();
            var founded = text1.findByName(file.getName());
            var text = !founded.isEmpty()?founded.iterator().next():new TextFiles(0L, file.getName(), file.getContent());

            if(!text.getVersion().equals(file.getVersion())) throw new IOException("Version %d is not valid for file: ".formatted(text.getVersion())+text.getName());

            if(file.getContent() == null) {
                text.setName(ASettings.SIGN_ERASIED_FILE +file.getName());
                text.update(file.getContent());
            }else {
                if(!file.getCharset().equals(GlobalValues.BINARY_CHARSET)){
                    // create uniDiff for text file.

                    List<String> original = text.getId() == 0L?List.of():Arrays.asList(new String(text.getContent(), FileCharsets.findByIndex(text.getCharset())).split(GlobalValues.DELIMITER));
                    //var target = Arrays.asList(new String(file.getContent(), FileCharsets.findByIndex(file.getCharset())).split(GlobalValues.DELIMITER));
                    var first = text.getId() == 0L?"":new String(text.getContent(), FileCharsets.findByIndex(text.getCharset()));
                    var second = new String(file.getContent(), FileCharsets.findByIndex(file.getCharset()));
                    var diffExp = DiffUtils.diff(first, second, null);
                    List<String> unifiedDiff = UnifiedDiffUtils.generateUnifiedDiff("original", "new", original, diffExp, 0);
                    mapUniDiff.put(text.getName(), String.join(GlobalValues.DELIMITER, unifiedDiff).getBytes());
                }
                text.update(file.getContent());
            }
            //text.setVersion(file.getVersion()+1L);
            text.setCharset(file.getCharset());
            textFiles.add(text);
        }

        ArrayList<byte[]> list = new ArrayList<>();
        var logIds = new ArrayList<Long>();

        for(var tfile : textFiles){
            if(tfile.getId().equals(0L)){
                tfile.setVersion(time);
                text1.insertTextFile(tfile);
            }else {
                tfile.setVersion(time);
                text1.updateTextFile(tfile);
            }
            logIds.add(tfile.getId());
            types.add(mapUniDiff.containsKey(tfile.getName())?ASettings.TEXT_FILE_TYPE:ASettings.BINARY_FILE_TYPE);
            list.add(mapUniDiff.getOrDefault(tfile.getName(), tfile.getContent()));
        }
        var byteArray = AppUtils.convert2byteArray(list);
        log1.insertLogFile(new LogFiles(0L, comment, email, AppUtils.toJson(logIds.stream().map(it -> it.toString()).toList()), AppUtils.toJson(types), byteArray, time));
        return time;
    }

    public List<String> list() throws IOException {
        var result = new ArrayList<String>();
        var currentPath = repo.resolve(ASettings.DIR_NAME).toAbsolutePath();
        FileVisitor<Path> rootVisitor = new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if(attrs.isRegularFile()){
                    var relPath = currentPath.relativize(file).toString();
                    System.out.println("rel:"+relPath);

                    if(relPath.endsWith(ASettings.PATCH_EXTENSION)){
                        if(!relPath.endsWith(ASettings.DB_NAME)){
                            result.add(relPath.substring(0, relPath.lastIndexOf(ASettings.PATCH_EXTENSION)));
                        }
                    }

                }
                return CONTINUE;
            }
        };

        Files.walkFileTree(currentPath, EnumSet.noneOf(FileVisitOption.class), 1, rootVisitor);
        return result;
    }

    public List<APatchHistory> getHistory(String patchname) throws IOException {
        var named = new NamedParameterJdbcTemplate(build2(patchname+ASettings.PATCH_EXTENSION, true));
        var sql = "SELECT * FROM %s;".formatted(Model.PATCH_HISTORY_TABLE);
        return named.query(
                sql,
                patchHistoryRowMapper
        );
    }
    public List<APatchHistory> getHistoryByTime(String patchname, Long time) throws IOException {
        var named = new NamedParameterJdbcTemplate(build2(patchname+ASettings.PATCH_EXTENSION, true));
        var sql = "SELECT * FROM %s WHERE updated = :updated;".formatted(Model.PATCH_HISTORY_TABLE);
        var params = new MapSqlParameterSource("updated", time);
        return named.query(
                sql,
                params,
                patchHistoryRowMapper
        );
    }

    public void clearHistory(String patchname) throws IOException {
        var named = new NamedParameterJdbcTemplate(build2(patchname+ASettings.PATCH_EXTENSION, true));
        named.getJdbcTemplate().execute("DROP TABLE IF EXISTS %s;".formatted(Model.PATCH_HISTORY_TABLE));
        named.getJdbcTemplate().execute(Model.createPATCH_HISTORY_TABLE.formatted(Model.PATCH_HISTORY_TABLE));
    }


}
class Settings {
    NamedParameterJdbcTemplate namedParamJdbcTemplate;

    public Settings(NamedParameterJdbcTemplate namedParamJdbcTemplate) {
        this.namedParamJdbcTemplate = namedParamJdbcTemplate;
    }

    public RepoSettings get(){
        var sql = "SELECT content FROM %s;".formatted(Model.SETTINGS_TABLE);
        return AppUtils.gson.fromJson(namedParamJdbcTemplate.queryForObject(sql, Map.of(), String.class), RepoSettings.class);
    }
    public int save(RepoSettings settings){
        namedParamJdbcTemplate.getJdbcTemplate().execute("DELETE FROM %s;".formatted(Model.SETTINGS_TABLE));
        var sql = "INSERT INTO %s (content) VALUES (:content)".formatted(Model.SETTINGS_TABLE);
        var parameters = new HashMap<String, Object>();
        parameters.put("content", AppUtils.toJson(settings));
        return namedParamJdbcTemplate.update(sql, parameters);
    }

    public void dropTable(){
        namedParamJdbcTemplate.getJdbcTemplate().execute("DROP TABLE IF EXISTS %s;".formatted(Model.SETTINGS_TABLE));
    }
    public void createTable(){
        namedParamJdbcTemplate.getJdbcTemplate().execute(Model.createSETTINGS_TABLE.formatted(Model.SETTINGS_TABLE));
    }
}
class Logs {
    NamedParameterJdbcTemplate namedParamJdbcTemplate;
    public Logs(NamedParameterJdbcTemplate namedParamJdbcTemplate) {
        this.namedParamJdbcTemplate = namedParamJdbcTemplate;
    }

    public static RowMapper<LogFiles> logFileRowMapper = (resultSet, rowNum) -> {
        var file = new LogFiles(resultSet.getLong("id"),
                resultSet.getString("comment"),
                resultSet.getString("email"),
                resultSet.getString("files"),
                resultSet.getString("types"),
                resultSet.getBytes("content"),
                resultSet.getLong("modified"));

        return file;
    };
    public List<LogFiles> logFiles(){
        var sql = "SELECT * FROM %s;".formatted(Model.LOG_TABLE);
        return namedParamJdbcTemplate.query(
                sql,
                logFileRowMapper
        );
    }
    public void insertLogFile(LogFiles file){
        var params = new BeanPropertySqlParameterSource(file);
        GeneratedKeyHolder holder = new GeneratedKeyHolder();
        var sql = """
                INSERT INTO %s 
            (comment, email, files, types, content, modified) 
            values (:comment, :email, :files, :types, :content, :modified);
                """.formatted(Model.LOG_TABLE);
        namedParamJdbcTemplate.update(sql, params, holder);
        file.setId(holder.getKey().longValue());
    }
    public void insertLogFiles(List<LogFiles> list){
        var sql = """
                    INSERT INTO %s 
            (id, comment, email, files, types, content, modified) 
            values (:id, :comment, :email, :files, :types, :content, :modified);
                    """.formatted(Model.LOG_TABLE);

        SqlParameterSource[] parameters = SqlParameterSourceUtils
                .createBatch(list.toArray());
        namedParamJdbcTemplate.batchUpdate(sql, parameters);
    }
    public List<LogFiles> getFromId2End(Long maxId){
        //var sql = "SELECT * FROM %s WHERE id BETWEEN :maxId AND (SELECT MAX(id) from %s);".formatted(Model.LOG_TABLE, Model.LOG_TABLE);
        var sql = "SELECT * FROM %s WHERE :maxId < id AND id <= (SELECT MAX(id) FROM %s)".formatted(Model.LOG_TABLE, Model.LOG_TABLE);
        var params = new MapSqlParameterSource("maxId", maxId);
        return namedParamJdbcTemplate.query(
                sql, params,
                logFileRowMapper
        );
    }
    /**
     * @return Long - max id for sync between repos!
     */
    public Long maxId(){
        var sql = "SELECT COALESCE (MAX(id), 0) FROM %s;".formatted(Model.LOG_TABLE);
        return namedParamJdbcTemplate.queryForObject(sql, Map.of(), Long.class);
    }

    /** Get max modified column from logs for sync between repos.
     * @return Long - max id for sync between repos!
     */
    public Long maxLogModified(){
        var sql = "SELECT COALESCE (MAX(modified), 0) FROM %s;".formatted(Model.LOG_TABLE);
        return namedParamJdbcTemplate.queryForObject(sql, Map.of(), Long.class);
    }

    public List<LogFiles> findByFileId(Long id){
        var sql = "SELECT DISTINCT %s.* FROM %s, json_each(files) WHERE json_each.value = '".formatted(Model.LOG_TABLE, Model.LOG_TABLE)+id.toString()+"' ORDER BY modified DESC;";
        return namedParamJdbcTemplate.query(
                sql,
                logFileRowMapper
        );
    }
    public List<LogFiles> findLastLogs(int lastCountRows){
        var sql = "SELECT DISTINCT * FROM %s ORDER BY modified DESC LIMIT %d;".formatted(Model.LOG_TABLE, lastCountRows);
        return namedParamJdbcTemplate.query(
                sql,
                logFileRowMapper
        );
    }
    public List<LogFiles> downloadLogsWithLimit(Long limit){
        var sql = "SELECT * FROM %s ORDER BY modified DESC LIMIT %d;".formatted(Model.LOG_TABLE, limit);
        return namedParamJdbcTemplate.query(
                sql,
                logFileRowMapper
        );
    }
    public List<LogFiles> getLogsAfterModified(Long lastLogModified) {
        var sql = "SELECT * FROM %s WHERE modified > :modified ORDER BY modified ASC;".formatted(Model.LOG_TABLE);
        var params = new MapSqlParameterSource("modified", lastLogModified);
        return namedParamJdbcTemplate.query(
                sql, params,
                logFileRowMapper
        );
    }
    public void dropTable(){
        namedParamJdbcTemplate.getJdbcTemplate().execute("DROP TABLE IF EXISTS %s;".formatted(Model.LOG_TABLE));
    }
    public void createTable(){
        namedParamJdbcTemplate.getJdbcTemplate().execute(Model.createLOG_TABLE.formatted(Model.LOG_TABLE));
    }


    public List<LogFiles> findByFileIdLessModified(Long id, Long lastModifiedLog) {
        var sql = "SELECT DISTINCT %s.* FROM %s, json_each(files) WHERE json_each.value = '".formatted(Model.LOG_TABLE, Model.LOG_TABLE)+id.toString()+"' AND modified < %d ORDER BY modified DESC LIMIT 1;".formatted(lastModifiedLog);
        return namedParamJdbcTemplate.query(
                sql,
                logFileRowMapper
        );
    }
}
class Status {
    static String FILE_ADDED = "A";
    static String FILE_MODIFIED = "M";
    static String FILE_DELETED = "D";
    static String FILE_ERASED = "E";
    static String ERROR = "Error";
    NamedParameterJdbcTemplate namedParamJdbcTemplate;

    public Status(NamedParameterJdbcTemplate namedParamJdbcTemplate) {
        this.namedParamJdbcTemplate = namedParamJdbcTemplate;
    }

    private static RowMapper<StatusFile> stringRowMapper2 = (resultSet, rowNum) -> {
        return new StatusFile(resultSet.getString("name"), resultSet.getString("action"));
    };

    public StatusResult status2() {
        var result = new StatusResult();

        var sql = """
                SELECT
                    ifnull(%2$s.name, %1$s.name) as 'name',
                    CASE
                           WHEN %1$s.name IS NULL THEN '%3$s'
                           WHEN %2$s.hash != %1$s.hash THEN '%5$s'
                           WHEN %1$s.hash IS NULL THEN '%6$s'
                           WHEN %2$s.name IS NULL THEN '%4$s'
                           ELSE ''
                       END 'action'
                FROM %1$s
                FULL OUTER JOIN %2$s ON
                    %2$s.name = %1$s.name;
        """;
        var sql2 = """
                SELECT
                    COALESCE(%2$s.name, %1$s.name) as 'name',
                    CASE
                           WHEN %1$s.name IS NULL THEN '%3$s'
                           WHEN %2$s.hash != %1$s.hash THEN '%5$s'
                           WHEN %1$s.hash IS NULL THEN '%6$s'
                           WHEN %2$s.name IS NULL THEN '%4$s'
                           ELSE ''
                       END 'action'
                FROM %1$s
                FULL OUTER JOIN %2$s ON
                    %2$s.name = %1$s.name;
                """;
        var res = namedParamJdbcTemplate.query(
                String.format(sql2, Model.FILE_TABLE, Model.TEMP_TABLE, FILE_ADDED, FILE_DELETED, FILE_MODIFIED, FILE_ERASED, ERROR),
                stringRowMapper2
        );

        //System.out.println("status 3:"+res);
        for(var t : res){
            if(t.getAction().equals(FILE_ADDED)){
                result.getAddedFiles().add(t.getName());
            } else if (t.getAction().equals(FILE_MODIFIED)) {
                result.getModifiedFiles().add(t.getName());
            } else if (t.getAction().equals(FILE_DELETED)) {
                result.getDeletedFiles().add(t.getName());
            }else if (t.getAction().equals(FILE_ERASED)) {
                result.getErasedFiles().add(t.getName());
            } else if (t.getAction().equals(ERROR)) {
                //result.getWrongFiles().add(t.getName());
            }
        }
        return result;
    }
}
class Text{
    NamedParameterJdbcTemplate namedParamJdbcTemplate;

    public Text(NamedParameterJdbcTemplate namedParamJdbcTemplate) {
        this.namedParamJdbcTemplate = namedParamJdbcTemplate;
    }

    public static RowMapper<TextFiles> textFileRowMapper = (resultSet, rowNum) -> {

        var textFile = new TextFiles(resultSet.getLong("id"),
                resultSet.getString("name"),
                resultSet.getBytes("content"));
        //textFile.setHash(Optional.ofNullable(resultSet.getLong("hash")).map(Long::longValue).orElse(null));
        textFile.setParent(resultSet.getLong("parent"));
        textFile.setCharset(resultSet.getInt("charset"));
        textFile.setVersion(resultSet.getLong("version"));
        return textFile;
    };
    public List<TextFiles> all(){
        var sql = "SELECT * FROM %s ;".formatted(Model.FILE_TABLE);
        return namedParamJdbcTemplate.query(
                sql,
                textFileRowMapper
        );
    }
    public List<TextFiles> allFilesWithContent(){
        var sql = "SELECT * FROM %s WHERE content IS NOT NULL;".formatted(Model.FILE_TABLE);
        return namedParamJdbcTemplate.query(
                sql,
                textFileRowMapper
        );
    }
    public List<TextFiles> findByName(String name){
        var sql = "SELECT DISTINCT * FROM %s WHERE name = :name AND content IS NOT NULL;".formatted(Model.FILE_TABLE);
        var params = new MapSqlParameterSource("name", name);
        return namedParamJdbcTemplate.query(
                sql,
                params,
                textFileRowMapper
        );
    }
    public int updateTextFile(TextFiles file){
        var sql = """ 
                UPDATE %s SET version = :version, hash = :hash, content = :content, name = :name WHERE id = :id;
                """.formatted(Model.FILE_TABLE);
        return namedParamJdbcTemplate.update(sql, new BeanPropertySqlParameterSource(file));
    }
    public void insertTextFile(TextFiles file){
        var params = new BeanPropertySqlParameterSource(file);
        GeneratedKeyHolder holder = new GeneratedKeyHolder();
        var sql = """
                INSERT INTO %s 
            (name, content, hash, parent, version, charset) 
            values (:name, :content, :hash, :parent, :version, :charset);
                """.formatted(Model.FILE_TABLE);
        namedParamJdbcTemplate.update(sql, params, holder);
        file.setId(holder.getKey().longValue());
    }
    public Long getVersionByName(String name){
        var sql = "SELECT DISTINCT * from %s WHERE name = :name;".formatted(Model.FILE_TABLE);
        var params = new MapSqlParameterSource("name", name);
        var result = namedParamJdbcTemplate.query(
                sql,
                params,
                textFileRowMapper
        );
        if(result.isEmpty()){
            return 0L;
        }else {
            return result.iterator().next().getVersion();
        }
    }
    public List<TextFiles> getFilesAfterVersion(Long version){
        var sql = "SELECT DISTINCT * from %s WHERE version > :version;".formatted(Model.FILE_TABLE);
        var params = new MapSqlParameterSource("version", version);
        return namedParamJdbcTemplate.query(
                sql,
                params,
                textFileRowMapper
        );
    }
    public void dropTable(){
        namedParamJdbcTemplate.getJdbcTemplate().execute("DROP TABLE IF EXISTS %s;".formatted(Model.FILE_TABLE));
    }
    public void createTable(){
        namedParamJdbcTemplate.getJdbcTemplate().execute(Model.createTABLE.formatted(Model.FILE_TABLE));
    }

    public List<TextFiles> findByIds(List<Long> ids) {
        SqlParameterSource parameters = new MapSqlParameterSource("ids", ids);
        var sql = "SELECT * FROM %s WHERE id IN (:ids);".formatted(Model.FILE_TABLE);
        return namedParamJdbcTemplate.query(
                sql,
                parameters,
                textFileRowMapper
        );
    }

    public Map<Long, TextFiles> findAsMapByIds(List<Long> ids){
        SqlParameterSource parameters = new MapSqlParameterSource("ids", ids);
        var sql = "SELECT * FROM %s WHERE id IN (:ids);".formatted(Model.FILE_TABLE);
        var result = new HashMap<Long, TextFiles>(ids.size());
        var founded = namedParamJdbcTemplate.query(
                sql,
                parameters,
                textFileRowMapper
        );
        founded.forEach(it -> result.put(it.getId(), it));
        return result;
    }

    public int deleteByIds(List<Long> ids) {
        SqlParameterSource parameters = new MapSqlParameterSource("ids", ids);
        var sql = "DELETE FROM %s WHERE id IN (:ids);".formatted(Model.FILE_TABLE);
        return namedParamJdbcTemplate.update(sql, parameters);
    }

    public int insertFiles(List<TextFiles> textFiles) {
        var sql = """
                    INSERT INTO %s 
            (id, name, content, hash, parent, version, charset) 
            values (:id, :name, :content, :hash, :parent, :version, :charset);
                    """.formatted(Model.FILE_TABLE);
        SqlParameterSource[] parameters = SqlParameterSourceUtils
                .createBatch(textFiles.toArray());
        namedParamJdbcTemplate.batchUpdate(sql, parameters);
        return 0;
    }
}
class Temp {
    NamedParameterJdbcTemplate namedParamJdbcTemplate;

    public Temp(NamedParameterJdbcTemplate namedParamJdbcTemplate) {
        this.namedParamJdbcTemplate = namedParamJdbcTemplate;
    }

    public static RowMapper<ATempFiles> fileRowMapper = (resultSet, rowNum) -> {
        var file = new ATempFiles(resultSet.getLong("id"),
                resultSet.getString("name"),
                resultSet.getBytes("content"));
        file.setHash(resultSet.getLong("hash"));
        return file;
    };

    public List<ATempFiles> findByName(String name){
        var sql = "SELECT DISTINCT * from %s WHERE name = :name;".formatted(Model.TEMP_TABLE);
        var params = new MapSqlParameterSource("name", name);
        return namedParamJdbcTemplate.query(
                sql,
                params,
                fileRowMapper
        );
    }

    public List<ATempFiles> all(){
        var sql = "SELECT * FROM %s;".formatted(Model.TEMP_TABLE);
        return namedParamJdbcTemplate.query(
                sql,
                fileRowMapper
        );
    }

    public void add(Path rootRepo, Set<String> ignoreDirs, Set<String> skipFiles) throws IOException {
        var allFiles = FSUtils.getAllFiles6(rootRepo, skipFiles, ignoreDirs);
        System.out.println("all files:"+allFiles.keySet());
        insertTempFiles(allFiles);
    }

    public void insertTempFiles(Map<String, byte[]> files){
        var sql = """
                    INSERT INTO %s 
            (name, content, hash) 
            values (:name, :content, :hash);
                    """.formatted(Model.TEMP_TABLE);
        //namedParamJdbcTemplate.update(sql, params, holder);
        var list = new ArrayList<ATempFiles>(files.size());
        for(var entity : files.entrySet()){
            var temp = new ATempFiles(0L, entity.getKey(), entity.getValue());
            list.add(temp);
        }
        SqlParameterSource[] parameters = SqlParameterSourceUtils
                .createBatch(list.toArray());
        namedParamJdbcTemplate.batchUpdate(sql, parameters);
    }

    public void dropTable(){
        namedParamJdbcTemplate.getJdbcTemplate().execute("DROP TABLE IF EXISTS %s;".formatted(Model.TEMP_TABLE));
    }

    public void createTable(){
        namedParamJdbcTemplate.getJdbcTemplate().execute(Model.createTEMP_TABLE.formatted(Model.TEMP_TABLE));
    }
}
class StatusFile {
    @Expose
    String name;
    @Expose
    String action;
    public StatusFile(String name, String action) {
        this.name = name;
        this.action = action;
    }

    public StatusFile() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    @Override
    public String toString() {
        return "StatusFile{" +
                "name='" + name + '\'' +
                ", action='" + action + '\'' +
                '}';
    }
}