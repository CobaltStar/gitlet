package gitlet;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class Branches implements Serializable {
    private String currentBranch;

    /**
     * Stores branch name and commit
     */
    Map<String, String> branchMap;

    public Branches() {
        currentBranch = "master";
        branchMap = new TreeMap<>();
        Utils.writeObject(GitletRepository.BRANCHES_FILE, this);
    }

    public String getCurrentBranch() {
        return currentBranch;
    }

    /**
     * @param branchName - name of branch being switched too
     */
    public void switchBranch(String branchName) {
        if (!branchMap.containsKey(branchName)) {
            System.out.println("No such branch exists.");
            System.exit(0);
        }
        currentBranch = branchName;

    }

    /**
     * If branch already exists, tell the user that it already does
     * Otherwise, add branch to branchMap
     *
     * @param branchName - name of branch being added
     */
    public void addBranch(String branchName) {
        if (branchMap.containsKey(branchName)) {
            System.out.println("A branch with that name already exists.");
            System.exit(0);
        }
        branchMap.put(branchName, branchMap.get(currentBranch));
        Utils.writeObject(GitletRepository.BRANCHES_FILE, this);
    }

    /**
     * Updates the current branch pointer with the specified hash in the branchMap
     *
     * @param hash
     */
    public void updateBranchPointer(String hash) {
        branchMap.put(currentBranch, hash);
        Utils.writeObject(GitletRepository.BRANCHES_FILE, this);
    }

    public String getCurrentBranchPointer() {
        return branchMap.get(currentBranch);
    }

    public String getBranchPointer(String branchName) {
        return branchMap.get(branchName);
    }

    public Set<String> getAllBranches() {
        return branchMap.keySet();
    }

    public void removeBranch(String branchName) {
        if (branchName.equals(currentBranch)) {
            System.out.println("Cannot remove the current branch.");
            System.exit(0);
        } else if (!branchMap.containsKey(branchName)) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        } else {
            branchMap.remove(branchName);
        }
    }

    public void write() {
        Utils.writeObject(GitletRepository.BRANCHES_FILE, this);
    }
}
