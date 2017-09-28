import java.io.*;
import java.util.*;


public class Bayespam
{
    // This defines the two types of messages we have.
    static enum MessageType
    {
        NORMAL, SPAM
    }

    // This a class with two counters (for regular and for spam)
    static class Multiple_Counter
    {
        int counter_spam    = 0;
        int counter_regular = 0;

        /// Class conditional probabilities.
        private double regularCCP = 0;
        private double spamCCP = 0;

        // Log of Class Conditional Probabilities
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

        /// Getters

        public double getRegularCCP () {
            return this.regularCCP;
        }

        public double getSpamCCP () {
            return this.spamCCP;
        }

        public double getRegularLCCP () {
            return this.regularLCCP;
        }

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

    // Listings of the two subdirectories (regular/ and spam/)
    private static File[] listing_regular = new File[0];
    private static File[] listing_spam = new File[0];

    /// Prior Probabilities.
    static double  prior_regular = 0;
    static double prior_spam    = 0;

    // A hash table for the vocabulary (word searching is very fast in a hash table)
    private static Hashtable <String, Multiple_Counter> vocab = new Hashtable <String, Multiple_Counter> ();

    
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


    // List the regular and spam messages
    private static void listDirs(File dir_location)
    {
        // List all files in the directory passed
        File[] dir_listing = dir_location.listFiles();

        // Check that there are 2 subdirectories
        if ( dir_listing.length != 2 )
        {
            System.out.println( "- Error: specified directory does not contain two subdirectories.\n" );
            Runtime.getRuntime().exit(0);
        }

        listing_regular = dir_listing[0].listFiles();
        listing_spam    = dir_listing[1].listFiles();
    }

    
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

    /// Sets all class conditional probabilities.
    public static void setCCPs (int epsilon) {
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

        if ((n = word.length()) < 4) {
            return false;
        }
        for (i = 0; i < n; i++) {
            if (Character.isLetter(word.charAt(i)) == false) {
                return false;
            }
        }
        return true;
    }

    // Read the words from messages and add them to your vocabulary. The boolean type determines whether the messages are regular or not  
    private static void readMessages(MessageType type)
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
                    if (isValidWord((word = st.nextToken()))) {         // add them to the vocabulary
                        addWord(word.toLowerCase(), type);              /// add only lower case variant.
                    }                
                }
            }

            in.close();
        }
    }

    /// Classifies new messages as either Normal or Spam.
    public static MessageType classify (String pathname) throws IOException {
        File file = new File(pathname);
        FileInputStream i_s = new FileInputStream(file);
        BufferedReader in = new BufferedReader(new InputStreamReader(i_s));
        String line, word;
        double posterior_spam = prior_spam, posterior_regular = prior_regular;

        while ((line = in.readLine()) != null) {
            StringTokenizer st = new StringTokenizer(line);         
            
            while (st.hasMoreTokens()) {
                if (vocab.containsKey((word = st.nextToken()))) {
                    posterior_regular += vocab.get(word).getRegularLCCP();
                    posterior_spam    += vocab.get(word).getSpamLCCP();
                }                
            }
        }
        in.close();       
        
        return (posterior_regular > posterior_spam ? MessageType.NORMAL : MessageType.SPAM);
    }
   
    public static void main(String[] args)
    throws IOException
    {
        // Location of the directory (the path) taken from the cmd line (first arg)
        File dir_location = new File( args[0] );
        
        // Check if the cmd line arg is a directory
        if ( !dir_location.isDirectory() )
        {
            System.out.println( "- Error: cmd line arg not a directory.\n" );
            Runtime.getRuntime().exit(0);
        }

        // Initialize the regular and spam lists
        listDirs(dir_location);

        /// Compute prior probabilities now that directory contents are loaded.
        double nregular      = listing_regular.length;
        double nspam         = listing_spam.length;
        double ntotal        = nregular + nspam;
        prior_regular = (nregular / ntotal);
        prior_spam    = (nspam / ntotal);

        // Read the e-mail messages
        readMessages(MessageType.NORMAL);
        readMessages(MessageType.SPAM);

        /// Set all class conditional probabilities.
        setCCPs(1);

        // Print out the hash table
        printVocab();

        /// Test regular or spam message.
        System.out.println(args[1] + " is a " + (classify(args[1]) == MessageType.NORMAL ? "regular" : "spam") + " message.");
        
        // Now all students must continue from here:
        //
        // 1) A priori class probabilities must be computed from the number of regular and spam messages
        // 2) The vocabulary must be clean: punctuation and digits must be removed, case insensitive
        // 3) Conditional probabilities must be computed for every word
        // 4) A priori probabilities must be computed for every word
        // 5) Zero probabilities must be replaced by a small estimated value
        // 6) Bayes rule must be applied on new messages, followed by argmax classification
        // 7) Errors must be computed on the test set (FAR = false accept rate (misses), FRR = false reject rate (false alarms))
        // 8) Improve the code and the performance (speed, accuracy)
        //
        // Use the same steps to create a class BigramBayespam which implements a classifier using a vocabulary consisting of bigrams
    }
}
