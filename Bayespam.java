import java.io.*;
import java.util.*;


public class Bayespam
{
    // This defines the two types of messages we have.
    static enum MessageType
    {
        NORMAL, SPAM
    }

    /* ************************* MULTIPLE_COUNTER ****************************/

    // This a class with two counters (for regular and for spam)
    static class Multiple_Counter
    {
        int counter_spam    = 0;
        int counter_regular = 0;

        /// Class conditional probabilities.
        private double regularCCP = 0;
        private double spamCCP = 0;

        /// Log of Class Conditional Probabilities
        private double regularLCCP = 0, spamLCCP = 0;

        /// Sets class conditional probabilities.
        public void setCCPs (double nregular, double nspam, double epsilon) {
            regularCCP = ((counter_regular == 0 ? epsilon : counter_regular) / nregular);
            spamCCP = ((counter_spam == 0 ? epsilon : counter_spam) / nspam);

            setLCCPs(nregular, nspam, epsilon);
        }

        /// Sets class log conditional probabilities.
        public void setLCCPs (double nregular, double nspam, double epsilon) {
            regularLCCP = (Math.log10(counter_regular == 0 ? epsilon : counter_regular) - Math.log10(nregular));
            spamLCCP = (Math.log10(counter_spam == 0 ? epsilon : counter_spam) - Math.log10(nspam));
        }

        /// Getter: RegularCCP
        public double getRegularCCP () {
            return this.regularCCP;
        }

        /// Getter: SpamCCP
        public double getSpamCCP () {
            return this.spamCCP;
        }

        /// Getter: RegularLCCP
        public double getRegularLCCP () {
            return this.regularLCCP;
        }

        /// Getter: RegularLCCP
        public double getSpamLCCP () {
            return this.spamLCCP;
        }

        // Increase one of the counters by one
        public void incrementCounter(MessageType type)
        {
            if ( type == MessageType.NORMAL ){
                ++counter_regular;
            } else {
                ++counter_spam;
            }
        }
    }
    
    /* **************************** PROPERTIES *******************************/

    /// Train and Test Directories.
    private static String trainPath         = null;
    private static String testPath          = null;

    /// Program Constants
    private static double epsilon           = 1.0;
    private static int minWordLength        = 4;

    // Listings of the two subdirectories (regular/ and spam/)
    private static File[] listing_regular = new File[0];
    private static File[] listing_spam = new File[0];

    /// Prior Probabilities.
    static double  logPrior_regular        = 0;
    static double logPrior_spam            = 0;

    // A hash table for the vocabulary (word searching is very fast in a hash table)
    private static Hashtable <String, Multiple_Counter> vocab = new Hashtable <String, Multiple_Counter> ();

    /* ************************* BAYESPAM METHODS ****************************/

    
    /* ************************** PRINT/UTILITY ******************************/

    // Print the current content of the vocabulary
    private static void printVocab()
    {
        Multiple_Counter counter = new Multiple_Counter();

        for (Enumeration<String> e = vocab.keys() ; e.hasMoreElements() ;)
        {   
            String word;
            
            word = e.nextElement();
            counter  = vocab.get(word);
            
            System.out.println( word + " | in regular: " + counter.counter_regular + 
                                " in spam: "    + counter.counter_spam + " | Regular LCCP: " + 
                                counter.getRegularLCCP() + " | Spam LCCP: " + counter.getSpamLCCP());
        }
    }

    /// Count the number of words of the specified type.
    public static int wordCount (MessageType type) {
        int n = 0;
        Multiple_Counter counter;

        for (Enumeration <String> e = vocab.keys(); e.hasMoreElements();) {
            counter = vocab.get(e.nextElement());
            n += (type == MessageType.NORMAL) ? counter.counter_regular : counter.counter_spam;
        }

        return n;
    }

    /* ************************** CCP/VALIDATION *****************************/

    /// Sets all class conditional probabilities.
    public static void setCCPs () {
        int nregular = wordCount(MessageType.NORMAL);
        int nspam = wordCount(MessageType.SPAM);
        Multiple_Counter counter;

        for (Enumeration <String> e = vocab.keys(); e.hasMoreElements();) {
            counter = vocab.get(e.nextElement());
            counter.setCCPs(nregular, nspam, epsilon);
        }
    }

    /// Returns True if the word
    /// 1. Has length >= 4
    /// 2. Is only composed of letters.
    private static Boolean isValidWord (String word) {
        int i, n;

        if ((n = word.length()) < minWordLength) {
            return false;
        }
        for (i = 0; i < n; i++) {
            if (Character.isLetter(word.charAt(i)) == false) {
                return false;
            }
        }
        return true;
    }

    /* *************************** CLASSIFICATION ****************************/

    /// Classifies new messages as either Normal or Spam.
    public static MessageType classify (File file) throws IOException {
        FileInputStream i_s = new FileInputStream(file);
        BufferedReader in = new BufferedReader(new InputStreamReader(i_s));
        String line, word;
        double posterior_spam = logPrior_spam, posterior_regular = logPrior_regular;
	int count = 0;
        while ((line = in.readLine()) != null) {
            StringTokenizer st = new StringTokenizer(line);         
            
            while (st.hasMoreTokens()) {
                word = st.nextToken().toLowerCase();
                if (vocab.containsKey(word)) {
		    count++;
                    posterior_regular += vocab.get(word).getRegularLCCP();
                    posterior_spam    += vocab.get(word).getSpamLCCP();
                }                
            }
        }
        in.close();       
        //System.out.println(file.getName() + " contains " + count + " words from the hash-table");
        return (posterior_regular > posterior_spam ? MessageType.NORMAL : MessageType.SPAM);
    }

    /// Determines the ratio of email classifications for files in a given directory. 
    public static void directoryClassifier (MessageType type) throws IOException {
        int spam = 0, regular = 0;

        /// Create list of all files in directory.
        File[] files = (type == MessageType.SPAM ? listing_spam : listing_regular);

        /// Classify all files.
        for (int i = 0; i < files.length; i++) {
            if (classify(files[i]) == MessageType.SPAM) {
                spam++;
            } else {
                regular++;
            }
        }

        /// Print ratio.
        String listingType = (type == MessageType.SPAM) ? "Spam" : "Regular";
        System.out.println(listingType + " has " + spam + " spam files and " + regular + " regular ones.");
    }

    /* ************************* VOCAB CONSTRUCTION **************************/

    // Add a word to the vocabulary
    private static void addWord(String word, MessageType type)
    {
        Multiple_Counter counter = new Multiple_Counter();

        if ( vocab.containsKey(word) ){                  // if word exists already in the vocabulary..
            counter = vocab.get(word);                  // get the counter from the hashtable
        }
        counter.incrementCounter(type);                 // increase the counter appropriately

        vocab.put(word, counter);                       // put the word with its counter into the hashtable
    }

    // Read the words from messages and add them to your vocabulary. The boolean type determines whether the messages are regular or not  
    private static void readMessages (MessageType type)
    throws IOException
    {
        File[] messages = new File[0];

        if (type == MessageType.NORMAL){
            messages = listing_regular;
        } else {
            messages = listing_spam;
        }
        
        for (int i = 0; i < messages.length; ++i)
        {
            FileInputStream i_s = new FileInputStream( messages[i] );
            BufferedReader in = new BufferedReader(new InputStreamReader(i_s));
            String line;
            String word;
            
            while ((line = in.readLine()) != null)                      // read a line
            {
                StringTokenizer st = new StringTokenizer(line);         // parse it into words
        
                while (st.hasMoreTokens())                              // while there are still words left..
                {
                    word = st.nextToken().toLowerCase();                /// add only lower case variant.
                    if (isValidWord(word)) {
                        addWord(word, type);              
                    }                
                }
            }

            in.close();
        }
    }

    // List the regular and spam messages
    private static void listDirs(File dir_location)
    throws IOException
    {
        // List all files in the directory passed
        File[] dir_listing = dir_location.listFiles();

        // Check that there are 2 subdirectories
        if ( dir_listing.length != 2 )
        {
            System.out.println( "- Error: " + dir_location.getName() + " does not contain two subdirectories.\n" );
            Runtime.getRuntime().exit(0);
        }

        listing_regular = dir_listing[0].listFiles();
        listing_spam    = dir_listing[1].listFiles();

        // Verify folders were chosen correctly.
        if (!(dir_listing[0].getName().equals("regular") && dir_listing[1].getName().equals("spam"))) {
            throw new FileNotFoundException("Can't locate regular and spam folders in " + dir_location.getName());
        }
    }

    /// Loads a directory and saves all spam listings to listing_spam and regular listings to listing_regular.
    public static void loadDirectory (String directoryPath) throws IOException {

        File dir_location = new File(directoryPath);
        
        /// Check if the cmd line arg is a directory
        if (!dir_location.isDirectory()) {
            throw new FileNotFoundException(directoryPath + " is not a directory!");
        }

        // Initialize the regular and spam lists
        listDirs(dir_location);
    }

    /* ****************************** MAIN ***********************************/

    /// Reads in all program flags.
    /// 1. <dir>        training directory.
    /// 2. <dir>        test directory.
    /// In any order following 1 and 2.
    /// *. -e=<double>  epsilon.
    /// *. -l=<int>     min word length.
    public static void getArgs (String [] args) throws RuntimeException {

        /// Require at minimum both train and test directories.
        if (args.length < 2) {
            throw new IllegalArgumentException("You must provide a training and testing directory!");
        }

        trainPath = args[0];
        testPath = args[1];

        /// Read in remaining optional flags.
        for (int i = 2; i < args.length; i++) {
            String prefix, suffix, arg = args[i];

            if (arg.length() < 4) {
                throw new IllegalArgumentException("Unknown argument: " + arg);
            } else {
                prefix = arg.substring(0,3);
                suffix = arg.substring(3);
            }
            
            if (prefix.equals("-l=")) {
                minWordLength = Integer.parseInt(suffix);
            } else if (prefix.equals("-e=")) {
                epsilon = Double.parseDouble(suffix);
            } else {
                throw new IllegalArgumentException("Unknown argument: " + arg);
            }
        }
    }
   
    public static void main(String[] args)
    throws IOException
    {
        /// Load arguments.
        getArgs(args);

        // Print program parameters.
        System.out.println("**************************** UNIGRAM SPAM CLASSIFIER ***************************\n");
        System.out.println("Minimum Word Length:\t\t" + minWordLength);
        System.out.println("Epsilon:\t\t\t" + epsilon);
        System.out.println("*********************************** RESULTS ************************************\n");

        /// Loading the training directory.
        loadDirectory(trainPath);

        /// Compute prior probabilities now that directory contents are loaded.
        double nregular         = listing_regular.length;
        double nspam            = listing_spam.length;
        double ntotal           = nregular + nspam;
        logPrior_regular        = Math.log10(nregular) - Math.log10(ntotal);
        logPrior_spam           = Math.log10(nspam) - Math.log10(ntotal);

        // Read the e-mail messages
        readMessages(MessageType.NORMAL);
        readMessages(MessageType.SPAM);


        /// Set all class conditional probabilities.
        setCCPs();

        /// Loading the testing directory.
        loadDirectory(testPath);
		
	/// Loading the test directory.
        loadDirectory(args[1]);
	
        /// Count classifications of files in both spam and regular.
        directoryClassifier(MessageType.NORMAL);
        directoryClassifier(MessageType.SPAM);
	System.out.println("Number of unique words: " + vocab.size());
    }
}
