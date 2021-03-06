/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.edisonproject.training.wsd;

import eu.edisonproject.utility.commons.Term;
import eu.edisonproject.utility.file.CSVFileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.parser.ParseException;

/**
 *
 * @author S. Koulouzis
 */
public class MetaDisambiguator extends DisambiguatorImpl {

    List<DisambiguatorImpl> disambiguators = new ArrayList<>();
//    private boolean sequentially;

    @Override
    public void configure(Properties properties) {
        super.configure(properties);

        String semantizatiorClassNames = properties.getProperty("disambiguators", "eu.edisonproject.training.wsd.BabelNet,eu.edisonproject.training.wsd.Wikipedia");

        String[] classes = semantizatiorClassNames.split(",");

        for (String className : classes) {
            try {
                Class c = Class.forName(className);
                Object obj = c.newInstance();
                DisambiguatorImpl disambiguator = (DisambiguatorImpl) obj;
                disambiguator.configure(properties);

                disambiguators.add(disambiguator);
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
                Logger.getLogger(MetaDisambiguator.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
//        sequentially = Boolean.valueOf(properties.getProperty("execute.sequentially", "false"));
    }

    @Override
    public Term getTerm(String term) throws IOException, ParseException {
        try {
            term = term.replaceAll(" ", "_");
//            if (sequentially) {
            return getTermSequentially(term);
//            } else {
//                return getTermConcurrently(term);
//            }
        } catch (ParseException | InterruptedException | ExecutionException ex) {
            if (ex instanceof IOException && ex.getMessage().contains("Your key is not valid or the daily requests limit has been reached")) {
                Logger.getLogger(MetaDisambiguator.class.getName()).log(Level.WARNING, null, ex);
            } else {
                Logger.getLogger(MetaDisambiguator.class.getName()).log(Level.SEVERE, null, ex);
            }

        }

        return null;
    }

    private Term getWinner(Set<Term> possibleTerms, double minimumSimilarity) {
//        long start = System.currentTimeMillis();
        double highScore = minimumSimilarity;
        CharSequence id = null;
        for (Term t : possibleTerms) {
//            System.err.println(t + " " + t.getConfidence());
            if (t.getConfidence() >= highScore) {
                highScore = t.getConfidence();
                id = t.getUid();
            }
        }
        if (id != null) {
            for (Term t : possibleTerms) {
                if (id.equals(t.getUid())) {
//                    Logger.getLogger(MetaDisambiguator.class.getName()).log(Level.INFO, "Elapsed: {0}.", new Object[]{System.currentTimeMillis() - start,});
                    return t;
                }
            }
        }
//        Logger.getLogger(MetaDisambiguator.class.getName()).log(Level.INFO, "Elapsed: {0}.", new Object[]{System.currentTimeMillis() - start,});
        return null;
    }

    private Term getTermSequentially(String term) throws IOException, ParseException, MalformedURLException, InterruptedException, ExecutionException {
        Set<Term> possibleTerms = new HashSet();
        for (Disambiguator s : disambiguators) {
            try {
                possibleTerms.addAll(s.getCandidates(term));
            } catch (IOException ex) {
                if (!ex.getMessage().contains("Your key is not valid")) {
                    throw ex;
                }
            }
        }
        String delimeter = ",";
        String wordSeperator = " ";
        Set<String> ngarms = CSVFileReader.getNGramsForTerm(term, getItemsFilePath(), delimeter, wordSeperator);
        if (getCandidateTermsFile() != null) {
            ngarms.addAll(CSVFileReader.getNGramsForTerm(term, getCandidateTermsFile(), delimeter, wordSeperator));
        }
        Term dis = disambiguate(term, possibleTerms, ngarms, getMinimumSimilarity());
//        Term dis = getWinner(possibleTerms, getMinimumSimilarity());
        if (dis == null) {
            Logger.getLogger(MetaDisambiguator.class.getName()).log(Level.INFO, "Couldn''''t figure out what ''{0}'' means", term);
        } else {
            Logger.getLogger(MetaDisambiguator.class.getName()).log(Level.INFO, "Term: {0}. Confidence: {1} URL: {3} Glosses: {2}  ", new Object[]{dis, dis.getConfidence(), dis.getGlosses(), dis.getUrl()});
        }
        return dis;
    }

//    private Term getTermConcurrently(String term) throws InterruptedException, ExecutionException {
//        Set<Term> possibleTerms = new HashSet();
//
//        ExecutorService pool = new ThreadPoolExecutor(disambiguators.size(), disambiguators.size(),
//                5000L, TimeUnit.MILLISECONDS,
//                new ArrayBlockingQueue<>(disambiguators.size(), true), new ThreadPoolExecutor.CallerRunsPolicy());
//
//        Set<Future<Term>> set = new HashSet<>();
//        for (int i = 0; i < disambiguators.size(); i++) {
//            DisambiguatorImpl s = disambiguators.get(i);
//            s.setTermToProcess(term);
//            Future<Term> future = pool.submit(s);
//            set.add(future);
//        }
//
//        for (Future<Term> future : set) {
//            while (!future.isDone()) {
////                Logger.getLogger(MetaDisambiguator.class.getName()).log(Level.INFO, "Task is not completed yet....");
//                Thread.currentThread().sleep(100);
//            }
//            Term t = future.get();
//            if (t != null) {
//                possibleTerms.add(t);
//            }
//        }
//        pool.shutdown();
//
//        Term dis = getWinner(possibleTerms, getMinimumSimilarity());
////        Term dis = SemanticUtils.disambiguate(term, possibleTerms, allTermsDictionaryPath, minimumSimilarity, true);
//        if (dis == null) {
//            Logger.getLogger(MetaDisambiguator.class.getName()).log(Level.INFO, "Couldn''''t figure out what ''{0}'' means", term);
//        } else {
//            Logger.getLogger(MetaDisambiguator.class.getName()).log(Level.INFO, "Term: {0}. Confidence: {1} URL: {3} Glosses: {2}  ", new Object[]{dis, dis.getConfidence(), dis.getGlosses(), dis.getUrl()});
//        }
//
//        return dis;
//    }
}
