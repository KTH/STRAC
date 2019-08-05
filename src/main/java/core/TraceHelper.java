package core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import core.data_structures.IArray;
import core.data_structures.memory.InMemoryArray;
import core.models.TraceMap;
import core.utils.HashingHelper;
import interpreter.dto.Alignment;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.*;
import java.util.logging.Level;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class TraceHelper {

    private Map<String, Integer> bag;
    private Map<Integer, String> inverseBag;

    private int sentencesCount;


    public TraceHelper(){
        bag = new HashMap<>();
        inverseBag = new HashMap<>();
    }

    public Map<String, Integer> getBag(){
        return this.bag;
    }

    public Map<Integer, String> getInverseBag(){
        return this.inverseBag;
    }

    public int getSentecesCount(){
        return this.sentencesCount;
    }

    public int getDifferentSentenceCount(){
        return this.bag.keySet().size();
    }

    public Integer updateBag(String sentence){

        this.sentencesCount++;
        if(!bag.containsKey(sentence)){
            bag.put(sentence, bag.keySet().size() + 1);
            inverseBag.put(bag.keySet().size(), sentence);

            return this.bag.keySet().size();
        }

        return bag.get(sentence);
    }

    public IArray<Integer> updateBag(Stream<String> sentences,
                                     Stream<String> patch,
                                     String fileName){


        int count = (int)patch.count();

        IArray<Integer> result = ServiceRegister.getProvider().allocateIntegerArray(
                null,
                count,
                        ServiceRegister.getProvider().selectMethod(4*count
                        ));

        long position = 0;

        for (Iterator<String> it = sentences.iterator(); it.hasNext(); ) {
            String sentence = it.next();
            this.sentencesCount++;
            if(!bag.containsKey(sentence)){
                bag.put(sentence, bag.keySet().size() + 1);
                inverseBag.put(bag.keySet().size(), sentence);

            }

            result.set(position++,bag.get(sentence));
        }

        return result;
    }

    public long countSentences(String separator, String[] remove,  InputStream stream, IHasNext hasNextIterator, INextProvider sentenceProvider){

        Scanner sc = sentenceProvider.setupScanner(new Scanner(stream, "UTF-8"), separator);

        long count = 0;

        while (hasNextIterator.hasNext(separator, sc)) {
            String line = sentenceProvider.getNext(separator, sc);

            for(String pattern: remove)
                line = line.replaceAll(pattern, "");

            line = line.trim();

            if(!line.equals(""))
                count++;
        }

        return count;
    }

    public TraceMap mapTraceFileByLine(String fileName, String separator, String[] remove, Alignment.Include include, IStreamProvider provider,
                                       boolean keepSentences, IHasNext hasNextIterator, INextProvider sentenceProvider) {

        LogProvider.LOGGER()
                .info("Processing " + fileName);


        long count = countSentences(separator, remove, provider.getStream(fileName), hasNextIterator, sentenceProvider);


        IArray<Integer> trace = ServiceRegister.getProvider().allocateIntegerArray(null, count,
                ServiceRegister.getProvider().selectMethod(count));


        Scanner sc = sentenceProvider.setupScanner(new Scanner(provider.getStream(fileName), "UTF-8"), separator);


        List<String> sentences = new ArrayList<>();
        long index = 0;

        while (hasNextIterator.hasNext(separator, sc)) {
            String line = sentenceProvider.getNext(separator, sc);


            for(String pattern: remove)
                line = line.replaceAll(pattern, "");

            line = line.trim();

            if(include != null) {

                Pattern p = Pattern.compile(include.pattern);

                Matcher m = p.matcher(line);

                MatchResult r = m.toMatchResult();
                while(m.find()){
                    line = m.group(include.group);
                    break;
                }
            }


            if(keepSentences && !line.equals(""))
                sentences.add(line);


            if(!line.equals(""))
                trace.set(index++, updateBag(line));

        }


        LogProvider.info("New trace added size: ", index);

        core.LogProvider.LOGGER()
                .info(String.format("Global bag info: total sentences %s different sentences %s", this.getSentecesCount(), this.getDifferentSentenceCount()));


        return new TraceMap(trace, fileName, sentences.toArray(new String[0]));

    }

    public List<TraceMap> mapTraceSetByFileLine(List<String> files, String separator,  IStreamProvider provider,
                                                boolean keepSentences, boolean complement) {

        return mapTraceSetByFileLine(files, separator, new String[0], null, provider, keepSentences, complement);

    }

    public List<TraceMap> mapTraceSetByFileLine(List<String> files, String separator, String[] remove, Alignment.Include include, IStreamProvider provider,
                                                boolean keepSentences, boolean complement){


        IHasNext separatorProvider = (pattern, sc) -> sc.hasNext();

        INextProvider sentenceProvider = new INextProvider() {
            @Override
            public String getNext(String pattern, Scanner sc) {
                return sc.next();
            }

            @Override
            public Scanner setupScanner(Scanner sc, String pattern) {
                return sc.useDelimiter(Pattern.compile(separator));
            }
        };

        return files.stream()
                .map(t -> this.mapTraceFileByLine(t, separator, remove, include, provider, keepSentences, separatorProvider, sentenceProvider))
                .collect(Collectors.toList());

    }

    public interface IHasNext{
        boolean hasNext(String pattern, Scanner sc);
    }

    public interface INextProvider{

        String getNext(String pattern, Scanner sc);

        Scanner setupScanner(Scanner sc, String pattern);
    }

    public interface IStreamProvider{

        InputStream getStream(String filename);

        boolean validate(String filename);
    }


    public void save(String filePath) throws IOException {

        FileWriter writer = new FileWriter(filePath);

        writer.write(
                new GsonBuilder()
                        .setPrettyPrinting()
                        .create().toJson(this));

        writer.close();

        LogProvider.info("Saving sentences map", filePath);

    }

    public static TraceHelper load(String filePath) throws FileNotFoundException {

        LogProvider.info("Loading sentences map", filePath);

        return new Gson().fromJson(new FileReader(filePath), TraceHelper.class);
    }

}
