package gitlet;
import java.io.File;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.ArrayList;
/** class.
 * @author Boaz */
public class Commit implements Serializable {
    /** cwd.*/
    private static File cwd = new File(System.getProperty("user.dir"));
    /** fileref. */
    private HashMap<String, String> fileRef;
    /** msg. */
    private String message;
    /** time. */
    private String time;
    /** parents. */
    private ArrayList<String> parents;
    /** sha1. */
    private String sha1;
    /** constructor.
     *  @param msg is the given message
     *  @param parent1 is the given parent*/
    Commit(String msg, String parent1) {
        parents = new ArrayList<String>();
        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("E MMM dd HH:mm:ss yyyy -0800");
        this.message = msg;
        String sec = (String) Utils.readContentsAsString
                (Utils.join(cwd, ".gitlet/secondparent"));
        if (parent1 != null) {
            LocalDateTime D = LocalDateTime.now();
            this.time = D.format(formatter);
            this.parents.add(parent1);
        } else {
            this.time = "Thu Jan 1 00:00:00 1970 -0800";
        }
        if (!sec.isBlank()) {
            this.parents.add(sec);
        }
        this.sha1 = makeSha1();
        fileRef = new HashMap<String, String>();
        Main.serialize();
    }
    /** make sha1.
     * @return the sha1id */
    public String makeSha1() {
        byte[] str = Utils.serialize(this);
        return Utils.sha1(str);
    }
    /** get sha1.
     * @return the actual id */
    public String getSha1() {
        return this.sha1;
    }
    /** get msg.
     * @return the message */
    public String getMessage() {
        return this.message;
    }
    /** get time.
     * @return the time */
    public String getTime() {
        return this.time;
    }
    /** get parent.
     * @return parent of commit */
    public String getParent() {
        if (this.parents.size() == 0) {
            return null;
        }
        return this.parents.get(0);
    }
    /** get second parent.
     * @return the second parent*/
    public String getSecondParent() {
        if (this.parents.size() != 2) {
            return null;
        }
        return this.parents.get(1);
    }
    /** get fileref.
     * @return file ref*/
    public HashMap<String, String> getFileRef() {
        return this.fileRef;
    }
    /** get put in filref.
     * @param key key values
     * @param value values of keys.*/
    public void putFileRef(String key, String value) {
        this.fileRef.put(key, value);
    }



}
