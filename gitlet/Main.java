package gitlet;

/**
 * Driver class for Gitlet, a subset of the Git version-control system.
 *
 * @author Archit Das
 */
public class Main {
    /**
     * Usage: java gitlet.Main ARGS, where ARGS contains
     * <COMMAND> <OPERAND1> <OPERAND2> ...
     *
     * @param args for git commands
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            System.exit(0);
        }

        if (args[0].equals("init")) { // init check happens beore gitlet check
            if (args.length != 1) {
                System.out.println("Incorrect operands.");
                System.exit(0);
            }
            GitletRepository.init();
            System.exit(0);
        }

        if (!GitletRepository.GITLET_FOLDER.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
        String firstArg = args[0];
        switch (firstArg) {
            case "add":
                if (args.length != 2) {
                    System.out.println("Incorrect operands.");
                    System.exit(0);
                }
                GitletRepository.add(args[1]);
                break;
            case "rm":
                if (args.length != 2) {
                    System.out.println("Incorrect operands.");
                    System.exit(0);
                }
                GitletRepository.rm(args[1]);
                break;
            case "commit":
                if (args.length != 2) {
                    System.out.println("Incorrect operands.");
                    System.exit(0);
                }
                GitletRepository.commit(args[1]);
                break;
            case "log":
                if (args.length != 1) {
                    System.out.println("Incorrect operands.");
                    System.exit(0);
                }
                GitletRepository.log();
                break;
            case "global-log":
                if (args.length != 1) {
                    System.out.println("Incorrect operands.");
                    System.exit(0);
                }
                GitletRepository.globalLog();
                break;
            case "find":
                if (args.length != 2) {
                    System.out.println("Incorrect operands.");
                    System.exit(0);
                }
                GitletRepository.find(args[1]);
                break;
            case "branch":
                if (args.length != 2) {
                    System.out.println("Incorrect operands.");
                    System.exit(0);
                }
                GitletRepository.branch(args[1]);
                break;
            case "rm-branch":
                if (args.length != 2) {
                    System.out.println("Incorrect operands.");
                    System.exit(0);
                }
                GitletRepository.rmBranch(args[1]);
                break;
            case "checkout":
                if (args.length == 3) {
                    if (args[1].equals("--")) {
                        GitletRepository.fileCheckOut(args[2]);
                    } else {
                        System.out.println("Incorrect operands.");
                    }
                } else if (args.length == 4) {
                    if (!args[2].equals("--")) {
                        System.out.println("Incorrect operands.");
                    } else if (args[1].length() == Utils.UID_LENGTH) {
                        GitletRepository.commitFileCheckout(args[1], args[3]);
                    } else if (args[1].length() < Utils.UID_LENGTH) {
                        GitletRepository.commitFileCheckout(args[1], args[3]);
                    } else {
                        System.out.println("Incorrect operands.");
                    }
                } else if (args.length == 2) {
                    GitletRepository.branchCheckOut(args[1], false);
                } else {
                    System.out.println("Incorrect operands.");
                }
                break;
            case "reset":
                if (args.length != 2) {
                    System.out.println("Incorrect operands.");
                    System.exit(0);
                }
                GitletRepository.reset(args[1]);
                break;
            case "status":
                if (args.length != 1) {
                    System.out.println("Incorrect operands.");
                    System.exit(0);
                }
                GitletRepository.status();
                break;
            case "merge":
                if (args.length != 2) {
                    System.out.println("Incorrect operands.");
                    System.exit(0);
                }
                GitletRepository.merge(args[1]);
                break;
            default:
                System.out.println("No command with that name exists.");
        }
        System.exit(0);
    }
}
