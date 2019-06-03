package core.data_structures.postgreSQL;

import core.data_structures.IArray;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

import static core.utils.HashingHelper.getRandomName;

public class PostgreArray implements IArray<Integer> {

    String _traceName;
    PostgreInterface _interface;
    long size;

    public PostgreArray(String name){
        this._traceName = name;
        _interface = PostgreInterface.getInstance();

        size = _interface.executeScalarQuery(String.format("SELECT COUNT(*) as count FROM TRACE WHERE name = '%s'", name), 0l);
    }

    @Override
    public void add(Integer value) {

        _interface.executeQuery(String.format("INSERT INTO TRACE(name, index, value) VALUES('%s',%s, %s)", _traceName, size++, value));
    }

    @Override
    public void write(int position, Integer value) {

    }

    @Override
    public Integer read(int position) {
        return _interface.executeScalarQuery(String.format("SELECT value FROM TRACE WHERE name='%s' AND index=%s", _traceName, position));
    }

    @Override
    public void close() {

    }

    @Override
    public void dispose() {
        _interface.executeQuery(String.format("DELETE FROM TRACE WHERE name='%s'", _traceName));
    }

    @Override
    public IArray<Integer> subArray(int index, int size) {

        String name = getRandomName();

        _interface.executeQuery(String.format("INSERT INTO TRACE(name, value, index, map)" +
                " (SELECT '%s', value, index - %s, map FROM TRACE " +
                "WHERE name='%s' AND index >= %s AND index <= %s)", name, index, _traceName, index, index + size - 1));

        return new PostgreArray(name);
    }

    @Override
    public int size() {
        return (int)size;
    }



    @NotNull
    @Override
    public Iterator<Integer> iterator(){
        return new PostGresIterator(this);
    }


    public class PostGresIterator implements Iterator<Integer>{

        PostgreArray arr;
        int index = 0;

        public PostGresIterator(PostgreArray arr){
            this.arr = arr;

        }

        @Override
        public boolean hasNext() {
            return index < arr.size - 1;
        }

        @Override
        public Integer next() {
            return arr.read(this.index++);
        }
    }
}
