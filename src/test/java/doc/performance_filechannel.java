package doc;

import org.mapdb20.DB;
import org.mapdb20.DBMaker;

import java.io.File;
import java.io.IOException;


public class performance_filechannel {

    public static void main(String[] args) throws IOException {
        File file = File.createTempFile("mapdb","mapdb");
        //a
        DB db = DBMaker
            .fileDB(file)
            .fileChannelEnable()
            .make();
        //z
    }
}
