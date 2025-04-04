# Gitlet Design Document

**Name**: Boaz Nam

## Classes and Data Structures
Commit: The commit Class where it stores all information like time it was committed, the information in the files, parents information. Where all of the commit commands/actions will take place. 

Fields - Time: holds the time in which the file was committed. Information: holds the information of that file. Parent: holds the information of the parent. Blobs: (HashMap?) where we will keep track of all the blobs that are created and edited(?) An example of a blob may be (wug 1: version1... wug2: version 1..) (String) Message: the message in which the user inputs to describe their file. (commit -m "_______")

Main: Holds folders, files, directories(Should be a relatively simple class)

Fields - Folders: creates all the necessary folders needed for Gitlet. Files: creates all the necessary files needed for Gitlet and adds more if needed(?). Directories: creates all the necessary directories needed for Gitlet,(again, creates more if needed, depending on the needs of the project) 


Repo: Repository class where the actual structure of the files where things are added, committed, and staged will be. 

Fields: Head: pointer that points to the 'Head'. Master: pointer that points to the 'Master'. NOTE: Head and Master are different in that Head where we currently are and looking and Master is where the system is. Directory: variable that keeps track of the whole system(the working branch, etc.)


## Algorithms
Commit: Class constructor, initializes the attributes to variables(time, information, parents). This is where we will deal with the blobs, the date of the files in which they were committed, and the parents, and message. Initialize all the necessary variables and just return the needed messages. 

Main: consists of the main method only(probably?) Similar to lab11,(look back for reference). Aim to create switch and case statements. Accounts for the various commands the user can input; ie: init, add, commit, rm, log, global-log, find, status, checkout, banch, rm-branch, reset, and merge. In each switch case should be an if case of some sort checking the arguments and then redirect to the needed methods. ALSO, this is where we will check for the necessary edge cases mentioned the spec. For example: if a user doesnt input any arguments, we would want to print the message "Please enter a command" LOOK AT SPEC FOR FULL LIST OF EDGE CASES.  

Repo: Class constructor, initialize all the variables. Creates all the directories here(?). This is where the persisting will happen? We want to keep track of each File and its contents and be able to refer back to previous Files using a data structure, maybe HashMap.



## Persistence
The Persisting will happen with the Blobs, and we would want to make a Hashmap dedicated for these. To get our previous save states within the HashMap, we use the name that we gave it(?) Make Repo/Commit serializable and use the idea in lab11 possibly(Look back at reference specifically saveDog and FromFile) Use readObject and write Object and look for the files when needed.  
