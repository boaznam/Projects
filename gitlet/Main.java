package gitlet;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Collections;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Boaz
 */
public class Main implements Serializable {
    /** cwd. */
    private static File cwd = new File(System.getProperty("user.dir"));
    /** blob files. */
    private static File blobs = new File(".gitlet/blobs");
    /** stage file. */
    private static File stage = new File(".gitlet/Staging");
    /** identify with commitID and contains commit OBJECT. */
    private static File commitFile = new File(".gitlet/commit");
    /** Current commit same thing as head. */
    private static Commit curr = null;
    /** String that tells what is the current branch. */
    private static String currentBranch;
    /**  staged to add. */
    private static HashMap<String, String> addMap = new HashMap<>();
    /** staged to remove. */
    private static ArrayList<String> removeList = new ArrayList<>();
    /** HashMap of Branches. */
    private static HashMap<String, String> branchMap = new HashMap<>();
    /** Latest ancestor. */
    private static ArrayList<String> ancestorList = new ArrayList<>();
    /** Current commit. */
    private static Commit head = null;
    /** second parent. */
    private static String sec = "";
    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) throws IOException {
        try {
            if (args.length == 0) {
                throw new GitletException("Please enter a command.");
            } else if (args[0].equals("init")) {
                init();
            } else if (args[0].equals("commit")) {
                commit(args[1]);
            } else if (args[0].equals("add")) {
                add(args[1]);
            } else if (args[0].equals("log")) {
                log();
            } else if (args[0].equals("checkout")) {
                checkout(args);
            } else if (args[0].equals("branch")) {
                branch(args[1]);
            } else if (args[0].equals("global-log")) {
                globalLog();
            } else if (args[0].equals("status")) {
                status();
            } else if (args[0].equals("rm")) {
                rm(args[1]);
            } else if (args[0].equals("find")) {
                find(args[1]);
            } else if (args[0].equals("rm-branch")) {
                rmbranch(args[1]);
            } else if (args[0].equals("reset")) {
                reset(args[1]);
            } else if (args[0].equals("merge")) {
                merge(args[1]);
            } else if (args[0].equals("add-remote")) {
                addRemote(args[1], args[2]);
            } else if (args[0].equals("rm-remote")) {
                rmRemote(args[1]);
            } else if (args[0].equals("pull")) {
                pull(args[1], args[2]);
            } else if (args[0].equals("fetch")) {
                fetch(args[1], args[2]);
            } else if (args[0].equals("push")) {
                push(args[1], args[2]);
            } else {
                throw new GitletException("No command with that name exists.");
            }
        } catch (GitletException excp) {
            System.err.printf(excp.getMessage());
        }
        System.exit(0);
    }
    /** create a master branch and head pointer.
     * make a gitlet repository .gitlet */
    public static void init() throws IOException {
        File gitlet = new File(cwd, ".gitlet");
        if (gitlet.exists()) {
            throw new GitletException
            ("A Gitlet version-control system already "
                 + "exists in the current directory.");
        }
        gitlet.mkdir();
        addMap = new HashMap<String, String>();
        removeList = new ArrayList<String>();
        branchMap = new HashMap<String, String>();
        blobs.mkdir();
        stage.mkdir();
        commitFile.mkdir();
        currentBranch = "master";
        serialize();
        Commit initial = new Commit("initial commit", null);
        deserialize();
        ancestorList.add(initial.getSha1());
        serialize();
        head = initial;
        curr = initial;
        branchMap.put("master", initial.getSha1());
        serialize();
        Utils.writeObject(Utils.join(".gitlet/commit",
                initial.getSha1()), initial);

    }
    /** stages a file for addition. contents of the file are called blobs.
     * @param file gives file */
    public static void add(String file) {
        deserialize();
        File addedFile = new File(file);
        if (addedFile.exists()) {
            byte[] blob = Utils.readContents(addedFile);
            String blobSha = Utils.sha1(blob);
            if (curr.getFileRef().containsKey(file)
                    && curr.getFileRef().get(file).equals(blobSha)) {
                if (removeList.contains(file)) {
                    removeList.remove(file);
                    serialize();
                }
                return;
            }
            if (removeList.contains(file)) {
                removeList.remove(file);
            }
            File blobs1 = new File(".gitlet/blobs/" + blobSha);
            addMap.put(file, blobSha);
            Utils.writeContents(blobs1, blob);
            serialize();
        } else {
            throw new GitletException("File does not exist.");
        }



    }
    /** clones the first commit from the init.
     * @param msg message user inputs */
    public static void commit(String msg) {
        deserialize();
        Commit clone = curr;
        if (addMap.isEmpty() && removeList.isEmpty()) {
            throw new GitletException("No changes added to the commit.");
        } else if (msg.equals("")) {
            throw new GitletException("Please enter a commit message.");
        }
        Commit newCommit = new Commit(msg, clone.getSha1());
        for (Map.Entry<String, String> entry : clone.getFileRef().entrySet()) {
            newCommit.putFileRef(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<String, String> entry : addMap.entrySet()) {
            newCommit.putFileRef(entry.getKey(), entry.getValue());
        }
        for (String files : removeList) {
            newCommit.getFileRef().remove(files);
        }
        head = newCommit;
        curr = newCommit;
        branchMap.put(currentBranch, newCommit.getSha1());
        Utils.writeObject(Utils.join(".gitlet/commit",
                newCommit.getSha1()), newCommit);
        addMap = new HashMap<String, String>();
        removeList = new ArrayList<String>();
        sec = "";
        serialize();
    }

    /** prints out the log. */
    public static void log() {
        deserialize();
        Commit currCommit = curr;
        while (curr != null) {
            System.out.println("===");
            System.out.println("commit " + currCommit.getSha1());
            if (currCommit.getSecondParent() != null) {
                System.out.println("Merge: "
                        + currCommit.getParent().substring(0, 7)
                        + " " + currCommit.getSecondParent().substring(0, 7));
            }
            System.out.println("Date: " + currCommit.getTime());
            System.out.println(currCommit.getMessage());
            System.out.println();
            if (currCommit.getParent() != null) {
                currCommit = (Commit) Utils.readObject(
                        new File(".gitlet/commit/"
                                + currCommit.getParent()), Commit.class);
            } else {
                serialize();
                break;
            }
        }
        serialize();
    }
    /** checkout helper.
     * @param branchName name of branch
     */
    public static void checkoutHelper(String branchName) {
        deserialize();
        if (!branchMap.containsKey(branchName)) {
            throw new GitletException("No such branch exists.");
        }
        if (currentBranch.equals(branchName)) {
            throw new GitletException("No need to checkout "
                    + "the current branch.");
        }
        String commitID = branchMap.get(currentBranch);
        File commitfiles = new File(commitFile + "/" + commitID);
        Commit newCommit = Utils.readObject(commitfiles, Commit.class);
        HashMap<String, String> fileref = newCommit.getFileRef();
        List<String> fileList = Utils.plainFilenamesIn(cwd);
        String newCommitID = branchMap.get(branchName);
        File newcommitfiles = new File(commitFile + "/" + newCommitID);
        Commit newCommit1 = Utils.readObject(newcommitfiles, Commit.class);
        HashMap<String, String> newFileref = newCommit1.getFileRef();

        for (int i = 0; i < fileList.size(); i++) {
            if (!fileref.containsKey(fileList.get(i))
                    && newFileref.containsKey(fileList.get(i))) {
                throw new GitletException("There is an untracked file in"
                        + " the way; delete it, "
                        + "or add and commit it first.");
            }
        }
        for (int i = 0; i < fileList.size(); i++) {
            Utils.restrictedDelete(Utils.join(cwd, fileList.get(i)));
        }
        for (Map.Entry<String, String> entry : newFileref.entrySet()) {
            File path = new File(blobs + "/" + entry.getValue());
            byte [] contents = Utils.readContents(path);
            Utils.writeContents(Utils.join(cwd.getPath(),
                    entry.getKey()), contents);
        }
        addMap = new HashMap<String, String>();
        removeList = new ArrayList<String>();
        currentBranch = branchName;
        head = newCommit1;
        curr = newCommit1;
        branchMap.put(currentBranch, head.getSha1());
        serialize();
    }
    /** checksout depending on inputs.
     * @param args list of args */
    public static void checkout(String... args) {
        if (args.length == 2) {
            String branchName = args[1];
            deserialize();
            checkoutHelper(branchName);
        }
        if (args.length == 3) {
            String file = args[2];
            deserialize();
            Commit comhead = head;
            File blobFile = (new File(cwd.getPath() + "/" + file));
            if (!comhead.getFileRef().containsKey(file)) {
                throw new GitletException("File does not "
                        + "exist in that commit.");
            }
            if (blobFile.exists()) {
                File path = new File(blobs + "/"
                        + comhead.getFileRef().get(file));
                byte[] contents = Utils.readContents(path);
                Utils.writeContents(Utils.join(cwd.getPath(), file), contents);
                serialize();
            }
        }
        if (args.length == 4) {
            deserialize();
            if (!args[2].equals("--")) {
                throw new GitletException("Incorrect operands.");
            }
            String id = args[1];
            List<String> commitList = Utils.plainFilenamesIn(commitFile);
            for (int i = 0; i < commitList.size(); i++) {
                if (id.regionMatches(0, commitList.get(i), 0, id.length())) {
                    id = commitList.get(i);
                    break;
                }
            }
            String file = args[3];
            File idfile = new File(commitFile + "/" + id);
            if (idfile.exists()) {
                Commit commitContents = Utils.readObject(idfile, Commit.class);
                if (!commitContents.getFileRef().containsKey(file)) {
                    throw new GitletException
                    ("File does not exist in that commit");
                }
                Utils.restrictedDelete(Utils.join(cwd.getPath(), file));
                File test = Utils.join(cwd.getPath(), file);
                String sha1 = commitContents.getFileRef().get(file);
                File path = new File(blobs + "/" + sha1);
                byte[] contents = Utils.readContents(path);
                Utils.writeContents(test, contents);
                serialize();
            } else {
                throw new GitletException("No commit with that id exists");
            }
        }
    }

    /** abritrary checkout.
     * @param id id*/
    public static void reset(String id) {
        deserialize();
        List<String> commitFiles = Utils.plainFilenamesIn(commitFile);
        if (!commitFiles.contains(id)) {
            throw new GitletException("No commit with that id exists");
        }
        Commit commitContents =
                Utils.readObject(Utils.join(commitFile, id), Commit.class);
        Commit currCommit = curr;
        List<String> fileList = Utils.plainFilenamesIn(cwd);
        for (String files : fileList) {
            if (!currCommit.getFileRef().containsKey(files)
                    && commitContents.getFileRef().containsKey(files)) {
                throw new GitletException
                ("There is an untracked file in the way;"
                                + " delete it, or add and commit it first.");
            }
        }
        for (String files : fileList) {
            if (!commitContents.getFileRef().containsKey(files)) {
                Utils.restrictedDelete(Utils.join(cwd, files));
            }

        }
        for (String files : commitContents.getFileRef().keySet()) {
            String blobsha = commitContents.getFileRef().get(files);
            File blobp = new File(blobs + "/" + blobsha);
            byte[] blobcontent = Utils.readContents(blobp);
            Utils.writeContents(Utils.join(cwd, files), blobcontent);
        }
        branchMap.put(currentBranch, commitContents.getSha1());
        head = commitContents;
        curr = commitContents;
        addMap = new HashMap<String, String>();
        removeList = new ArrayList<String>();
        serialize();
    }

    /** creates new branch.
     * @param branchName branch name */
    public static void branch(String branchName) {
        deserialize();
        if (branchMap.containsKey(branchName)) {
            throw new GitletException("A branch with that name already exists");
        }
        branchMap.put(branchName, head.getSha1());
        if (!ancestorList.contains(head.getSha1())) {
            ancestorList.add(head.getSha1());
        }
        serialize();
    }

    /** prints log of everything. */
    public static void globalLog() {
        deserialize();
        List<String> commitID = Utils.plainFilenamesIn(commitFile);
        for (int i = 0; i < commitID.size(); i++) {
            Commit commits = Utils.readObject(Utils.join
                    (commitFile, commitID.get(i)), Commit.class);
            System.out.println("===");
            System.out.println("commit " + commits.getSha1());
            System.out.println("Date: " + commits.getTime());
            System.out.println(commits.getMessage());
            System.out.println();
        }
        serialize();

    }

    /** helper for merge.
     * @param currcommit current commit
     * @param split the split point string
     * @return boolean value */
    public static boolean hasPath(Commit currcommit, String split) {
        ArrayList<String> parents = new ArrayList<>();
        if (currcommit.getParent() != null) {
            parents.add(currcommit.getParent());
        }
        if (currcommit.getSecondParent() != null) {
            parents.add(currcommit.getSecondParent());
        }
        String commitID = currcommit.getSha1();
        if (split.equals(commitID)) {
            return true;
        }
        for (String id : parents) {
            File branchFile = new File(commitFile + "/" + id);
            Commit branchCommit = Utils.readObject(branchFile, Commit.class);
            if (hasPath(branchCommit, split)) {
                return true;
            }
        }
        return false;
    }

    /** other helper for merge.
     * @param valid valid string
     * @param commit Current commit
     * @return int value*/
    public static int distance(String valid, Commit commit) {
        ArrayList<String> parents = new ArrayList<>();
        parents.add(commit.getParent());
        if (commit.getSecondParent() != null) {
            parents.add(commit.getSecondParent());
        }
        String commitID = commit.getSha1();
        if (valid.equals(commitID)) {
            return 1;
        }
        for (String id : parents) {
            File parentfile = new File(commitFile + "/" + id);
            Commit parentCommit = Utils.readObject(parentfile, Commit.class);
            if (hasPath(parentCommit, valid)) {
                return 1 + distance(valid, parentCommit);
            }
        }
        return 0;
    }

    /** other helper for merge.
     * @param currBranch current branch
     * @param givenBranch given branch
     * @return a string value*/
    public static String bestSplit(String currBranch, String givenBranch) {
        deserialize();
        String currBranchID =
                branchMap.get(currBranch);
        String givenBranchID =
                branchMap.get(givenBranch);
        File currBranchFile =
                new File(commitFile + "/" + currBranchID);
        File givenBranchFile =
                new File(commitFile + "/" + givenBranchID);
        Commit currBranchCommit =
                Utils.readObject(currBranchFile, Commit.class);
        Commit givenBranchCommit =
                Utils.readObject(givenBranchFile, Commit.class);
        ArrayList<String> validSplit =  new ArrayList<>();
        for (String possSplit : ancestorList) {
            if (hasPath(currBranchCommit, possSplit)) {
                validSplit.add(possSplit);
            }
        }
        ArrayList<String> temp = new ArrayList<>();
        for (String possSplit : validSplit) {
            if (hasPath(givenBranchCommit, possSplit)) {
                continue;
            } else {
                temp.add(possSplit);
            }
        }
        for (String s : temp) {
            validSplit.remove(s);
        }
        int test = Integer.MAX_VALUE;
        String best = "";
        for (String valid : validSplit) {
            int count = distance(valid, currBranchCommit);
            if (count <= test) {
                test = count;
                best = valid;
            }
        }
        return best;
    }
    /** helper for failure.
     * @param branchName name of branch */
    public static void failure(String branchName) {
        deserialize();
        if (!addMap.isEmpty() || !removeList.isEmpty()) {
            throw new GitletException("You have uncommitted changes.");
        }
        if (!branchMap.containsKey(branchName)) {
            throw new GitletException("A branch with "
                    + "that name does not exist.");
        }
        if (currentBranch.equals(branchName)) {
            throw new GitletException("Cannot merge a branch with itself.");
        }
    }

    /** big helper for merge.
     *
     * @param combined combined
     * @param splitCommit split commit
     * @param currbranchcommit curr branch commit
     * @param givenbranchcommit given branch commit
     */
    public static void mergeCombinedHelper(ArrayList<String> combined,
                                           Commit splitCommit,
                                           Commit currbranchcommit,
                                           Commit givenbranchcommit) {
        for (String s : splitCommit.getFileRef().keySet()) {
            combined.add(s);
        }
        for (String s : currbranchcommit.getFileRef().keySet()) {
            if (combined.contains(s)) {
                continue;
            } else {
                combined.add(s);
            }
        }
        for (String s : givenbranchcommit.getFileRef().keySet()) {
            if (combined.contains(s)) {
                continue;
            } else {
                combined.add(s);
            }
        }
    }

    /** big helper.
     *
     * @param givenbranchcommit given branch commit
     * @param currbranchcommit curr branch commit
     * @param s String s
     * @param splitCommit split commit
     */
    public static void mergeBigHelper(Commit givenbranchcommit,
                                      Commit currbranchcommit,
                                      String s, Commit splitCommit) {
        if (modified1(s, splitCommit, givenbranchcommit)
                && !(modified1(s, splitCommit, currbranchcommit))) {
            String[] temp = {"checkout",
                    givenbranchcommit.getSha1(), "--", s};
            checkout(temp);
        }
        if (!splitCommit.getFileRef().containsKey(s)
                && !currbranchcommit.getFileRef().containsKey(s)
                && givenbranchcommit.getFileRef().containsKey(s)) {
            String[] temp = {"checkout",
                    givenbranchcommit.getSha1(), "--", s};
            checkout(temp);
            add(s);
        }
        if (splitCommit.getFileRef().containsKey(s)
                && !modified(s, splitCommit, currbranchcommit)
                && !givenbranchcommit.getFileRef().containsKey(s)) {
            removeList.add(s);
            Utils.restrictedDelete(Utils.join(cwd, s));
        }
        if (conflict(s, givenbranchcommit, currbranchcommit, splitCommit)) {
            if (!givenbranchcommit.getFileRef().containsKey(s)) {
                String currcontent = Utils.readContentsAsString
                        (Utils.join(blobs,
                                currbranchcommit.getFileRef().get(s)));
                String text = "<<<<<<< HEAD\n" + currcontent
                        + "=======\n" + "" + ">>>>>>>\n";
                Utils.writeContents(Utils.join(cwd, s), text);
            } else if (!currbranchcommit.getFileRef().containsKey(s)) {
                String branchcontent = Utils.readContentsAsString
                        (Utils.join(blobs,
                                givenbranchcommit.getFileRef().get(s)));
                String text = "<<<<<<< HEAD\n" + ""
                        + "=======\n" + branchcontent + ">>>>>>>\n";
                Utils.writeContents(Utils.join(cwd, s), text);
            } else {
                String currcontent = Utils.readContentsAsString
                        (Utils.join(blobs,
                                currbranchcommit.getFileRef().get(s)));
                String branchcontent = Utils.readContentsAsString
                        (Utils.join(blobs,
                                givenbranchcommit.getFileRef().get(s)));
                String text = "<<<<<<< HEAD\n" + currcontent
                        + "=======\n" + branchcontent + ">>>>>>>\n";
                Utils.writeContents(Utils.join(cwd, s), text);
            }
            add(s);
            System.out.println("Encountered a merge conflict");
        }
    }
    /** merges branches.
     * @param branchName name of branch */
    public static void merge(String branchName) {
        deserialize();
        failure(branchName);
        String commitID = branchMap.get(branchName);
        File commitfiles = new File(commitFile + "/" + commitID);
        Commit currCommit = curr;
        Commit branchCommit = Utils.readObject(commitfiles, Commit.class);
        List<String> fileList = Utils.plainFilenamesIn(cwd);
        for (String files : fileList) {
            if (!currCommit.getFileRef().containsKey(files)
                    && branchCommit.getFileRef().containsKey(files)) {
                throw new GitletException("There is an "
                        + "untracked file in the way; "
                        + "delete it, or add and commit it first.");
            }
        }
        if (hasPath(currCommit, branchCommit.getSha1())) {
            System.out.println("Given branch is an "
                    + "ancestor of the current branch.");
            return;
        }
        String splitpoint = bestSplit(currentBranch, branchName);
        if (splitpoint.equals(branchMap.get(currentBranch))) {
            String[] test = {"checkout", branchName};
            checkout(test);
            System.out.println("Current branch fast-forwarded.");
            return;
        }
        Commit splitCommit = (Commit) Utils.readObject
                (new File(".gitlet/commit/"
                + splitpoint), Commit.class);
        HashMap<String, String> bestCommitHashMap =
                splitCommit.getFileRef();
        Commit currbranchcommit = (Commit) Utils.readObject
                (new File(".gitlet/commit/"
                + branchMap.get(currentBranch)), Commit.class);
        HashMap<String, String> currCommitHashMap =
                currbranchcommit.getFileRef();
        Commit givenbranchcommit = (Commit) Utils.readObject(new File(
                ".gitlet/commit/"
                        + branchMap.get(branchName)), Commit.class);
        HashMap<String, String> givenbranchHashMap =
                givenbranchcommit.getFileRef();
        ArrayList<String> combined = new ArrayList<>();
        mergeCombinedHelper(combined, splitCommit,
                currbranchcommit, givenbranchcommit);
        for (String s : combined) {
            deserialize();
            mergeBigHelper(givenbranchcommit, currbranchcommit, s, splitCommit);
            serialize();
        }
        sec = givenbranchcommit.getSha1();
        Utils.writeContents(Utils.join(cwd, ".gitlet/secondparent"), sec);
        commit("Merged " + branchName + " into " + currentBranch + ".");
        ancestorList.add(givenbranchcommit.getSha1());
        Utils.writeContents(Utils.join(cwd, ".gitlet/secondparent"), "");
        serialize();
    }

    /** helper for merge.
     * @param s string
     * @param givencommit given commit
     * @param currcommit1 curent commit
     * @param split split commit
     * @return boolean value*/
    public static boolean conflict(String s, Commit givencommit,
                                   Commit currcommit1, Commit split) {
        if (!givencommit.getFileRef().containsKey(s)
                && !currcommit1.getFileRef().containsKey(s)) {
            return false;
        }
        if (modified(s, split, givencommit)
                && modified(s, split, currcommit1)) {
            if ((!currcommit1.getFileRef().containsKey(s)
                    && givencommit.getFileRef().containsKey(s))
                    || (currcommit1.getFileRef().containsKey(s)
                    && !givencommit.getFileRef().containsKey(s))
                    || !currcommit1.getFileRef().get(s).equals
                    (givencommit.getFileRef().get(s))) {
                return true;
            }
            return false;
        }
        return false;
    }

    /** helper for merge.
     * @param file string
     * @param split given commit
     * @param compare curent commit
     * @return boolean value*/
    public static boolean modified(String file, Commit split, Commit compare) {
        if (split.getFileRef().containsKey(file)
                && compare.getFileRef().containsKey(file)) {
            return !split.getFileRef().get(file).equals
                    (compare.getFileRef().get(file));
        } else if (!split.getFileRef().containsKey(file)
                && compare.getFileRef().containsKey(file)) {
            return true;
        } else {
            return split.getFileRef().containsKey(file)
                    && !compare.getFileRef().containsKey(file);
        }
    }

    /** helper for merge.
     * @param file string
     * @param split given commit
     * @param compare curent commit
     * @return boolean value*/
    public static boolean modified1(String file, Commit split, Commit compare) {
        if (split.getFileRef().containsKey(file)
                && compare.getFileRef().containsKey(file)) {
            return !split.getFileRef().get(file).equals
                    (compare.getFileRef().get(file));
        } else {
            return false;
        }
    }

    /** status helper 1.
     *
     * @param deleted deleted files
     * @param modified modified files
     */
    public static void statusHelper(ArrayList<String> deleted,
                                    ArrayList<String> modified) {
        for (Map.Entry<String, String> entry : curr.getFileRef().entrySet()) {
            if (!removeList.contains(entry.getKey())
                    && !Utils.join(cwd, entry.getKey()).exists()) {
                deleted.add(entry.getKey() + " (deleted)");
            } else if (Utils.join(cwd, entry.getKey()).exists()) {
                byte[] blob = Utils.readContents(Utils.join(cwd,
                        entry.getKey()));
                String blobSha = Utils.sha1(blob);
                if ((!addMap.containsKey(entry.getKey())
                        && !removeList.contains(entry.getKey()))
                        && !(blobSha.equals(entry.getValue()))) {
                    modified.add(entry.getKey() + " (modified)");
                }
            }
        }
    }

    /** status helper 2.
     *
     * @param deleted deleted files
     * @param modified modfied files
     */
    public static void statusHelper2(ArrayList<String> deleted,
                                     ArrayList<String> modified) {
        for (Map.Entry<String, String> entry : addMap.entrySet()) {
            if (!Utils.join(cwd, entry.getKey()).exists()) {
                deleted.add(entry.getKey() + " (deleted)");
            } else if (Utils.join(cwd, entry.getKey()).exists()) {
                byte[] blob = Utils.readContents(Utils.join(cwd,
                        entry.getKey()));
                String blobSha = Utils.sha1(blob);
                if (!(entry.getValue().equals(blobSha))) {
                    modified.add(entry.getKey() + " (modified)");
                }
            }
        }
    }

    /** status helper 3.
     *
     * @param deleted deleted files
     * @param modified modified files
     * @param untracked untracked files
     */
    public static void statusHelper3(ArrayList<String> deleted,
                                     ArrayList<String> modified,
                                     ArrayList<String> untracked) {
        System.out.println("=== Branches ===");
        System.out.println("*" + currentBranch);
        ArrayList<String> temp = new ArrayList<>();
        for (String key : branchMap.keySet()) {
            if (key.equals(currentBranch)) {
                continue;
            } else {
                temp.add(key);
            }
        }
        Collections.sort(temp);
        for (String key : temp) {
            System.out.println(key);
        }
        System.out.println("");
        System.out.println("=== Staged Files ===");
        ArrayList<String> temp1 = new ArrayList<>();
        for (String key : addMap.keySet()) {
            temp1.add(key);
        }
        Collections.sort(temp1);
        for (String key : temp1) {
            System.out.println(key);
        }
        System.out.println("");
        System.out.println("=== Removed Files ===");
        Collections.sort(removeList);
        for (String key : removeList) {
            System.out.println(key);
        }
        System.out.println("");
        System.out.println("=== Modifications Not Staged For Commit ===");
        for (String key : modified) {
            System.out.println(key);
        }
        System.out.println("");
        System.out.println("=== Untracked Files ===");
        for (String key : untracked) {
            System.out.println(key);
        }
    }

    /** shows current status. */
    public static void status() {
        File git = new File(cwd, ".gitlet");
        if (!git.exists()) {
            throw new GitletException("Not in an initialized "
                    + "Gitlet directory.");
        }
        deserialize();
        ArrayList<String> modified = new ArrayList<>();
        ArrayList<String> deleted = new ArrayList<>();
        ArrayList<String> untracked = new ArrayList<>();
        statusHelper(deleted, modified);
        statusHelper2(deleted, modified);
        List<String> cwdList = Utils.plainFilenamesIn(cwd);
        Collections.sort(cwdList);
        for (String str : deleted) {
            modified.add(str);
        }
        Collections.sort(modified);
        for (String cwdName : cwdList) {
            if (!addMap.containsKey(cwdName)
                    && !curr.getFileRef().containsKey(cwdName)) {
                untracked.add(cwdName);
            }
        }
        statusHelper3(deleted, modified, untracked);
        System.out.println("");
        serialize();
    }

    /** removes files.
     * @param file File string*/
    public static void rm(String file) {
        deserialize();
        if (!addMap.containsKey(file) && !head.getFileRef().containsKey(file)) {
            throw new GitletException("No reason to remove the file.");
        } else if (addMap.containsKey(file)) {
            addMap.remove(file, addMap.get(file));
        } else if (head.getFileRef().containsKey(file)) {
            removeList.add(file);
            Utils.restrictedDelete(cwd + "/" + file);
        }
        serialize();
    }

    /** removes branch.
     * @param branch branch name*/
    public static void rmbranch(String branch) {
        deserialize();
        if (!branchMap.containsKey(branch)) {
            throw new GitletException("A branch with that name "
                    + "does not exist.");
        }
        if (branch.equals(currentBranch)) {
            throw new GitletException("Cannot remove the current branch.");
        }
        branchMap.remove(branch, branchMap.get(branch));
        serialize();
    }

    /** finds file.
     * @param msg msg*/
    public static void find(String msg) {
        deserialize();
        List<String> lst = Utils.plainFilenamesIn(commitFile);
        int count = 0;
        for (String id : lst) {
            Commit commits = Utils.readObject(Utils.join(commitFile, id),
                   Commit.class);
            if (commits.getMessage().equals(msg)) {
                System.out.println(commits.getSha1());
                count++;
            }
        }
        if (count == 0) {
            throw new GitletException("Found no commit with that message.");
        }
    }

    /** add remote.
     *
     * @param remoteName remote name
     * @param remoteDirectory remote directory
     */
    public static void addRemote(String remoteName, String remoteDirectory) {
        deserialize();
        if (Utils.join(new File("../", "saved"), remoteName).exists()) {
            throw new GitletException
            ("A remote with that name already exists.");
        }
        File remote = new File("../", "saved");
        remote.mkdir();
        File remotePath = Utils.join(remote, remoteName);
        remoteDirectory = remoteDirectory.replace("/", File.separator);
        Utils.writeContents(remotePath, remoteDirectory);
        serialize();
    }

    /** rmremote.
     *
     * @param remoteName remote name
     */
    public static void rmRemote(String remoteName) {
        deserialize();
        if (!Utils.join(new File("../", "saved"), remoteName).exists()) {
            throw new GitletException
            ("A remote with that name does not exist.");
        }
        File remote = new File("../", "saved");
        File remotePath = Utils.join(remote, remoteName);
        remotePath.delete();
        serialize();
    }

    /** push.
     *
     * @param remoteName remote name
     * @param remoteBranch remote branch
     */
    public static void push(String remoteName, String remoteBranch) {
        deserialize();
        File remote = new File("../", "saved");
        File remotePath = Utils.join(remote, remoteName);
        String remoteDirectoryPath = Utils.readContentsAsString(remotePath);
        File remoteDirectoryBranch = new File(remoteDirectoryPath, "/branch");
        Commit currcommit = curr;
        boolean check = false;
        if (!new File(remoteDirectoryPath).exists()) {
            throw new GitletException("Remote directory not found.");
        }
        HashMap<String, String> remoteBranchMap
                = Utils.readObject(remoteDirectoryBranch, HashMap.class);
        String remoteBranchID = remoteBranchMap.get(remoteBranch);
        while (currcommit != null) {
            if (remoteBranchID.equals(currcommit.getSha1())) {
                check = true;
                break;
            }
            if (currcommit.getParent() != null) {
                currcommit = (Commit) Utils.readObject
                (new File(".gitlet/commit/"
                        + currcommit.getParent()), Commit.class);
            } else {
                break;
            }
        }
        if (check) {
            remoteBranchMap.put(remoteBranch, head.getSha1());
            Utils.writeObject(Utils.join
                    (remoteDirectoryPath, "branch"), remoteBranchMap);
            Utils.writeObject(Utils.join
                    (remoteDirectoryPath,  "curr"), curr);
        } else {
            throw new GitletException
            ("Please pull down remote changes before pushing.");
        }
        serialize();
    }

    /** fetch.
     *
     * @param remoteName remote name
     * @param remoteBranch remote branch
     */
    public static void fetch(String remoteName, String remoteBranch) {
        deserialize();
        File remote = new File("../", "saved");
        File remotePath = Utils.join(remote, remoteName);
        String remoteDirectoryPath = Utils.readContentsAsString(remotePath);
        if (!new File(remoteDirectoryPath).exists()) {
            throw new GitletException("Remote directory not found.");
        }
        File remoteDirectoryBranch = new File(remoteDirectoryPath, "/branch");
        HashMap<String, String> remoteBranchMap =
                Utils.readObject(remoteDirectoryBranch, HashMap.class);
        if (!remoteBranchMap.containsKey(remoteBranch)) {
            throw new GitletException("That remote does not have that branch.");
        }
        String remoteBranchID = remoteBranchMap.get(remoteBranch);
        File remoteDirectoryCommit = new File(remoteDirectoryPath, "/commit");
        File remoteDirectoryBlobs = new File(remoteDirectoryPath, "/blobs");
        List<String> blobList = Utils.plainFilenamesIn(remoteDirectoryBlobs);
        List<String> commitList = Utils.plainFilenamesIn(remoteDirectoryCommit);
        for (String s : blobList) {
            if (!Utils.join(blobs, s).exists()) {
                byte[] read = Utils.readContents
                        (Utils.join(remoteDirectoryBlobs, s));
                File blobDir = Utils.join(blobs, s);
                Utils.writeContents(blobDir, read);
            }
        }
        for (String s : commitList) {
            if (!Utils.join(commitFile, s).exists()) {
                Commit read = Utils.readObject
                        (Utils.join(remoteDirectoryCommit, s), Commit.class);
                File comDir = Utils.join(commitFile, s);
                Utils.writeObject(comDir, read);
            }
        }
        branchMap.put(remoteName + "/" + remoteBranch, remoteBranchID);
        serialize();
    }

    /** pull.
     *
     * @param remoteName remote name
     * @param remoteBranch remote branch
     */
    public static void pull(String remoteName, String remoteBranch) {
        fetch(remoteName, remoteBranch);
        merge(remoteName + "/" + remoteBranch);
    }

    /** serialize tool. */
    public static void serialize() {
        Utils.writeObject(Utils.join(cwd, ".gitlet/Staging/addMap"),
                addMap);
        Utils.writeObject(Utils.join(cwd, ".gitlet/Staging/removeList"),
                removeList);
        Utils.writeObject(Utils.join(cwd, ".gitlet/curr"),
                curr);
        Utils.writeContents(Utils.join(cwd, ".gitlet/currentBranch"),
                currentBranch);
        Utils.writeObject(Utils.join(cwd, ".gitlet/HEAD"),
                head);
        Utils.writeObject(Utils.join(cwd, ".gitlet/branch"),
                branchMap);
        Utils.writeContents(Utils.join(cwd, ".gitlet/secondparent"),
                sec);
        Utils.writeObject(Utils.join(cwd, ".gitlet/ancestorList"),
                ancestorList);
    }

    /** deserialize tool. */
    public static void deserialize() {
        addMap = (HashMap<String, String>) Utils.readObject(Utils.join(cwd,
                ".gitlet/Staging/addMap"), HashMap.class);
        removeList = (ArrayList<String>) Utils.readObject(Utils.join(cwd,
                ".gitlet/Staging/removeList"), ArrayList.class);
        curr = (Commit) Utils.readObject(Utils.join(cwd, ".gitlet/curr"),
                Commit.class);
        head = (Commit) Utils.readObject(Utils.join(cwd, ".gitlet/HEAD"),
                Commit.class);
        currentBranch = (String) Utils.readContentsAsString(Utils.join(cwd,
                ".gitlet/currentBranch"));
        branchMap = (HashMap<String, String>) Utils.readObject(Utils.join(cwd,
                ".gitlet/branch"), HashMap.class);
        sec = (String) Utils.readContentsAsString(Utils.join(cwd,
                ".gitlet/secondparent"));
        ancestorList = (ArrayList<String>) Utils.readObject(Utils.join(cwd,
                ".gitlet/ancestorList"), ArrayList.class);
    }
}
