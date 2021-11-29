package gitlet;


import java.io.File;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.*;

import static gitlet.GitletRepository.*;

/**
 * Represents a gitlet commit object.
 *
 * @author Archit Das
 */
public class Commit implements Serializable {

    /**
     * The message of this Commit.
     */
    private String message;

    /**
     * The SHA-1 hash id of the commit
     */
    private String hash;

    /**
     * The SHA-1 hash id of the commit
     */
    private Date date;

    /**
     * The hash of the parent commit
     */
    private String parentHash;

    /**
     * The hash of the second parent commit - for merges
     */
    private String secondParentHash;

    /**
     * The Map representing the files to the blobs
     *
     * @Key The name of the file
     * @Value The hash of the corresponding blob
     */
    private final Map<String, String> filesMapped;

    /**
     * Files staged to be added
     */
    private final Set<File> filesToBeAdded;

    /**
     * Files staged to be removed
     */
    private final Set<File> filesToBeRemoved;

    /**
     * The Constructor will create the new active commit
     */
    public Commit(String parentHash, Map<String, String> filesMapped) {
        this.parentHash = parentHash;

        this.filesToBeAdded = new TreeSet<>();
        this.filesToBeRemoved = new TreeSet<>();
        this.filesMapped = filesMapped;

        Utils.writeObject(STAGING_AREA_COMMIT, this);
    }

    public void add(String fileName) {
        File curr = Utils.join(CWD, fileName);
        if (!curr.exists()) {
            System.out.println("File does not exist.");
            System.exit(0);
        }

        boolean notTracked = !filesMapped.containsKey(fileName);
        boolean modified = checkIfModified(curr);
        filesToBeAdded.remove(curr);
        if (notTracked || modified) {
            filesToBeAdded.add(curr);
        }
        filesToBeRemoved.remove(curr);
    }

    public boolean rm(String fileName) {
        File curr = Utils.join(CWD, fileName);

        filesToBeRemoved.add(curr);
        Utils.restrictedDelete(curr);
        return true;
    }


    /**
     * Checks if file has been modified and NOT ADDED as a new file
     *
     * @param file The file that is being checked
     * @return
     */
    public boolean checkIfModified(File file) {
        boolean notModified =
                Utils.sha1(Utils.readContents(file)).equals(
                        filesMapped.get(file.getName()));
        return !notModified && filesMapped.containsKey(file.getName());
    }

    public boolean isStaged(String fileName) {
        File file = Utils.join(CWD, fileName);
        return filesToBeAdded.contains(file);
    }

    /**
     * Finalizes the commit and creates a new working commit
     *
     * @param commitDate    date of commit
     * @param commitMessage message of commit
     */
    public void bakeCommit(Date commitDate, String commitMessage) {
        if (commitMessage.trim().equals("")) {
            System.out.println("Please enter a commit message.");
            System.exit(0);
        }
        if (!commitDate.equals(
                new Date(0)) && filesToBeAdded.isEmpty() && filesToBeRemoved.isEmpty()) {
            System.out.println("No changes added to the commit.");
            System.exit(0);
        }

        //Handles files to be removed
        for (File file : filesToBeRemoved) {
            filesMapped.remove(file.getName());
        }

        //Handles files to be added
        for (File file : filesToBeAdded) {
            byte[] contBlob = Utils.readContents(file);
            String blobHash = Utils.sha1(contBlob);
            File blobFile = Utils.join(BLOBS_FOLDER, blobHash);
            Utils.writeContents(blobFile, contBlob);
            filesMapped.put(file.getName(), blobHash);
        }


        this.date = commitDate;
        this.message = commitMessage;

        filesToBeAdded.clear();
        filesToBeRemoved.clear();

        this.hash = Utils.sha1(Utils.serialize(this));
        Utils.writeObject(Utils.join(COMMITS_FOLDER, this.hash), this);

        GitletRepository.getBranches().updateBranchPointer(this.hash);

        new Commit(this.hash, this.filesMapped);
    }

    /**
     * Creates the initial commit
     */
    public void initCommit() {
        bakeCommit(new Date(0), "initial commit");
    }

    /**
     * Prints commit for logs
     */
    public void printCommit() {
        String pattern = "EEE MMM d HH:mm:ss YYYY Z";
        SimpleDateFormat formatter = new SimpleDateFormat(pattern);
        formatter.setTimeZone(TimeZone.getTimeZone("PST"));


        System.out.println("===");
        System.out.println("commit " + hash);
        System.out.println("Date: " + formatter.format(this.date));
        System.out.println(message);
        System.out.println();
    }

    public boolean isUntracked(String fileName) {
        return !filesMapped.containsKey(fileName);
        //!filesToBeAdded.contains(Utils.join(CWD, fileName)) &&
    }

    public Date getDate() {
        return this.date;
    }

    public String getParentHash() {
        return parentHash;
    }

    public void setParentHash(String parentHash) {
        this.parentHash = parentHash;
    }

    public Map<String, String> getFilesMapped() {
        return filesMapped;
    }

    public Set<File> getFilesToBeAdded() {
        return filesToBeAdded;
    }

    public String getMessage() {
        return message;
    }

    public String getHash() {
        return hash;
    }

    public Set<File> getFilesToBeRemoved() {
        return filesToBeRemoved;
    }

    public void setSecondParentHash(String secondParentHash) {
        this.secondParentHash = secondParentHash;
    }

    public String getSecondParentHash() {
        return secondParentHash;
    }
}
