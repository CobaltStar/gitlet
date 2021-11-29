# Gitlet Design Document

**Name**: Archit Das

## Classes and Data Structures

### Main
Where the program starts. It will take the args and using a switch statement parse the info and call whatever command the user wants.

### GitletRepository
This will have the main file paths
It will handle the actual commands that Main requested

#### Fields
1.	static final File CWD = new File(System.getProperty("user.dir")) 
2.	static final File GITLET_FOLDER = Utils.join(GITLET_FOLDER, ".gitlet") - main .gitlet folder
3.	static final File COMMITS_FOLDER = Utils.join(GITLET_FOLDER, "commits")- stores the commits
4.	static final File HEADS = Utils.join(GITLET_FOLDER, "blobs")
5.	static final File HEADS_FOLDER = Utils.join(GITLET_FOLDER, "HEADS") - where the heads are stored
6.	static final File MASTER_HEAD = Utils.join(HEADS_FOLDER, "master") - file location of master head



### Commit
It will be the commit node structure
Each commit will have a hash
Use SHA-1 HashStrings to point to blobs

#### Fields

1.	String parentHash – refers to has of parent commit – initial commit will not have this
2.	Date date – date and time of commit – only done during commit()
3.	String message – message of commit – only done during commit()
4.	HashMap<String, String> filesMapped- first string will be name of file, the second the hash of the file blob
5.	HashSet<File> trackedFiles – files that have are being tracked – initially what the parent is tracking
6.	ArrayList<File> filesToBeAdded – files that will be added – they must be new or modified – also add to tracking – removing will remove the file from here
7.	ArrayList<File> filesToBeRemoved – files that will be removed 

### Branches
It will store all the branch pointers
It will also store the HEAD

#### Fields
1. String head - Head pointer
2. HashMap<String, String> branchPointers - stores each branch inside with the value being the commit hash it is pointing to

## Algorithms

### GitletRepository
init()
*	Create the initial commit
*	Create master head

checkout()
*	Three types – it will basically restore code

log()
* prints the commits iteratively from HEAD up to the initial commit

globalLog()
* prints all the commits in the commit folder



### Commit
bakeCommit(String message) 
*	it will put it into the tree and move the head to that commit
*	A new “transitory commit” will be in the main directory 
*	Create new blobs if necessary
*	Update HEAD

add()
*	Stages files if new or modified and begins tracking them

rm() 
*	remove files from tracking and delete file in local repo

printCommit()
* prints the commit

## Persistence
Directory Structure
* .gitlet
    *	current_commit
    *	branches
    *	commits
        *	Commit (initial)
        *	Commit1
        *	Commit2 (head)
        *	[where current_Commit will go after baked]
    *	BlobsDir
        *	Blob1
        *	Blob2

### GitletRepository
Create all persistence files upon init()

