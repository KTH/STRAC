package core.data_structures.buffered;

import core.ServiceRegister;
import core.data_structures.IArray;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Iterator;

import static core.utils.HashingHelper.getRandomName;

public class BufferedCollection<T> implements IArray<T> {

    RandomAccessFile aFile;
    MappedByteBuffer buffer;
    String filename;
    FileChannel inChannel;
    ITypeAdaptor<T> adaptor;
    long _size;

    public BufferedCollection(String fileName, int size,  ITypeAdaptor<T> adaptor) {
        aFile = null;
        this.filename = fileName;
        this.adaptor = adaptor;
        _size = 0;

        try {
            aFile = new RandomAccessFile
                    (fileName, "rw");

        } catch (FileNotFoundException e) {
            throw new RuntimeException(e.getMessage());
        }

        inChannel = aFile.getChannel();
        buffer = null;

        try {
            if(inChannel.size() > 0)
                _size = inChannel.size()/adaptor.size();

            long realSize = size > 0? size*adaptor.size() : inChannel.size();

            buffer = inChannel.map(FileChannel.MapMode.READ_WRITE, 0, realSize);


        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }


        buffer.load();


    }

    public BufferedCollection(String fileName, ITypeAdaptor<T> adaptor){
        this(fileName, 0, adaptor);
    }



    @Override
    public void add(T value) {

       // throw new RuntimeException("Method not allowed");
        byte[] data = adaptor.toBytes(value);

        _size++;
        buffer.put(data);
    }

    @Override
    public void add(int position, T value) {

        position = position*adaptor.size();

        int previous = buffer.position();
        buffer.position(position);

        byte[] bytes = adaptor.toBytes(value);
        buffer.put(bytes);

        buffer.position(previous);

    }

    @Override
    public T read(int position) {

        position = position*adaptor.size();

        int previous = buffer.position();

        byte[] chunk = new byte[adaptor.size()];
        buffer.position(position);
        buffer.get(chunk);
        T value = adaptor.fromBytes(chunk);

        buffer.position(previous);

        return value;
    }

    @Override
    public void close() {
        buffer.clear(); // do something with the data and clear/compact it.
        try {
            inChannel.close();
            aFile.close();
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public void dispose() {
        new File(this.filename).delete();
    }

    @Override
    public IArray<T> subArray(int index, int size) {

        BufferedCollection<T> subArray = new BufferedCollection<>(getRandomName(), size*adaptor.size(), adaptor);


        return null;
    }

    @Override
    public int size() {
        return (int)_size;
    }

    @NotNull
    @Override
    public Iterator<T> iterator() {
        return null;
    }

    public interface ITypeAdaptor<T>{

        T fromBytes(byte[] chunk);

        byte[] toBytes(T i);

        int size();
    }
}
