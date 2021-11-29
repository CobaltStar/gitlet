package gitlet;

import java.io.File;
import java.util.*;

public class GitletRepository {
    static final File CWD = new File(System.getProperty("user.dir"));

    /**
     * Main metadata folder.
     */
    static final File GITLET_FOLDER = Utils.join(CWD, ".gitlet");

    /**
     * Folder for commits
     */
    static final File COMMITS_FOLDER = Utils.join(GITLET_FOLDER, "commits");

    /**
     * Folder for blobs
     */
    static final File BLOBS_FOLDER = Utils.join(GITLET_FOLDER, "blobs");

    /**
     * Staging Area Commit
     */
    static final File STAGING_AREA_COMMIT = Utils.join(GITLET_FOLDER, "staging_area");

    /**
     * Branches File
     */
    static final File BRANCHES_FILE = Utils.join(GITLET_FOLDER, "branches");

    /**
     * Object which stores all the branches
     */
    static Branches branchesInstance;

    /**
     * Handles the init command
     */
    static void init() {
        if (GITLET_FOLDER.exists()) {
            System.out.println(
                    "A Gitlet version-control system already exists in the current "
                            + "directory.");
            System.exit(0);
        }
        GITLET_FOLDER.mkdir();
        COMMITS_FOLDER.mkdir();
        BLOBS_FOLDER.mkdir();

        branchesInstance = new Branches();

        Commit dummy = new Commit("I am the initial commit",
                new HashMap<String, String>());
        dummy.initCommit();
    }

    /**
     * Handles adding files
     *
     * @param fileName Name of the file that is being added
     */
    static void add(String fileName) {
        Commit comm = Utils.readObject(STAGING_AREA_COMMIT, Commit.class);
        comm.add(fileName);
        Utils.writeObject(STAGING_AREA_COMMIT, comm);
    }

    /**
     * handles the commit command
     */
    static void commit(String message) {
        Commit comm = Utils.readObject(STAGING_AREA_COMMIT, Commit.class);
        comm.bakeCommit(new Date(), message);
    }

    static void rm(String fileName) {
        Commit comm = Utils.readObject(STAGING_AREA_COMMIT, Commit.class);
        Set<File> staged = comm.getFilesToBeAdded();

        if (comm.isUntracked(fileName) && !comm.isStaged(fileName)) {
            System.out.println("No reason to remove the file.");
            System.exit(0);
        }
        if (!comm.isUntracked(fileName)) {
            comm.rm(fileName);
        }
        staged.remove(Utils.join(CWD, fileName));
        Utils.writeObject(STAGING_AREA_COMMIT, comm);

    }

    /**
     * Prints log of the commits so far
     * Does this recursively
     */
    public static void log() {
        File currFile = Utils.join(COMMITS_FOLDER,
                getBranches().getCurrentBranchPointer());
        while (true) {
            Commit currCommit = Utils.readObject(currFile, Commit.class);
            currCommit.printCommit();

            if (currCommit.getDate().equals(new Date(0))) {
                break;
            }
            currFile = Utils.join(COMMITS_FOLDER, currCommit.getParentHash());
        }
    }


    /**
     * Checks out a file at the HEAD commit
     *
     * @param fileName Thhe file that will be checked out
     */
    public static void fileCheckOut(String fileName) {
        File commitFile = Utils.join(COMMITS_FOLDER,
                getBranches().getCurrentBranchPointer());
        Commit comm = checkOutHelper(commitFile, fileName);
        comm.getFilesToBeAdded().remove(Utils.join(CWD, fileName));
        Utils.writeObject(STAGING_AREA_COMMIT, comm);
    }

    public static void commitFileCheckout(String commitID, String fileName) {
        //abbrev functionality
        if (commitID.length() < Utils.UID_LENGTH) {
            List<String> list = Utils.plainFilenamesIn(COMMITS_FOLDER);
            for (String longHash : list) {
                if (longHash.startsWith(commitID)) {
                    commitID = longHash;
                    break;
                }
            }
        }

        File commitFile = Utils.join(COMMITS_FOLDER, commitID);
        if (!commitFile.exists()) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }
        checkOutHelper(commitFile, fileName);
    }

    /**
     * Contains logic for checking out
     *
     * @param commitFile - file of commit from where the checkout is requested
     * @param fileName   - name of file being checked out
     * @return - the commit it was checked out from
     */
    private static Commit checkOutHelper(File commitFile, String fileName) {
        Commit comm = Utils.readObject(commitFile, Commit.class);
        File fileCheck = Utils.join(CWD, fileName);
        Map<String, String> filesMapped = comm.getFilesMapped();

        if (!filesMapped.containsKey(fileName)) {
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        }
        String blobHash = filesMapped.get(fileName);
        File blobFile = Utils.join(BLOBS_FOLDER, blobHash);
        Utils.writeContents(fileCheck, Utils.readContents(blobFile));
        return comm;
    }

    public static void branchCheckOut(String branchName, boolean reset) {
        Commit stageCommit = Utils.readObject(STAGING_AREA_COMMIT, Commit.class);
        Set<String> filesTrackedPrevBranch = stageCommit.getFilesMapped().keySet();

        Branches branches = getBranches();
        if (branchName.equals(branches.getCurrentBranch()) && !reset) {
            System.out.println("No need to checkout the current branch.");
            System.exit(0);
        }
        branches.switchBranch(branchName);

        File commitFile = Utils.join(COMMITS_FOLDER, branches.getCurrentBranchPointer());
        Commit branchCommit = Utils.readObject(commitFile, Commit.class);

        //check for working files that are untracked
        List<String> cwdFiles = Utils.plainFilenamesIn(CWD);
        for (String fileName : cwdFiles) { //deletion of previously tracked files
            if (stageCommit.isUntracked(fileName) && !branchCommit.isUntracked(
                    fileName)) {
                System.out.println(
                        "There is an untracked file in the way; delete it, or add and "
                                + "commit it first.");
                System.exit(0);
            }
        }


        Map<String, String> filesMapped = branchCommit.getFilesMapped();
        Set<String> filesTrackedCurrBranch = filesMapped.keySet();
        for (String fileName : filesTrackedCurrBranch) { //writes the files
            File fileCheck = Utils.join(CWD, fileName);
            String blobHash = filesMapped.get(fileName);
            File blobFile = Utils.join(BLOBS_FOLDER, blobHash);
            Utils.writeContents(fileCheck, Utils.readContents(blobFile));
        }

        List<String> currCwdFiles = Utils.plainFilenamesIn(CWD);
        for (String fileName : currCwdFiles) { //deletion of previously tracked files
            if (filesTrackedPrevBranch.contains(
                    fileName) && !filesTrackedCurrBranch.contains(fileName)) {
                Utils.join(CWD, fileName).delete();
            }
        }
        stageCommit = branchCommit;
        stageCommit.setParentHash(branchCommit.getHash());
        Utils.writeObject(STAGING_AREA_COMMIT, stageCommit); //update staging area
        Utils.writeObject(GitletRepository.BRANCHES_FILE, branches);

    }

    public static void globalLog() {
        List<String> commits = Utils.plainFilenamesIn(COMMITS_FOLDER);
        for (String fileName : commits) {
            File file = Utils.join(COMMITS_FOLDER, fileName);
            Commit comm = Utils.readObject(file, Commit.class);
            comm.printCommit();
        }
    }

    public static void find(String word) {
        List<String> commits = Utils.plainFilenamesIn(COMMITS_FOLDER);
        boolean foundItem = false;
        for (String fileName : commits) {
            File file = Utils.join(COMMITS_FOLDER, fileName);
            Commit comm = Utils.readObject(file, Commit.class);
            if (comm.getMessage().equals(word)) {
                System.out.println(comm.getHash());
                foundItem = true;
            }
        }
        if (!foundItem) {
            System.out.println("Found no commit with that message.");
        }
    }

    /**
     * prints out status of the stage currently
     */
    public static void status() {
        Branches branches = getBranches();
        File currFile = Utils.join(COMMITS_FOLDER, branches.getCurrentBranchPointer());
        Commit comm = Utils.readObject(STAGING_AREA_COMMIT, Commit.class);

        System.out.println(
                "=== Branches ==="); //Prints out all the branches - puts a star for
        // the current one
        for (String branch : branches.getAllBranches()) {
            if (branch.equals(branches.getCurrentBranch())) {
                System.out.println("*" + branch);
            } else {
                System.out.println(branch);
            }
        }
        System.out.println();

        System.out.println("=== Staged Files ===");
        for (File file : comm.getFilesToBeAdded()) {
            System.out.println(file.getName());
        }
        System.out.println();

        System.out.println("=== Removed Files ===");
        for (File file : comm.getFilesToBeRemoved()) {
            System.out.println(file.getName());
        }
        System.out.println();

        System.out.println("=== Modifications Not Staged For Commit ===");
        List<String> cwdFiles = Utils.plainFilenamesIn(CWD);

        for (String fileName : comm.getFilesMapped().keySet()) { // removed
            if (!cwdFiles.contains(fileName)
                    && !comm.getFilesToBeRemoved().contains(Utils.join(CWD, fileName))) {
                System.out.println(fileName + (" (deleted)"));
            }
        }

        for (String fileName : cwdFiles) { // checks for modified
            if (comm.checkIfModified(Utils.join(CWD, fileName))
                    && !comm.getFilesToBeAdded().contains(Utils.join(CWD, fileName))) {
                System.out.println(fileName + (" (modified)"));
            }
        }

        System.out.println();
        System.out.println("=== Untracked Files ===");

        for (String fileName : cwdFiles) { // checks for modified
            if (comm.isUntracked(fileName) && !comm.getFilesToBeAdded()
                    .contains(Utils.join(CWD, fileName))) {
                System.out.println(fileName);
            }
        }
        System.out.println();
    }

    /**
     * Creates a new branch
     * Puts in commit hash of the current branch hash
     *
     * @param branchName The file name of the new branch
     */
    public static void branch(String branchName) {
        Branches branches = getBranches();
        getBranches().addBranch(branchName);
        Utils.writeObject(GitletRepository.BRANCHES_FILE, branches);
    }

    public static void rmBranch(String branchName) {
        Branches branches = getBranches();
        branches.removeBranch(branchName);
        branches.write();

    }

    public static Branches getBranches() {
        if (branchesInstance == null) {
            branchesInstance = Utils.readObject(BRANCHES_FILE, Branches.class);
        }

        return branchesInstance;
    }

    public static Commit getCommit(String commitHash) {
        return Utils.readObject(Utils.join(COMMITS_FOLDER, commitHash), Commit.class);
    }

    public static void reset(String commitID) {
        File commitFile = Utils.join(COMMITS_FOLDER, commitID);
        if (!commitFile.exists()) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }

        Branches branches = getBranches();
        branches.updateBranchPointer(commitID);

        String currBranch = branches.getCurrentBranch(); //temp branch place holder
        branchCheckOut(branches.getCurrentBranch(), true);

    }

    public static void merge(String givenBranchName) {
        Commit stageCommit = Utils.readObject(STAGING_AREA_COMMIT, Commit.class);
        Branches branches = getBranches();

        if (!branches.getAllBranches().contains(givenBranchName)) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }

        String splitPoint = findSplitPoint(givenBranchName);
        String currentBranchCommitID = branches.getCurrentBranchPointer();
        String givenBranchCommitID = branches.getBranchPointer(givenBranchName);

        Commit currentBranchCommit = getCommit(currentBranchCommitID);
        Commit givenBranchCommit = getCommit(givenBranchCommitID);
        Commit splitPointCommit = getCommit(splitPoint);

        Map<String, String> currentBranchMap = currentBranchCommit.getFilesMapped();
        Map<String, String> givenBranchMap = givenBranchCommit.getFilesMapped();
        Map<String, String> splitPointMap = splitPointCommit.getFilesMapped();

        Set<String> masterSet = new HashSet<>();
        masterSet.addAll(currentBranchMap.keySet());
        masterSet.addAll(givenBranchMap.keySet());
        masterSet.addAll(splitPointMap.keySet());

        List<String> cwdFiles = Utils.plainFilenamesIn(
                CWD);  //untracked files in merge check

        if (!stageCommit.getFilesToBeAdded().isEmpty()
                || !stageCommit.getFilesToBeRemoved().isEmpty()) {
            System.out.println("You have uncommitted changes.");
            System.exit(0);
        }

        for (String fileName : cwdFiles) {
            if (stageCommit.isUntracked(fileName) && masterSet.contains(fileName)) {
                System.out.println(
                        "There is an untracked file in the way; delete it, or add and "
                                + "commit it first.");
                System.exit(0);
            }
        }

        if (currentBranchCommitID.equals(givenBranchCommitID)) {
            System.out.println("Cannot merge a branch with itself.");
            System.exit(0);
        }

        if (givenBranchCommitID.equals(
                splitPoint)) { // split point is the same commit as the given branch
            System.out.println("Given branch is an ancestor of the current branch.");
            System.exit(0);
        }

        if (currentBranchCommitID.equals(
                splitPoint)) { // split point is the current branch
            branchCheckOut(givenBranchName, false);
            branches.write();
            System.out.println("Current branch fast-forwarded.");
            System.exit(0);
        }

        performMerge(masterSet, givenBranchCommitID, stageCommit, currentBranchMap,
                givenBranchMap, splitPointMap);

        stageCommit.setSecondParentHash(givenBranchCommitID);
        stageCommit.bakeCommit(new Date(),
                String.format("Merged %s into %s.", givenBranchName,
                        branches.getCurrentBranch()));
    }

    private static void performMerge(Set<String> masterSet, String givenBranchCommitID,
                                     Commit stageCommit,
                                     Map<String, String> currentBranchMap,
                                     Map<String, String> givenBranchMap,
                                     Map<String, String> splitPointMap) {
        for (String fileName : masterSet) {
            String currentBranchFileHash = currentBranchMap.get(fileName);
            String givenBranchFileHash = givenBranchMap.get(fileName);
            String splitPointFileHash = splitPointMap.get(fileName);

            if (Objects.equals(currentBranchFileHash, givenBranchFileHash)) {
                continue;
            } else if (givenBranchFileHash == null && Objects.equals(
                    currentBranchFileHash, splitPointFileHash)) {
                stageCommit.rm(fileName);
            } else if (currentBranchFileHash == null && Objects.equals(
                    givenBranchFileHash, splitPointFileHash)) {
                continue;
            } else if (Objects.equals(currentBranchFileHash,
                    splitPointFileHash)) { // change happened on given branch
                commitFileCheckout(givenBranchCommitID, fileName);
                stageCommit.add(fileName);

            } else if (Objects.equals(givenBranchFileHash, splitPointFileHash)) {
                continue;
            } else { // merge conflict case
                String currentBranchString = "";
                String givenBranchString = "";

                if (currentBranchFileHash != null) {
                    File currentBranchBlob = Utils.join(BLOBS_FOLDER,
                            currentBranchFileHash);
                    currentBranchString = Utils.readContentsAsString(currentBranchBlob);
                }
                if (givenBranchFileHash != null) {
                    File givenBranchBlob = Utils.join(BLOBS_FOLDER, givenBranchFileHash);
                    givenBranchString = Utils.readContentsAsString(givenBranchBlob);
                }

                String head = "<<<<<<< HEAD";
                String separator = "=======";
                String end = ">>>>>>>";
                String contents =
                        head + System.lineSeparator() + currentBranchString + separator
                                + System.lineSeparator() + givenBranchString + end
                                + System.lineSeparator();

                Utils.writeContents(Utils.join(CWD, fileName), contents);
                System.out.println("Encountered a merge conflict.");
                stageCommit.add(fileName);
            }
        }
    }

    private static String findSplitPoint(String givenBranchName) {
        Branches branches = getBranches();
        File currentBranchFile = Utils.join(COMMITS_FOLDER,
                branches.getCurrentBranchPointer());
        File givenBranchFile = Utils.join(COMMITS_FOLDER,
                branches.getBranchPointer(givenBranchName));

        Set<String> currentBranchCommits = scanAncestors(
                branches.getCurrentBranchPointer());

        while (true) {
            Commit currCommit = Utils.readObject(givenBranchFile, Commit.class);
            if (currentBranchCommits.contains(currCommit.getHash())) {
                return currCommit.getHash();
            }
            if (currCommit.getDate().equals(new Date(0))) {
                break;
            }
            givenBranchFile = Utils.join(COMMITS_FOLDER, currCommit.getParentHash());
        }

        return "Unknown error";
    }

    private static Set<String> scanAncestors(String commitID) {
        Set<String> ancestors = new HashSet<>();
        File traversalFile = Utils.join(COMMITS_FOLDER, commitID);
        while (true) {
            Commit traversalCommit = Utils.readObject(traversalFile, Commit.class);
            ancestors.add(traversalCommit.getHash());

            if (traversalCommit.getSecondParentHash() != null) {
                ancestors.addAll(scanAncestors(traversalCommit.getSecondParentHash()));
            }

            if (traversalCommit.getDate().equals(new Date(0))) {
                break;
            }
            traversalFile = Utils.join(COMMITS_FOLDER, traversalCommit.getParentHash());
        }
        return ancestors;
    }
}
